package dev.naturalis.client.perception;

import net.minecraft.util.Mth;

/** Beak lunge: sharp forward dip for peck. */
public final class MorphPeckClientState {

    private static float intensity;
    private static float phase;
    private static boolean struckEntity;
    private static boolean struckBlock;

    private MorphPeckClientState() {
    }

    public static void pulse(boolean entity, boolean block) {
        struckEntity = entity;
        struckBlock = block;
        intensity = entity ? 1.15F : (block ? 0.95F : 0.72F);
        phase = 0.0F;
    }

    public static void tick() {
        if (intensity <= 0.001F) {
            intensity = 0.0F;
            phase = 0.0F;
            return;
        }
        phase = Math.min(1.0F, phase + 0.11F);
        intensity = Mth.lerp(0.14F, intensity, 0.0F);
        if (phase >= 1.0F && intensity < 0.05F) {
            intensity = 0.0F;
            phase = 0.0F;
        }
    }

    public static boolean isActive() {
        return intensity > 0.04F;
    }

    public static float pitchOffsetDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        float env = peckEnvelope(phase);
        float strike = struckEntity ? 10.0F : (struckBlock ? 6.0F : 3.0F);
        return env * strike * intensity;
    }

    public static float yawOffsetDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        return (float) Math.sin(phase * 14.0D) * 1.8F * intensity * peckEnvelope(phase);
    }

    public static double forwardBlocks() {
        if (!isActive()) {
            return 0.0D;
        }
        return peckEnvelope(phase) * (struckEntity ? 0.14D : 0.09D) * intensity;
    }

    public static double verticalBlocks() {
        if (!isActive()) {
            return 0.0D;
        }
        return -peckEnvelope(phase) * 0.04D * intensity;
    }

    private static float peckEnvelope(float t) {
        float rise = smoothStep(0.0F, 0.25F, t);
        float fall = 1.0F - smoothStep(0.4F, 1.0F, t);
        return rise * fall;
    }

    private static float smoothStep(float edge0, float edge1, float x) {
        float u = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }
}
