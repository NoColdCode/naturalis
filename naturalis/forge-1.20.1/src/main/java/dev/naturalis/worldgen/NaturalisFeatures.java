package dev.naturalis.worldgen;

import dev.naturalis.Naturalis;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge 1.20.1 version — uses {@code RegistryObject} instead of {@code DeferredHolder}.
 */
public final class NaturalisFeatures {

    private NaturalisFeatures() {}

    private static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, Naturalis.MOD_ID);

    public static final RegistryObject<EchoSpikeFeature> ECHO_SPIKE =
        FEATURES.register("echo_spike",
            () -> new EchoSpikeFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
