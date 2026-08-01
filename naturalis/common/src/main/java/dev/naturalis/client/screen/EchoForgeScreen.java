package dev.naturalis.client.screen;

import dev.naturalis.world.menu.EchoForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EchoForgeScreen extends AbstractContainerScreen<EchoForgeMenu> {

    public EchoForgeScreen(EchoForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Main panel with a subtle arcane tint.
        guiGraphics.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF2A2A2E, 0xFF202025);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, 0xFF5A5A66);
        guiGraphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF121216);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, 0xFF5A5A66);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF121216);

        // Forge ring slots.
        drawSlot(guiGraphics, x + 80, y + 17);
        drawSlot(guiGraphics, x + 104, y + 35);
        drawSlot(guiGraphics, x + 92, y + 57);
        drawSlot(guiGraphics, x + 68, y + 57);
        drawSlot(guiGraphics, x + 56, y + 35);
        drawOutputSlot(guiGraphics, x + 80, y + 35);

        // Player inventory slot rows.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiGraphics, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(guiGraphics, x + 8 + col * 18, y + 142);
        }

        // Animated center core and forge progress pulse.
        int progress = menu.getProgress();
        long animTick = System.currentTimeMillis() / 80L;
        int pulse = (int) ((Math.sin(animTick / 6.0D) + 1.0D) * 20.0D);
        guiGraphics.fill(x + 87, y + 42, x + 89, y + 50, 0xFF8B53FF);
        guiGraphics.fill(x + 84, y + 45, x + 92, y + 47, 0xFF8B53FF);
        guiGraphics.fill(x + 85, y + 43, x + 91, y + 49, (0xA0 + pulse) << 24 | 0x00BEE8FF);

        if (progress > 0) {
            guiGraphics.fill(x + 76, y + 66, x + 76 + progress, y + 68, 0xFF6FFFE8);
            guiGraphics.fill(x + 76, y + 68, x + 76 + progress, y + 70, 0xFF29CFC0);
        }

        drawTopRightCookProgress(guiGraphics, x + imageWidth - 38, y + 14, progress);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE8E8F2, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFD9D9E3, false);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF2F2F36);
        guiGraphics.fill(x, y, x + 16, y + 1, 0xFF64647A);
        guiGraphics.fill(x, y, x + 1, y + 16, 0xFF64647A);
        guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFF1A1A20);
        guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFF1A1A20);
    }

    private static void drawOutputSlot(GuiGraphics guiGraphics, int x, int y) {
        drawSlot(guiGraphics, x, y);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, 0x66227F99);
        guiGraphics.fill(x + 3, y + 3, x + 13, y + 13, 0x6635D6FF);
    }

    private static void drawTopRightCookProgress(GuiGraphics guiGraphics, int x, int y, int progress) {
        guiGraphics.fill(x, y, x + 28, y + 12, 0xFF15171D);
        guiGraphics.fill(x, y, x + 28, y + 1, 0xFF6A7280);
        guiGraphics.fill(x, y + 11, x + 28, y + 12, 0xFF090A0D);
        guiGraphics.fill(x, y, x + 1, y + 12, 0xFF6A7280);
        guiGraphics.fill(x + 27, y, x + 28, y + 12, 0xFF090A0D);

        // Furnace-like arrow body.
        guiGraphics.fill(x + 3, y + 4, x + 20, y + 8, 0xFF232A33);
        guiGraphics.fill(x + 20, y + 2, x + 24, y + 10, 0xFF232A33);
        guiGraphics.fill(x + 24, y + 4, x + 26, y + 8, 0xFF232A33);

        if (progress <= 0) {
            return;
        }

        int fill = Math.min(24, progress);
        int bodyFill = Math.min(17, fill);
        if (bodyFill > 0) {
            guiGraphics.fill(x + 3, y + 4, x + 3 + bodyFill, y + 8, 0xFFB792FF);
        }

        if (fill > 17) {
            int headFill = Math.min(4, fill - 17);
            guiGraphics.fill(x + 20, y + 2, x + 20 + headFill, y + 10, 0xFF6FFFE8);
        }
        if (fill > 21) {
            int tipFill = Math.min(2, fill - 21);
            guiGraphics.fill(x + 24, y + 4, x + 24 + tipFill, y + 8, 0xFF6FFFE8);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
