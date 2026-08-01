package dev.naturalis.client;

import dev.naturalis.experience.NaturalisExperienceMode;

/**
 * Client copy of the overworld experience mode, synced from the server.
 * Supports local preview on the choice screen before confirming.
 */
public final class ExperienceModeClientCache {

    private static volatile NaturalisExperienceMode mode = NaturalisExperienceMode.UNSET;
    private static volatile boolean showPrompt;
    private static volatile boolean promptPending;
    /** Local try-before-confirm on the choice screen (not sent to server). */
    private static volatile NaturalisExperienceMode previewMode;

    private ExperienceModeClientCache() {
    }

    public static NaturalisExperienceMode getMode() {
        return mode;
    }

    /** Mode used for client gates (preview → synced mode → realistic if unset). */
    public static NaturalisExperienceMode getEffectiveMode() {
        if (previewMode != null) {
            return previewMode;
        }
        if (mode == NaturalisExperienceMode.UNSET || mode == null) {
            return NaturalisExperienceMode.REALISTIC;
        }
        return mode;
    }

    public static boolean shouldShowPrompt() {
        return showPrompt;
    }

    public static boolean isPromptPending() {
        return promptPending;
    }

    public static void clearPromptPending() {
        promptPending = false;
    }

    public static void set(NaturalisExperienceMode newMode, boolean prompt) {
        mode = newMode;
        showPrompt = prompt;
        previewMode = null;
        // Always sync pending — Survival-as sends prompt=false and must clear a stale pending flag.
        promptPending = prompt;
    }

    public static void setPreview(NaturalisExperienceMode preview) {
        previewMode = preview;
    }

    public static void clearPreview() {
        previewMode = null;
    }

    public static boolean consumePromptPending() {
        if (!promptPending) {
            return false;
        }
        promptPending = false;
        return true;
    }
}
