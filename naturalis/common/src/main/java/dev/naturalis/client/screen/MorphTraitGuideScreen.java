package dev.naturalis.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrolling encyclopedia of morph traits (icons + explanations), opened from Remorphed.
 */
public class MorphTraitGuideScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int ROW_HEIGHT = 52;
    private static final int PAD = 12;

    private final Screen parent;
    private double scrollOffset;
    private int listTop;
    private int listBottom;
    private int listHeight;
    private int contentHeight;
    private final List<MorphTraitGuideCatalog.Entry> entries = new ArrayList<>(MorphTraitGuideCatalog.entries());

    public MorphTraitGuideScreen(Screen parent) {
        super(Component.translatable("gui.naturalis.trait_guide.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        listTop = 40;
        listBottom = height - 36;
        listHeight = Math.max(40, listBottom - listTop);
        contentHeight = entries.size() * ROW_HEIGHT;
        scrollOffset = Mth.clamp(scrollOffset, 0.0D, maxScroll());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(width / 2 - 50, height - 28, 100, 20)
            .build());
    }

    private double maxScroll() {
        return Math.max(0.0D, contentHeight - listHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= listTop && mouseY <= listBottom) {
            scrollOffset = Mth.clamp(scrollOffset - scrollY * 16.0D, 0.0D, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid Screen.renderBackground's menu blur — it smears this overlay's text/icons.
        renderTransparentBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelLeft = width / 2 - PANEL_WIDTH / 2;
        graphics.fill(panelLeft - 4, 8, panelLeft + PANEL_WIDTH + 4, height - 8, 0xE010161E);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFE8C86A);
        graphics.drawCenteredString(
            font,
            Component.translatable("gui.naturalis.trait_guide.subtitle"),
            width / 2,
            28,
            0xFF9DB7D8
        );

        graphics.enableScissor(panelLeft, listTop, panelLeft + PANEL_WIDTH, listBottom);
        int y = listTop - (int) scrollOffset;
        for (MorphTraitGuideCatalog.Entry entry : entries) {
            if (y + ROW_HEIGHT >= listTop && y <= listBottom) {
                renderRow(graphics, entry, panelLeft, y, mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();

        if (maxScroll() > 0.0D) {
            int barHeight = Math.max(16, (int) (listHeight * (listHeight / (double) contentHeight)));
            int barTravel = listHeight - barHeight;
            int barY = listTop + (int) (barTravel * (scrollOffset / maxScroll()));
            int barX = panelLeft + PANEL_WIDTH - 4;
            graphics.fill(barX, listTop, barX + 3, listBottom, 0x66000000);
            graphics.fill(barX, barY, barX + 3, barY + barHeight, 0xFFE8C86A);
        }

        // Render widgets only — do not call super.render (it may re-apply blur over our text).
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderRow(GuiGraphics graphics, MorphTraitGuideCatalog.Entry entry, int panelLeft, int y, int mouseX, int mouseY) {
        int rowBottom = y + ROW_HEIGHT - 4;
        boolean hovered = mouseX >= panelLeft && mouseX < panelLeft + PANEL_WIDTH
            && mouseY >= Math.max(y, listTop) && mouseY < Math.min(rowBottom, listBottom);
        graphics.fill(panelLeft, y, panelLeft + PANEL_WIDTH - 8, rowBottom, hovered ? 0x4424384A : 0x33182028);

        int iconX = panelLeft + PAD;
        int iconY = y + 10;
        graphics.renderItem(new ItemStack(entry.icon()), iconX, iconY);
        graphics.renderItemDecorations(font, new ItemStack(entry.icon()), iconX, iconY);

        int textX = iconX + 24;
        int textWidth = PANEL_WIDTH - PAD * 2 - 24;
        graphics.drawString(font, entry.title(), textX, y + 6, 0xFFF0E6C8, false);

        int bodyY = y + 18;
        for (var line : font.split(entry.body(), textWidth)) {
            if (bodyY + font.lineHeight > rowBottom - 2) {
                break;
            }
            graphics.drawString(font, line, textX, bodyY, 0xFFB0C4D8, false);
            bodyY += font.lineHeight + 1;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
