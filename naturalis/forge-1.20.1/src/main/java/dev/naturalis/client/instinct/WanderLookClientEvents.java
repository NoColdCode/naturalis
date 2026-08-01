package dev.naturalis.client.instinct;

import dev.naturalis.Naturalis;
import dev.naturalis.instinct.InstinctClientDebug;
import dev.naturalis.instinct.InstinctDebug;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies wander look on the client after the vanilla camera is set up (integrated server packet timing).
 */
@Mod.EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class WanderLookClientEvents {

    private WanderLookClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            WanderLookClientState.tick(mc.player);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        WanderLookClientState.applyAfterCameraSetup(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!InstinctDebug.enabled() || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        int y = 4;
        var font = Minecraft.getInstance().font;
        for (String line : InstinctClientDebug.hudLines()) {
            event.getGuiGraphics().drawString(font, line, 4, y, 0xFF88CCFF, true);
            y += 10;
        }
        if (WanderLookClientState.isActive()) {
            String status = String.format(
                "wander-look active yaw=%.1f camDrift=%.1f",
                WanderLookClientState.targetYaw(),
                WanderLookClientState.lastCameraDrift()
            );
            event.getGuiGraphics().drawString(font, status, 4, y, 0xFFFFCC66, true);
        }
    }
}
