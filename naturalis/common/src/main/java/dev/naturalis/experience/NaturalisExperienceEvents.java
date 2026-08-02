package dev.naturalis.experience;

import dev.naturalis.NaturalisMod;
import dev.naturalis.network.SetExperienceModePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class NaturalisExperienceEvents {

    private NaturalisExperienceEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        NaturalisExperienceRuntime.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        NaturalisExperienceRuntime.syncPlayer(player);
    }

    public static void syncPlayer(ServerPlayer player) {
        NaturalisExperienceRuntime.syncPlayer(player);
    }

    public static void requestChoiceScreen(ServerPlayer player) {
        NaturalisExperienceRuntime.requestChoiceScreen(player);
    }

    public static void syncAll(net.minecraft.server.MinecraftServer server) {
        NaturalisExperienceRuntime.syncAll(server);
    }

    public static boolean applyChoice(ServerPlayer player, NaturalisExperienceMode mode) {
        return NaturalisExperienceRuntime.applyChoice(player, mode);
    }

    public static void handleSetExperiencePayload(SetExperienceModePayload payload, ServerPlayer player) {
        NaturalisExperienceRuntime.handleSetExperiencePayload(payload, player);
    }
}
