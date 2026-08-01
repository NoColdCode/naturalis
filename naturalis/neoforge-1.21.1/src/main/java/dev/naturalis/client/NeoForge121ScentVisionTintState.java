package dev.naturalis.client;

import dev.naturalis.client.perception.MorphSniffClientState;

import java.util.Optional;

public final class NeoForge121ScentVisionTintState {

    private static final int FILTER_FILL_ARGB = 0xFFC9CDD6;

    private NeoForge121ScentVisionTintState() {
    }

    public static boolean shouldStyleScentedMobs() {
        return MorphSniffClientState.isScentVisionActive() || ScentTrailClient.hasDeepRibbons();
    }

    public static int filterFillArgb() {
        return FILTER_FILL_ARGB;
    }

    public static Optional<Integer> outlineForEntity(int entityId) {
        if (!shouldStyleScentedMobs()) {
            return Optional.empty();
        }
        return ScentTrailClient.scentCategoryForEntity(entityId)
            .map(category -> ScentTrailClient.ribbonColor(category, 1.0F) | 0xFF000000);
    }
}
