package dev.naturalis.world;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.world.menu.EchoForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class EchoForgeBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT_COUNT = 5;
    public static final int OUTPUT_SLOT      = 5;
    public static final int TOTAL_SLOTS      = 6;
    public static final int COOK_TIME_TOTAL  = 800;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            return stack.is(NaturalisItems.FILLED_ECHO_VIAL.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int cookTime;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> COOK_TIME_TOTAL;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { if (index == 0) cookTime = value; }
        @Override public int getCount() { return 2; }
    };

    public EchoForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(NaturalisBlockEntities.ECHO_FORGE.get(), pos, blockState);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getData() { return data; }

    public static void tickServer(Level level, BlockPos pos, BlockState state, EchoForgeBlockEntity be) {
        if (level.isClientSide()) return;

        if (be.canInfuse()) {
            be.cookTime++;
            if (be.cookTime >= COOK_TIME_TOTAL) {
                be.finishInfusion();
                be.cookTime = 0;
            }
        } else if (be.cookTime != 0) {
            be.cookTime = 0;
        }
        be.setChanged();
    }

    private boolean canInfuse() {
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) return false;
        String mobId = null;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.is(NaturalisItems.FILLED_ECHO_VIAL.get())) return false;
            String stackMobId = MorphDataUtil.getMobId(stack);
            if (stackMobId == null || stackMobId.isEmpty()) return false;
            if (mobId == null) mobId = stackMobId;
            else if (!mobId.equals(stackMobId)) return false;
        }
        return mobId != null;
    }

    private void finishInfusion() {
        String mobId = MorphDataUtil.getMobId(itemHandler.getStackInSlot(0));
        if (mobId == null || mobId.isEmpty()) return;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) itemHandler.extractItem(i, 1, false);
        ItemStack orb = new ItemStack(NaturalisItems.MORPH_ORB.get());
        MorphDataUtil.setMobId(orb, mobId);
        itemHandler.setStackInSlot(OUTPUT_SLOT, orb);
    }

    // ── 1.20.1 persistence ────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", CompatAccess.serializeItemHandler(itemHandler, null));
        tag.putInt("CookTime", cookTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompatAccess.deserializeItemHandler(itemHandler, null,
            CompatAccess.getCompound(tag, "Inventory"));
        cookTime = tag.getInt("CookTime");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.naturalis.echo_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EchoForgeMenu(containerId, inventory, this);
    }
}
