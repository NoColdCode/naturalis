package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client → server: player chose realistic or softened for this world. */
public record SetExperienceModePayload(byte modeId)
    implements CustomPacketPayload {

    public static final Type<SetExperienceModePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "set_experience_mode"));

    public static final StreamCodec<ByteBuf, SetExperienceModePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE, SetExperienceModePayload::modeId,
        SetExperienceModePayload::new
    );

    @Override
    public Type<SetExperienceModePayload> type() {
        return TYPE;
    }
}
