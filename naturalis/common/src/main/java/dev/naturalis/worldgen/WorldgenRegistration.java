package dev.naturalis.worldgen;

import com.mojang.serialization.MapCodec;
import dev.naturalis.NaturalisMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the custom chunk-generator and biome-source codecs into the
 * vanilla built-in registries so the dimension JSON can reference them.
 *
 * <p>Call {@link #init(IEventBus)} from the mod constructor so that
 * registration is deferred to the correct {@code RegisterEvent} phase
 * (before registries are frozen).
 */
public final class WorldgenRegistration {

    private WorldgenRegistration() {}

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GEN_CODECS =
        DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, NaturalisMod.ID);

    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCE_CODECS =
        DeferredRegister.create(BuiltInRegistries.BIOME_SOURCE, NaturalisMod.ID);

    static {
        CHUNK_GEN_CODECS.register("island", () -> IslandChunkGenerator.CODEC);
        BIOME_SOURCE_CODECS.register("island", () -> IslandBiomeSource.CODEC);
    }

    public static void init(IEventBus modEventBus) {
        CHUNK_GEN_CODECS.register(modEventBus);
        BIOME_SOURCE_CODECS.register(modEventBus);
    }
}
