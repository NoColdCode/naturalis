package dev.naturalis.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Direct inventory entity preview for NeoForge 1.21.1. */
public final class MorphQuickSlotEntityPreviewSupport {

    private MorphQuickSlotEntityPreviewSupport() {
    }

    public static void render(
        GuiGraphics graphics,
        int x1,
        int y1,
        int x2,
        int y2,
        float renderScale,
        float spin,
        LivingEntity entity
    ) {
        float centerX = (x1 + x2) * 0.5F;
        float centerY = (y1 + y2) * 0.5F;
        InventoryScreen.renderEntityInInventory(
            graphics,
            centerX,
            centerY,
            renderScale,
            new Vector3f(),
            new Quaternionf().rotationXYZ(0.43633232F, spin, (float) Math.PI),
            new Quaternionf(),
            entity
        );
    }
}
