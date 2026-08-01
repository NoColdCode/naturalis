package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client → Server: refresh quick-slot assignments before the wheel opens. */
public record MorphQuickSlotResyncPayload() implements CustomPacketPayload {

    public static final Type<MorphQuickSlotResyncPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_quick_slot_resync"));

    public static final StreamCodec<ByteBuf, MorphQuickSlotResyncPayload> STREAM_CODEC =
        StreamCodec.unit(new MorphQuickSlotResyncPayload());

    @Override
    public Type<MorphQuickSlotResyncPayload> type() {
        return TYPE;
    }
}
