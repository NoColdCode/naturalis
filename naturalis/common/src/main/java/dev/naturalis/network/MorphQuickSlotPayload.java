package dev.naturalis.network;

import dev.naturalis.NaturalisMod;
import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MorphQuickSlotPayload(
    int unlockedSlots,
    int globalXp,
    List<ResourceLocation> slots
) implements CustomPacketPayload {

    public static final Type<MorphQuickSlotPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "morph_quick_slots"));

    public static final StreamCodec<ByteBuf, MorphQuickSlotPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> encode(payload, buf),
        MorphQuickSlotPayload::decode
    );

    public MorphQuickSlotPayload {
        slots = normalizedSlots(slots);
    }

    private static void encode(MorphQuickSlotPayload payload, ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, payload.unlockedSlots());
        ByteBufCodecs.VAR_INT.encode(buf, payload.globalXp());
        for (ResourceLocation slot : payload.slots()) {
            ByteBufCodecs.STRING_UTF8.encode(buf, slot == null ? "" : slot.toString());
        }
    }

    private static MorphQuickSlotPayload decode(ByteBuf buf) {
        int unlocked = ByteBufCodecs.VAR_INT.decode(buf);
        int globalXp = ByteBufCodecs.VAR_INT.decode(buf);
        List<ResourceLocation> slots = new ArrayList<>(MorphQuickSlotCategory.SLOT_COUNT);
        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            String raw = ByteBufCodecs.STRING_UTF8.decode(buf);
            slots.add(raw.isEmpty() ? null : ResourceLocation.parse(raw));
        }
        return new MorphQuickSlotPayload(unlocked, globalXp, slots);
    }

    private static List<ResourceLocation> normalizedSlots(List<ResourceLocation> slots) {
        List<ResourceLocation> out = new ArrayList<>(MorphQuickSlotCategory.SLOT_COUNT);
        if (slots == null) {
            for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
                out.add(null);
            }
            return out;
        }
        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            out.add(i < slots.size() ? slots.get(i) : null);
        }
        return out;
    }

    @Override
    public Type<MorphQuickSlotPayload> type() {
        return TYPE;
    }
}
