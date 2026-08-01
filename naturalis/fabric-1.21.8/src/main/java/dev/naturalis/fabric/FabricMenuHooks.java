package dev.naturalis.fabric;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Opens block menus with synced {@link BlockPos} using Fabric extended screen handlers.
 */
public final class FabricMenuHooks {

    private FabricMenuHooks() {
    }

    public static void openMenuAt(ServerPlayer player, MenuProvider menuProvider, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public Component getDisplayName() {
                return menuProvider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player pl) {
                return menuProvider.createMenu(syncId, inventory, pl);
            }

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer openingPlayer) {
                return pos;
            }
        });
    }
}
