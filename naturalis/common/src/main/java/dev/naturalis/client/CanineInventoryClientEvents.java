package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.gameplay.MorphAnimalInteraction;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Wolf/fox morphs carry items in the mouth — not a human hotbar.
 */
@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class CanineInventoryClientEvents {

    private CanineInventoryClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiLayerEvent.Post event) {
        if (!"hotbar".equals(event.getName().getPath())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (!MorphAnimalInteraction.isCanineCarrierMorph(morphId)) {
            return;
        }

        if (!RuleFlagsClientCache.isInventoryRestrictionEnabled()) {
            return;
        }

        int allowed = MorphLevelClientCache.getHotbarSlots();
        if (allowed > 1) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int hotbarX = width / 2 - 91;
        int hotbarY = height - 22;

        GuiGraphics g = event.getGuiGraphics();
        for (int slot = 1; slot < 9; slot++) {
            int left = hotbarX + slot * 20 + 2;
            int top = hotbarY + 2;
            g.fill(left, top, left + 16, top + 16, 0xCC0A0A0A);
        }

        int mouthLeft = hotbarX + 2;
        int mouthTop = hotbarY - 10;
        g.drawString(
            mc.font,
            Component.translatable("gui.naturalis.inventory.canine_mouth"),
            mouthLeft,
            mouthTop,
            0xFFE8D4B8,
            true
        );
    }
}
