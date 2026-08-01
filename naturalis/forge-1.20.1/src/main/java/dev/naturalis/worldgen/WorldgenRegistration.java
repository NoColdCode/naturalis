package dev.naturalis.worldgen;

import dev.naturalis.Naturalis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Registers {@code naturalis:island} into vanilla codec registries so
 * {@code dimension/natural_dimension.json} can reference the chunk generator and biome source.
 */
public final class WorldgenRegistration {

    private WorldgenRegistration() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(WorldgenRegistration::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.CHUNK_GENERATOR)) {
            event.register(
                Registries.CHUNK_GENERATOR,
                new ResourceLocation(Naturalis.MOD_ID, "island"),
                () -> IslandChunkGenerator.CODEC
            );
        } else if (event.getRegistryKey().equals(Registries.BIOME_SOURCE)) {
            event.register(
                Registries.BIOME_SOURCE,
                new ResourceLocation(Naturalis.MOD_ID, "island"),
                () -> IslandBiomeSource.CODEC
            );
        }
    }
}
