package dev.naturalis.fabric.client.quickslot;

import dev.naturalis.client.MorphQuickSlotClientState;
import dev.naturalis.client.MorphQuickSlotEntityPreview;
import dev.naturalis.client.MorphQuickSlotMouseCapture;
import dev.naturalis.client.MorphQuickSlotReleaseHelper;
import dev.naturalis.client.MorphQuickSlotWheelInput;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.config.NaturalisConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Fabric port of NeoForge MorphQuickSlotClient — hold Walkers transform key for the wheel.
 * Resolves WalkersClient.TRANSFORM_KEY reflectively (package differs by Walkers version).
 */
public final class FabricMorphQuickSlotClient {

    private static final int HOLD_TICKS = 8;

    private static boolean keyWasDown;
    private static int holdTicks;
    private static boolean wheelOpen;
    private static boolean suppressWalkersSwap;
    private static int hoveredSlot = -1;
    private static KeyMapping transformKey;
    private static boolean transformKeyResolved;

    private FabricMorphQuickSlotClient() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.screen != null) {
            reset();
            return;
        }

        KeyMapping key = resolveTransformKey();
        if (key == null) {
            return;
        }

        boolean down = key.isDown();
        if (down) {
            suppressWalkersSwap = true;
        }

        if (suppressWalkersSwap || wheelOpen || key.isDown()) {
            while (key.consumeClick()) {
                // Block Woodwalkers toggle while holding for the quick-slot wheel.
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
            } else if (holdTicks > 0 && holdTicks < HOLD_TICKS && !isTransformBlocked(client)) {
                sendWalkersSwap();
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

    private static KeyMapping resolveTransformKey() {
        if (transformKeyResolved) {
            return transformKey;
        }
        transformKeyResolved = true;
        String[] classes = {
            "tocraft.walkers.WalkersClient",
            "dev.tocraft.walkers.WalkersClient"
        };
        for (String name : classes) {
            try {
                Class<?> clazz = Class.forName(name);
                Object field = clazz.getField("TRANSFORM_KEY").get(null);
                if (field instanceof KeyMapping mapping) {
                    transformKey = mapping;
                    return transformKey;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void sendWalkersSwap() {
        String[] classes = {
            "tocraft.walkers.network.impl.SwapPackets",
            "dev.tocraft.walkers.network.impl.SwapPackets"
        };
        for (String name : classes) {
            try {
                Class.forName(name).getMethod("sendSwapRequest").invoke(null);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void requestSlotSync(Minecraft client) {
        if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(
            dev.naturalis.network.MorphQuickSlotResyncPayload.TYPE)) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new dev.naturalis.network.MorphQuickSlotResyncPayload());
        }
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
