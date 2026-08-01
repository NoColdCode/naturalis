package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphHearingClient;
import dev.naturalis.client.perception.MorphHearingHudOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphHearingClientEvents {

    private MorphHearingClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MorphHearingClient.clientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        MorphHearingHudOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
