package dev.naturalis.network;

import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class MorphQuickSlotPayload {

    private final int unlockedSlots;
    private final int globalXp;
    private final List<ResourceLocation> slots;

    public MorphQuickSlotPayload(int unlockedSlots, int globalXp, List<ResourceLocation> slots) {
        this.unlockedSlots = unlockedSlots;
        this.globalXp = Math.max(0, globalXp);
        this.slots = normalize(slots);
    }

    public static MorphQuickSlotPayload decode(FriendlyByteBuf buf) {
        int unlocked = buf.readVarInt();
        int globalXp = buf.readVarInt();
        List<ResourceLocation> slots = new ArrayList<>(MorphQuickSlotCategory.SLOT_COUNT);
        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            String raw = buf.readUtf();
            slots.add(raw.isEmpty() ? null : ResourceLocation.tryParse(raw));
        }
        return new MorphQuickSlotPayload(unlocked, globalXp, slots);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(unlockedSlots);
        buf.writeVarInt(globalXp);
        for (ResourceLocation slot : slots) {
            buf.writeUtf(slot == null ? "" : slot.toString());
        }
    }

    public int unlockedSlots() {
        return unlockedSlots;
    }

    public int globalXp() {
        return globalXp;
    }

    public List<ResourceLocation> slots() {
        return slots;
    }

    private static List<ResourceLocation> normalize(List<ResourceLocation> slots) {
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
}
