package dev.naturalis.client;

public final class HumanityClientCache {

    private static int humanity = 100;
    private static boolean active;

    private HumanityClientCache() {
    }

    public static int getHumanity() {
        return Math.max(0, Math.min(100, humanity));
    }

    public static boolean isActive() {
        return active;
    }

    public static void set(int value, boolean activeNow) {
        humanity = Math.max(0, Math.min(100, value));
        active = activeNow;
    }

    public static void reset() {
        humanity = 100;
        active = false;
    }
}
