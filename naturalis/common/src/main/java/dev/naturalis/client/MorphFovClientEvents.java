package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class MorphFovClientEvents {

    private static final double MAX_MORPH_FOV = 150.0D;

    private MorphFovClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getCameraEntity() != mc.player) {
            return;
        }

        double multiplier = MorphFovLogic.getActiveMorphFovMultiplier(mc);
        if (Math.abs(multiplier - 1.0D) < 1.0E-6D) {
            return;
        }

        double adjustedFov = event.getFOV() * multiplier;
        event.setFOV(Math.max(30.0D, Math.min(MAX_MORPH_FOV, adjustedFov)));
    }
}
