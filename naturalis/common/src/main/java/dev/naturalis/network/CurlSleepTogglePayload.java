package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CurlSleepTogglePayload() implements CustomPacketPayload {

    public static final Type<CurlSleepTogglePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "curl_sleep_toggle"));

    public static final StreamCodec<ByteBuf, CurlSleepTogglePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
        },
        buf -> new CurlSleepTogglePayload()
    );

    @Override
    public Type<CurlSleepTogglePayload> type() {
        return TYPE;
    }
}