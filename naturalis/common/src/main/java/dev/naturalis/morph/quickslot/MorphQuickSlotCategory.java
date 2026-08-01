package dev.naturalis.morph.quickslot;

import net.minecraft.network.chat.Component;

public enum MorphQuickSlotCategory {
    GROUND("gui.naturalis.quick_slot.ground"),
    AERIAL("gui.naturalis.quick_slot.aerial"),
    AQUATIC("gui.naturalis.quick_slot.aquatic"),
    NETHER("gui.naturalis.quick_slot.nether"),
    HOSTILE("gui.naturalis.quick_slot.hostile"),
    HIGH_DAMAGE("gui.naturalis.quick_slot.high_damage");

    public static final int SLOT_COUNT = values().length;
    public static final int[] UNLOCK_XP = {0, 200, 1500, 8000, 20000, 80000};

    public static int unlockedCountForGlobalXp(int globalXp) {
        int count = 0;
        for (int threshold : UNLOCK_XP) {
            if (globalXp >= threshold) {
                count++;
            }
        }
        return count;
    }

    private final String translationKey;

    MorphQuickSlotCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public int index() {
        return ordinal();
    }

    public Component label() {
        return Component.translatable(translationKey);
    }

    public static MorphQuickSlotCategory byIndex(int index) {
        MorphQuickSlotCategory[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
