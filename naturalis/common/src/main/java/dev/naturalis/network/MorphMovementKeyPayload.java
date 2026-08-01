package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorphMovementKeyPayload(boolean pressed) implements CustomPacketPayload {

    public static final Type<MorphMovementKeyPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_movement_key"));

    public static final StreamCodec<ByteBuf, MorphMovementKeyPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, MorphMovementKeyPayload::pressed,
        MorphMovementKeyPayload::new
    );

    @Override
    public Type<MorphMovementKeyPayload> type() {
        return TYPE;
    }
}
