package dev.naturalis.client;

import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MorphQuickSlotClientState {

    private static int unlockedSlots = 1;
    private static int globalXp;
    private static List<ResourceLocation> slots = emptySlots();
    private static boolean wheelOpen;
    private static int hoveredSlot = -1;

    private MorphQuickSlotClientState() {
    }

    public static void set(int unlocked, List<ResourceLocation> resolved) {
        set(unlocked, resolved, 0);
    }

    public static void set(int unlocked, List<ResourceLocation> resolved, int syncedGlobalXp) {
        globalXp = Math.max(0, syncedGlobalXp);
        int fromXp = MorphQuickSlotCategory.unlockedCountForGlobalXp(globalXp);
        unlockedSlots = Math.max(
            Math.max(0, Math.min(MorphQuickSlotCategory.SLOT_COUNT, unlocked)),
            fromXp
        );
        slots = new ArrayList<>(MorphQuickSlotCategory.SLOT_COUNT);
        if (resolved == null) {
            slots.addAll(emptySlots());
            return;
        }
        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            slots.add(i < resolved.size() ? resolved.get(i) : null);
        }
    }

    /** Optimistic client update right after the assign screen sends to the server. */
    public static void assignLocal(int slotIndex, ResourceLocation morphId) {
        if (slotIndex < 0 || slotIndex >= MorphQuickSlotCategory.SLOT_COUNT || morphId == null) {
            return;
        }
        while (slots.size() < MorphQuickSlotCategory.SLOT_COUNT) {
            slots.add(null);
        }
        slots.set(slotIndex, morphId);
    }

    public static int unlockedSlots() {
        return unlockedSlots;
    }

    public static int globalXp() {
        return globalXp;
    }

    /** Prefer XP-derived unlock count from quick-slot sync or periodic level sync. */
    public static int effectiveUnlockedSlots() {
        int xp = Math.max(globalXp, dev.naturalis.client.MorphLevelClientCache.getGlobalXp());
        if (xp > 0) {
            return MorphQuickSlotCategory.unlockedCountForGlobalXp(xp);
        }
        return unlockedSlots;
    }

    public static int effectiveGlobalXp() {
        return Math.max(globalXp, dev.naturalis.client.MorphLevelClientCache.getGlobalXp());
    }

    public static List<ResourceLocation> slots() {
        return Collections.unmodifiableList(slots);
    }

    public static ResourceLocation slot(int index) {
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        return slots.get(index);
    }

    public static boolean isWheelOpen() {
        return wheelOpen;
    }

    public static void setWheelOpen(boolean open) {
        wheelOpen = open;
    }

    public static int hoveredSlot() {
        return hoveredSlot;
    }

    public static void setHoveredSlot(int slot) {
        hoveredSlot = slot;
    }

    private static List<ResourceLocation> emptySlots() {
        List<ResourceLocation> out = new ArrayList<>(MorphQuickSlotCategory.SLOT_COUNT);
        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            out.add(null);
        }
        return out;
    }
}
