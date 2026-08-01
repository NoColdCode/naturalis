package dev.naturalis.client.screen;

import dev.naturalis.item.MorphArmorTier;
import dev.naturalis.world.menu.MorphArmorForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MorphArmorForgeScreen extends AbstractContainerScreen<MorphArmorForgeMenu> {

    // Tier button layout constants
    private static final int TIER_BTN_X     = 8;
    private static final int TIER_BTN_Y_START = 14;
    private static final int TIER_BTN_W     = 18;
    private static final int TIER_BTN_H     = 10;
    private static final int TIER_BTN_GAP   = 1;

    // Tier colors (ARGB) for button highlight and armor preview tint
    private static final int[] TIER_COLORS = {
        0xFF8B5E3C,  // leather  – brown
        0xFF9EB0BE,  // chainmail – grey-blue
        0xFFC8C8C8,  // iron     – silver
        0xFFFFD700,  // gold     – gold
        0xFF89CFF0,  // diamond  – cyan
        0xFF2EE5A9,  // netherite – teal
        0xFF59FFE6,  // echo      – bright echo-cyan
    };

    public MorphArmorForgeScreen(MorphArmorForgeMenu menu, Inventory playerInventory,
                                 Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 194;
        this.inventoryLabelY = 102;
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Background panel
        g.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E24, 0xFF141418);
        g.fill(x, y,              x + imageWidth, y + 1,           0xFF5A5A6A);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF0D0D10);
        g.fill(x, y,              x + 1,          y + imageHeight, 0xFF5A5A6A);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF0D0D10);

        // Tier selector panel on the left
        g.fill(x + 3, y + 10, x + 30, y + 80, 0xFF252530);
        g.fill(x + 3, y + 10, x + 30, y + 11, 0xFF666680);
        g.fill(x + 3, y + 10, x + 4,  y + 80, 0xFF666680);

        drawTierButtons(g, x, y, mouseX, mouseY);

        // Slot frames
        drawSlot(g, x + 35, y + 35);   // orb
        drawSlot(g, x + 80, y + 35);   // material
        drawOutputSlot(g, x + 125, y + 35); // output

        // Arrow from material to output
        g.fill(x + 97, y + 42, x + 121, y + 44, 0xFF333340);
        g.fill(x + 97, y + 44, x + 121, y + 46, 0xFF22222A);
        int progress = menu.getProgress();
        if (progress > 0) {
            g.fill(x + 97, y + 42, x + 97 + Math.min(24, progress), y + 46, 0xFF6FFFE8);
        }

        // Cost readout above material slot
        int cost = menu.getRequiredCost();
        int tier  = menu.getSelectedTierIndex();
        String costText = cost > 0 ? "x" + cost : "—";
        g.drawString(this.font, costText, x + 80, y + 57, 0xFFBBBBCC, false);

        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, x + 8 + col * 18, y + 114 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(g, x + 8 + col * 18, y + 172);
        }

        // Animated core pulse between orb and material
        long animTick = System.currentTimeMillis() / 100L;
        int pulse = (int) ((Math.sin(animTick / 5.0) + 1.0) * 12);
        int tierColor = tier < TIER_COLORS.length ? TIER_COLORS[tier] : TIER_COLORS[2];
        int glowAlpha = (0x50 + pulse) << 24;
        int glowColor = (glowAlpha) | (tierColor & 0x00FFFFFF);
        g.fill(x + 56, y + 41, x + 75, y + 45, glowColor);
    }

    private void drawTierButtons(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        MorphArmorTier[] tiers = MorphArmorTier.values();
        int selected = menu.getSelectedTierIndex();

        for (int i = 0; i < tiers.length; i++) {
            int bx = x + TIER_BTN_X;
            int by = y + TIER_BTN_Y_START + i * (TIER_BTN_H + TIER_BTN_GAP);
            int color = TIER_COLORS[i];
            boolean isSelected = (i == selected);
            boolean hovered    = mouseX >= bx && mouseX < bx + TIER_BTN_W
                              && mouseY >= by  && mouseY < by + TIER_BTN_H;

            int bg = isSelected ? (0xCC000000 | (color & 0x00FFFFFF))
                    : hovered   ? 0xFF303040
                    :             0xFF252530;

            g.fill(bx, by, bx + TIER_BTN_W, by + TIER_BTN_H, bg);
            g.fill(bx, by, bx + TIER_BTN_W, by + 1, isSelected ? color : 0xFF404050);
            g.fill(bx, by, bx + 1, by + TIER_BTN_H, isSelected ? color : 0xFF404050);

            // Colored dot
            g.fill(bx + 3, by + 3, bx + 7, by + 7, color);

            // Label abbreviation (2 chars)
            String label = tiers[i].id.substring(0, 2).toUpperCase();
            g.drawString(this.font, label, bx + 9, by + 2,
                isSelected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX + 22, this.titleLabelY, 0xFFE0E0F0, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
            0xFFD0D0E0, false);

        // Tooltip for hovered tier button
        int x = leftPos;
        int y = topPos;
        MorphArmorTier[] tiers = MorphArmorTier.values();
        for (int i = 0; i < tiers.length; i++) {
            int bx = TIER_BTN_X;
            int by = TIER_BTN_Y_START + i * (TIER_BTN_H + TIER_BTN_GAP);
            if (mouseX >= x + bx && mouseX < x + bx + TIER_BTN_W
                && mouseY >= y + by && mouseY < y + by + TIER_BTN_H) {
                String tierName = tiers[i].id.substring(0, 1).toUpperCase() + tiers[i].id.substring(1);
                renderTierTooltip(g,
                    java.util.List.of(
                        net.minecraft.network.chat.Component.literal(tierName),
                        net.minecraft.network.chat.Component.translatable(
                            "tooltip.naturalis.morph_armor.tier_stats",
                            tiers[i].armor, tiers[i].toughness)
                    ),
                    mouseX - x, mouseY - y);
                break;
            }
        }
    }

    private void renderTierTooltip(GuiGraphics g, java.util.List<net.minecraft.network.chat.Component> lines, int x, int y) {
        try {
            g.getClass()
                .getMethod("renderTooltip", net.minecraft.client.gui.Font.class, java.util.List.class, java.util.Optional.class, int.class, int.class)
                .invoke(g, this.font, lines, java.util.Optional.empty(), x, y);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            java.util.List<net.minecraft.util.FormattedCharSequence> rendered = new java.util.ArrayList<>();
            for (var line : lines) {
                rendered.add(line.getVisualOrderText());
            }
            g.getClass()
                .getMethod("renderTooltip", net.minecraft.client.gui.Font.class, java.util.List.class, int.class, int.class)
                .invoke(g, this.font, rendered, x, y);
        } catch (ReflectiveOperationException ignored) {
            // Last-resort no-op.
        }
    }

    // ── Mouse click → tier button ─────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            MorphArmorTier[] tiers = MorphArmorTier.values();
            for (int i = 0; i < tiers.length; i++) {
                int bx = leftPos + TIER_BTN_X;
                int by = topPos  + TIER_BTN_Y_START + i * (TIER_BTN_H + TIER_BTN_GAP);
                if (mouseX >= bx && mouseX < bx + TIER_BTN_W
                    && mouseY >= by && mouseY < by + TIER_BTN_H) {
                    this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    // ── Shared slot draw helpers ──────────────────────────────────────────────

    private static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 16, y + 16, 0xFF2A2A34);
        g.fill(x, y, x + 16, y + 1,  0xFF606070);
        g.fill(x, y, x + 1,  y + 16, 0xFF606070);
        g.fill(x, y + 15, x + 16, y + 16, 0xFF181820);
        g.fill(x + 15, y,  x + 16, y + 16, 0xFF181820);
    }

    private static void drawOutputSlot(GuiGraphics g, int x, int y) {
        drawSlot(g, x, y);
        g.fill(x + 1, y + 1, x + 15, y + 15, 0x5522446A);
        g.fill(x + 3, y + 3, x + 13, y + 13, 0x4466AAFF);
    }
}
