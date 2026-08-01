package dev.naturalis.client.perception;

import net.minecraft.util.Mth;

/**
 * Scales resolved morph perception by embodiment depth (humanity loss + time morphed).
 */
public final class MorphPerceptionScaling {

    private MorphPerceptionScaling() {
    }

    public static MorphHearingProfile hearing(MorphHearingProfile base) {
        if (!base.hasEnhancedHearing()) {
            return base;
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        return new MorphHearingProfile(
            base.volumeMultiplier() * lerp(1.0F, 1.28F, drift),
            base.scanRange() * lerp(1.0D, 1.22D, drift),
            Math.max(3, (int) (base.scanIntervalTicks() * lerp(1.0F, 0.72F, drift))),
            base.minCueIntensity() * lerp(1.0F, 0.82F, drift),
            base.preyEntityPaths(),
            base.threatEntityPaths(),
            base.directionalCues(),
            base.ambientPulse()
        );
    }

    public static MorphVibrationProfile vibration(MorphVibrationProfile base) {
        if (!base.hasPawVibrationSense()) {
            return base;
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        return new MorphVibrationProfile(
            base.scanRange() * lerp(1.0D, 1.25D, drift),
            Math.max(2, (int) (base.scanIntervalTicks() * lerp(1.0F, 0.75F, drift))),
            base.minIntensity() * lerp(1.0F, 0.85F, drift),
            base.actionBarThreshold() * lerp(1.0F, 0.88F, drift),
            base.cameraTremorScale() * lerp(1.0F, 1.35F, drift)
        );
    }

    public static MorphMusicProfile music(MorphMusicProfile base) {
        if (!base.altersMusicPerception()) {
            return base;
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        float damp = lerp(1.0F, 0.72F, drift);
        return new MorphMusicProfile(
            base.musicVolumeMultiplier() * damp,
            base.recordVolumeMultiplier() * damp,
            base.distantHarmonicVolume()
        );
    }

    public static double fovMultiplier(MorphEmbodimentProfile profile) {
        if (!profile.hasEmbodiment()) {
            return profile.fovMultiplier();
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        return profile.fovMultiplier() * lerp(1.0D, 1.08D, drift);
    }

    public static float sniffGaitStrength(MorphEmbodimentProfile profile) {
        if (!profile.hasEmbodiment()) {
            return 1.0F;
        }
        return lerp(1.0F, 1.22F, MorphIdentityDriftClient.embodimentBlend());
    }

    private static float lerp(float from, float to, float t) {
        return Mth.lerp(Mth.clamp(t, 0.0F, 1.0F), from, to);
    }

    private static double lerp(double from, double to, float t) {
        return Mth.lerp(Mth.clamp(t, 0.0F, 1.0F), from, to);
    }
}
