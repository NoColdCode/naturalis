package dev.naturalis.client.screen;

import dev.naturalis.client.ExperienceModeClientActions;
import dev.naturalis.client.ExperienceModeClientCache;
import dev.naturalis.experience.NaturalisExperienceMessages;
import dev.naturalis.experience.NaturalisExperienceMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * First-world prompt: preview each profile, then confirm. Closing without confirming uses realistic.
 */
public class NaturalisExperienceChoiceScreen extends Screen {

    private final Screen parent;
    private NaturalisExperienceMode selected = NaturalisExperienceMode.REALISTIC;
    private boolean confirmed;

    public NaturalisExperienceChoiceScreen(Screen parent) {
        super(Component.translatable("gui.naturalis.experience.title").withStyle(NaturalisExperienceMessages.titleStyle()));
        this.parent = parent;
        ExperienceModeClientCache.setPreview(NaturalisExperienceMode.REALISTIC);
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonW = 200;
        int y = height / 2 + 4;

        addRenderableWidget(Button.builder(
            Component.translatable("gui.naturalis.experience.try_realistic")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
            b -> preview(NaturalisExperienceMode.REALISTIC)
        ).bounds(centerX - buttonW - 6, y, buttonW, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("gui.naturalis.experience.try_softened")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
            b -> preview(NaturalisExperienceMode.SOFTENED)
        ).bounds(centerX + 6, y, buttonW, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("gui.naturalis.experience.confirm")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            b -> confirm()
        ).bounds(centerX - 110, y + 32, 220, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("gui.naturalis.experience.use_default")
                .withStyle(ChatFormatting.GRAY),
            b -> confirmDefault()
        ).bounds(centerX - 110, y + 58, 220, 20).build());
    }

    private void preview(NaturalisExperienceMode mode) {
        selected = mode;
        ExperienceModeClientCache.setPreview(mode);
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                mode.isRealistic()
                    ? NaturalisExperienceMessages.previewRealistic()
                    : NaturalisExperienceMessages.previewSoftened(),
                true
            );
        }
    }

    private void confirm() {
        confirmed = true;
        sendChoice(selected);
        onClose();
    }

    private void confirmDefault() {
        confirmed = true;
        sendChoice(NaturalisExperienceMode.REALISTIC);
        onClose();
    }

    private void sendChoice(NaturalisExperienceMode mode) {
        ExperienceModeClientActions.sendChoice(mode.id());
        ExperienceModeClientCache.clearPreview();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid Screen's menu blur — 1.21.8+ allows only one blur per frame.
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 88, 0xFFFFD700);

        drawWrapped(graphics, Component.translatable("gui.naturalis.experience.intro")
            .withStyle(ChatFormatting.WHITE), height / 2 - 72, 0xFFE0E0E0);

        int selColor = NaturalisExperienceMessages.highlightColor(selected);
        MutableComponent sel = Component.translatable("gui.naturalis.experience.selected",
            selected.isRealistic()
                ? Component.translatable("gui.naturalis.experience.realistic").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.translatable("gui.naturalis.experience.softened").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
        );
        graphics.drawCenteredString(font, sel, width / 2, height / 2 - 14, selColor | 0xFF000000);

        drawWrapped(graphics, Component.translatable("gui.naturalis.experience.realistic.desc")
            .withStyle(ChatFormatting.GRAY), height / 2 + 8, 0xFFA8A8A8);
        drawWrapped(graphics, Component.translatable("gui.naturalis.experience.softened.desc")
            .withStyle(ChatFormatting.GRAY), height / 2 + 78, 0xFFA8A8A8);

        drawWrapped(graphics, Component.translatable("gui.naturalis.experience.footer")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), height / 2 + 118, 0xFF707070);
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int y, int color) {
        for (var line : font.split(text, 300)) {
            graphics.drawCenteredString(font, line, width / 2, y, color);
            y += font.lineHeight + 2;
        }
    }

    @Override
    public void onClose() {
        if (!confirmed) {
            sendChoice(NaturalisExperienceMode.REALISTIC);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(NaturalisExperienceMessages.defaultApplied(), true);
            }
        }
        ExperienceModeClientCache.clearPreview();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
