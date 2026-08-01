package dev.naturalis;

import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisBlocks;
import dev.naturalis.content.NaturalisCreativeTabs;
import dev.naturalis.content.NaturalisEntityTypes;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMenus;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.command.MorphCommandBridge;
import dev.naturalis.diet.DietEvents;
import dev.naturalis.effect.BrewedMorphBrewingEvents;
import dev.naturalis.resonance.ResonanceCurlBridge;
import dev.naturalis.resonance.ResonanceEvents;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.gameplay.NaturalisGameplayEvents;
import dev.naturalis.item.MorphArmorEvents;
import dev.naturalis.knowledge.KnowledgeClientSync;
import dev.naturalis.instinct.WanderLookSync;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.network.MorphLevelPayload;
import dev.naturalis.network.NaturalisNetwork;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.worldgen.WorldgenRegistration;
import dev.naturalis.rule.NaturalisGameRules;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Naturalis.MOD_ID)
public class Naturalis {

    public static final String MOD_ID = "naturalis";

    public Naturalis() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        NaturalisGameRules.init();

        WorldgenRegistration.init(modEventBus);
        dev.naturalis.worldgen.NaturalisFeatures.register(modEventBus);

        NaturalisItems.register(modEventBus);
        NaturalisMobEffects.register(modEventBus);
        NaturalisBlocks.register(modEventBus);
        NaturalisBlockEntities.register(modEventBus);
        NaturalisMenus.register(modEventBus);
        NaturalisCreativeTabs.register(modEventBus);
        NaturalisEntityTypes.register(modEventBus);

        NaturalisGameplayEvents.register(modEventBus);
        BrewedMorphBrewingEvents.register(modEventBus);
        MorphEffectEvents.registerShapeGuards();
        MorphArmorEvents.register();
        MorphCommandBridge.installNeoForge(
            MorphEffectEvents::applyBrewedMorph,
            ResonanceEvents::onBondSet,
            p -> MorphCommandBridge.RebirthOutcome.valueOf(ResonanceEvents.triggerHumanRebirth(p).name()),
            p -> MorphCommandBridge.InstinctOutcome.valueOf(ResonanceEvents.triggerActiveInstinct(p).name()),
            DietEvents::debugDiet);
        ResonanceCurlBridge.register(ResonanceEvents::tryTriggerRebirthFromCurlKey);

        // Register the SimpleChannel and all message types.
        NaturalisNetwork.register();

        NaturalisConfig.register();

        KnowledgeClientSync.register(player -> {
            var morphId = CurrentMorphUtil.getCurrentMorphId(player);
            int globalXp = MorphKnowledgeManager.getEffectiveGlobalXp(player);
            if (morphId == null) {
                NaturalisNetwork.sendToPlayer(player, new MorphLevelPayload(0, 3, false, 0, globalXp));
            } else {
                int morphLevel = MorphKnowledgeManager.getLevel(player, morphId);
                int slots = MorphKnowledgeManager.getAllowedHotbarSlots(player, morphId);
                boolean inventoryUnlocked = MorphKnowledgeManager.canOpenInventory(player, morphId);
                int utilitiesRank = MorphKnowledgeManager.getUtilitiesRank(player, morphId);
                NaturalisNetwork.sendToPlayer(player, new MorphLevelPayload(morphLevel, slots, inventoryUnlocked, utilitiesRank, globalXp));
            }
        });

        PlayToClientSender.register(NaturalisNetwork::sendToPlayer);

        WanderLookSync.registerImmediateClientMirror(payload -> {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null || !server.isSingleplayer()) {
                return;
            }
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.minecraft.client.Minecraft.getInstance().execute(
                    () -> dev.naturalis.client.instinct.WanderLookClientState.applyPayload(payload)
                )
            );
        });

        // Pre-warm the island heightmap in a background thread so it is fully
        // loaded long before any player teleports to the natural dimension.
        Thread heightmapPrewarm = new Thread(
            dev.naturalis.worldgen.IslandHeightmap::getOrLoad,
            "naturalis-heightmap-prewarm"
        );
        heightmapPrewarm.setDaemon(true);
        heightmapPrewarm.start();
    }
}
