package dev.naturalis.world;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphArmorTier;
import dev.naturalis.metabolism.MetabolismManager;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.world.menu.MorphArmorForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

public class MorphArmorForgeBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int SLOT_ORB = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int TOTAL_SLOTS = 3;

    public static final int COOK_TIME_TOTAL = 600;

    private int cookTime;
    private int selectedTier = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

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
        super(CompatAccess.naturalisBlockEntityType("morph_armor_forge"), pos, blockState);
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
        if (!items.get(SLOT_OUTPUT).isEmpty()) {
            return false;
        }

        String mobId = getMobIdFromOrb();
        if (mobId == null || mobId.isEmpty()) {
            return false;
        }

        MorphArmorTier tier = MorphArmorTier.fromIndex(selectedTier);
        ItemStack material = items.get(SLOT_MATERIAL);
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

        ItemStack orb = items.get(SLOT_ORB);
        orb.shrink(1);
        if (orb.isEmpty()) {
            items.set(SLOT_ORB, ItemStack.EMPTY);
        }

        ItemStack mat = items.get(SLOT_MATERIAL);
        mat.shrink(cost);
        if (mat.isEmpty()) {
            items.set(SLOT_MATERIAL, ItemStack.EMPTY);
        }

        ItemStack armor = new ItemStack(CompatAccess.naturalisItem("morph_armor"));
        MorphDataUtil.setMobId(armor, mobId);
        MorphArmorItem.setTier(armor, tier);
        armor.set(DataComponents.ATTRIBUTE_MODIFIERS, MorphArmorItem.buildModifiers(tier));
        items.set(SLOT_OUTPUT, armor);
    }

    private String getMobIdFromOrb() {
        ItemStack orb = items.get(SLOT_ORB);
        Item morphOrb = CompatAccess.naturalisItem("morph_orb");
        if (orb.isEmpty() || !orb.is(morphOrb)) {
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

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_ORB -> stack.is(CompatAccess.naturalisItem("morph_orb"));
            case SLOT_MATERIAL -> isMaterialForAnyTier(stack);
            case SLOT_OUTPUT -> false;
            default -> false;
        };
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", CompatAccess.serializeItemStacks(items, registries));
        tag.putInt("CookTime", cookTime);
        tag.putInt("SelectedTier", selectedTier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompatAccess.loadItemStacks(items, registries, CompatAccess.getCompound(tag, "Inventory"));
        cookTime = CompatAccess.getInt(tag, "CookTime");
        selectedTier = CompatAccess.getInt(tag, "SelectedTier");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.naturalis.morph_armor_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MorphArmorForgeMenu(containerId, inventory, this);
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
