package dev.naturalis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the main-hand item at the morph mouth in third- and front-third-person views (Forge 1.20.1).
 */
public final class MorphQuadrupedMouthRender {

    private MorphQuadrupedMouthRender() {
    }

    public static boolean shouldShowMouthCarry(AbstractClientPlayer player, Minecraft mc) {
        if (player == null || mc == null) {
            return false;
        }
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return false;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        return MorphQuadrupedShapeCache.isQuadrupedMorph(morphId);
    }

    public static void hideShapeHandItems(LivingEntity shape) {
        if (shape == null) {
            return;
        }
        shape.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        shape.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        shape.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        shape.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    public static void renderInEntitySpace(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        AbstractClientPlayer player,
        LivingEntity shape,
        ItemStack stack,
        float partialTick
    ) {
        render(poseStack, bufferSource, packedLight, player, shape, stack, partialTick, false);
    }

    private static void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        AbstractClientPlayer player,
        LivingEntity shape,
        ItemStack stack,
        float partialTick,
        boolean applyWorldTranslation
    ) {
        if (player == null || shape == null || stack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();

        float headYaw = Mth.lerp(partialTick, shape.yHeadRotO, shape.yHeadRot);
        float headPitch = Mth.lerp(partialTick, shape.xRotO, shape.getXRot());
        float bodyYaw = Mth.lerp(partialTick, shape.yBodyRotO, shape.yBodyRot);

        poseStack.pushPose();
        if (applyWorldTranslation) {
            poseStack.translate(
                Mth.lerp(partialTick, shape.xo, shape.getX()),
                Mth.lerp(partialTick, shape.yo, shape.getY()),
                Mth.lerp(partialTick, shape.zo, shape.getZ())
            );
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        float headOffsetY = shape.getBbHeight() * 0.42F;
        float headForward = shape.getBbWidth() * 0.58F;
        poseStack.translate(0.0F, headOffsetY, headForward);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees(headYaw - bodyYaw)));
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch * 0.55F));
        poseStack.scale(0.58F, 0.58F, 0.58F);

        itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.GROUND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            player.level(),
            player.getId()
        );
        poseStack.popPose();
    }
}
