package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.minecraft.client.Minecraft;
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

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !HumanityClientCache.isActive()) {
            return;
        }

        int humanity = HumanityClientCache.getHumanity();
        float drift = dev.naturalis.client.perception.MorphIdentityDriftClient.embodimentBlend();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // Intentionally subtle: tiny, low-contrast, tucked near the hotbar edge.
        int barWidth = 44;
        int barHeight = 3;
        int x = width / 2 + 92;
        int y = height - 30;

        int fill = Math.max(0, Math.min(barWidth, humanity * barWidth / 100));
        int baseColor = humanity <= 20 ? 0xC0C85B5B : (humanity <= 40 ? 0xC0C89C5B : 0xA086AFA0);
        int animalTint = (int) (drift * 90) << 16 | (int) (drift * 40) << 8;
        int color = blendArgb(baseColor, 0xA0705050 | animalTint, drift * 0.45F);

        event.getGuiGraphics().fill(x, y, x + barWidth, y + barHeight, 0x4A000000);
        if (fill > 0) {
            event.getGuiGraphics().fill(x, y, x + fill, y + barHeight, color);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HumanityClientCache.reset();
        dev.naturalis.client.perception.MorphIdentityDriftClient.reset();
    }

    private static int blendArgb(int base, int overlay, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int ba = (base >> 24) & 0xFF;
        int br = (base >> 16) & 0xFF;
        int bg = (base >> 8) & 0xFF;
        int bb = base & 0xFF;
        int oa = (overlay >> 24) & 0xFF;
        int or = (overlay >> 16) & 0xFF;
        int og = (overlay >> 8) & 0xFF;
        int ob = overlay & 0xFF;
        int a = (int) (ba + (oa - ba) * t);
        int r = (int) (br + (or - br) * t);
        int g = (int) (bg + (og - bg) * t);
        int b = (int) (bb + (ob - bb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
