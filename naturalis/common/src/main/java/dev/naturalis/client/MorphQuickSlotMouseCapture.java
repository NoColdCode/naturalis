package dev.naturalis.client;

import net.minecraft.client.Minecraft;

/**
 * Releases the game mouse while the morph wheel is open so slice selection works in first person.
 */
public final class MorphQuickSlotMouseCapture {

    private static boolean releasedForWheel;

    private MorphQuickSlotMouseCapture() {
    }

    public static void onWheelOpened(Minecraft client) {
        if (client.screen != null) {
            return;
        }
        client.mouseHandler.releaseMouse();
        releasedForWheel = true;
    }

    public static void onWheelClosed(Minecraft client) {
        if (!releasedForWheel) {
            return;
        }
        releasedForWheel = false;
        if (client.screen == null && client.player != null) {
            client.mouseHandler.grabMouse();
        }
    }

    public static void forceClose(Minecraft client) {
        onWheelClosed(client);
    }
}
