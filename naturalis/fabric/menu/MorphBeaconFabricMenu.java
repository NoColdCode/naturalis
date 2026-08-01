package dev.naturalis.fabric.menu;

import dev.naturalis.fabric.FabricNaturalisMenus;
import dev.naturalis.fabric.blockentity.MorphBeaconFabricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public class MorphBeaconFabricMenu extends AbstractContainerMenu {

    private final MorphBeaconFabricBlockEntity blockEntity;
    private final ContainerData data;
    private final String initialMorphId;

    public MorphBeaconFabricMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, (MorphBeaconFabricBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public MorphBeaconFabricMenu(int containerId, Inventory playerInventory, MorphBeaconFabricBlockEntity blockEntity) {
        super(FabricNaturalisMenus.MORPH_BEACON, containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getData();
        this.initialMorphId = blockEntity.getTargetMorphId();
        addDataSlots(this.data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < MorphBeaconFabricBlockEntity.TargetMode.values().length) {
            blockEntity.setTargetMode(id);
            return true;
        }
        return false;
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
