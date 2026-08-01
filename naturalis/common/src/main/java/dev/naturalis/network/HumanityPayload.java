package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HumanityPayload(int humanity, boolean active) implements CustomPacketPayload {

    public static final Type<HumanityPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "humanity"));

    public static final StreamCodec<ByteBuf, HumanityPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, HumanityPayload::humanity,
        ByteBufCodecs.BOOL, HumanityPayload::active,
        HumanityPayload::new
    );

    @Override
    public Type<HumanityPayload> type() {
        return TYPE;
    }
}
