package dev.naturalis.fabric;

import dev.naturalis.command.MorphCommandBridge;
import dev.naturalis.effect.BrewedMorphBridge;
import dev.naturalis.item.MorphArmorEvents;
import dev.naturalis.resonance.ResonanceCurlBridge;
import dev.naturalis.loader.NaturalisRuntime;
import dev.naturalis.rule.NaturalisGameRules;
import dev.naturalis.worldgen.VanillaRegistrations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class NaturalisFabricEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        NaturalisRuntime.setConfigDirectory(() -> FabricLoader.getInstance().getConfigDir());
        NaturalisGameRules.init();
        VanillaRegistrations.register();
        FabricNetworkBootstrap.registerPayloadTypes();
        FabricNaturalisEntityTypes.register();
        FabricNaturalisBlockEntities.register();
        FabricNaturalisMenus.register();
        FabricNaturalisItems.register();
        FabricMorphEffects.register();
        MorphArmorEvents.register();
        BrewedMorphBridge.register(FabricMorphEffects::applyBrewedMorph);
        MorphCommandBridge.installFabric(FabricMorphEffects::applyBrewedMorph);
        ResonanceCurlBridge.register(p -> false);
        FabricMorphCommand.register();
        FabricNetworkBootstrap.registerServerHandlers();
        FabricNaturalisServerHooks.register();
        FabricGameplayHooks.register();
    }
}
