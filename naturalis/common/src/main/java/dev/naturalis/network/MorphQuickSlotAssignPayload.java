package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorphQuickSlotAssignPayload(int slotIndex, ResourceLocation morphId) implements CustomPacketPayload {

    public static final Type<MorphQuickSlotAssignPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_quick_slot_assign"));

    public static final StreamCodec<ByteBuf, MorphQuickSlotAssignPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MorphQuickSlotAssignPayload::slotIndex,
        ResourceLocation.STREAM_CODEC, MorphQuickSlotAssignPayload::morphId,
        MorphQuickSlotAssignPayload::new
    );

    @Override
    public Type<MorphQuickSlotAssignPayload> type() {
        return TYPE;
    }
}
