package dev.naturalis;

import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.config.NaturalisConfigEvents;
import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisBlocks;
import dev.naturalis.content.NaturalisCreativeTabs;
import dev.naturalis.content.NaturalisEntityTypes;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMenus;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.command.MorphCommandBridge;
import dev.naturalis.diet.DietEvents;
import dev.naturalis.effect.BrewedMorphBridge;
import dev.naturalis.resonance.ResonanceCurlBridge;
import dev.naturalis.resonance.ResonanceEvents;
import dev.naturalis.resonance.ResonanceLogic;
import dev.naturalis.effect.BrewedMorphBrewingEvents;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.util.ForceHumanBridge;
import dev.naturalis.util.MorphAcquisition;
import dev.naturalis.gameplay.NaturalisGameplayEvents;
import dev.naturalis.item.MorphArmorEvents;
import dev.naturalis.gameplay.FeralCurlSleepSystem;
import dev.naturalis.knowledge.KnowledgeClientSync;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.network.ClientSoundPrefsPayload;
import dev.naturalis.network.CurlSleepTogglePayload;
import dev.naturalis.network.ExperienceModePayload;
import dev.naturalis.network.HumanityPayload;
import dev.naturalis.network.ListenPulsePayload;
import dev.naturalis.network.MorphLevelPayload;
import dev.naturalis.network.MorphMovementKeyPayload;
import dev.naturalis.network.MorphQuickSlotAssignPayload;
import dev.naturalis.network.MorphQuickSlotPayload;
import dev.naturalis.network.MorphQuickSlotSelectPayload;
import dev.naturalis.network.MorphQuickSlotResyncPayload;
import dev.naturalis.network.PeckPulsePayload;
import dev.naturalis.network.RuleFlagsPayload;
import dev.naturalis.network.ScentHintPayload;
import dev.naturalis.network.SniffPulsePayload;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.network.SetBeaconMorphPayload;
import dev.naturalis.network.SetExperienceModePayload;
import dev.naturalis.network.SurvivalAsLockPayload;
import dev.naturalis.network.SurvivalAsTraitsPayload;
import dev.naturalis.experience.NaturalisExperienceRuntime;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.loader.NaturalisRuntime;
import dev.naturalis.world.MorphBeaconBlockEntity;
import dev.naturalis.rule.NaturalisGameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(NaturalisMod.ID)
public class Naturalis {

    public static final String MOD_ID = NaturalisMod.ID;

    public Naturalis(IEventBus modEventBus, ModContainer modContainer) {
        NaturalisRuntime.setConfigDirectory(() -> net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        NaturalisConfig.register(modContainer);
        modEventBus.addListener(NaturalisConfigEvents::onConfigLoad);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bootstrapClient(modContainer, modEventBus);
        }
        NaturalisGameRules.init();

        // Register island chunk-generator and biome-source codecs via DeferredRegister
        // so they are registered during RegisterEvent before the registries are frozen.
        dev.naturalis.worldgen.WorldgenRegistration.init(modEventBus);

        dev.naturalis.worldgen.NaturalisFeatures.register(modEventBus);

        NaturalisItems.register(modEventBus);
        NaturalisMobEffects.register(modEventBus);
        NaturalisBlocks.register(modEventBus);
        NaturalisBlockEntities.register(modEventBus);
        NaturalisMenus.register(modEventBus);
        NaturalisCreativeTabs.register(modEventBus);
        NaturalisEntityTypes.register(modEventBus);

        // Walkers trait integration must exist before the first TraitDataManager reload.
        try {
            Class.forName("dev.naturalis.compat.walkers.NaturalisWalkersTraits")
                .getMethod("ensureIntegrationRegistered")
                .invoke(null);
        } catch (Throwable ignored) {
        }

        NaturalisGameplayEvents.register(modEventBus);
        BrewedMorphBrewingEvents.register();
        MorphEffectEvents.registerShapeGuards();
        BrewedMorphBridge.register(MorphEffectEvents::applyBrewedMorph);
        ForceHumanBridge.register(MorphAcquisition::forceHuman);
        MorphCommandBridge.installNeoForge(
            MorphEffectEvents::applyBrewedMorph,
            ResonanceEvents::onBondSet,
            p -> MorphCommandBridge.RebirthOutcome.valueOf(ResonanceEvents.triggerHumanRebirth(p).name()),
            p -> MorphCommandBridge.InstinctOutcome.valueOf(ResonanceEvents.triggerActiveInstinct(p).name()),
            DietEvents::debugDiet);
        ResonanceCurlBridge.register(ResonanceLogic::tryTriggerRebirthFromCurlKey);
        MorphArmorEvents.register();

        // Pre-warm the island heightmap in a background thread so it is fully
        // loaded long before any player teleports to the natural dimension and
        // triggers chunk generation.  Without this, the first call from a
        // chunk-gen worker blocks every other gen thread on the synchronized
        // load() and can cause a visible freeze.
        Thread heightmapPrewarm = new Thread(
            dev.naturalis.worldgen.IslandHeightmap::getOrLoad,
            "naturalis-heightmap-prewarm"
        );
        heightmapPrewarm.setDaemon(true);
        heightmapPrewarm.start();

        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar(MOD_ID);
            if (FMLEnvironment.dist.isDedicatedServer()) {
                registerDedicatedServerPlayToClientTypes(registrar);
            }
            registrar.playToServer(
                SetExperienceModePayload.TYPE,
                SetExperienceModePayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> NaturalisExperienceRuntime.handleSetExperiencePayload(payload, player));
                    }
                }
            );
            registrar.playToServer(
                CurlSleepTogglePayload.TYPE,
                CurlSleepTogglePayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> FeralCurlSleepSystem.handleToggleRequest(player));
                    }
                }
            );
            registrar.playToServer(
                MorphQuickSlotSelectPayload.TYPE,
                MorphQuickSlotSelectPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> MorphQuickSlotBridge.handleSelect(player, payload.slotIndex(), payload.morphId()));
                    }
                }
            );
            registrar.playToServer(
                MorphQuickSlotAssignPayload.TYPE,
                MorphQuickSlotAssignPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> MorphQuickSlotBridge.handleAssign(player, payload.slotIndex(), payload.morphId()));
                    }
                }
            );
            registrar.playToServer(
                MorphQuickSlotResyncPayload.TYPE,
                MorphQuickSlotResyncPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> MorphQuickSlotBridge.sync(player));
                    }
                }
            );
            registrar.playToServer(
                MorphMovementKeyPayload.TYPE,
                MorphMovementKeyPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> NaturalisGameplayEvents.setPrimalMovementKey(player, payload.pressed()));
                    }
                }
            );
            registrar.playToServer(
                SetBeaconMorphPayload.TYPE,
                SetBeaconMorphPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ctx.enqueueWork(() -> {
                            var level = (net.minecraft.server.level.ServerLevel) player.level();
                            if (level.getBlockEntity(payload.pos()) instanceof MorphBeaconBlockEntity be) {
                                if (payload.targetMode() >= 0
                                    && payload.targetMode() < MorphBeaconBlockEntity.TargetMode.values().length) {
                                    be.setTargetMode(payload.targetMode());
                                }
                                be.setTargetMorphId(payload.morphId());
                            }
                        });
                    }
                }
            );
        });

        KnowledgeClientSync.register(player -> {
            var morphId = CurrentMorphUtil.getCurrentMorphId(player);
            if (morphId == null) {
                PlayToClientSender.send(player, new MorphLevelPayload(0, 3, false));
            } else {
                int morphLevel = MorphKnowledgeManager.getLevel(player, morphId);
                int slots = MorphKnowledgeManager.getAllowedHotbarSlots(player, morphId);
                boolean inventoryUnlocked = MorphKnowledgeManager.canOpenInventory(player, morphId);
                int utilitiesRank = MorphKnowledgeManager.getUtilitiesRank(player, morphId);
                PlayToClientSender.send(player, new MorphLevelPayload(morphLevel, slots, inventoryUnlocked, utilitiesRank));
            }
        });

        PlayToClientSender.register((player, payload) -> {
            if (payload instanceof CustomPacketPayload cpp) {
                PacketDistributor.sendToPlayer(player, cpp);
            }
        });

        // MorphQuickSlotManager registers server handlers in its static initializer.
        try {
            Class.forName("dev.naturalis.morph.quickslot.MorphQuickSlotManager");
        } catch (ClassNotFoundException ignored) {
            // Loaders without quick-slot server support (e.g. Fabric).
        }
    }

    /** Packet types the dedicated server sends to clients; handlers run only on the client. */
    private static void registerDedicatedServerPlayToClientTypes(PayloadRegistrar registrar) {
        registrar.playToClient(MorphLevelPayload.TYPE, MorphLevelPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ScentHintPayload.TYPE, ScentHintPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(SniffPulsePayload.TYPE, SniffPulsePayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ListenPulsePayload.TYPE, ListenPulsePayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(PeckPulsePayload.TYPE, PeckPulsePayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(HumanityPayload.TYPE, HumanityPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(RuleFlagsPayload.TYPE, RuleFlagsPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ClientSoundPrefsPayload.TYPE, ClientSoundPrefsPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ExperienceModePayload.TYPE, ExperienceModePayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(MorphQuickSlotPayload.TYPE, MorphQuickSlotPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(SurvivalAsTraitsPayload.TYPE, SurvivalAsTraitsPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(SurvivalAsLockPayload.TYPE, SurvivalAsLockPayload.STREAM_CODEC, (p, c) -> {});
    }

    /** Loaded via reflection so this class never references client types in its constant pool. */
    private static void bootstrapClient(ModContainer modContainer, IEventBus modEventBus) {
        try {
            Class<?> init = Class.forName("dev.naturalis.client.NaturalisClientInit");
            init.getMethod("register", ModContainer.class, IEventBus.class)
                .invoke(null, modContainer, modEventBus);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Naturalis client bootstrap failed", e);
        }
    }
}
