package dev.naturalis.client;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import dev.naturalis.network.MorphQuickSlotResyncPayload;
import dev.naturalis.network.NaturalisNetwork;
import net.minecraft.client.Minecraft;
import tocraft.walkers.WalkersClient;

/**
 * Hold the Woodwalkers transform key (G) to open a morph quick-slot wheel.
 * Quick tap without holding still toggles human / last morph via walkers swap.
 */
public final class MorphQuickSlotClient {

    private static final int HOLD_TICKS = 8;

    private static boolean keyWasDown;
    private static int holdTicks;
    private static boolean wheelOpen;
    private static boolean suppressWalkersSwap;
    private static int hoveredSlot = -1;
    private static boolean sessionActive;

    private MorphQuickSlotClient() {
    }

    public static boolean shouldSuppressWalkersSwap() {
        return suppressWalkersSwap;
    }

    public static boolean shouldBlockTransformKey() {
        return suppressWalkersSwap || wheelOpen || WalkersClient.TRANSFORM_KEY.isDown();
    }

    public static boolean isWheelOpen() {
        return wheelOpen;
    }

    public static int hoveredSlot() {
        return hoveredSlot;
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.screen != null) {
            reset();
            return;
        }

        boolean down = WalkersClient.TRANSFORM_KEY.isDown();
        if (down && !keyWasDown) {
            beginSession(client);
        }
        if (down) {
            suppressWalkersSwap = true;
        }

        if (shouldBlockTransformKey()) {
            while (WalkersClient.TRANSFORM_KEY.consumeClick()) {
                // Block Woodwalkers toggle while holding G for the quick-slot wheel.
            }
        }

        if (down) {
            holdTicks++;
            if (holdTicks >= HOLD_TICKS && !wheelOpen) {
                requestSlotSync();
            }
            if (holdTicks >= HOLD_TICKS) {
                if (!wheelOpen) {
                    MorphQuickSlotMouseCapture.onWheelOpened(client);
                }
                wheelOpen = true;
            }
        } else if (keyWasDown) {
            boolean wheelWasOpen = wheelOpen;
            int releaseHovered = wheelOpen ? MorphQuickSlotWheelInput.pickHoveredSlot(client) : -1;
            if (wheelWasOpen) {
                MorphQuickSlotReleaseHelper.onWheelRelease(client, true, releaseHovered);
            } else if (holdTicks > 0 && holdTicks < HOLD_TICKS && !isQuickSwapBlocked(client)) {
                suppressWalkersSwap = false;
                tocraft.walkers.network.impl.SwapPackets.sendSwapRequest();
            }
            resetHoldState(client);
        } else if (holdTicks > 0 || suppressWalkersSwap || wheelOpen) {
            resetHoldState(client);
        }

        if (wheelOpen) {
            MorphQuickSlotClientState.setWheelOpen(true);
            hoveredSlot = MorphQuickSlotWheelInput.pickHoveredSlot(client);
            MorphQuickSlotClientState.setHoveredSlot(hoveredSlot);
        } else {
            MorphQuickSlotClientState.setWheelOpen(false);
            MorphQuickSlotClientState.setHoveredSlot(-1);
        }

        keyWasDown = down;
    }

    private static void requestSlotSync() {
        NaturalisNetwork.CHANNEL.sendToServer(new MorphQuickSlotResyncPayload());
    }

    private static boolean isQuickSwapBlocked(Minecraft client) {
        return isTransformBlocked(client);
    }

    private static boolean isTransformBlocked(Minecraft client) {
        return NaturalisConfig.morphBindingEnabled()
            && NaturalisConfig.morphBindingBlockTransformKey()
            && client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("morph_binding"));
    }

    private static void beginSession(Minecraft client) {
        if (sessionActive || client.getConnection() == null) {
            return;
        }
        MorphQuickSlotClientActions.notifySessionActive();
        sessionActive = true;
    }

    private static void endSession(Minecraft client) {
        if (!sessionActive) {
            return;
        }
        if (client.getConnection() != null) {
            MorphQuickSlotClientActions.notifySessionInactive();
        }
        sessionActive = false;
    }

    private static void resetHoldState(Minecraft client) {
        MorphQuickSlotMouseCapture.forceClose(client);
        if (wheelOpen) {
            MorphQuickSlotEntityPreview.clearCache();
        }
        endSession(client);
        holdTicks = 0;
        wheelOpen = false;
        suppressWalkersSwap = false;
        hoveredSlot = -1;
        MorphQuickSlotWheelInput.reset();
        MorphQuickSlotClientState.setWheelOpen(false);
        MorphQuickSlotClientState.setHoveredSlot(-1);
    }

    private static void reset() {
        keyWasDown = false;
        resetHoldState(Minecraft.getInstance());
    }
}
