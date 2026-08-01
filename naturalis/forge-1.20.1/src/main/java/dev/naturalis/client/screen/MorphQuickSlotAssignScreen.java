package dev.naturalis.client.screen;

import dev.naturalis.client.MorphQuickSlotClientState;
import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MorphQuickSlotAssignScreen extends Screen {

    private final ResourceLocation morphId;
    private int renderedUnlocked = -1;

    public MorphQuickSlotAssignScreen(ResourceLocation morphId) {
        super(Component.translatable("gui.naturalis.quick_slot.assign_title"));
        this.morphId = morphId;
    }

    @Override
    protected void init() {
        int unlocked = MorphQuickSlotClientState.effectiveUnlockedSlots();
        renderedUnlocked = unlocked;
        int centerX = width / 2;
        int startY = height / 2 - 72;
        int buttonWidth = 180;

        for (int i = 0; i < MorphQuickSlotCategory.SLOT_COUNT; i++) {
            MorphQuickSlotCategory category = MorphQuickSlotCategory.byIndex(i);
            if (category == null) {
                continue;
            }
            int slotIndex = i;
            boolean locked = i >= unlocked;
            Component label = locked
                ? Component.translatable("gui.naturalis.quick_slot.assign_locked", category.label(), MorphQuickSlotCategory.UNLOCK_XP[i])
                : Component.translatable("gui.naturalis.quick_slot.assign_option", category.label());
            addRenderableWidget(Button.builder(label, button -> assign(slotIndex))
                .bounds(centerX - buttonWidth / 2, startY + i * 24, buttonWidth, 20)
                .build()).active = !locked;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
            .bounds(centerX - 60, startY + MorphQuickSlotCategory.SLOT_COUNT * 24 + 8, 120, 20)
            .build());
    }

    @Override
    public void tick() {
        super.tick();
        int unlocked = MorphQuickSlotClientState.effectiveUnlockedSlots();
        if (unlocked != renderedUnlocked) {
            rebuildWidgets();
        }
    }

    private void assign(int slotIndex) {
        MorphQuickSlotClientActions.sendAssign(slotIndex, morphId);
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 92, 0xFFFFFF);
        String morphName = morphId.toString();
        graphics.drawCenteredString(font, morphName, width / 2, height / 2 - 80, 0xAAAAAA);
        int globalXp = MorphQuickSlotClientState.effectiveGlobalXp();
        if (globalXp > 0) {
            graphics.drawCenteredString(
                font,
                Component.translatable("gui.naturalis.quick_slot.assign_global_xp", globalXp),
                width / 2,
                height / 2 - 68,
                0x88CC88
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
