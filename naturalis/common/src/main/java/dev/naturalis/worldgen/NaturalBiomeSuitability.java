package dev.naturalis.worldgen;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;

public final class NaturalBiomeSuitability {

    public enum Suitability {
        ADAPTED,
        HARSH,
        HOSTILE,
        FORBIDDEN
    }

    private static final Set<String> VOLCANIC = Set.of(
        "blaze", "magma_cube", "ghast", "strider", "wither", "wither_skeleton", "piglin", "piglin_brute", "zombified_piglin"
    );

    private static final Set<String> SNOW_ADAPTED = Set.of(
        "polar_bear", "snow_golem", "stray", "goat", "wolf"
    );

    private static final Set<String> ENDER_ADAPTED = Set.of(
        "enderman", "endermite", "shulker", "ender_dragon"
    );

    private static final Set<String> CAVE_ADAPTED = Set.of(
        "bat", "spider", "cave_spider", "silverfish", "warden"
    );

    private NaturalBiomeSuitability() {
    }

    public static boolean isVolcanicMorph(String path) {
        return VOLCANIC.contains(path);
    }

    public static boolean isVolcanicMorph(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.isVolcanicMorph(morphId)) {
            return true;
        }
        return isVolcanicMorph(morphId.getPath());
    }

    public static boolean isEnderAdaptedMorph(String path) {
        return ENDER_ADAPTED.contains(path);
    }

    public static boolean isEnderAdaptedMorph(ResourceLocation morphId) {
        if (dev.naturalis.profile.MobProfileRegistry.isEnderAdaptedMorph(morphId)) {
            return true;
        }
        return isEnderAdaptedMorph(morphId.getPath());
    }

    public static Suitability evaluate(ResourceLocation morphId, Holder<Biome> biomeHolder) {
        if (morphId == null || biomeHolder == null) {
            return Suitability.ADAPTED;
        }

        String path = morphId.getPath();
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        MobCategory category = type == null ? MobCategory.MISC : type.getCategory();

        boolean aquatic = EnvironmentalSusceptibilityManager.isDryVulnerable(morphId);
        boolean flying = category == MobCategory.AMBIENT || path.contains("bat") || path.contains("phantom") || path.contains("parrot") || path.contains("bee")
            || path.contains("ghast") || path.contains("blaze");
        boolean volcanic = VOLCANIC.contains(path);
        boolean snow = SNOW_ADAPTED.contains(path);
        boolean ender = ENDER_ADAPTED.contains(path);
        boolean cave = CAVE_ADAPTED.contains(path);

        if (biomeHolder.is(NaturalDimensionKeys.DEEP_WATER) || biomeHolder.is(NaturalDimensionKeys.CORAL_WATER)) {
            if (aquatic) {
                return Suitability.ADAPTED;
            }
            if (flying) {
                return Suitability.HARSH;
            }
            return Suitability.HOSTILE;
        }

        if (biomeHolder.is(NaturalDimensionKeys.VOLCANO)) {
            if (volcanic) {
                return Suitability.ADAPTED;
            }
            if (snow || aquatic) {
                return Suitability.FORBIDDEN;
            }
            return Suitability.HOSTILE;
        }

        if (biomeHolder.is(NaturalDimensionKeys.SNOWY_MOUNTAIN) || biomeHolder.is(NaturalDimensionKeys.HIGH_PEAK)) {
            if (snow || flying) {
                return Suitability.ADAPTED;
            }
            if (volcanic || aquatic) {
                return Suitability.FORBIDDEN;
            }
            return Suitability.HARSH;
        }

        if (biomeHolder.is(NaturalDimensionKeys.ENDER_FOREST) || biomeHolder.is(NaturalDimensionKeys.NATURAL_ECHO)) {
            if (ender) {
                return Suitability.ADAPTED;
            }
            if (volcanic || aquatic) {
                return Suitability.HOSTILE;
            }
            return Suitability.HARSH;
        }

        if (biomeHolder.is(NaturalDimensionKeys.DARK_CAVES)) {
            if (cave || ender) {
                return Suitability.ADAPTED;
            }
            if (aquatic) {
                return Suitability.FORBIDDEN;
            }
            return Suitability.HARSH;
        }

        if (biomeHolder.is(NaturalDimensionKeys.ARID_SAVANNA)) {
            if (volcanic) {
                return Suitability.HARSH;
            }
            if (aquatic || snow) {
                return Suitability.HOSTILE;
            }
            return Suitability.ADAPTED;
        }

        if (biomeHolder.is(NaturalDimensionKeys.JUNGLE_REAL) || biomeHolder.is(NaturalDimensionKeys.DENSE_FOREST) || biomeHolder.is(NaturalDimensionKeys.NATURAL_PLAIN) || biomeHolder.is(NaturalDimensionKeys.NATURAL_BEACH)) {
            if (snow) {
                return Suitability.HARSH;
            }
            if (volcanic) {
                return Suitability.HOSTILE;
            }
            return Suitability.ADAPTED;
        }

        return Suitability.ADAPTED;
    }
}
