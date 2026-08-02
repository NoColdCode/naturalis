package dev.naturalis.inventory;

import dev.naturalis.NaturalisMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class InventoryRestrictionEvents {

    private InventoryRestrictionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InventoryRestrictionLogic.tick(player);
    }
}
