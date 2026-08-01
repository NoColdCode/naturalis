package dev.naturalis.worldgen;

import dev.naturalis.NaturalisMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class NaturalDimensionKeys {

    public static final ResourceKey<Level> NATURAL_DIMENSION = ResourceKey.create(
        net.minecraft.core.registries.Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "natural_dimension")
    );

    public static final ResourceKey<Biome> NATURAL_ECHO = biome("natural_echo");
    public static final ResourceKey<Biome> NATURAL_PLAIN = biome("natural_plain");
    public static final ResourceKey<Biome> NATURAL_BEACH = biome("natural_beach");
    public static final ResourceKey<Biome> DENSE_FOREST = biome("dense_forest");
    public static final ResourceKey<Biome> HIGH_PEAK = biome("high_peak");
    public static final ResourceKey<Biome> SNOWY_MOUNTAIN = biome("snowy_mountain");
    public static final ResourceKey<Biome> DEEP_WATER = biome("deep_water");
    public static final ResourceKey<Biome> CORAL_WATER = biome("coral_water");
    public static final ResourceKey<Biome> VOLCANO = biome("volcano");
    public static final ResourceKey<Biome> ENDER_FOREST = biome("ender_forest");
    public static final ResourceKey<Biome> JUNGLE_REAL = biome("jungle_real");
    public static final ResourceKey<Biome> ARID_SAVANNA = biome("arid_savanna");
    public static final ResourceKey<Biome> DARK_CAVES = biome("dark_caves");

    private NaturalDimensionKeys() {
    }

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(
            net.minecraft.core.registries.Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, path)
        );
    }
}
