package dev.naturalis.client;

/**
 * Client mirror of Survival-as lock. Used to hide Remorphed / Walkers morph UI
 * without applying the Morph Binding potion effect.
 */
public final class SurvivalAsClientCache {

    private static volatile boolean locked;

    private SurvivalAsClientCache() {
    }

    public static boolean isLocked() {
        return locked;
    }

    public static void setLocked(boolean value) {
        locked = value;
    }

    public static void clear() {
        locked = false;
    }
}
