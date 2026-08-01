package dev.naturalis.inventory;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class InventoryRestrictionManager {

    private static final Set<String> QUADRUPED_MORPHS = Set.of(
        "wolf", "fox", "cat", "ocelot", "rabbit", "pig", "cow", "sheep", "goat", "horse", "llama",
        "camel", "polar_bear", "hoglin", "ravager", "strider", "spider", "cave_spider"
    );

    private InventoryRestrictionManager() {
    }

    public static boolean isQuadruped(ResourceLocation morphId) {
        return morphId != null && QUADRUPED_MORPHS.contains(morphId.getPath());
    }
}
