package dev.naturalis.client;

import dev.naturalis.Naturalis;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class HumanityHudClientEvents {

    private HumanityHudClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderHotbarLayer(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !HumanityClientCache.isActive()) {
            return;
        }

        int humanity = HumanityClientCache.getHumanity();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // Intentionally subtle: tiny, low-contrast, tucked near the hotbar edge.
        int barWidth = 44;
        int barHeight = 3;
        int x = width / 2 + 92;
        int y = height - 30;

        int fill = Math.max(0, Math.min(barWidth, humanity * barWidth / 100));
        int color = humanity <= 20 ? 0xC0C85B5B : (humanity <= 40 ? 0xC0C89C5B : 0xA086AFA0);

        event.getGuiGraphics().fill(x, y, x + barWidth, y + barHeight, 0x4A000000);
        if (fill > 0) {
            event.getGuiGraphics().fill(x, y, x + fill, y + barHeight, color);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HumanityClientCache.reset();
    }
}
