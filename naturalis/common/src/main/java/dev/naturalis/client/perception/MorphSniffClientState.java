package dev.naturalis.client.perception;

import net.minecraft.util.Mth;

/**
 * Active sniff first-person motion and temporary scent vision overlay.
 */
public final class MorphSniffClientState {

    private static float intensity;
    private static float phase;
    private static int pulseCount;
    private static int scentVisionTicks;

    private MorphSniffClientState() {
    }

    public static void pulse(int strength, int trailCount, int preyCount, int hostileCount) {
        float boost = Mth.clamp(strength, 1, 4) * 0.28F + Mth.clamp(trailCount, 0, 14) * 0.04F;
        boost += Mth.clamp(preyCount + hostileCount, 0, 10) * 0.03F;
        intensity = Math.min(1.35F, Math.max(intensity, boost));
        int visionBase = 140 + trailCount * 10 + (preyCount + hostileCount) * 8;
        scentVisionTicks = Math.max(scentVisionTicks, visionBase);
        if (phase > 0.42F) {
            pulseCount++;
            phase = 0.12F;
        } else if (phase <= 0.001F) {
            phase = 0.0F;
            pulseCount = 1;
        }
    }

    public static void tick() {
        if (scentVisionTicks > 0) {
            scentVisionTicks--;
        }
        if (intensity <= 0.001F) {
            intensity = 0.0F;
            phase = 0.0F;
            pulseCount = 0;
            return;
        }
        float speed = 0.038F + intensity * 0.028F;
        phase = Math.min(1.0F, phase + speed);
        if (phase >= 0.38F && pulseCount < 2) {
            pulseCount++;
            phase = 0.14F;
            intensity = Math.min(1.35F, intensity + 0.18F);
        }
        intensity = Mth.lerp(0.07F, intensity, 0.0F);
        if (phase >= 1.0F && intensity < 0.04F) {
            intensity = 0.0F;
            phase = 0.0F;
            pulseCount = 0;
        }
    }

    public static boolean isScentVisionActive() {
        return scentVisionTicks > 0;
    }

    public static boolean isActive() {
        return intensity > 0.035F;
    }

    public static float pitchOffsetDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        float env = sniffEnvelope(phase);
        float second = pulseCount > 1 && phase < 0.45F ? sniffEnvelope(phase * 2.2F) * 0.55F : 0.0F;
        float wiggle = phase > 0.2F ? (float) Math.sin(phase * 16.0D) * (1.0F - phase) * 3.0F : 0.0F;
        return (env * 26.0F + second * 14.0F + wiggle) * Mth.clamp(intensity, 0.0F, 1.35F);
    }

    public static float yawOffsetDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        float env = sniffEnvelope(phase);
        return (float) (Math.sin(phase * 12.0D) * 3.6F * intensity * env
            + Math.sin(phase * 5.5D + 1.1D) * 1.4F * intensity);
    }

    public static float rollDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        return (float) Math.sin(phase * 10.0D + 0.5D) * 6.0F * intensity * sniffEnvelope(phase);
    }

    public static double forwardBlocks() {
        if (!isActive()) {
            return 0.0D;
        }
        return sniffEnvelope(phase) * 0.16D * intensity;
    }

    public static double verticalBlocks() {
        if (!isActive()) {
            return 0.0D;
        }
        return -sniffEnvelope(phase) * 0.07D * intensity;
    }

    public static float fovOffsetDegrees() {
        if (isScentVisionActive() || !isActive()) {
            return 0.0F;
        }
        return -sniffEnvelope(phase) * 9.0F * Mth.clamp(intensity, 0.0F, 1.2F);
    }

    private static float sniffEnvelope(float t) {
        float rise = smoothStep(0.0F, 0.18F, t);
        float hold = smoothStep(0.18F, 0.42F, t) * (1.0F - smoothStep(0.55F, 0.72F, t));
        float fall = 1.0F - smoothStep(0.68F, 1.0F, t);
        return rise * Math.max(fall, hold * 0.85F);
    }

    private static float smoothStep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
