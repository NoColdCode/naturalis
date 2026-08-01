package dev.naturalis.client;

public final class RuleFlagsClientCache {

    private static boolean colorFilterEnabled = true;
    private static boolean inventoryRestrictionEnabled = true;
    private static boolean instinctsEnabled = true;

    private RuleFlagsClientCache() {
    }

    public static boolean isColorFilterEnabled() {
        return colorFilterEnabled;
    }

    public static boolean isInventoryRestrictionEnabled() {
        return inventoryRestrictionEnabled;
    }

    public static boolean isInstinctsEnabled() {
        return instinctsEnabled;
    }

    public static void set(boolean colorFilter, boolean inventoryRestriction, boolean instincts) {
        colorFilterEnabled = colorFilter;
        inventoryRestrictionEnabled = inventoryRestriction;
        instinctsEnabled = instincts;
    }

    public static void reset() {
        colorFilterEnabled = true;
        inventoryRestrictionEnabled = true;
        instinctsEnabled = true;
    }
}
