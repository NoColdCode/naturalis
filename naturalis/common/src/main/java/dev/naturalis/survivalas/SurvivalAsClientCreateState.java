package dev.naturalis.survivalas;

import net.minecraft.resources.ResourceLocation;

/**
 * Client-side staging for the Create World screen. Same JVM as the integrated server,
 * so {@link SurvivalAsEvents} can flush this into world storage on server start.
 */
public final class SurvivalAsClientCreateState {

    private static boolean modeSelected;
    private static ResourceLocation morphId;

    private SurvivalAsClientCreateState() {
    }

    public static void clear() {
        modeSelected = false;
        morphId = null;
    }

    /** Enter Survival-as game mode (mob may still be unset). */
    public static void selectMode() {
        modeSelected = true;
    }

    public static void set(ResourceLocation morph) {
        if (morph == null) {
            morphId = null;
            return;
        }
        modeSelected = true;
        morphId = morph;
    }

    public static boolean isModeSelected() {
        return modeSelected;
    }

    /** Ready to create: Survival-as mode with a chosen mob. */
    public static boolean isActive() {
        return modeSelected && morphId != null;
    }

    public static ResourceLocation getMorphId() {
        return morphId;
    }
}
