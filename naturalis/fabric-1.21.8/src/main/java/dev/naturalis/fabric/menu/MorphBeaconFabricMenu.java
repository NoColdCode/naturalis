package dev.naturalis.fabric.menu;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.fabric.blockentity.MorphBeaconFabricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unchecked")
public class MorphBeaconFabricMenu extends AbstractContainerMenu {

    private final MorphBeaconFabricBlockEntity blockEntity;
    private final ContainerData data;
    private final String initialMorphId;

    public MorphBeaconFabricMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(3), "");
    }

    public MorphBeaconFabricMenu(int containerId, Inventory playerInventory, MorphBeaconFabricBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.getData(), blockEntity.getTargetMorphId());
    }

    public MorphBeaconFabricMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
            (MorphBeaconFabricBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    private MorphBeaconFabricMenu(int containerId, Inventory playerInventory, MorphBeaconFabricBlockEntity blockEntity, ContainerData data, String initialMorphId) {
        super((MenuType<MorphBeaconFabricMenu>) CompatAccess.naturalisMenuType("morph_beacon"), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.initialMorphId = initialMorphId;
        addDataSlots(this.data);
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new net.minecraft.world.inventory.Slot(playerInventory, row * 9 + col + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity != null && id >= 0 && id < MorphBeaconFabricBlockEntity.TargetMode.values().length) {
            blockEntity.setTargetMode(id);
            return true;
        }
        return false;
    }

    public int pyramidLevel() {
        return Math.max(0, data.get(0));
    }

    public int range() {
        return Math.max(0, data.get(1));
    }

    public int targetMode() {
        return Math.max(0, data.get(2));
    }

    public String initialMorphId() {
        return initialMorphId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
