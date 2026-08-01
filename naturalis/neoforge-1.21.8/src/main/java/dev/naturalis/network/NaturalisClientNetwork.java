package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import dev.naturalis.client.ExperienceModeClientCache;
import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.client.MorphLevelClientCache;
import dev.naturalis.client.MorphQuickSlotClientState;
import dev.naturalis.client.NaturalisClientPrefs;
import dev.naturalis.client.RuleFlagsClientCache;
import dev.naturalis.client.ScentTrailClient;
import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.client.SurvivalAsTraitsClientPending;
import dev.naturalis.client.perception.MorphListenClientState;
import dev.naturalis.client.perception.MorphPeckClientState;
import dev.naturalis.client.perception.MorphSniffClientState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Client-only packet handlers. Kept separate from {@link dev.naturalis.Naturalis} so dedicated
 * servers never load client classes when registering play-to-client payloads.
 */
@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class NaturalisClientNetwork {

    private NaturalisClientNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NaturalisMod.ID);
        registrar.playToClient(
            MorphLevelPayload.TYPE,
            MorphLevelPayload.STREAM_CODEC,
            (payload, ctx) -> MorphLevelClientCache.setState(
                payload.level(),
                payload.hotbarSlots(),
                payload.inventoryUnlocked(),
                payload.utilitiesRank()
            )
        );
        registrar.playToClient(
            ScentHintPayload.TYPE,
            ScentHintPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                ScentTrailClient.pushHint(payload, payload.strength() >= 5))
        );
        registrar.playToClient(
            SniffPulsePayload.TYPE,
            SniffPulsePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                MorphSniffClientState.pulse(
                    payload.intensity(),
                    payload.trailCount(),
                    payload.preyCount(),
                    payload.hostileCount()))
        );
        registrar.playToClient(
            ListenPulsePayload.TYPE,
            ListenPulsePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                MorphListenClientState.pulse(
                    payload.bearingTimesTen() / 10.0D,
                    payload.lockedOn(),
                    payload.category(),
                    payload.distanceBlocks(),
                    payload.entityId()))
        );
        registrar.playToClient(
            PeckPulsePayload.TYPE,
            PeckPulsePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                MorphPeckClientState.pulse(payload.struckEntity(), payload.struckBlock()))
        );
        registrar.playToClient(
            HumanityPayload.TYPE,
            HumanityPayload.STREAM_CODEC,
            (payload, ctx) -> HumanityClientCache.set(payload.humanity(), payload.active())
        );
        registrar.playToClient(
            RuleFlagsPayload.TYPE,
            RuleFlagsPayload.STREAM_CODEC,
            (payload, ctx) -> RuleFlagsClientCache.set(
                payload.colorFilterEnabled(),
                payload.inventoryRestrictionEnabled(),
                payload.instinctsEnabled())
        );
        registrar.playToClient(
            ClientSoundPrefsPayload.TYPE,
            ClientSoundPrefsPayload.STREAM_CODEC,
            (payload, ctx) -> NaturalisClientPrefs.setMuteMorphPerceptionSounds(payload.muteMorphPerceptionSounds())
        );
        registrar.playToClient(
            ExperienceModePayload.TYPE,
            ExperienceModePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                ExperienceModeClientCache.set(payload.mode(), payload.showPrompt()))
        );
        registrar.playToClient(
            MorphQuickSlotPayload.TYPE,
            MorphQuickSlotPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                MorphQuickSlotClientState.set(payload.unlockedSlots(), payload.slots(), payload.globalXp()))
        );
        registrar.playToClient(
            SurvivalAsLockPayload.TYPE,
            SurvivalAsLockPayload.STREAM_CODEC,
            (payload, ctx) -> {
                SurvivalAsClientCache.setLocked(payload.locked());
                if (payload.locked()) {
                    ExperienceModeClientCache.clearPromptPending();
                }
            }
        );
        registrar.playToClient(
            SurvivalAsTraitsPayload.TYPE,
            SurvivalAsTraitsPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                SurvivalAsClientCache.setLocked(true);
                ExperienceModeClientCache.clearPromptPending();
                // Queue — don't setScreen here or Remorphed/experience can overwrite it.
                SurvivalAsTraitsClientPending.queue(
                    payload.morphId(),
                    payload.mass(),
                    payload.dietId(),
                    payload.traitIds(),
                    payload.traitExtras()
                );
            })
        );
    }
}
