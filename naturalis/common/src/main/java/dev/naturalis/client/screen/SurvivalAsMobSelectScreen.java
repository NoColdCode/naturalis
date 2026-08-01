package dev.naturalis.client.screen;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.survivalas.SurvivalAsClientCreateState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Create-world picker: choose the mob you will live as for the entire Survival-as save.
 */
public class SurvivalAsMobSelectScreen extends Screen {

    private final Screen parent;
    private EditBox searchBox;
    private MobList list;
    private Button confirm;
    private ResourceLocation selected;
    private final List<ResourceLocation> allMobs = new ArrayList<>();

    public SurvivalAsMobSelectScreen(Screen parent) {
        super(Component.translatable("gui.naturalis.survival_as.title"));
        this.parent = parent;
        this.selected = SurvivalAsClientCreateState.getMorphId();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = CompatAccess.getEntityType(id);
            if (type == null || type == EntityType.PLAYER) {
                continue;
            }
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())
                || type.getCategory() != MobCategory.MISC) {
                allMobs.add(id);
            }
        }
        allMobs.sort(Comparator.comparing(ResourceLocation::toString));
    }

    @Override
    protected void init() {
        searchBox = new EditBox(font, width / 2 - 150, 28, 300, 20, Component.translatable("gui.naturalis.survival_as.search"));
        searchBox.setHint(Component.translatable("gui.naturalis.survival_as.search_hint"));
        searchBox.setResponder(s -> rebuildList());
        addRenderableWidget(searchBox);

        list = new MobList(minecraft, width, height - 108, 56, 20);
        addRenderableWidget(list);
        rebuildList();

        confirm = Button.builder(Component.translatable("gui.naturalis.survival_as.confirm").withStyle(ChatFormatting.GREEN), b -> {
            if (selected != null) {
                SurvivalAsClientCreateState.set(selected);
            }
            onClose();
        }).bounds(width / 2 - 155, height - 32, 150, 20).build();
        addRenderableWidget(confirm);

        addRenderableWidget(Button.builder(Component.translatable("gui.naturalis.survival_as.clear").withStyle(ChatFormatting.RED), b -> {
            SurvivalAsClientCreateState.set(null);
            selected = null;
            SurvivalAsClientCreateState.selectMode();
            onClose();
        }).bounds(width / 2 + 5, height - 32, 150, 20).build());

        updateConfirm();
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        list.clearEntries();
        for (ResourceLocation id : allMobs) {
            if (!matches(id, q)) {
                continue;
            }
            list.addMob(id);
        }
        if (selected != null) {
            list.selectId(selected);
        }
    }

    private boolean matches(ResourceLocation id, String q) {
        if (q.isEmpty()) {
            return true;
        }
        if (id.toString().contains(q) || id.getPath().contains(q)) {
            return true;
        }
        EntityType<?> type = CompatAccess.getEntityType(id);
        if (type == null) {
            return false;
        }
        return type.getDescription().getString().toLowerCase(Locale.ROOT).contains(q);
    }

    private void updateConfirm() {
        if (confirm != null) {
            confirm.active = selected != null;
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid Screen's menu blur — 1.21.8+ allows only one blur per frame.
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 1.21.8+ GuiGraphics.drawString ignores colors with alpha 0 — always use ARGB.
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        if (selected != null) {
            EntityType<?> type = CompatAccess.getEntityType(selected);
            Component label = type != null ? type.getDescription() : Component.literal(selected.toString());
            graphics.drawCenteredString(font,
                Component.translatable("gui.naturalis.survival_as.selected", label).withStyle(ChatFormatting.GOLD),
                width / 2, height - 48, 0xFFFFD700);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private final class MobList extends ObjectSelectionList<MobList.Entry> {
        MobList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        void addMob(ResourceLocation id) {
            addEntry(new Entry(id));
        }

        void selectId(ResourceLocation id) {
            for (Entry e : children()) {
                if (e.id.equals(id)) {
                    setSelected(e);
                    ensureVisible(e);
                    return;
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        final class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ResourceLocation id;

            Entry(ResourceLocation id) {
                this.id = id;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                EntityType<?> type = CompatAccess.getEntityType(id);
                Component name = type != null ? type.getDescription() : Component.literal(id.toString());
                // ARGB required on 1.21.8+ (alpha 0 ⇒ text not submitted).
                int color = id.equals(selected) ? 0xFFFFD700 : (hovering ? 0xFFFFFFAA : 0xFFE0E0E0);
                graphics.drawString(SurvivalAsMobSelectScreen.this.font, name, left + 4, top + 2, color, false);
                graphics.drawString(SurvivalAsMobSelectScreen.this.font, id.toString(), left + 4, top + 11, 0xFF808080, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selected = id;
                MobList.this.setSelected(this);
                updateConfirm();
                return true;
            }

            @Override
            public Component getNarration() {
                EntityType<?> type = CompatAccess.getEntityType(id);
                return type != null ? type.getDescription() : Component.literal(id.toString());
            }
        }
    }
}
