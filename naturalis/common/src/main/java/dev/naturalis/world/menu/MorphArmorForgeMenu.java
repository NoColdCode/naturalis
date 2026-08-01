package dev.naturalis.world.menu;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.item.MorphArmorTier;
import dev.naturalis.world.MorphArmorForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unchecked")
public class MorphArmorForgeMenu extends AbstractContainerMenu {

    private final MorphArmorForgeBlockEntity blockEntity;
    private final ContainerData data;

    public MorphArmorForgeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
            (MorphArmorForgeBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public MorphArmorForgeMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
            (MorphArmorForgeBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    public MorphArmorForgeMenu(int containerId, Inventory playerInventory, MorphArmorForgeBlockEntity blockEntity) {
        super((MenuType<MorphArmorForgeMenu>) CompatAccess.naturalisMenuType("morph_armor_forge"), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getData();

        addForgeSlots(blockEntity);
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    private void addForgeSlots(MorphArmorForgeBlockEntity inventory) {
        addSlot(new Slot(inventory, MorphArmorForgeBlockEntity.SLOT_ORB, 35, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(CompatAccess.naturalisItem("morph_orb"));
            }
        });

        addSlot(new Slot(inventory, MorphArmorForgeBlockEntity.SLOT_MATERIAL, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return inventory.canPlaceItem(MorphArmorForgeBlockEntity.SLOT_MATERIAL, stack);
            }
        });

        addSlot(new Slot(inventory, MorphArmorForgeBlockEntity.SLOT_OUTPUT, 125, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startY = 114;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, startY + 58));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int tierCount = MorphArmorTier.values().length;
        if (id >= 0 && id < tierCount) {
            blockEntity.setSelectedTier(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot source = slots.get(index);

        if (source.hasItem()) {
            ItemStack sourceStack = source.getItem();
            result = sourceStack.copy();

            if (index < MorphArmorForgeBlockEntity.TOTAL_SLOTS) {
                if (!moveItemStackTo(sourceStack, MorphArmorForgeBlockEntity.TOTAL_SLOTS,
                    slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = moveItemStackTo(sourceStack, MorphArmorForgeBlockEntity.SLOT_ORB,
                    MorphArmorForgeBlockEntity.SLOT_ORB + 1, false);
                if (!moved) {
                    moved = moveItemStackTo(sourceStack, MorphArmorForgeBlockEntity.SLOT_MATERIAL,
                        MorphArmorForgeBlockEntity.SLOT_MATERIAL + 1, false);
                }
                if (!moved) {
                    return ItemStack.EMPTY;
                }
            }

            if (sourceStack.isEmpty()) {
                source.set(ItemStack.EMPTY);
            } else {
                source.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
            && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
    }

    public int getProgress() {
        int cookTime = data.get(0);
        int cookTotal = data.get(1);
        if (cookTime <= 0 || cookTotal <= 0) {
            return 0;
        }
        return Math.min(28, cookTime * 28 / cookTotal);
    }

    public int getSelectedTierIndex() {
        return data.get(2);
    }

    public int getRequiredCost() {
        return data.get(3);
    }
}
