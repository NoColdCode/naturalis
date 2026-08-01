package dev.naturalis.client.perception;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * Client-only state for the active hearing cue (HUD + debug).
 */
public final class MorphHearingClientState {

    @Nullable
    private static MorphHearingCue activeCue;
    private static int cueDisplayTicks;
    private static float hudPulse;

    private MorphHearingClientState() {
    }

    @Nullable
    public static MorphHearingCue activeCue() {
        return activeCue;
    }

    public static float hudPulse() {
        return hudPulse;
    }

    public static void setCue(@Nullable MorphHearingCue cue, int displayTicks) {
        activeCue = cue;
        cueDisplayTicks = displayTicks;
    }

    public static void tickHudPulse() {
        hudPulse = (hudPulse + 0.12F) % (float) (Math.PI * 2.0D);
        if (cueDisplayTicks > 0) {
            cueDisplayTicks--;
        }
        if (cueDisplayTicks <= 0) {
            activeCue = null;
        }
    }

    @Nullable
    public static Component hudLabel() {
        return activeCue != null ? activeCue.label() : null;
    }

    public static double hudBearing() {
        return activeCue != null ? activeCue.bearingDegrees() : 0.0D;
    }
}
