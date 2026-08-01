package dev.naturalis.worldgen;

/**
 * Defaults for worldgen behavior; Forge 1.20.1 replaces this class (see that module's {@code build.gradle}).
 */
public final class NaturalisWorldgenFlags {

    private NaturalisWorldgenFlags() {
    }

    /**
     * Inactive Natural portal frames placed on overworld swamp chunk load.
     */
    public static boolean overworldSwampInactivePortalsOnChunkLoad() {
        return true;
    }
}
