package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.screen.MorphQuickSlotAssignScreen;
import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge fallback for shift-right-click assign when Remorphed pseudo-mixins are skipped.
 */
@Mod.EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphQuickSlotAssignForgeEvents {

    private MorphQuickSlotAssignForgeEvents() {
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
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new MorphQuickSlotAssignScreen(morphId)));
    }
}
