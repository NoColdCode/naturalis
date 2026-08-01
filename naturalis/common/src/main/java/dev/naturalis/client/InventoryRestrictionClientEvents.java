package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class InventoryRestrictionClientEvents {

    private static int lastSelectedSlot = -1;

    private InventoryRestrictionClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
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

        // Handling rank 6+: full hotbar; inventory opens at max handling rank (8).
        if (allowedSlots >= 9) {
            lastSelectedSlot = -1;
            if (!MorphLevelClientCache.isInventoryUnlocked() && mc.screen instanceof InventoryScreen) {
                mc.setScreen(null);
                mc.player.displayClientMessage(Component.translatable("message.naturalis.inventory_restricted_quadruped"), true);
            }
            return;
        }

        int maxSlotIndex = allowedSlots - 1;
        int currentSelected = mc.player.getInventory().selected;
        if (lastSelectedSlot < 0 || lastSelectedSlot > 8) {
            lastSelectedSlot = currentSelected;
        }

        if (currentSelected > maxSlotIndex) {
            int resolved = resolveRestrictedSlot(lastSelectedSlot, currentSelected, maxSlotIndex);
            mc.player.getInventory().selected = resolved;
            currentSelected = resolved;
        }

        lastSelectedSlot = currentSelected;

        if (mc.screen instanceof InventoryScreen) {
            mc.setScreen(null);
            mc.player.displayClientMessage(Component.translatable("message.naturalis.inventory_restricted_quadruped"), true);
        }
    }

    @SubscribeEvent
    public static void onRenderHotbarLayer(RenderGuiLayerEvent.Post event) {
        // Fire specifically after the hotbar layer so our fill lands on top of items.
        if (!"hotbar".equals(event.getName().getPath())) {
            return;
        }

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
            return; // no slots to lock
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // Standard hotbar item area: each slot's icon is 16×16 at offset (+2, +3) within its 20px cell.
        int hotbarX = width / 2 - 91;
        int hotbarY = height - 22;

        for (int slot = allowedSlots; slot < 9; slot++) {
            int left = hotbarX + slot * 20 + 2;
            int top = hotbarY + 2;
            int right = left + 16;
            int bottom = top + 16;
            // Dark semi-transparent tint over the item icon area.
            event.getGuiGraphics().fill(left, top, right, bottom, 0xBB111111);
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
}
