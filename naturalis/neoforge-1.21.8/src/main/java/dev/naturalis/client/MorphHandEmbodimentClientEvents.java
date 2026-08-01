package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.perception.MorphArmInteractionLogic;
import dev.naturalis.client.perception.MorphEmbodimentLogic;
import dev.naturalis.client.perception.MorphEmbodimentProfile;
import dev.naturalis.client.perception.MorphEmbodimentProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphHandEmbodimentClientEvents {

    private static float breakAnim;
    private static float placeAnim;

    private MorphHandEmbodimentClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (event.isCanceled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        MorphEmbodimentProfile profile = MorphEmbodimentLogic.profileFor(mc.player);
        if (!profile.hasEmbodiment()) {
            return;
        }

        // Full FP morph body is drawn by NaturalisFirstPersonMorphMixin + Walkers shape state.
        if (MorphEmbodimentLogic.shouldRenderFirstPersonMorphBody(profile)) {
            event.setCanceled(true);
            return;
        }

        boolean breaking = MorphEmbodimentLogic.isBreakingBlock(mc);
        boolean placing = MorphEmbodimentLogic.isPlacingBlock(mc, event.getHand());
        breakAnim = MorphArmInteractionLogic.lerpAnim(breakAnim, breaking);
        placeAnim = MorphArmInteractionLogic.lerpAnim(placeAnim, placing);

        if (MorphEmbodimentLogic.shouldHideFirstPersonArm(mc, profile, breaking, placing)
            || (breaking && MorphEmbodimentLogic.usesPawDigging(profile))) {
            // Morph limb renderer is 1.21.1-only (Walkers arm API); hide vanilla arm here.
            event.setCanceled(true);
            return;
        }

        double normalization = MorphEmbodimentProfiles.armOffsetNormalization(profile);
        if (normalization <= 1.0E-6D) {
            return;
        }

        double side = event.getHand() == InteractionHand.MAIN_HAND ? 1.0D : -1.0D;
        MorphArmInteractionLogic.applyArmBaseOffset(event.getPoseStack(), normalization, side);

        if (profile.armInteractionStyle() != dev.naturalis.client.perception.MorphArmInteractionStyle.NONE) {
            float tickTime = mc.player.tickCount + event.getPartialTick();
            MorphArmInteractionLogic.applyInteractionMotion(
                event.getPoseStack(),
                profile.armInteractionStyle(),
                breakAnim,
                placeAnim,
                side,
                tickTime
            );
        }
    }
}
