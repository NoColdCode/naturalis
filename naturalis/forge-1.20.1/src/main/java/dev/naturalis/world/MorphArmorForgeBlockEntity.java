package dev.naturalis.world;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphArmorTier;
import dev.naturalis.metabolism.MetabolismManager;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.world.menu.MorphArmorForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

public class MorphArmorForgeBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_ORB = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int TOTAL_SLOTS = 3;

    public static final int COOK_TIME_TOTAL = 600;

    private int cookTime;
    private int selectedTier = 2;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_ORB -> stack.is(NaturalisItems.MORPH_ORB.get());
                case SLOT_MATERIAL -> isMaterialForAnyTier(stack);
                case SLOT_OUTPUT -> false;
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> COOK_TIME_TOTAL;
                case 2 -> selectedTier;
                case 3 -> getRequiredCost();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 2 -> selectedTier = Math.max(0, Math.min(MorphArmorTier.values().length - 1, value));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public MorphArmorForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(NaturalisBlockEntities.MORPH_ARMOR_FORGE.get(), pos, blockState);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public void setSelectedTier(int index) {
        selectedTier = Math.max(0, Math.min(MorphArmorTier.values().length - 1, index));
        setChanged();
    }

    public int getRequiredCost() {
        String mobId = getMobIdFromOrb();
        if (mobId == null) {
            return 0;
        }
        ResourceLocation id = ResourceLocation.tryParse(mobId);
        if (id == null) {
            return 0;
        }
        double mass = MetabolismManager.getMass(id);
        return MorphArmorTier.fromIndex(selectedTier).computeCost(mass);
    }

    private boolean canCraft() {
        if (!itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return false;
        }

        String mobId = getMobIdFromOrb();
        if (mobId == null || mobId.isEmpty()) {
            return false;
        }

        MorphArmorTier tier = MorphArmorTier.fromIndex(selectedTier);
        ItemStack material = itemHandler.getStackInSlot(SLOT_MATERIAL);
        if (material.isEmpty() || !material.is(tier.material)) {
            return false;
        }

        int required = getRequiredCost();
        return material.getCount() >= required;
    }

    private void finishCraft() {
        String mobId = getMobIdFromOrb();
        if (mobId == null || mobId.isEmpty()) {
            return;
        }

        MorphArmorTier tier = MorphArmorTier.fromIndex(selectedTier);
        int cost = getRequiredCost();

        itemHandler.extractItem(SLOT_ORB, 1, false);

        ItemStack mat = itemHandler.getStackInSlot(SLOT_MATERIAL);
        mat.shrink(cost);
        if (mat.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_MATERIAL, ItemStack.EMPTY);
        }

        ItemStack armor = new ItemStack(NaturalisItems.MORPH_ARMOR.get());
        MorphDataUtil.setMobId(armor, mobId);
        MorphArmorItem.setTier(armor, tier);
        itemHandler.setStackInSlot(SLOT_OUTPUT, armor);
    }

    private String getMobIdFromOrb() {
        ItemStack orb = itemHandler.getStackInSlot(SLOT_ORB);
        if (orb.isEmpty() || !orb.is(NaturalisItems.MORPH_ORB.get())) {
            return null;
        }
        return MorphDataUtil.getMobId(orb);
    }

    private static boolean isMaterialForAnyTier(ItemStack stack) {
        for (MorphArmorTier t : MorphArmorTier.values()) {
            if (stack.is(t.material)) {
                return true;
            }
        }
        return false;
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, MorphArmorForgeBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }

        if (be.canCraft()) {
            be.cookTime++;
            if (be.cookTime >= COOK_TIME_TOTAL) {
                be.finishCraft();
                be.cookTime = 0;
            }
        } else if (be.cookTime != 0) {
            be.cookTime = 0;
        }

        be.setChanged();
    }

    // ── 1.20.1 persistence ────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", CompatAccess.serializeItemHandler(itemHandler, null));
        tag.putInt("CookTime", cookTime);
        tag.putInt("SelectedTier", selectedTier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompatAccess.deserializeItemHandler(itemHandler, null,
            CompatAccess.getCompound(tag, "Inventory"));
        cookTime = tag.getInt("CookTime");
        selectedTier = tag.getInt("SelectedTier");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.naturalis.morph_armor_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MorphArmorForgeMenu(containerId, inventory, this);
    }
}
