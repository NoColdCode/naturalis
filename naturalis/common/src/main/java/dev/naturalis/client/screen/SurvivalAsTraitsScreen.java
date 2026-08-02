package dev.naturalis.client.screen;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** First-join popup listing permanent Survival-as identity traits (icons + creature lists). */
public class SurvivalAsTraitsScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PAD = 12;
    private static final int BASE_ROW = 56;

    private final String morphId;
    private final double mass;
    private final String dietId;
    private final List<String> traitIds;
    private final List<String> traitExtras;
    private final List<Row> rows = new ArrayList<>();

    private double scrollOffset;
    private int listTop;
    private int listBottom;
    private int listHeight;
    private int contentHeight;

    public SurvivalAsTraitsScreen(String morphId, double mass, String dietId, List<String> traitIds) {
        this(morphId, mass, dietId, traitIds, List.of());
    }

    public SurvivalAsTraitsScreen(
        String morphId,
        double mass,
        String dietId,
        List<String> traitIds,
        List<String> traitExtras
    ) {
        super(Component.translatable("gui.naturalis.survival_as.traits.title"));
        this.morphId = morphId;
        this.mass = mass;
        this.dietId = dietId;
        this.traitIds = traitIds == null ? List.of() : List.copyOf(traitIds);
        this.traitExtras = traitExtras == null ? List.of() : List.copyOf(traitExtras);
    }

    @Override
    protected void init() {
        rows.clear();
        ResourceLocation id = ResourceLocation.tryParse(morphId);
        EntityType<?> type = id == null ? null : CompatAccess.getEntityType(id);
        Component name = type != null ? type.getDescription() : Component.literal(morphId);

        rows.add(Row.header(
            Component.translatable("gui.naturalis.survival_as.traits.intro", name),
            Component.translatable("gui.naturalis.survival_as.traits.mass", String.format(Locale.ROOT, "%.1f", mass)),
            Component.translatable("gui.naturalis.survival_as.traits.diet",
                Component.translatable("gui.naturalis.survival_as.diet." + dietId))
        ));

        for (int i = 0; i < traitIds.size(); i++) {
            String traitId = traitIds.get(i);
            String extra = i < traitExtras.size() ? traitExtras.get(i) : "";
            MorphTraitGuideCatalog.Entry entry = MorphTraitGuideCatalog.resolve(traitId);
            Component entityLine = null;
            if (extra != null && !extra.isBlank()) {
                entityLine = Component.translatable(
                    "naturalis:hunter".equals(traitId)
                        ? "gui.naturalis.survival_as.traits.prey_list"
                        : "gui.naturalis.survival_as.traits.entity_list",
                    extra
                );
            }
            rows.add(Row.trait(entry, entityLine));
        }

        listTop = 36;
        listBottom = height - 36;
        listHeight = Math.max(40, listBottom - listTop);
        contentHeight = 0;
        for (Row row : rows) {
            contentHeight += rowHeight(row);
        }
        scrollOffset = Mth.clamp(scrollOffset, 0.0D, maxScroll());

        addRenderableWidget(Button.builder(
            Component.translatable("gui.naturalis.survival_as.traits.ok"),
            b -> onClose()
        ).bounds(width / 2 - 60, height - 28, 120, 20).build());
    }

    private double maxScroll() {
        return Math.max(0.0D, contentHeight - listHeight);
    }

    private int rowHeight(Row row) {
        int textWidth = PANEL_WIDTH - PAD * 2 - (row.icon() != null ? 24 : 0);
        int lines = 0;
        for (Component line : row.lines()) {
            lines += font.split(line, textWidth).size();
        }
        return Math.max(row.icon() != null ? BASE_ROW : 44, 10 + lines * (font.lineHeight + 1) + 8);
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
        // Avoid Screen's menu blur — 1.21.8+ allows only one blur per frame.
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelLeft = width / 2 - PANEL_WIDTH / 2;
        graphics.fill(panelLeft - 4, 8, panelLeft + PANEL_WIDTH + 4, height - 8, 0xE010161E);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFE8C86A);

        graphics.enableScissor(panelLeft, listTop, panelLeft + PANEL_WIDTH, listBottom);
        int y = listTop - (int) scrollOffset;
        for (Row row : rows) {
            int h = rowHeight(row);
            if (y + h >= listTop && y <= listBottom) {
                renderRow(graphics, row, panelLeft, y, h, mouseX, mouseY);
            }
            y += h;
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

        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void renderRow(GuiGraphics graphics, Row row, int panelLeft, int y, int height, int mouseX, int mouseY) {
        int rowBottom = y + height - 4;
        boolean hovered = mouseX >= panelLeft && mouseX < panelLeft + PANEL_WIDTH
            && mouseY >= Math.max(y, listTop) && mouseY < Math.min(rowBottom, listBottom);
        graphics.fill(panelLeft, y, panelLeft + PANEL_WIDTH - 8, rowBottom, hovered ? 0x4424384A : 0x33182028);

        int textX = panelLeft + PAD;
        if (row.icon() != null) {
            ItemStack stack = new ItemStack(row.icon());
            graphics.renderItem(stack, panelLeft + PAD, y + 10);
            graphics.renderItemDecorations(font, stack, panelLeft + PAD, y + 10);
            textX = panelLeft + PAD + 24;
        }

        int textWidth = PANEL_WIDTH - PAD * 2 - (row.icon() != null ? 24 : 0);
        int bodyY = y + 6;
        boolean first = true;
        for (Component line : row.lines()) {
            int color = first ? 0xFFF0E6C8 : 0xFFB0C4D8;
            if (row.entityLine() != null && line == row.entityLine()) {
                color = 0xFF9FE8A0;
            }
            for (var seq : font.split(line, textWidth)) {
                if (bodyY + font.lineHeight > rowBottom - 2) {
                    return;
                }
                graphics.drawString(font, seq, textX, bodyY, color, false);
                bodyY += font.lineHeight + 1;
            }
            first = false;
        }
    }

    @Override
    public void onClose() {
        dev.naturalis.client.SurvivalAsTraitsClientPending.dismiss();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Row(net.minecraft.world.item.Item icon, List<Component> lines, Component entityLine) {
        static Row header(Component... lines) {
            return new Row(null, List.of(lines), null);
        }

        static Row trait(MorphTraitGuideCatalog.Entry entry, Component entityLine) {
            List<Component> lines = new ArrayList<>();
            lines.add(entry.title());
            lines.add(entry.body());
            if (entityLine != null) {
                lines.add(entityLine);
            }
            return new Row(entry.icon(), lines, entityLine);
        }
    }
}
