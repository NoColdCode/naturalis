package dev.naturalis.client.perception;

import dev.naturalis.network.ScentHintPayload;
import net.minecraft.util.Mth;

/**
 * Active listen: sustained focus, smoothed ear-turn, and directional HUD tint (no text).
 */
public final class MorphListenClientState {

    private static int focusTicks;
    private static float intensity;
    private static float phase;
    private static float targetBearing;
    private static float smoothedBearing;
    private static byte category = ScentHintPayload.CATEGORY_UNKNOWN;
    private static int trackedEntityId = -1;
    private static float proximityGain;

    private MorphListenClientState() {
    }

    public static void pulse(
        double bearingDegrees,
        boolean lockedOn,
        byte pulseCategory,
        int distanceBlocks,
        int entityId
    ) {
        targetBearing = (float) bearingDegrees;
        category = pulseCategory;
        trackedEntityId = entityId;
        proximityGain = distanceBlocks <= 0
            ? 0.25F
            : Mth.clamp(1.0F - distanceBlocks / 48.0F, 0.2F, 1.0F);
        float boost = lockedOn ? 0.55F + proximityGain * 0.45F : 0.28F;
        intensity = Math.min(1.25F, Math.max(intensity, boost));
        focusTicks = lockedOn ? Math.max(focusTicks, 74) : Math.max(focusTicks, 10);
        if (phase <= 0.001F) {
            phase = 0.0F;
        }
    }

    public static void tick() {
        if (focusTicks > 0) {
            focusTicks--;
        }
        smoothedBearing = Mth.lerp(0.22F, smoothedBearing, targetBearing);
        if (focusTicks <= 0 && intensity <= 0.001F) {
            intensity = 0.0F;
            phase = 0.0F;
            targetBearing = 0.0F;
            smoothedBearing = 0.0F;
            category = ScentHintPayload.CATEGORY_UNKNOWN;
            trackedEntityId = -1;
            proximityGain = 0.0F;
            return;
        }
        phase = Math.min(1.0F, phase + 0.032F);
        float decay = focusTicks > 0 ? 0.045F : 0.11F;
        intensity = Mth.lerp(decay, intensity, focusTicks > 0 ? 0.35F : 0.0F);
        if (focusTicks <= 0 && phase >= 1.0F && intensity < 0.06F) {
            intensity = 0.0F;
            phase = 0.0F;
        }
    }

    public static boolean isFocusActive() {
        return focusTicks > 0;
    }

    public static boolean isActive() {
        return focusTicks > 0 || intensity > 0.04F;
    }

    public static float focusStrength() {
        return Mth.clamp(intensity * (0.55F + proximityGain * 0.45F), 0.0F, 1.0F);
    }

    public static float smoothedBearing() {
        return smoothedBearing;
    }

    public static byte category() {
        return category;
    }

    public static int trackedEntityId() {
        return trackedEntityId;
    }

    public static float pitchOffsetDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        float env = listenEnvelope(phase);
        float cup = focusTicks > 0 ? 5.5F : 3.0F;
        return -env * cup * intensity;
    }

    public static float yawOffsetDegrees() {
        if (!isActive()) {
            return 0.0F;
        }
        float env = listenEnvelope(phase);
        float turn = Mth.clamp(smoothedBearing, -82.0F, 82.0F) * env * intensity;
        float tremor = focusTicks > 0
            ? (float) Math.sin(phase * 6.5D + smoothedBearing * 0.05D) * 0.9F * proximityGain
            : 0.0F;
        return turn + tremor;
    }

    public static float rollDegrees() {
        if (!isFocusActive()) {
            return 0.0F;
        }
        return Mth.clamp(smoothedBearing * 0.08F, -4.0F, 4.0F) * intensity;
    }

    public static float fovOffsetDegrees() {
        if (!isFocusActive()) {
            return 0.0F;
        }
        return -listenEnvelope(phase) * 5.0F * intensity;
    }

    private static float listenEnvelope(float t) {
        float rise = smoothStep(0.0F, 0.18F, t);
        float fall = 1.0F - smoothStep(0.5F, 1.0F, t);
        return rise * Math.max(fall, 0.35F);
    }

    private static float smoothStep(float edge0, float edge1, float x) {
        float u = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }
}
