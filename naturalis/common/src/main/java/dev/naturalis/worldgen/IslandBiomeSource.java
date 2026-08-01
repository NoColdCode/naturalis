package dev.naturalis.worldgen;

import com.mojang.serialization.MapCodec;
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
 * Biome source for the Natural dimension.
 * Reads biome assignment per block column directly from the island biome-map
 * PNG via {@link IslandHeightmap}.
 */
public final class IslandBiomeSource extends BiomeSource {

    private static final Logger LOGGER = LogUtils.getLogger();

    // All biomes this source can emit (must be a superset of what the PNG contains)
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

    /** Codec used for serialization / registry registration. */
    public static final MapCodec<IslandBiomeSource> CODEC = RecordCodecBuilder.mapCodec(inst ->
        inst.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
        ).apply(inst, IslandBiomeSource::new)
    );

    private final HolderGetter<Biome> biomeGetter;
    // Loaded lazily so the constructor is safe to call on the render thread
    // (NeoForge decodes the dimension codec client-side during teleport).
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
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return ALL_BIOME_KEYS.stream().map(biomeGetter::getOrThrow);
    }

    /**
     * x, y, z are in biome space (= block / 4).  We convert back to block
     * coordinates for the PNG lookup.
     */
    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        IslandHeightmap hm = heightmap();
        int blockX = x * 4;
        int blockZ = z * 4;
        ResourceKey<Biome> key = hm != null ? hm.getBiomeKey(blockX, blockZ) : NaturalDimensionKeys.NATURAL_PLAIN;

        // DARK_CAVES is an underground biome — only assign it when the query Y
        // is well below the surface.  At and above the surface, fall back to
        // HIGH_PEAK (dark-cave regions are always in mountainous terrain).
        if (NaturalDimensionKeys.DARK_CAVES.equals(key) && hm != null) {
            int surfY  = hm.getSurfaceY(blockX, blockZ);
            int blockY = y * 4;
            if (blockY >= surfY - 20) {
                key = NaturalDimensionKeys.HIGH_PEAK;
            }
        }

        // SNOWY_MOUNTAIN automatically generates DARK_CAVES biome in the rock
        // mass beneath it — no need to paint it in the biome map.
        // The cave biome starts 32 blocks below the surface.
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
