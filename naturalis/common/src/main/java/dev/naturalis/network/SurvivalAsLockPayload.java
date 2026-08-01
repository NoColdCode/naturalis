package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Syncs whether this world is Survival-as locked (no Morph Binding effect involved). */
public record SurvivalAsLockPayload(boolean locked) implements CustomPacketPayload {

    public static final Type<SurvivalAsLockPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "survival_as_lock"));

    public static final StreamCodec<ByteBuf, SurvivalAsLockPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, SurvivalAsLockPayload::locked,
        SurvivalAsLockPayload::new
    );

    @Override
    public Type<SurvivalAsLockPayload> type() {
        return TYPE;
    }
}
