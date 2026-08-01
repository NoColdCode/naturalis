package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetBeaconMorphPayload(BlockPos pos, String morphId, int targetMode) implements CustomPacketPayload {

    public static final Type<SetBeaconMorphPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "set_beacon_morph"));

    public static final StreamCodec<ByteBuf, SetBeaconMorphPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SetBeaconMorphPayload::pos,
        ByteBufCodecs.STRING_UTF8, SetBeaconMorphPayload::morphId,
        ByteBufCodecs.VAR_INT, SetBeaconMorphPayload::targetMode,
        SetBeaconMorphPayload::new
    );

    @Override
    public Type<SetBeaconMorphPayload> type() {
        return TYPE;
    }
}
