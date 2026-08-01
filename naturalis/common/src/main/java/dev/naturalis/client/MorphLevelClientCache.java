package dev.naturalis.client;

/**
 * Client-side cache for the current morph knowledge level, synced from the
 * server every 20 ticks via MorphLevelPayload.
 */
public final class MorphLevelClientCache {

    private MorphLevelClientCache() {
    }

    private static volatile int currentLevel = 0;
    private static volatile int currentHotbarSlots = 3;
    private static volatile boolean currentInventoryUnlocked = false;
    private static volatile int currentUtilitiesRank = 0;
    private static volatile int currentGlobalXp = 0;

    public static int get() {
        return currentLevel;
    }

    public static void set(int level) {
        currentLevel = level;
    }

    public static void setState(int level, int hotbarSlots, boolean inventoryUnlocked, int utilitiesRank) {
        setState(level, hotbarSlots, inventoryUnlocked, utilitiesRank, currentGlobalXp);
    }

    public static void setState(int level, int hotbarSlots, boolean inventoryUnlocked, int utilitiesRank, int globalXp) {
        currentLevel = level;
        currentHotbarSlots = Math.max(1, Math.min(9, hotbarSlots));
        currentInventoryUnlocked = inventoryUnlocked;
        currentUtilitiesRank = Math.max(0, utilitiesRank);
        currentGlobalXp = Math.max(0, globalXp);
    }

    public static int getGlobalXp() {
        return currentGlobalXp;
    }

    public static int getHotbarSlots() {
        return currentHotbarSlots;
    }

    public static boolean isInventoryUnlocked() {
        return currentInventoryUnlocked;
    }

    public static int getUtilitiesRank() {
        return currentUtilitiesRank;
    }
}
