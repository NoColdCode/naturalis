package dev.naturalis.network;

import dev.naturalis.Naturalis;
import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.client.NaturalisClientPrefs;
import dev.naturalis.client.MorphLevelClientCache;
import dev.naturalis.client.RuleFlagsClientCache;
import dev.naturalis.client.MorphQuickSlotClientState;
import dev.naturalis.client.ScentTrailClient;
import dev.naturalis.gameplay.FeralCurlSleepSystem;
import dev.naturalis.gameplay.NaturalisGameplayEvents;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.morph.quickslot.MorphQuickSlotDebug;
import dev.naturalis.morph.quickslot.MorphQuickSlotManager;
import dev.naturalis.world.MorphBeaconBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Forge 1.20.1 SimpleChannel network layer — replaces the NeoForge
 * {@code RegisterPayloadHandlersEvent} / {@code CustomPacketPayload} pattern.
 */
public final class NaturalisNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Naturalis.MOD_ID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );

    private static int nextId = 0;

    private NaturalisNetwork() {
    }

    public static void register() {
        // Register quick-slot handlers before wiring packet consumers.
        MorphQuickSlotManager.init();
        // ── Server → Client ──────────────────────────────────────────────────────
        CHANNEL.messageBuilder(MorphLevelPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MorphLevelPayload::encode)
            .decoder(MorphLevelPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    MorphLevelClientCache.setState(
                        msg.level(),
                        msg.hotbarSlots(),
                        msg.inventoryUnlocked(),
                        msg.utilitiesRank(),
                        msg.globalXp()
                    )
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(HumanityPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HumanityPayload::encode)
            .decoder(HumanityPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    HumanityClientCache.set(msg.humanity(), msg.active())
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(RuleFlagsPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(RuleFlagsPayload::encode)
            .decoder(RuleFlagsPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    RuleFlagsClientCache.set(
                        msg.colorFilterEnabled(),
                        msg.inventoryRestrictionEnabled(),
                        msg.instinctsEnabled()
                    )
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(ClientSoundPrefsPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((ClientSoundPrefsPayload msg, net.minecraft.network.FriendlyByteBuf buf) -> buf.writeBoolean(msg.muteMorphPerceptionSounds()))
            .decoder(buf -> new ClientSoundPrefsPayload(buf.readBoolean()))
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    NaturalisClientPrefs.setMuteMorphPerceptionSounds(msg.muteMorphPerceptionSounds())
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(ScentHintPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ScentHintPayload::encode)
            .decoder(ScentHintPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ScentTrailClient.pushHint(msg.entityId(), msg.category(), msg.strength(), msg.strength() >= 5)
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(SniffPulsePayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SniffPulsePayload::encode)
            .decoder(SniffPulsePayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    dev.naturalis.client.perception.MorphSniffClientState.pulse(
                        msg.intensity(),
                        msg.trailCount(),
                        msg.preyCount(),
                        msg.hostileCount()
                    )
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(WanderLookPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(WanderLookPayload::encode)
            .decoder(WanderLookPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    dev.naturalis.client.instinct.WanderLookClientState.applyPayload(msg)
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(PeckPulsePayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(PeckPulsePayload::encode)
            .decoder(PeckPulsePayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    dev.naturalis.client.perception.MorphPeckClientState.pulse(msg.struckEntity(), msg.struckBlock())
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(MorphQuickSlotPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MorphQuickSlotPayload::encode)
            .decoder(MorphQuickSlotPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    MorphQuickSlotClientState.set(msg.unlockedSlots(), msg.slots(), msg.globalXp())
                );
                ctx.get().setPacketHandled(true);
            })
            .add();

        // ── Client → Server ──────────────────────────────────────────────────────
        CHANNEL.messageBuilder(CurlSleepTogglePayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CurlSleepTogglePayload::encode)
            .decoder(CurlSleepTogglePayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    FeralCurlSleepSystem.handleToggleRequest(player);
                }
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(MorphMovementKeyPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MorphMovementKeyPayload::encode)
            .decoder(MorphMovementKeyPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    NaturalisGameplayEvents.setPrimalMovementKey(player, msg.pressed());
                }
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(MorphQuickSlotSelectPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MorphQuickSlotSelectPayload::encode)
            .decoder(MorphQuickSlotSelectPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    MorphQuickSlotDebug.event(
                        "server",
                        "select packet slot=" + msg.slotIndex() + " morph=" + msg.morphId()
                    );
                    MorphQuickSlotManager.handleSelection(player, msg.slotIndex(), msg.morphId());
                }
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(MorphQuickSlotAssignPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MorphQuickSlotAssignPayload::encode)
            .decoder(MorphQuickSlotAssignPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && msg.morphId() != null) {
                    MorphQuickSlotManager.assignFromGui(player, msg.slotIndex(), msg.morphId());
                }
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(MorphQuickSlotResyncPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MorphQuickSlotResyncPayload::encode)
            .decoder(MorphQuickSlotResyncPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    MorphQuickSlotManager.syncToClient(player);
                }
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(MorphQuickSlotSessionPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(MorphQuickSlotSessionPayload::encode)
            .decoder(MorphQuickSlotSessionPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    MorphQuickSlotBridge.setSessionActive(player, msg.active());
                }
                ctx.get().setPacketHandled(true);
            })
            .add();

        CHANNEL.messageBuilder(SetBeaconMorphPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SetBeaconMorphPayload::encode)
            .decoder(SetBeaconMorphPayload::decode)
            .consumerMainThread((msg, ctx) -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.level() instanceof ServerLevel level) {
                    if (level.getBlockEntity(msg.pos()) instanceof MorphBeaconBlockEntity be) {
                        if (msg.targetMode() >= 0
                            && msg.targetMode() < MorphBeaconBlockEntity.TargetMode.values().length) {
                            be.setTargetMode(msg.targetMode());
                        }
                        be.setTargetMorphId(msg.morphId());
                    }
                }
                ctx.get().setPacketHandled(true);
            })
            .add();
    }

    /** Send a server-bound message to the given player. */
    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
