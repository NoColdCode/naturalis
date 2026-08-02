package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.util.CurrentMorphUtil;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class MorphHandOffsetClientEvents {

    // Normalization range matches configured multipliers in MorphFovClientEvents.
    private static final double MIN_MULTIPLIER = 1.0D;
    private static final double MAX_MULTIPLIER = 1.40D;
    private static float breakAnim;
    private static float placeAnim;

    private MorphHandOffsetClientEvents() {
    }

    /**
     * Run last: other mods may cancel {@link RenderHandEvent} after posting. If we mutate the pose stack
     * and the hand draw is skipped, vanilla can under-pop the matrix stack and later crash in {@code GameRenderer}.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (event.isCanceled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        boolean isBreaking = mc.options.keyAttack.isDown()
            && mc.hitResult != null
            && mc.hitResult.getType() == HitResult.Type.BLOCK;
        boolean isPlacing = mc.options.keyUse.isDown()
            && mc.hitResult != null
            && mc.hitResult.getType() == HitResult.Type.BLOCK
            && mc.player.getItemInHand(event.getHand()).getItem() instanceof BlockItem;

        breakAnim = Mth.lerp(0.35F, breakAnim, isBreaking ? 1.0F : 0.0F);
        placeAnim = Mth.lerp(0.35F, placeAnim, isPlacing ? 1.0F : 0.0F);

        double multiplier = MorphFovLogic.getActiveMorphFovMultiplier(mc);
        if (multiplier <= MIN_MULTIPLIER + 1.0E-6D) {
            // Keep animation optional to morph only; normal player remains vanilla.
            return;
        }

        double t = (multiplier - MIN_MULTIPLIER) / (MAX_MULTIPLIER - MIN_MULTIPLIER);
        t = Math.max(0.0D, Math.min(1.0D, t));

        double side = event.getHand() == InteractionHand.MAIN_HAND ? 1.0D : -1.0D;

        // Push arm slightly outward/down when FOV is widened to keep full paw/hand out of frame.
        double xOffset = 0.18D * t * side;
        double yOffset = -0.06D * t;
        double zOffset = 0.02D * t;
        event.getPoseStack().translate(xOffset, yOffset, zOffset);

        if (morphId != null) {
            applyMorphInteractionMotion(event, morphId, side);
        }
    }

    private static void applyMorphInteractionMotion(RenderHandEvent event, ResourceLocation morphId, double side) {
        if (breakAnim < 0.01F && placeAnim < 0.01F) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float time = (mc.player != null ? mc.player.tickCount : 0);
        String path = morphId.getPath();

        double breakPhase = Math.sin(time * 1.7D);
        double placePhase = Math.sin(time * 1.35D + 0.6D);

        if (isSpiderLike(path)) {
            // Fast, twitchy limb-like motions.
            event.getPoseStack().translate(0.11D * breakAnim * side * breakPhase, -0.06D * breakAnim, -0.08D * breakAnim);
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees((float) (7.5D * breakAnim * breakPhase * side)));
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees((float) (-10.0D * breakAnim + -6.0D * placeAnim * placePhase)));
        } else if (isCanineOrFeline(path)) {
            // Paw-swipe style attack-and-press motion.
            event.getPoseStack().translate(0.09D * breakAnim * side * breakPhase, -0.04D * breakAnim, -0.06D * breakAnim);
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees((float) (8.0D * breakAnim * side * breakPhase)));
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees((float) (-8.0D * breakAnim + -4.0D * placeAnim * placePhase)));
        } else if (isHoofed(path)) {
            // Heavy head-bump style movement.
            event.getPoseStack().translate(0.04D * breakAnim * side, -0.08D * breakAnim * Math.abs(breakPhase), -0.10D * breakAnim);
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees((float) (-13.0D * breakAnim - 5.0D * placeAnim * placePhase)));
        } else if (isAvian(path)) {
            // Pecking, dart-like interactions.
            event.getPoseStack().translate(0.03D * side * breakAnim * breakPhase, -0.03D * breakAnim, -0.12D * (breakAnim + 0.5F * placeAnim * (float) Math.abs(placePhase)));
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees((float) (-15.0D * breakAnim - 9.0D * placeAnim * placePhase)));
        } else if (isAquatic(path)) {
            // Fluid, swaying aquatic manipulation.
            event.getPoseStack().translate(0.06D * side * (breakAnim + placeAnim) * breakPhase, -0.02D * breakAnim, -0.05D * (breakAnim + placeAnim));
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees((float) (5.0D * (breakAnim + placeAnim) * breakPhase * side)));
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees((float) (-6.0D * breakAnim - 4.0D * placeAnim * placePhase)));
        } else {
            // Generic animal head/forelimb nudge.
            event.getPoseStack().translate(0.05D * breakAnim * side * breakPhase, -0.03D * breakAnim, -0.05D * (breakAnim + 0.5F * placeAnim));
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees((float) (-7.0D * breakAnim - 4.0D * placeAnim * placePhase)));
        }
    }

    private static boolean isSpiderLike(String path) {
        return path.contains("spider") || path.contains("mite") || path.contains("silverfish");
    }

    private static boolean isCanineOrFeline(String path) {
        return path.contains("wolf") || path.contains("fox") || path.contains("cat") || path.contains("ocelot");
    }

    private static boolean isHoofed(String path) {
        return path.contains("horse") || path.contains("goat") || path.contains("cow") || path.contains("sheep") || path.contains("camel") || path.contains("llama");
    }

    private static boolean isAvian(String path) {
        return path.contains("bird") || path.contains("chicken") || path.contains("parrot") || path.contains("bat") || path.contains("phantom");
    }

    private static boolean isAquatic(String path) {
        return path.contains("fish") || path.contains("guardian") || path.contains("dolphin") || path.contains("squid") || path.contains("axolotl") || path.contains("turtle");
    }
}
