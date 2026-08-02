package dev.naturalis.fabric;

import dev.naturalis.experience.NaturalisExperienceRuntime;
import dev.naturalis.survivalas.SurvivalAsCircadian;
import dev.naturalis.survivalas.SurvivalAsRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/** Fabric wiring for Survival as… and experience-mode sync (NeoForge: SurvivalAsEvents / NaturalisExperienceEvents). */
public final class FabricSurvivalAsHooks {

    private FabricSurvivalAsHooks() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SurvivalAsRuntime.onServerStarting(server);
            NaturalisExperienceRuntime.onServerStarting(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            SurvivalAsRuntime.onPlayerJoin(player);
            NaturalisExperienceRuntime.syncPlayer(player);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 40 == 0) {
                    SurvivalAsRuntime.tickEnforce(player);
                }
                SurvivalAsCircadian.tick(player);
            }
        });
    }
}
