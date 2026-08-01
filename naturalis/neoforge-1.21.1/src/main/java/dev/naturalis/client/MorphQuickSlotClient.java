package dev.naturalis.client;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.config.NaturalisConfig;
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
                requestSlotSync(client);
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

    private static void requestSlotSync(Minecraft client) {
        var connection = client.getConnection();
        if (connection != null) {
            connection.send(new dev.naturalis.network.MorphQuickSlotResyncPayload());
        }
    }

    private static boolean isQuickSwapBlocked(Minecraft client) {
        return isTransformBlocked(client);
    }

    private static boolean isTransformBlocked(Minecraft client) {
        return NaturalisConfig.morphBindingEnabled()
            && NaturalisConfig.morphBindingBlockTransformKey()
            && client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("morph_binding"));
    }

    private static void resetHoldState(Minecraft client) {
        if (wheelOpen) {
            MorphQuickSlotMouseCapture.onWheelClosed(client);
            MorphQuickSlotEntityPreview.clearCache();
        }
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
