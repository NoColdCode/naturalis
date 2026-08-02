package dev.naturalis.fabric;

import dev.naturalis.client.ExperienceModeClientCache;
import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.client.MorphLevelClientCache;
import dev.naturalis.client.NaturalisClientPrefs;
import dev.naturalis.client.RuleFlagsClientCache;
import dev.naturalis.client.ScentTrailClient;
import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.client.SurvivalAsTraitsClientPending;
import dev.naturalis.experience.NaturalisExperienceRuntime;
import dev.naturalis.fabric.blockentity.MorphBeaconFabricBlockEntity;
import dev.naturalis.gameplay.FeralCurlSleepSystem;
import dev.naturalis.gameplay.PrimalMovementState;
import dev.naturalis.knowledge.KnowledgeClientSync;
import dev.naturalis.network.ClientSoundPrefsPayload;
import dev.naturalis.network.CurlSleepTogglePayload;
import dev.naturalis.network.ExperienceModePayload;
import dev.naturalis.network.HumanityPayload;
import dev.naturalis.network.MorphLevelPayload;
import dev.naturalis.network.MorphMovementKeyPayload;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.network.RuleFlagsPayload;
import dev.naturalis.network.ScentHintPayload;
import dev.naturalis.network.SetExperienceModePayload;
import dev.naturalis.network.SurvivalAsLockPayload;
import dev.naturalis.network.SurvivalAsTraitsPayload;
import dev.naturalis.network.SniffPulsePayload;
import dev.naturalis.network.ListenPulsePayload;
import dev.naturalis.network.PeckPulsePayload;
import dev.naturalis.client.perception.MorphSniffClientState;
import dev.naturalis.client.perception.MorphListenClientState;
import dev.naturalis.client.perception.MorphPeckClientState;
import dev.naturalis.network.MorphQuickSlotAssignPayload;
import dev.naturalis.network.MorphQuickSlotPayload;
import dev.naturalis.network.MorphQuickSlotResyncPayload;
import dev.naturalis.network.MorphQuickSlotSelectPayload;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.client.MorphQuickSlotClientState;
import dev.naturalis.network.SetBeaconMorphPayload;
import dev.naturalis.util.CurrentMorphUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Mirrors NeoForge payload wiring from {@link dev.naturalis.Naturalis}. */
public final class FabricNetworkBootstrap {

    private static volatile boolean payloadTypesRegistered;

    private FabricNetworkBootstrap() {
    }

    public static void registerPayloadTypes() {
        if (payloadTypesRegistered) {
            return;
        }
        synchronized (FabricNetworkBootstrap.class) {
            if (payloadTypesRegistered) {
                return;
            }
            doRegisterPayloadTypes();
            payloadTypesRegistered = true;
        }
    }

    private static void doRegisterPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(MorphLevelPayload.TYPE, MorphLevelPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ScentHintPayload.TYPE, ScentHintPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SniffPulsePayload.TYPE, SniffPulsePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ListenPulsePayload.TYPE, ListenPulsePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PeckPulsePayload.TYPE, PeckPulsePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HumanityPayload.TYPE, HumanityPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RuleFlagsPayload.TYPE, RuleFlagsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientSoundPrefsPayload.TYPE, ClientSoundPrefsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ExperienceModePayload.TYPE, ExperienceModePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SurvivalAsLockPayload.TYPE, SurvivalAsLockPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SurvivalAsTraitsPayload.TYPE, SurvivalAsTraitsPayload.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(MorphQuickSlotPayload.TYPE, MorphQuickSlotPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(CurlSleepTogglePayload.TYPE, CurlSleepTogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MorphQuickSlotSelectPayload.TYPE, MorphQuickSlotSelectPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MorphQuickSlotAssignPayload.TYPE, MorphQuickSlotAssignPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MorphQuickSlotResyncPayload.TYPE, MorphQuickSlotResyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MorphMovementKeyPayload.TYPE, MorphMovementKeyPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetBeaconMorphPayload.TYPE, SetBeaconMorphPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetExperienceModePayload.TYPE, SetExperienceModePayload.STREAM_CODEC);
    }

    public static void registerServerHandlers() {
        PlayToClientSender.register((player, payload) -> {
            if (payload instanceof CustomPacketPayload cpp && player instanceof ServerPlayer sp) {
                ServerPlayNetworking.send(sp, cpp);
            }
        });

        KnowledgeClientSync.register(player -> {
            var morphId = CurrentMorphUtil.getCurrentMorphId(player);
            if (morphId == null) {
                PlayToClientSender.send(player, new MorphLevelPayload(0, 3, false));
            } else {
                int morphLevel = dev.naturalis.knowledge.MorphKnowledgeManager.getLevel(player, morphId);
                int slots = dev.naturalis.knowledge.MorphKnowledgeManager.getAllowedHotbarSlots(player, morphId);
                boolean inventoryUnlocked = dev.naturalis.knowledge.MorphKnowledgeManager.canOpenInventory(player, morphId);
                int utilitiesRank = dev.naturalis.knowledge.MorphKnowledgeManager.getUtilitiesRank(player, morphId);
                PlayToClientSender.send(player, new MorphLevelPayload(morphLevel, slots, inventoryUnlocked, utilitiesRank));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(CurlSleepTogglePayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                FeralCurlSleepSystem.handleToggleRequest(player);
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(MorphMovementKeyPayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PrimalMovementState.setPrimalKeyDown(player, payload.pressed());
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(MorphQuickSlotSelectPayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MorphQuickSlotBridge.handleSelect(player, payload.slotIndex(), payload.morphId());
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(MorphQuickSlotAssignPayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MorphQuickSlotBridge.handleAssign(player, payload.slotIndex(), payload.morphId());
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(MorphQuickSlotResyncPayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MorphQuickSlotBridge.sync(player);
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(SetBeaconMorphPayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var level = player.serverLevel();
                if (level.getBlockEntity(payload.pos()) instanceof MorphBeaconFabricBlockEntity be) {
                    be.setTargetMorphId(payload.morphId());
                    if (payload.targetMode() >= 0) {
                        be.setTargetMode(payload.targetMode());
                    }
                }
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(SetExperienceModePayload.TYPE, (payload, context) -> context.player().getServer().execute(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NaturalisExperienceRuntime.handleSetExperiencePayload(payload, player);
            }
        }));
    }

    @Environment(EnvType.CLIENT)
    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(MorphLevelPayload.TYPE, (payload, context) ->
            context.client().execute(() -> MorphLevelClientCache.setState(
                payload.level(),
                payload.hotbarSlots(),
                payload.inventoryUnlocked(),
                payload.utilitiesRank()
            )));
        ClientPlayNetworking.registerGlobalReceiver(ScentHintPayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                ScentTrailClient.pushHint(payload, payload.strength() >= 5)));
        ClientPlayNetworking.registerGlobalReceiver(SniffPulsePayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                MorphSniffClientState.pulse(
                    payload.intensity(),
                    payload.trailCount(),
                    payload.preyCount(),
                    payload.hostileCount())));
        ClientPlayNetworking.registerGlobalReceiver(ListenPulsePayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                MorphListenClientState.pulse(
                    payload.bearingTimesTen() / 10.0D,
                    payload.lockedOn(),
                    payload.category(),
                    payload.distanceBlocks(),
                    payload.entityId())));
        ClientPlayNetworking.registerGlobalReceiver(PeckPulsePayload.TYPE, (payload, context) ->
            context.client().execute(() ->
                MorphPeckClientState.pulse(payload.struckEntity(), payload.struckBlock())));
        ClientPlayNetworking.registerGlobalReceiver(HumanityPayload.TYPE, (payload, context) ->
            context.client().execute(() -> HumanityClientCache.set(payload.humanity(), payload.active())));
        ClientPlayNetworking.registerGlobalReceiver(RuleFlagsPayload.TYPE, (payload, context) ->
            context.client().execute(() -> RuleFlagsClientCache.set(
                payload.colorFilterEnabled(),
                payload.inventoryRestrictionEnabled(),
                payload.instinctsEnabled())));
        ClientPlayNetworking.registerGlobalReceiver(ClientSoundPrefsPayload.TYPE, (payload, context) ->
            context.client().execute(() -> NaturalisClientPrefs.setMuteMorphPerceptionSounds(payload.muteMorphPerceptionSounds())));
        ClientPlayNetworking.registerGlobalReceiver(ExperienceModePayload.TYPE, (payload, context) ->
            context.client().execute(() -> ExperienceModeClientCache.set(payload.mode(), payload.showPrompt())));
        ClientPlayNetworking.registerGlobalReceiver(SurvivalAsLockPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                SurvivalAsClientCache.setLocked(payload.locked());
                if (payload.locked()) {
                    ExperienceModeClientCache.clearPromptPending();
                }
            }));
        ClientPlayNetworking.registerGlobalReceiver(MorphQuickSlotPayload.TYPE, (payload, context) ->
            context.client().execute(() -> MorphQuickSlotClientState.set(payload.unlockedSlots(), payload.slots(), payload.globalXp())));
        ClientPlayNetworking.registerGlobalReceiver(SurvivalAsTraitsPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                SurvivalAsClientCache.setLocked(true);
                ExperienceModeClientCache.clearPromptPending();
                SurvivalAsTraitsClientPending.queue(
                    payload.morphId(),
                    payload.mass(),
                    payload.dietId(),
                    payload.traitIds(),
                    payload.traitExtras()
                );
            }));
    }

    public static void ensureClientRegistered() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            registerClientHandlers();
        }
    }
}
