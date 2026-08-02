package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class HumanityHudClientEvents {

    private HumanityHudClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderHotbarLayer(RenderGuiLayerEvent.Post event) {
        if (!"hotbar".equals(event.getName().getPath())) {
            return;
        }
        HumanityHudLogic.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HumanityHudLogic.onLogout();
    }
}
