package dev.naturalis.client;

import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class MorphQuickSlotOverlay {

    private MorphQuickSlotOverlay() {
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!MorphQuickSlotClientState.isWheelOpen()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float cx = width * 0.5F;
        float cy = height * 0.5F;
        float radius = 86.0F;

        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.fill((int) cx - 3, (int) cy - 3, (int) cx + 3, (int) cy + 3, 0xCCFFFFFF);

        int unlocked = MorphQuickSlotClientState.effectiveUnlockedSlots();
        int hovered = MorphQuickSlotClientState.hoveredSlot();

        ResourceLocation hoveredMorph = hovered >= 0 && hovered < unlocked ? MorphQuickSlotClientState.slot(hovered) : null;
        if (hoveredMorph != null) {
            int previewSize = 72;
            int previewLeft = (int) cx - previewSize / 2;
            int previewTop = (int) cy - previewSize / 2 - 8;
            graphics.fill(previewLeft - 2, previewTop - 2, previewLeft + previewSize + 2, previewTop + previewSize + 2, 0xAA1A2430);
            graphics.fill(previewLeft - 1, previewTop - 1, previewLeft + previewSize + 1, previewTop + previewSize + 1, 0xFF4A708C);
            MorphQuickSlotEntityPreview.render(graphics, cx, cy - 4, previewSize, partialTick, hoveredMorph);
        }

        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            boolean locked = i >= unlocked;
            boolean selected = i == hovered;
            float start = (float) (-Math.PI / 2.0D + (Math.PI * 2.0D / MorphQuickSlotCategory.SLOT_COUNT) * i);
            float end = start + (float) (Math.PI * 2.0D / MorphQuickSlotCategory.SLOT_COUNT);
            float mid = (start + end) * 0.5F;

            float iconX = cx + Mth.cos(mid) * radius;
            float iconY = cy + Mth.sin(mid) * radius;

            int bg = locked ? 0xAA2A2A2A : selected ? 0xCC3D6E52 : 0xAA1A1F24;
            int border = locked ? 0xFF555555 : selected ? 0xFF9BE7B3 : 0xFF4A5560;
            int size = selected ? 34 : 30;
            int left = (int) iconX - size / 2;
            int top = (int) iconY - size / 2;
            graphics.fill(left - 1, top - 1, left + size + 1, top + size + 1, border);
            graphics.fill(left, top, left + size, top + size, bg);

            ResourceLocation morphId = MorphQuickSlotClientState.slot(i);
            if (!locked && morphId != null) {
                MorphQuickSlotEntityPreview.render(graphics, iconX, iconY + 2, size + (selected ? 8 : 4), partialTick, morphId);
            }

            MorphQuickSlotCategory category = MorphQuickSlotCategory.byIndex(i);
            if (category != null) {
                String categoryLabel = mc.font.plainSubstrByWidth(category.label().getString(), size + 30);
                graphics.drawCenteredString(mc.font, categoryLabel, (int) iconX, top - 12, locked ? 0xFF777777 : 0xFFCCCCCC);
            }

            String morphLabel = locked
                ? mc.font.plainSubstrByWidth(
                    net.minecraft.network.chat.Component.translatable("gui.naturalis.quick_slot.locked_short").getString(),
                    size + 24)
                : morphId == null
                    ? net.minecraft.network.chat.Component.translatable("gui.naturalis.quick_slot.empty").getString()
                    : mc.font.plainSubstrByWidth(prettyMorphName(morphId), size + 24);
            graphics.drawCenteredString(mc.font, morphLabel, (int) iconX, top + size + 4, locked ? 0xFF777777 : 0xFFDDDDDD);
        }

        String hint = hovered >= 0 && hovered < unlocked && hoveredMorph != null
            ? mc.font.plainSubstrByWidth(prettyMorphName(hoveredMorph), width - 40)
            : hovered >= 0 && hovered < unlocked
                ? net.minecraft.network.chat.Component.translatable("gui.naturalis.quick_slot.empty").getString()
                : "Release without a slice: unmorph / last morph";
        graphics.drawCenteredString(mc.font, hint, width / 2, height / 2 + 28, 0xFFE8E8E8);
    }

    private static String prettyMorphName(ResourceLocation morphId) {
        String path = morphId.getPath().replace('_', ' ');
        if (path.length() > 14) {
            return path.substring(0, 13) + "…";
        }
        return path;
    }
}
