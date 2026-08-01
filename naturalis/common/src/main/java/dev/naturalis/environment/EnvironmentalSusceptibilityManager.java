package dev.naturalis.environment;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.worldgen.NaturalBiomeSuitability;
import dev.naturalis.worldgen.NaturalDimensionKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class EnvironmentalSusceptibilityManager {

    // Cold vulnerable creatures - desert/tropical/fire creatures
    private static final Set<String> COLD_VULNERABLE = new HashSet<>();
    
    // Hot vulnerable creatures - arctic/aquatic creatures
    private static final Set<String> HOT_VULNERABLE = new HashSet<>();
    
    // Wet vulnerable creatures - fire/dry creatures
    private static final Set<String> WET_VULNERABLE = new HashSet<>();
    
    // Dry vulnerable creatures - aquatic creatures
    private static final Set<String> DRY_VULNERABLE = new HashSet<>();

    // Nyctalop hostile creatures: naturally adapted to darkness.
    private static final Set<String> NYCTALOP_HOSTILE = new HashSet<>();

    // Sunlight-sensitive creatures: bright clear daytime impairs their vision.
    private static final Set<String> SUNLIGHT_SENSITIVE = new HashSet<>();

    static {
        // Cold vulnerable (desert, tropical, fire-based)
        COLD_VULNERABLE.addAll(Set.of(
            "camel",
            "blaze",
            "magma_cube",
            "wither",
            "ghast",
            "strider",
            "piglin",
            "piglin_brute",
            "zombified_piglin",
            "creeper",
            "slime",
            "tropical_fish",
            "pufferfish",
            "axolotl"
        ));

        // Hot vulnerable (arctic, aquatic, cold-dwelling)
        HOT_VULNERABLE.addAll(Set.of(
            "polar_bear",
            "fox",
            "panda",
            "snow_golem",
            "dolphin",
            "cod",
            "salmon",
            "squid",
            "glow_squid",
            "guardian",
            "elder_guardian",
            "frog",
            "tadpole",
            "warden",
            "allay"
        ));

        // Wet vulnerable (fire, dry, teleport-based, undead)
        WET_VULNERABLE.addAll(Set.of(
            "blaze",
            "ghast",
            "wither",
            "enderman",
            "endermite",
            "ender_dragon",
            "magma_cube",
            "creeper",
            "spider",
            "cave_spider",
            "silverfish"
        ));

        // Dry vulnerable (aquatic creatures)
        DRY_VULNERABLE.addAll(Set.of(
            "axolotl",
            "dolphin",
            "cod",
            "salmon",
            "tropical_fish",
            "pufferfish",
            "squid",
            "glow_squid",
            "tadpole",
            "guardian",
            "elder_guardian",
            "frog"
        ));

        // Selected hostile nyctalop morphs.
        NYCTALOP_HOSTILE.addAll(Set.of(
            "fox",
            "cat",
            "ocelot",
            "bat",
            "wolf",
            "spider",
            "cave_spider",
            "enderman",
            "endermite",
            "silverfish",
            "skeleton",
            "stray",
            "zombie",
            "husk",
            "drowned",
            "creeper",
            "phantom",
            "witch"
        ));

        // Explicitly sensitive set for vanilla-like dark-adapted morphs.
        SUNLIGHT_SENSITIVE.addAll(Set.of(
            "bat",
            "spider",
            "cave_spider",
            "silverfish",
            "phantom",
            "skeleton",
            "stray",
            "bogged",
            "zombie",
            "husk",
            "drowned",
            "zombie_villager",
            "vex",
            "warden"
        ));
    }

    private EnvironmentalSusceptibilityManager() {
    }

    /**
     * Determines the environment type the player is in.
     */
    public static EnvironmentType getEnvironmentType(Level level, Vec3 position, boolean isInWater) {
        // Check for water
        if (isInWater) {
            return EnvironmentType.WET;
        }

        // Check for rain (open sky + raining)
        if (level.isRaining() && isOpenToSky(level, position)) {
            return EnvironmentType.WET;
        }

        // Check biome temperature
        int x = net.minecraft.util.Mth.floor(position.x);
        int y = net.minecraft.util.Mth.floor(position.y);
        int z = net.minecraft.util.Mth.floor(position.z);
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);

        net.minecraft.world.level.biome.Biome biome = level.getBiome(pos).value();
        float temperature = biome.getBaseTemperature();

        if (temperature < 0.15F) {
            return EnvironmentType.COLD;
        } else if (temperature > 1.5F) {
            return EnvironmentType.HOT;
        }

        return EnvironmentType.TEMPERATE;
    }

    /**
     * Checks if a position is open to the sky (for rain detection).
     */
    private static boolean isOpenToSky(Level level, Vec3 position) {
        int x = net.minecraft.util.Mth.floor(position.x);
        int y = net.minecraft.util.Mth.floor(position.y);
        int z = net.minecraft.util.Mth.floor(position.z);

        // Simple raycast upward to check for blocks above
        for (int dy = y + 1; dy < CompatAccess.getMaxBuildHeight(level); dy++) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(x, dy, z)).isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the creature is far enough from water (for dry vulnerability).
     */
    public static boolean isAwayFromWater(Level level, Vec3 position, double minDistance) {
        int x = net.minecraft.util.Mth.floor(position.x);
        int y = net.minecraft.util.Mth.floor(position.y);
        int z = net.minecraft.util.Mth.floor(position.z);

        // Check a radius around the player
        int radius = (int) Math.ceil(minDistance);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x + dx, y + dy, z + dz);
                    if (level.getBlockState(pos).getFluidState().isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if a creature should suffer from environmental effects.
     */
    public static EnvironmentalVulnerability checkVulnerability(ResourceLocation morphId, EnvironmentType environmentType) {
        switch (environmentType) {
            case COLD -> {
                var profile = dev.naturalis.profile.MobProfileRegistry.getColdVulnerable(morphId);
                if (profile.isPresent()) {
                    return profile.get() ? EnvironmentalVulnerability.COLD_VULNERABLE : EnvironmentalVulnerability.NONE;
                }
            }
            case HOT -> {
                var profile = dev.naturalis.profile.MobProfileRegistry.getHotVulnerable(morphId);
                if (profile.isPresent()) {
                    return profile.get() ? EnvironmentalVulnerability.HOT_VULNERABLE : EnvironmentalVulnerability.NONE;
                }
            }
            case WET -> {
                var profile = dev.naturalis.profile.MobProfileRegistry.getWetVulnerable(morphId);
                if (profile.isPresent()) {
                    return profile.get() ? EnvironmentalVulnerability.WET_VULNERABLE : EnvironmentalVulnerability.NONE;
                }
            }
            default -> { }
        }

        String morphPath = morphId.getPath();

        switch (environmentType) {
            case COLD -> {
                if (COLD_VULNERABLE.contains(morphPath)) {
                    return EnvironmentalVulnerability.COLD_VULNERABLE;
                }
            }
            case HOT -> {
                if (HOT_VULNERABLE.contains(morphPath)) {
                    return EnvironmentalVulnerability.HOT_VULNERABLE;
                }
            }
            case WET -> {
                if (WET_VULNERABLE.contains(morphPath)) {
                    return EnvironmentalVulnerability.WET_VULNERABLE;
                }
            }
        }

        return EnvironmentalVulnerability.NONE;
    }

    /**
     * Determines if a creature is water-dependent and suffering from dry conditions.
     */
    public static boolean isDrySuffering(ResourceLocation morphId, Level level, Vec3 position) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getDryVulnerable(morphId);
        if (profile.isPresent()) {
            if (!profile.get()) {
                return false;
            }
            return isAwayFromWater(level, position, 16.0D);
        }

        String morphPath = morphId.getPath();

        if (!DRY_VULNERABLE.contains(morphPath)) {
            return false;
        }

        return isAwayFromWater(level, position, 16.0D); // 16 blocks minimum distance
    }

    /**     * Determines if a creature is water-dependent (dry vulnerable).
     */
    public static boolean isDryVulnerable(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getDryVulnerable(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }
        return DRY_VULNERABLE.contains(morphId.getPath());
    }

    public static boolean isNyctalopHostile(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getNyctalopHostile(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }
        return NYCTALOP_HOSTILE.contains(morphId.getPath());
    }

    /**
     * Clear daytime sun directly overhead with open sky (matches legacy sun-blindness trigger).
     */
    public static boolean isClearSunnyExposure(Level level, BlockPos pos) {
        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay < 1000L || timeOfDay > 12000L) {
            return false;
        }
        if (level.isRaining() || level.isThundering()) {
            return false;
        }
        return level.canSeeSky(pos);
    }

    /**
     * Natural dimension biomes where nether/end-adapted morphs tolerate daylight glare.
     */
    public static boolean isSunPhotophobiaBiomeExempt(Level level, ResourceLocation morphId, BlockPos pos) {
        if (!level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return false;
        }
        String path = morphId.getPath();
        var biomeHolder = level.getBiome(pos);
        if (biomeHolder.is(NaturalDimensionKeys.VOLCANO)) {
            return NaturalBiomeSuitability.isVolcanicMorph(morphId);
        }
        if (biomeHolder.is(NaturalDimensionKeys.ENDER_FOREST)) {
            return NaturalBiomeSuitability.isEnderAdaptedMorph(morphId);
        }
        return false;
    }

    /**
     * Open skylight bright enough that Minecraft {@link net.minecraft.world.effect.MobEffects#NIGHT_VISION}
     * washes out morph {@code PhotoStress}. Strip NV or skip granting it when true (sun-sensitive morphs only).
     */
    public static boolean shouldSuppressMorphNightVision(Level level, BlockPos pos, ResourceLocation morphId) {
        if (!isSunlightSensitive(morphId)) {
            return false;
        }
        if (!level.dimensionType().hasSkyLight()) {
            return false;
        }
        if (!level.canSeeSky(pos)) {
            return false;
        }
        return level.getBrightness(LightLayer.SKY, pos) >= 10;
    }

    public static boolean isSunlightSensitive(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getSunlightSensitive(morphId);
        if (profile.isPresent()) {
            return profile.get();
        }

        String path = morphId.getPath();
        if ("wolf".equals(path) || "fox".equals(path)) {
            return false;
        }
        if (SUNLIGHT_SENSITIVE.contains(path) || NYCTALOP_HOSTILE.contains(path)) {
            return true;
        }

        // Fallback to cover every other vanilla/modded morph deterministically.
        if (path.contains("bat")
            || path.contains("spider")
            || path.contains("silverfish")
            || path.contains("phantom")
            || path.contains("skeleton")
            || path.contains("zombie")
            || path.contains("vex")
            || path.contains("warden")) {
            return true;
        }

        var type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return false;
        }

        // Hostile monsters tend to be less sun-adapted by default.
        return type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    /**     * Environmental types for categorization.
     */
    public enum EnvironmentType {
        COLD,
        HOT,
        WET,
        TEMPERATE
    }

    /**
     * Vulnerability categories for creatures.
     */
    public enum EnvironmentalVulnerability {
        COLD_VULNERABLE,
        HOT_VULNERABLE,
        WET_VULNERABLE,
        NONE
    }
}
