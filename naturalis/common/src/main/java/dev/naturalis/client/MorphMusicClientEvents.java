package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.client.perception.MorphMusicPerceptionClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * @deprecated Logic lives in {@link MorphMusicPerceptionClient}; this class only forwards ticks.
 */
@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class MorphMusicClientEvents {

    private MorphMusicClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MorphMusicPerceptionClient.clientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MorphMusicPerceptionClient.clientTick(Minecraft.getInstance());
    }
}
