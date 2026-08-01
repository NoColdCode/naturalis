package dev.naturalis.client;

import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import dev.naturalis.morph.quickslot.MorphQuickSlotDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Shared release handling for the morph quick-slot wheel. */
public final class MorphQuickSlotReleaseHelper {

    private MorphQuickSlotReleaseHelper() {
    }

    public static void onWheelRelease(Minecraft client, boolean wheelWasOpen, int releaseHovered) {
        if (!wheelWasOpen || client.getConnection() == null) {
            return;
        }

        int selected = MorphQuickSlotWheelInput.resolveSelection(releaseHovered);
        ResourceLocation morphId = resolveMorphId(releaseHovered, selected);
        MorphQuickSlotDebug.event(
            "select",
            "onWheelRelease selected=" + selected + " morph=" + (morphId == null ? "null" : morphId)
        );
        if (selected < 0 && morphId == null) {
            MorphQuickSlotDebug.event("select", "wheel release with no selection — skip morph");
            return;
        }
        MorphQuickSlotClientActions.sendSelect(selected, morphId);
    }

    @Nullable
    public static ResourceLocation resolveMorphId(int releaseHovered, int selected) {
        if (selected >= 0 && selected < MorphQuickSlotCategory.SLOT_COUNT) {
            ResourceLocation morphId = MorphQuickSlotClientState.slot(selected);
            if (morphId != null) {
                return morphId;
            }
        }
        if (releaseHovered >= 0 && releaseHovered < MorphQuickSlotCategory.SLOT_COUNT) {
            ResourceLocation hoveredMorph = MorphQuickSlotClientState.slot(releaseHovered);
            if (hoveredMorph != null) {
                return hoveredMorph;
            }
        }
        return MorphQuickSlotWheelInput.lastHoveredMorph();
    }
}
