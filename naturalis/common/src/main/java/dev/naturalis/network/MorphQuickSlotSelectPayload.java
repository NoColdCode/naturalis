package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record MorphQuickSlotSelectPayload(int slotIndex, @Nullable ResourceLocation morphId) implements CustomPacketPayload {

    public MorphQuickSlotSelectPayload(int slotIndex) {
        this(slotIndex, null);
    }

    public static final Type<MorphQuickSlotSelectPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_quick_slot_select"));

    public static final StreamCodec<ByteBuf, MorphQuickSlotSelectPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.VAR_INT.encode(buf, payload.slotIndex());
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.morphId() == null ? "" : payload.morphId().toString());
        },
        buf -> {
            int slotIndex = ByteBufCodecs.VAR_INT.decode(buf);
            String raw = ByteBufCodecs.STRING_UTF8.decode(buf);
            ResourceLocation morphId = raw.isEmpty() ? null : ResourceLocation.parse(raw);
            return new MorphQuickSlotSelectPayload(slotIndex, morphId);
        }
    );

    @Override
    public Type<MorphQuickSlotSelectPayload> type() {
        return TYPE;
    }
}
