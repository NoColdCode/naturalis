package dev.naturalis.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Forge 1.20.1 version — uses {@code Codec} instead of {@code MapCodec}.
 */
public final class IslandBiomeSource extends BiomeSource {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<ResourceKey<Biome>> ALL_BIOME_KEYS = List.of(
        NaturalDimensionKeys.NATURAL_ECHO,
        NaturalDimensionKeys.NATURAL_PLAIN,
        NaturalDimensionKeys.NATURAL_BEACH,
        NaturalDimensionKeys.DENSE_FOREST,
        NaturalDimensionKeys.HIGH_PEAK,
        NaturalDimensionKeys.SNOWY_MOUNTAIN,
        NaturalDimensionKeys.DEEP_WATER,
        NaturalDimensionKeys.CORAL_WATER,
        NaturalDimensionKeys.VOLCANO,
        NaturalDimensionKeys.ENDER_FOREST,
        NaturalDimensionKeys.JUNGLE_REAL,
        NaturalDimensionKeys.ARID_SAVANNA,
        NaturalDimensionKeys.DARK_CAVES
    );

    public static final Codec<IslandBiomeSource> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
        ).apply(inst, IslandBiomeSource::new)
    );

    private final HolderGetter<Biome> biomeGetter;
    private volatile IslandHeightmap heightmap;

    public IslandBiomeSource(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        LOGGER.info("[naturalis-biome] IslandBiomeSource constructed on thread '{}'", Thread.currentThread().getName());
    }

    private IslandHeightmap heightmap() {
        IslandHeightmap hm = this.heightmap;
        if (hm == null) {
            hm = IslandHeightmap.tryGet();
            if (hm != null) {
                LOGGER.info("[naturalis-biome] heightmap resolved on thread '{}'", Thread.currentThread().getName());
                this.heightmap = hm;
            }
        }
        return hm;
    }

    public HolderGetter<Biome> getBiomeLookup() {
        return biomeGetter;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return ALL_BIOME_KEYS.stream().map(biomeGetter::getOrThrow);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        IslandHeightmap hm = heightmap();
        int blockX = x * 4;
        int blockZ = z * 4;
        ResourceKey<Biome> key = hm != null ? hm.getBiomeKey(blockX, blockZ) : NaturalDimensionKeys.NATURAL_PLAIN;

        if (NaturalDimensionKeys.DARK_CAVES.equals(key) && hm != null) {
            int surfY  = hm.getSurfaceY(blockX, blockZ);
            int blockY = y * 4;
            if (blockY >= surfY - 20) {
                key = NaturalDimensionKeys.HIGH_PEAK;
            }
        }

        if (NaturalDimensionKeys.SNOWY_MOUNTAIN.equals(key) && hm != null) {
            int surfY  = hm.getSurfaceY(blockX, blockZ);
            int blockY = y * 4;
            if (blockY < surfY - 32) {
                key = NaturalDimensionKeys.DARK_CAVES;
            }
        }

        return biomeGetter.getOrThrow(key);
    }
}
