package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.client.perception.MorphArmInteractionLogic;
import dev.naturalis.client.perception.MorphEmbodimentLogic;
import dev.naturalis.client.perception.MorphEmbodimentProfile;
import dev.naturalis.client.perception.MorphEmbodimentProfiles;
import dev.naturalis.client.perception.MorphFirstPersonMorphHandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Hides the vanilla first-person arm for embodied morphs and draws the morph limb/shape instead.
 * Required for integration morphs (Naturalist, Aether, etc.) whose renderers skip Walkers arm providers.
 */
@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
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

        boolean breaking = MorphEmbodimentLogic.isBreakingBlock(mc);
        boolean placing = MorphEmbodimentLogic.isPlacingBlock(mc, event.getHand());
        breakAnim = MorphArmInteractionLogic.lerpAnim(breakAnim, breaking);
        placeAnim = MorphArmInteractionLogic.lerpAnim(placeAnim, placing);

        boolean replaceWithMorphBody = MorphEmbodimentLogic.shouldRenderFirstPersonMorphBody(profile);
        boolean hideVanilla = MorphEmbodimentLogic.shouldHideFirstPersonArm(mc, profile, breaking, placing)
            || (breaking && MorphEmbodimentLogic.usesPawDigging(profile));

        if (replaceWithMorphBody || hideVanilla) {
            double side = event.getHand() == InteractionHand.MAIN_HAND ? 1.0D : -1.0D;
            if (mc.player instanceof AbstractClientPlayer clientPlayer) {
                MorphFirstPersonMorphHandRenderer.render(
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight(),
                    clientPlayer,
                    event.getPartialTick(),
                    side
                );
            }
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
