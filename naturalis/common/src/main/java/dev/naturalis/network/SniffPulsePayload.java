package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Fires client sniff camera + scent vision when the player actively sniffs. */
public record SniffPulsePayload(int intensity, int trailCount, int preyCount, int hostileCount) implements CustomPacketPayload {

    public static final Type<SniffPulsePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "sniff_pulse"));

    public static final StreamCodec<ByteBuf, SniffPulsePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SniffPulsePayload::intensity,
        ByteBufCodecs.VAR_INT, SniffPulsePayload::trailCount,
        ByteBufCodecs.VAR_INT, SniffPulsePayload::preyCount,
        ByteBufCodecs.VAR_INT, SniffPulsePayload::hostileCount,
        SniffPulsePayload::new
    );

    @Override
    public Type<SniffPulsePayload> type() {
        return TYPE;
    }
}
