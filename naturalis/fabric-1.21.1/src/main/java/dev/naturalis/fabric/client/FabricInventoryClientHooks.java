package dev.naturalis.fabric.client;

import dev.naturalis.client.MorphLevelClientCache;
import dev.naturalis.client.RuleFlagsClientCache;
import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric mirror of NeoForge {@code InventoryRestrictionClientEvents}
 * (hotbar slot clamp, inventory screen block, locked-slot overlay).
 * Mouth/shape render hooks are NeoForge/Walkers-package specific on 1.21.1 and are omitted here.
 */
public final class FabricInventoryClientHooks {

    private static int lastSelectedSlot = -1;

    private FabricInventoryClientHooks() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FabricInventoryClientHooks::onClientTick);
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> onHudRender(graphics));
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }

        if (!RuleFlagsClientCache.isInventoryRestrictionEnabled()) {
            lastSelectedSlot = -1;
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (!InventoryRestrictionManager.isQuadruped(morphId)) {
            lastSelectedSlot = -1;
            return;
        }

        int allowedSlots = MorphLevelClientCache.getHotbarSlots();

        if (allowedSlots >= 9) {
            lastSelectedSlot = -1;
            if (!MorphLevelClientCache.isInventoryUnlocked() && mc.screen instanceof InventoryScreen) {
                mc.setScreen(null);
                mc.player.displayClientMessage(Component.translatable("message.naturalis.inventory_restricted_quadruped"), true);
            }
            return;
        }

        int maxSlotIndex = allowedSlots - 1;
        int currentSelected = getSelectedSlot(mc);
        if (lastSelectedSlot < 0 || lastSelectedSlot > 8) {
            lastSelectedSlot = currentSelected;
        }

        if (currentSelected > maxSlotIndex) {
            int resolved = resolveRestrictedSlot(lastSelectedSlot, currentSelected, maxSlotIndex);
            setSelectedSlot(mc, resolved);
            currentSelected = resolved;
        }

        lastSelectedSlot = currentSelected;

        if (mc.screen instanceof InventoryScreen) {
            mc.setScreen(null);
            mc.player.displayClientMessage(Component.translatable("message.naturalis.inventory_restricted_quadruped"), true);
        }
    }

    private static void onHudRender(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        if (!RuleFlagsClientCache.isInventoryRestrictionEnabled()) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (!InventoryRestrictionManager.isQuadruped(morphId)) {
            return;
        }

        int allowedSlots = MorphLevelClientCache.getHotbarSlots();
        if (allowedSlots >= 9) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int hotbarX = width / 2 - 91;
        int hotbarY = height - 22;

        for (int slot = allowedSlots; slot < 9; slot++) {
            int left = hotbarX + slot * 20 + 2;
            int top = hotbarY + 2;
            int right = left + 16;
            int bottom = top + 16;
            graphics.fill(left, top, right, bottom, 0xBB111111);
        }
    }

    private static int resolveRestrictedSlot(int previousSlot, int attemptedSlot, int maxSlotIndex) {
        if (attemptedSlot <= maxSlotIndex) {
            return attemptedSlot;
        }

        if (previousSlot == maxSlotIndex && attemptedSlot == maxSlotIndex + 1) {
            return 0;
        }
        if (previousSlot == 0 && attemptedSlot == 8) {
            return maxSlotIndex;
        }

        int forwardStep = Math.floorMod(attemptedSlot - previousSlot, 9);
        int backwardStep = Math.floorMod(previousSlot - attemptedSlot, 9);
        if (forwardStep == 1) {
            return 0;
        }
        if (backwardStep == 1) {
            return maxSlotIndex;
        }

        return maxSlotIndex;
    }

    private static int getSelectedSlot(Minecraft mc) {
        try {
            Object raw = mc.player.getInventory().getClass().getField("selected").get(mc.player.getInventory());
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = mc.player.getInventory().getClass().getMethod("getSelectedSlot").invoke(mc.player.getInventory());
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return 0;
    }

    private static void setSelectedSlot(Minecraft mc, int slot) {
        try {
            mc.player.getInventory().getClass().getField("selected").set(mc.player.getInventory(), slot);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            mc.player.getInventory().getClass().getMethod("setSelectedSlot", int.class).invoke(mc.player.getInventory(), slot);
        } catch (ReflectiveOperationException ignored) {
            // Last-resort no-op.
        }
    }
}
