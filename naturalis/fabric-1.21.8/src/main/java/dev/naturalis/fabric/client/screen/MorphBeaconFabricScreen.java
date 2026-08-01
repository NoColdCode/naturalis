package dev.naturalis.fabric.client.screen;

import dev.naturalis.fabric.menu.MorphBeaconFabricMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

/**
 * MorphBeacon GUI Screen - Allows setting target morph and viewing pyramid status.
 */
public class MorphBeaconFabricScreen extends AbstractContainerScreen<MorphBeaconFabricMenu> {

    private static final String[] TARGET_LABELS = {
        "gui.naturalis.morph_beacon.target.enemies",
        "gui.naturalis.morph_beacon.target.players",
        "gui.naturalis.morph_beacon.target.passive",
        "gui.naturalis.morph_beacon.target.all_mobs",
        "gui.naturalis.morph_beacon.target.all_living"
    };

    private static final int TARGET_X = 6;
    private static final int TARGET_Y = 28;
    private static final int TARGET_W = 106;
    private static final int TARGET_H = 13;
    private static final int TARGET_GAP = 2;

    private static final int PREVIEW_X1 = 116;
    private static final int PREVIEW_Y1 = 28;
    private static final int PREVIEW_X2 = 234;
    private static final int PREVIEW_Y2 = 118;

    private EditBox morphInput;
    private Button applyButton;

    @Nullable
    private LivingEntity previewEntity;
    private String lastPreviewId = "";

    public MorphBeaconFabricScreen(MorphBeaconFabricMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 244;
        this.imageHeight = 178;
        this.inventoryLabelY = 9999;
    }

    @Override
    protected void init() {
        super.init();

        morphInput = new EditBox(this.font, leftPos + PREVIEW_X1 + 2, topPos + PREVIEW_Y2 + 6, 96, 12,
            Component.translatable("gui.naturalis.morph_beacon.morph_input"));
        morphInput.setMaxLength(128);
        morphInput.setValue(menu.initialMorphId());
        morphInput.setResponder(s -> {});
        addRenderableWidget(morphInput);

        applyButton = Button.builder(
                Component.translatable("gui.naturalis.morph_beacon.apply"),
                btn -> sendMorphId())
            .bounds(leftPos + PREVIEW_X1 + 102, topPos + PREVIEW_Y2 + 4, 30, 14)
            .build();
        addRenderableWidget(applyButton);

        updatePreviewEntity(menu.initialMorphId());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF121A28, 0xFF1D2230);
        g.fill(x, y, x + imageWidth, y + 1, 0xFF5FA6E8);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF0C1018);

        int ty = y + TARGET_Y;
        for (int i = 0; i < TARGET_LABELS.length; i++) {
            int by = ty + i * (TARGET_H + TARGET_GAP);
            boolean selected = menu.targetMode() == i;
            g.fill(x + TARGET_X, by, x + TARGET_X + TARGET_W, by + TARGET_H,
                selected ? 0xAA3C7AB7 : 0x66323A4A);
            g.drawString(this.font, Component.translatable(TARGET_LABELS[i]),
                x + TARGET_X + 4, by + 3, selected ? 0xFFD8F0FF : 0xFF9CB1CB, false);
        }

        g.fill(x + 113, y + 26, x + 114, y + imageHeight - 4, 0x44AACCFF);

        g.fill(x + PREVIEW_X1, y + PREVIEW_Y1, x + PREVIEW_X2, y + PREVIEW_Y1 + 1, 0x44AACCFF);
        g.fill(x + PREVIEW_X1, y + PREVIEW_Y2 - 1, x + PREVIEW_X2, y + PREVIEW_Y2, 0x44AACCFF);
        g.fill(x + PREVIEW_X1, y + PREVIEW_Y1, x + PREVIEW_X1 + 1, y + PREVIEW_Y2, 0x44AACCFF);
        g.fill(x + PREVIEW_X2 - 1, y + PREVIEW_Y1, x + PREVIEW_X2, y + PREVIEW_Y2, 0x44AACCFF);

        if (previewEntity != null) {
            float bbMax = Math.max(previewEntity.getBbHeight(), previewEntity.getBbWidth());
            int scale = (int) Math.max(1, 40.0 / bbMax);
            InventoryScreen.renderEntityInInventory(g,
                x + PREVIEW_X1 + 2, y + PREVIEW_Y1 + 2, x + PREVIEW_X2 - 2, y + PREVIEW_Y2 - 2,
                scale,
                new Vector3f(),
                new Quaternionf().rotationXYZ(0.3F, (float) Math.PI, (float) Math.PI),
                null,
                previewEntity);
        } else if (!morphInput.getValue().isEmpty()) {
            String msg = Component.translatable("gui.naturalis.morph_beacon.invalid_morph").getString();
            g.drawString(this.font, msg,
                x + (PREVIEW_X1 + PREVIEW_X2) / 2 - this.font.width(msg) / 2,
                y + (PREVIEW_Y1 + PREVIEW_Y2) / 2 - 4, 0xFF886666, false);
        } else {
            String msg = Component.translatable("gui.naturalis.morph_beacon.no_morph").getString();
            g.drawString(this.font, msg,
                x + (PREVIEW_X1 + PREVIEW_X2) / 2 - this.font.width(msg) / 2,
                y + (PREVIEW_Y1 + PREVIEW_Y2) / 2 - 4, 0xFF556677, false);
        }

        g.fill(x + PREVIEW_X1 + 1, y + PREVIEW_Y2 + 5,
            x + PREVIEW_X1 + 100, y + PREVIEW_Y2 + 18, 0x88000000);
        morphInput.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, title, 6, 8, 0xFFB9E2FF, false);
        g.drawString(this.font, Component.translatable("gui.naturalis.morph_beacon.level", menu.pyramidLevel()),
            6, 17, 0xFFE8D9A6, false);
        int range = menu.range();
        String rangeStr = range > 0
            ? Component.translatable("gui.naturalis.morph_beacon.range", range).getString()
            : Component.translatable("gui.naturalis.morph_beacon.no_pyramid").getString();
        g.drawString(this.font, rangeStr, 70, 17, range > 0 ? 0xFF9FD8FF : 0xFF886666, false);
        g.drawString(this.font, Component.translatable("gui.naturalis.morph_beacon.morph_label"),
            PREVIEW_X1, PREVIEW_Y2 + 3, 0xFF8AACCC, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = leftPos;
            int y = topPos;
            int ty = y + TARGET_Y;
            for (int i = 0; i < TARGET_LABELS.length; i++) {
                int by = ty + i * (TARGET_H + TARGET_GAP);
                if (mouseX >= x + TARGET_X && mouseX < x + TARGET_X + TARGET_W
                    && mouseY >= by && mouseY < by + TARGET_H) {
                    this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    return true;
                }
            }
        }
        if (morphInput.mouseClicked(mouseX, mouseY, button)) {
            setFocused(morphInput);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (morphInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                sendMorphId();
                return true;
            }
            if (keyCode == 256) {
                morphInput.setFocused(false);
                return false;
            }
            if (morphInput.keyPressed(keyCode, scanCode, modifiers)) {
                updatePreviewEntity(morphInput.getValue());
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (morphInput.isFocused() && morphInput.charTyped(codePoint, modifiers)) {
            updatePreviewEntity(morphInput.getValue());
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    private void sendMorphId() {
        String id = morphInput.getValue().trim();
        // TODO: Send packet to server to update beacon morph ID
        // Objects.requireNonNull(minecraft.getConnection())
        //     .send(new SetBeaconMorphPayload(menu.getBlockPos(), id));
        updatePreviewEntity(id);
    }

    private void updatePreviewEntity(String morphId) {
        if (morphId.equals(lastPreviewId)) return;
        lastPreviewId = morphId;
        previewEntity = null;
        if (morphId.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(morphId);
        if (id == null) return;
        Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (opt.isEmpty()) {
            id = ResourceLocation.tryParse("minecraft:" + morphId);
            if (id != null) opt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        }
        if (opt.isEmpty()) return;
        try {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                var entity = opt.get().create(level, EntitySpawnReason.LOAD);
                if (entity instanceof LivingEntity living) {
                    previewEntity = living;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
