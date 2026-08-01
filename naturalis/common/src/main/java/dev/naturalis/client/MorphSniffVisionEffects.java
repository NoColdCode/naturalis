package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Morph vision + scent overlay post chains ({@code *_scent.json}).
 */
public final class MorphSniffVisionEffects {

    private static final Set<String> SNIFF_CHAINS = Set.of(
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

    private static final Map<String, String> MORPH_TO_SNIFF = Map.ofEntries(
        Map.entry("wolf_vision", "wolf_scent"),
        Map.entry("mammal_vision", "mammal_scent"),
        Map.entry("avian_vision", "avian_scent"),
        Map.entry("aquatic_vision", "aquatic_scent"),
        Map.entry("reptile_vision", "reptile_scent"),
        Map.entry("undead_vision", "undead_scent"),
        Map.entry("nether_vision", "nether_scent"),
        Map.entry("arcane_vision", "arcane_scent"),
        Map.entry("fungal_vision", "fungal_scent"),
        Map.entry("insect_vision", "arcane_scent"),
        Map.entry("cephalopod_vision", "aquatic_scent"),
        Map.entry("abyssal_vision", "aquatic_scent"),
        Map.entry("crystalline_vision", "arcane_scent"),
        Map.entry("ferrous_vision", "nether_scent"),
        Map.entry("fae_vision", "arcane_scent"),
        Map.entry("tempest_vision", "nether_scent"),
        Map.entry("viscous_vision", "reptile_scent"),
        Map.entry("void_vision", "undead_scent")
    );

    private MorphSniffVisionEffects() {
    }

    public static ResourceLocation resolveSniffChain(ResourceLocation morphVisionShader) {
        if (morphVisionShader == null) {
            return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "mammal_scent");
        }
        String sniff = MORPH_TO_SNIFF.get(morphVisionShader.getPath());
        if (sniff == null) {
            ResourceLocation resolved = MorphVisionPostEffects.resolveForSetPostEffect(morphVisionShader);
            sniff = MORPH_TO_SNIFF.getOrDefault(resolved.getPath(), "mammal_scent");
        }
        if (!SNIFF_CHAINS.contains(sniff)) {
            sniff = "mammal_scent";
        }
        return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, sniff);
    }
}
