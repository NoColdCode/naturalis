package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientSoundPrefsPayload(boolean muteMorphPerceptionSounds) implements CustomPacketPayload {

    public static final Type<ClientSoundPrefsPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "client_sound_prefs"));

    public static final StreamCodec<ByteBuf, ClientSoundPrefsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ClientSoundPrefsPayload::muteMorphPerceptionSounds,
        ClientSoundPrefsPayload::new
    );

    @Override
    public Type<ClientSoundPrefsPayload> type() {
        return TYPE;
    }
}
