package dev.naturalis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Loader-neutral humanity hotbar HUD render. */
public final class HumanityHudLogic {

    private HumanityHudLogic() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !HumanityClientCache.isActive()) {
            return;
        }

        int humanity = HumanityClientCache.getHumanity();
        float drift = dev.naturalis.client.perception.MorphIdentityDriftClient.embodimentBlend();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int barWidth = 44;
        int barHeight = 3;
        int x = width / 2 + 92;
        int y = height - 30;

        int fill = Math.max(0, Math.min(barWidth, humanity * barWidth / 100));
        int baseColor = humanity <= 20 ? 0xC0C85B5B : (humanity <= 40 ? 0xC0C89C5B : 0xA086AFA0);
        int animalTint = (int) (drift * 90) << 16 | (int) (drift * 40) << 8;
        int color = blendArgb(baseColor, 0xA0705050 | animalTint, drift * 0.45F);

        graphics.fill(x, y, x + barWidth, y + barHeight, 0x4A000000);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + barHeight, color);
        }
    }

    public static void onLogout() {
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
