package dev.naturalis.world;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.world.menu.EchoForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EchoForgeBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int INPUT_SLOT_COUNT = 5;
    public static final int OUTPUT_SLOT = 5;
    public static final int TOTAL_SLOTS = 6;
    public static final int COOK_TIME_TOTAL = 800;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int cookTime;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> COOK_TIME_TOTAL;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                cookTime = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public EchoForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(CompatAccess.naturalisBlockEntityType("echo_forge"), pos, blockState);
    }

    public ContainerData getData() {
        return data;
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, EchoForgeBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        if (blockEntity.canInfuse()) {
            blockEntity.cookTime++;
            if (blockEntity.cookTime >= COOK_TIME_TOTAL) {
                blockEntity.finishInfusion();
                blockEntity.cookTime = 0;
            }
        } else if (blockEntity.cookTime != 0) {
            blockEntity.cookTime = 0;
        }

        blockEntity.setChanged();
    }

    private boolean canInfuse() {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            return false;
        }

        Item filled = CompatAccess.naturalisItem("filled_echo_vial");

        String mobId = null;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            if (!stack.is(filled)) {
                return false;
            }

            String stackMobId = MorphDataUtil.getMobId(stack);
            if (stackMobId == null || stackMobId.isEmpty()) {
                return false;
            }

            if (mobId == null) {
                mobId = stackMobId;
            } else if (!mobId.equals(stackMobId)) {
                return false;
            }
        }

        return mobId != null;
    }

    private void finishInfusion() {
        String mobId = MorphDataUtil.getMobId(items.get(0));
        if (mobId == null || mobId.isEmpty()) {
            return;
        }

        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            stack.shrink(1);
            if (stack.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
            }
        }

        ItemStack orb = new ItemStack(CompatAccess.naturalisItem("morph_orb"));
        MorphDataUtil.setMobId(orb, mobId);
        MorphDataUtil.copyShapeData(items.get(0), orb);
        items.set(OUTPUT_SLOT, orb);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", CompatAccess.serializeItemStacks(items, registries));
        tag.putInt("CookTime", cookTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompatAccess.loadItemStacks(items, registries, CompatAccess.getCompound(tag, "Inventory"));
        cookTime = CompatAccess.getInt(tag, "CookTime");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.naturalis.echo_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EchoForgeMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
