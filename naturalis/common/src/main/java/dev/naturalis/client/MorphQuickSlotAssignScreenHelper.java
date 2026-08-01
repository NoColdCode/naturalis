package dev.naturalis.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** Shared Remorphed screen hit-testing for shift-right-click quick-slot assign. */
public final class MorphQuickSlotAssignScreenHelper {

    private MorphQuickSlotAssignScreenHelper() {
    }

    public static boolean isRemorphedMorphScreen(Screen screen) {
        String name = screen.getClass().getName().toLowerCase(Locale.ROOT);
        return name.contains("remorphed");
    }

    public static ResourceLocation findMorphAt(Screen screen, double mouseX, double mouseY) {
        for (var child : screen.children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            if (!isRemorphedShapeWidget(widget)) {
                continue;
            }
            ResourceLocation morphId = MorphQuickSlotAssignSupport.resolveMorphId(widget);
            if (morphId != null) {
                return morphId;
            }
        }
        return null;
    }

    private static boolean isRemorphedShapeWidget(AbstractWidget widget) {
        String name = widget.getClass().getName();
        return name.contains("remorphed") && name.contains("Widget");
    }
}
