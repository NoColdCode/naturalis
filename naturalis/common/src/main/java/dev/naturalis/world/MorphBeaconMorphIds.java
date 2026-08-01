package dev.naturalis.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Normalizes morph ids entered in the Morph Beacon GUI. */
public final class MorphBeaconMorphIds {

    private MorphBeaconMorphIds() {
    }

    @Nullable
    public static ResourceLocation normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(trimmed);
        if (id == null) {
            id = ResourceLocation.tryParse("minecraft:" + trimmed);
        }
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        return id;
    }

    public static String normalizeString(String raw) {
        ResourceLocation id = normalize(raw);
        return id != null ? id.toString() : "";
    }
}
