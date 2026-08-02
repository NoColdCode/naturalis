package dev.naturalis.fabric;

import dev.naturalis.inventory.InventoryRestrictionLogic;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/** Fabric server tick bridge for {@link InventoryRestrictionLogic}. */
public final class FabricInventoryHooks {

    private FabricInventoryHooks() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                InventoryRestrictionLogic.tick(player);
            }
        });
    }
}
