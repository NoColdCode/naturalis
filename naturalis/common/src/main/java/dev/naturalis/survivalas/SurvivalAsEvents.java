package dev.naturalis.survivalas;

import dev.naturalis.NaturalisMod;
import dev.naturalis.effect.MorphEffectEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class SurvivalAsEvents {

    private SurvivalAsEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SurvivalAsRuntime.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SurvivalAsRuntime.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            SurvivalAsRuntime.tickEnforce(player);
        }
        SurvivalAsCircadian.tick(player);
    }
}
