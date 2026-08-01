package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Maps logical morph vision ids to {@code assets/.../post_effect/*.json} chains present on 1.21.x.
 * Legacy {@code shaders/post/*.json} paths are not loaded by {@code setPostEffect} and produce invalid pipelines.
 */
public final class MorphVisionPostEffects {

    private static final Set<String> NATIVE_POST_EFFECTS = Set.of(
        "wolf_vision",
        "mammal_vision",
        "avian_vision",
        "aquatic_vision",
        "reptile_vision",
        "undead_vision",
        "nether_vision",
        "arcane_vision",
        "fungal_vision",
        "scent_vision",
        "wolf_scent",
        "mammal_scent",
        "avian_scent",
        "aquatic_scent",
        "reptile_scent",
        "undead_scent",
        "nether_scent",
        "arcane_scent",
        "fungal_scent"
    );

    /** Logical shader id → nearest shipped {@code post_effect} id (palette tuning may still use the logical id). */
    private static final Map<String, String> LOGICAL_FALLBACK = Map.ofEntries(
        Map.entry("insect_vision", "arcane_vision"),
        Map.entry("cephalopod_vision", "aquatic_vision"),
        Map.entry("abyssal_vision", "aquatic_vision"),
        Map.entry("crystalline_vision", "arcane_vision"),
        Map.entry("ferrous_vision", "nether_vision"),
        Map.entry("fae_vision", "arcane_vision"),
        Map.entry("tempest_vision", "nether_vision"),
        Map.entry("viscous_vision", "reptile_vision"),
        Map.entry("void_vision", "undead_vision")
    );

    private MorphVisionPostEffects() {
    }

    /**
     * Resource location passed to {@code GameRenderer.setPostEffect} on 1.21.x.
     */
    public static ResourceLocation resolveForSetPostEffect(ResourceLocation logical) {
        if (logical == null) {
            return null;
        }
        if (!NaturalisMod.ID.equals(logical.getNamespace())) {
            return logical;
        }
        String path = logical.getPath();
        if (NATIVE_POST_EFFECTS.contains(path)) {
            return logical;
        }
        String fallback = LOGICAL_FALLBACK.get(path);
        if (fallback != null) {
            return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, fallback);
        }
        return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "mammal_vision");
    }

    public static boolean hasNativePostEffect(ResourceLocation logical) {
        return logical != null
            && NaturalisMod.ID.equals(logical.getNamespace())
            && NATIVE_POST_EFFECTS.contains(logical.getPath());
    }
}
