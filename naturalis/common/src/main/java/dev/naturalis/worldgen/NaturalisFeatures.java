package dev.naturalis.worldgen;

import dev.naturalis.NaturalisMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NaturalisFeatures {

    private NaturalisFeatures() {}

    private static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(BuiltInRegistries.FEATURE, NaturalisMod.ID);

    public static final DeferredHolder<Feature<?>, EchoSpikeFeature> ECHO_SPIKE =
        FEATURES.register("echo_spike",
            () -> new EchoSpikeFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
