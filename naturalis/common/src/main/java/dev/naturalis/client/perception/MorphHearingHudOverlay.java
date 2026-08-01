package dev.naturalis.client.perception;

import dev.naturalis.network.ScentHintPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Directional listen vignette (no text): colored arc on screen edge toward the sound.
 */
public final class MorphHearingHudOverlay {

    private MorphHearingHudOverlay() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!MorphListenClientState.isFocusActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float strength = MorphListenClientState.focusStrength();
        float bearing = MorphListenClientState.smoothedBearing();
        int color = tintForCategory(MorphListenClientState.category(), strength);

        float cx = width * 0.5F;
        float cy = height * 0.5F;
        float radius = Math.max(width, height) * 0.72F;
        float bearingRad = (float) Math.toRadians(bearing);
        float arcX = cx + (float) Math.sin(bearingRad) * radius;
        float arcY = cy - (float) Math.cos(bearingRad) * radius;

        int band = (int) (42.0F + strength * 38.0F);
        graphics.fill(0, 0, width, height, desaturateOverlay(strength));

        for (int layer = 0; layer < 3; layer++) {
            int size = band + layer * 18;
            int alpha = (int) ((55 + strength * 90) * (1.0F - layer * 0.28F));
            int layerColor = (alpha << 24) | (color & 0x00FFFFFF);
            graphics.fill((int) arcX - size / 2, (int) arcY - size / 2, (int) arcX + size / 2, (int) arcY + size / 2, layerColor);
        }

        int edge = (int) (18 + strength * 22);
        if (bearing > 8.0F) {
            graphics.fill(width - edge, 0, width, height, sideTint(color, strength * 0.35F));
        } else if (bearing < -8.0F) {
            graphics.fill(0, 0, edge, height, sideTint(color, strength * 0.35F));
        }
    }

    private static int tintForCategory(byte category, float strength) {
        float a = Mth.clamp(0.35F + strength * 0.5F, 0.0F, 1.0F);
        int alpha = (int) (a * 255.0F) << 24;
        return switch (category) {
            case ScentHintPayload.CATEGORY_PLAYER -> alpha | 0x0098D8FF;
            case ScentHintPayload.CATEGORY_PASSIVE -> alpha | 0x0068F0FF;
            case ScentHintPayload.CATEGORY_PREY -> alpha | 0x00FFD040;
            case ScentHintPayload.CATEGORY_HOSTILE -> alpha | 0x00FF6666;
            case ScentHintPayload.CATEGORY_NATURE -> alpha | 0x00F5F5F5;
            default -> alpha | 0x005588CC;
        };
    }

    private static int desaturateOverlay(float strength) {
        int alpha = (int) (28 + strength * 42);
        return (alpha << 24) | 0x00101012;
    }

    private static int sideTint(int base, float scale) {
        int a = (int) (((base >>> 24) & 0xFF) * scale);
        return (a << 24) | (base & 0x00FFFFFF);
    }
}
