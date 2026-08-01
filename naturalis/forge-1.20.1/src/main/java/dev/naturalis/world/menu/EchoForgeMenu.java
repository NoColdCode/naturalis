package dev.naturalis.world.menu;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.world.EchoForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

@SuppressWarnings("unchecked")
public class EchoForgeMenu extends AbstractContainerMenu {

    private final EchoForgeBlockEntity blockEntity;
    private final ContainerData data;

    public EchoForgeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
            (EchoForgeBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public EchoForgeMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
            (EchoForgeBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    public EchoForgeMenu(int containerId, Inventory playerInventory, EchoForgeBlockEntity blockEntity) {
        super((MenuType<EchoForgeMenu>) CompatAccess.naturalisMenuType("echo_forge"), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getData();

        addForgeSlots(blockEntity);
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    private void addForgeSlots(EchoForgeBlockEntity be) {
        var handler = be.getItemHandler();
        Item filled = CompatAccess.naturalisItem("filled_echo_vial");

        addSlot(new SlotItemHandler(handler, 0, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(filled);
            }
        });
        addSlot(new SlotItemHandler(handler, 1, 104, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(filled);
            }
        });
        addSlot(new SlotItemHandler(handler, 2, 92, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(filled);
            }
        });
        addSlot(new SlotItemHandler(handler, 3, 68, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(filled);
            }
        });
        addSlot(new SlotItemHandler(handler, 4, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(filled);
            }
        });

        addSlot(new SlotItemHandler(handler, EchoForgeBlockEntity.OUTPUT_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startY = 84;

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
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot source = slots.get(index);

        Item filled = CompatAccess.naturalisItem("filled_echo_vial");

        if (source.hasItem()) {
            ItemStack sourceStack = source.getItem();
            result = sourceStack.copy();

            if (index < EchoForgeBlockEntity.TOTAL_SLOTS) {
                if (!moveItemStackTo(sourceStack, EchoForgeBlockEntity.TOTAL_SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;
                if (sourceStack.is(filled)) {
                    moved = moveItemStackTo(sourceStack, 0, EchoForgeBlockEntity.INPUT_SLOT_COUNT, false);
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
        return blockEntity.getLevel() != null && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
    }

    public int getProgress() {
        int cookTime = data.get(0);
        int cookTotal = data.get(1);
        if (cookTime <= 0 || cookTotal <= 0) {
            return 0;
        }
        return Math.min(24, cookTime * 24 / cookTotal);
    }
}
