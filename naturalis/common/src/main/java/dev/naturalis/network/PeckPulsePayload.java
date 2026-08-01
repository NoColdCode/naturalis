package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client beak lunge when the player pecks. */
public record PeckPulsePayload(boolean struckEntity, boolean struckBlock) implements CustomPacketPayload {

    public static final Type<PeckPulsePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "peck_pulse"));

    public static final StreamCodec<ByteBuf, PeckPulsePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, PeckPulsePayload::struckEntity,
        ByteBufCodecs.BOOL, PeckPulsePayload::struckBlock,
        PeckPulsePayload::new
    );

    @Override
    public Type<PeckPulsePayload> type() {
        return TYPE;
    }
}
