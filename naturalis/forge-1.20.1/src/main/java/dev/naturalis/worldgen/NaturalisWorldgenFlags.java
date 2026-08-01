package dev.naturalis.worldgen;

/**
 * Forge 1.20.1 override: chunk-load swamp inactive portals stall integrated-server world generation.
 */
public final class NaturalisWorldgenFlags {

    private NaturalisWorldgenFlags() {
    }

    public static boolean overworldSwampInactivePortalsOnChunkLoad() {
        return false;
    }
}
