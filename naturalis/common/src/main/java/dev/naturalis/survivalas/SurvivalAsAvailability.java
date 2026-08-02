package dev.naturalis.survivalas;

/**
 * Survival as… is a 1.21+ Create World feature (NeoForge / Fabric).
 * Forge 1.20.1 overrides this to {@code false}.
 */
public final class SurvivalAsAvailability {

    private SurvivalAsAvailability() {
    }

    public static boolean isSupported() {
        return true;
    }
}
