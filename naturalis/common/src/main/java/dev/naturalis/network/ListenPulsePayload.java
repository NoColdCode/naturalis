package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Active listen focus: bearing, category tint, and optional tracked entity. */
public record ListenPulsePayload(
    int bearingTimesTen,
    boolean lockedOn,
    byte category,
    int distanceBlocks,
    int entityId
) implements CustomPacketPayload {

    public static final Type<ListenPulsePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "listen_pulse"));

    public static final StreamCodec<ByteBuf, ListenPulsePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ListenPulsePayload::bearingTimesTen,
        ByteBufCodecs.BOOL, ListenPulsePayload::lockedOn,
        ByteBufCodecs.BYTE, ListenPulsePayload::category,
        ByteBufCodecs.VAR_INT, ListenPulsePayload::distanceBlocks,
        ByteBufCodecs.VAR_INT, ListenPulsePayload::entityId,
        ListenPulsePayload::new
    );

    @Override
    public Type<ListenPulsePayload> type() {
        return TYPE;
    }
}
