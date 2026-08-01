package dev.naturalis.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Direct inventory entity preview for NeoForge 1.21.x. */
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
        InventoryScreen.renderEntityInInventory(
            graphics,
            x1,
            y1,
            x2,
            y2,
            (int) renderScale,
            new Vector3f(),
            new Quaternionf().rotationXYZ(0.43633232F, (float) Math.PI + spin, (float) Math.PI),
            null,
            entity
        );
    }
}
