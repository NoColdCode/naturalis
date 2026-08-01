package dev.naturalis.worldgen;

import com.mojang.serialization.MapCodec;
import dev.naturalis.NaturalisMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Fabric {@code ModInitializer} bootstrap; NeoForge keeps DeferredRegister via {@link WorldgenRegistration}. */
public final class VanillaRegistrations {

    private VanillaRegistrations() {
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    public static void register() {
        ResourceLocation island = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "island");
        Registry.register(
            (Registry<MapCodec<? extends ChunkGenerator>>) (Object) BuiltInRegistries.CHUNK_GENERATOR,
            island,
            IslandChunkGenerator.CODEC
        );
        Registry.register(
            (Registry<MapCodec<? extends BiomeSource>>) (Object) BuiltInRegistries.BIOME_SOURCE,
            island,
            IslandBiomeSource.CODEC
        );

        Registry.register(
            BuiltInRegistries.FEATURE,
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "echo_spike"),
            new EchoSpikeFeature(NoneFeatureConfiguration.CODEC)
        );

        Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_binding"),
            new MobEffect(MobEffectCategory.NEUTRAL, 0x7284A2) {
            }
        );
        Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "brewed_morph"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0x7B9A4A) {
            }
        );
        Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "storm_attunement"),
            new MobEffect(MobEffectCategory.NEUTRAL, 0x63C7FF) {
            }
        );
    }
}
