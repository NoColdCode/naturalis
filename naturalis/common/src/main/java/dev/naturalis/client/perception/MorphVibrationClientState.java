package dev.naturalis.client.perception;

import org.jetbrains.annotations.Nullable;

/**
 * Felt vibration through paws (intensity drives camera tremor, not HUD arrows).
 */
public final class MorphVibrationClientState {

    @Nullable
    private static MorphVibrationCue activeCue;
    private static float feltIntensity;
    private static float tremorPhase;
    private static int actionBarCooldown;

    private MorphVibrationClientState() {
    }

    public static float feltIntensity() {
        return feltIntensity;
    }

    public static float tremorPhase() {
        return tremorPhase;
    }

    @Nullable
    public static MorphVibrationCue activeCue() {
        return activeCue;
    }

    public static void tickDecay() {
        tremorPhase = (tremorPhase + 0.18F) % (float) (Math.PI * 2.0D);
        feltIntensity *= 0.78F;
        if (feltIntensity < 0.03F) {
            activeCue = null;
        }
        if (actionBarCooldown > 0) {
            actionBarCooldown--;
        }
    }

    public static void absorb(MorphVibrationCue cue) {
        activeCue = cue;
        feltIntensity = Math.max(feltIntensity, cue.intensity());
    }

    public static boolean canShowActionBar() {
        return actionBarCooldown <= 0;
    }

    public static void markActionBarShown() {
        actionBarCooldown = 45;
    }
}
