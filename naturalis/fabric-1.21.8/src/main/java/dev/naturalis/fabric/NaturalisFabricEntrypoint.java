package dev.naturalis.fabric;

import dev.naturalis.command.MorphCommandBridge;
import dev.naturalis.effect.BrewedMorphBridge;
import dev.naturalis.item.MorphArmorEvents;
import dev.naturalis.resonance.ResonanceCurlBridge;
import dev.naturalis.resonance.ResonanceLogic;
import dev.naturalis.loader.NaturalisRuntime;
import dev.naturalis.rule.NaturalisGameRules;
import dev.naturalis.util.ForceHumanBridge;
import dev.naturalis.util.MorphAcquisition;
import dev.naturalis.worldgen.VanillaRegistrations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NaturalisFabricEntrypoint implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("naturalis");

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
        ResonanceCurlBridge.register(ResonanceLogic::tryTriggerRebirthFromCurlKey);
        ForceHumanBridge.register(MorphAcquisition::forceHuman);
        tryLoadMorphQuickSlotManager();
        tryRegisterWalkersTraits();
        FabricMorphCommand.register();
        FabricNetworkBootstrap.registerServerHandlers();
        FabricNaturalisServerHooks.register();
        FabricGameplayHooks.register();
        FabricGameplaySystemsHooks.register();
        FabricInventoryHooks.register();
        FabricSurvivalAsHooks.register();
    }

    private static void tryLoadMorphQuickSlotManager() {
        try {
            Class.forName("dev.naturalis.morph.quickslot.MorphQuickSlotManager");
        } catch (Throwable t) {
            LOGGER.warn("[Naturalis] MorphQuickSlotManager unavailable: {}", t.toString());
        }
    }

    private static void tryRegisterWalkersTraits() {
        // NaturalisWalkersTraits is NeoForge EventBusSubscriber + Walkers TraitRegistry —
        // excluded from Fabric compile. Attempt reflective register() if a compatible jar is present.
        try {
            Class<?> clazz = Class.forName("dev.naturalis.compat.walkers.NaturalisWalkersTraits");
            clazz.getMethod("ensureIntegrationRegistered").invoke(null);
            clazz.getMethod("register").invoke(null);
            LOGGER.info("[Naturalis] Walkers traits registered reflectively.");
        } catch (ClassNotFoundException e) {
            LOGGER.info("[Naturalis] Walkers traits skipped (NaturalisWalkersTraits not on classpath — Fabric compile excludes NeoForge Walkers trait glue).");
        } catch (Throwable t) {
            LOGGER.warn("[Naturalis] Walkers traits register failed: {}", t.toString());
        }
    }
}
