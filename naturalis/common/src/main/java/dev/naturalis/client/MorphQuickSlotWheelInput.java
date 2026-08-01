package dev.naturalis.client;

import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Mouse aim for the morph quick-slot wheel.
 */
public final class MorphQuickSlotWheelInput {

    private static final double OUTER_RADIUS = 118.0D;
    private static final double INNER_SNAP_RADIUS = 10.0D;

    private static int lastHoveredSlot = -1;

    private MorphQuickSlotWheelInput() {
    }

    public static void reset() {
        lastHoveredSlot = -1;
    }

    @Nullable
    public static ResourceLocation lastHoveredMorph() {
        if (lastHoveredSlot < 0 || lastHoveredSlot >= MorphQuickSlotCategory.SLOT_COUNT) {
            return null;
        }
        return MorphQuickSlotClientState.slot(lastHoveredSlot);
    }

    public static int pickHoveredSlot(Minecraft client) {
        int keySlot = pickNumberKeySlot(client);
        if (keySlot >= 0) {
            lastHoveredSlot = keySlot;
            return keySlot;
        }

        double mouseX = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        float cx = width * 0.5F;
        float cy = height * 0.5F;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > OUTER_RADIUS) {
            return -1;
        }

        if (dist < INNER_SNAP_RADIUS) {
            return lastHoveredSlot >= 0 ? lastHoveredSlot : 0;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D;
        if (angle < 0.0D) {
            angle += 360.0D;
        }

        int slice = (int) (angle / (360.0D / MorphQuickSlotCategory.SLOT_COUNT));
        if (slice < 0 || slice >= MorphQuickSlotCategory.SLOT_COUNT) {
            return -1;
        }

        lastHoveredSlot = slice;
        return slice;
    }

    /** Slot to morph into on release; falls back to the last hovered slice near the center. */
    public static int resolveSelection(int hoveredSlot) {
        int selected = hoveredSlot;
        if (selected < 0) {
            selected = lastHoveredSlot;
        }
        if (selected < 0 || selected >= MorphQuickSlotCategory.SLOT_COUNT) {
            return -1;
        }
        if (MorphQuickSlotClientState.slot(selected) == null) {
            return -1;
        }
        return selected;
    }

    private static int pickNumberKeySlot(Minecraft client) {
        KeyMapping[] hotbarKeys = client.options.keyHotbarSlots;
        int limit = Math.min(MorphQuickSlotCategory.SLOT_COUNT, hotbarKeys.length);
        for (int i = 0; i < limit; i++) {
            if (hotbarKeys[i].isDown()) {
                return i;
            }
        }
        return -1;
    }
}
