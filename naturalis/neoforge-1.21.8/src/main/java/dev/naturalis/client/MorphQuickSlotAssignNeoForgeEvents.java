package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.screen.MorphQuickSlotAssignScreen;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * NeoForge fallback for shift-right-click assign when Remorphed pseudo-mixins are skipped at load time.
 */
@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphQuickSlotAssignNeoForgeEvents {

    private MorphQuickSlotAssignNeoForgeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1 || !Screen.hasShiftDown()) {
            return;
        }

        Screen screen = event.getScreen();
        if (!MorphQuickSlotAssignScreenHelper.isRemorphedMorphScreen(screen)) {
            return;
        }

        ResourceLocation morphId = MorphQuickSlotAssignScreenHelper.findMorphAt(screen, event.getMouseX(), event.getMouseY());
        if (morphId == null) {
            return;
        }

        event.setCanceled(true);
        MorphQuickSlotClientActions.requestResync();
        Minecraft.getInstance().setScreen(new MorphQuickSlotAssignScreen(morphId));
    }
}
