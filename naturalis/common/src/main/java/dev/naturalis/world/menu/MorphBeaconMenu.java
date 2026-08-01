package dev.naturalis.world.menu;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.world.MorphBeaconBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public class MorphBeaconMenu extends AbstractContainerMenu {

    private final MorphBeaconBlockEntity blockEntity;
    private final ContainerData data;

    public MorphBeaconMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, (MorphBeaconBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public MorphBeaconMenu(int containerId, Inventory playerInventory, MorphBeaconBlockEntity blockEntity) {
        super(CompatAccess.naturalisMenuType("morph_beacon"), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getData();
        addDataSlots(this.data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < MorphBeaconBlockEntity.TargetMode.values().length) {
            blockEntity.setTargetMode(id);
            broadcastChanges();
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

    public String currentMorphId() {
        return blockEntity.getTargetMorphId();
    }

    /** @deprecated use {@link #currentMorphId()} */
    @Deprecated
    public String initialMorphId() {
        return currentMorphId();
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
