package dev.naturalis.client;

import net.minecraft.client.Minecraft;

/**
 * Partial tick across NeoForge 1.21.1 ({@code getTimer}) and 1.21.8+ ({@code getDeltaTracker}).
 */
public final class ClientPartialTick {

    private ClientPartialTick() {
    }

    public static float get(Minecraft mc) {
        if (mc == null) {
            return 1.0F;
        }
        try {
            Object tracker = Minecraft.class.getMethod("getDeltaTracker").invoke(mc);
            return invokePartialTick(tracker);
        } catch (ReflectiveOperationException ignored) {
            // 1.21.1 and earlier
        }
        try {
            Object tracker = Minecraft.class.getMethod("getTimer").invoke(mc);
            return invokePartialTick(tracker);
        } catch (ReflectiveOperationException ignored) {
            return 1.0F;
        }
    }

    private static float invokePartialTick(Object tracker) throws ReflectiveOperationException {
        return (Float) tracker.getClass()
            .getMethod("getGameTimeDeltaPartialTick", boolean.class)
            .invoke(tracker, true);
    }
}
