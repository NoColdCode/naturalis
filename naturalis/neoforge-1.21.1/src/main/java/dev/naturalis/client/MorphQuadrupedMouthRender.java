package dev.naturalis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tocraft.walkers.api.PlayerShape;

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
        return morphId != null && InventoryRestrictionManager.isQuadruped(morphId);
    }

    public static void hideShapeHandItems(LivingEntity shape) {
        if (shape == null) {
            return;
        }
        shape.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        shape.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        shape.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        shape.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
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
        if (player == null || shape == null || stack.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        float headYaw = Mth.lerp(partialTick, shape.yHeadRotO, shape.yHeadRot);
        float headPitch = Mth.lerp(partialTick, shape.xRotO, shape.getXRot());
        float bodyYaw = Mth.lerp(partialTick, shape.yBodyRotO, shape.yBodyRot);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.translate(0.0F, shape.getBbHeight() * 0.42F, shape.getBbWidth() * 0.58F);
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
