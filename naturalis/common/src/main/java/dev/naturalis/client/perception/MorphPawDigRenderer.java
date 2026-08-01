package dev.naturalis.client.perception;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Renders both mob front legs in first person while digging (wolf paws, fox legs, etc.).
 */
public final class MorphPawDigRenderer {

    private MorphPawDigRenderer() {
    }

    public static void render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        AbstractClientPlayer player,
        float partialTick,
        @Nullable ResourceLocation textureOverride
    ) {
        MorphEmbodimentProfile profile = MorphEmbodimentLogic.profileFor(player);
        if (!MorphEmbodimentLogic.usesPawDigging(profile) || MorphDigClientState.digAnim() < 0.05F) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        var limbsOpt = MorphPawDigModelCache.resolve(morphId);
        if (limbsOpt.isEmpty()) {
            return;
        }

        MorphPawDigModelCache.MorphPawDigLimbs limbs = limbsOpt.get();
        ResourceLocation texture = textureOverride != null ? textureOverride : limbs.texture();

        float dig = MorphDigClientState.digAnim();
        float progress = Math.max(MorphDigClientState.destroyProgress(), dig * 0.35F);
        float intensity = Mth.clamp(0.5F + progress * 0.5F + dig * 0.25F, 0.0F, 1.0F);
        float phase = player.tickCount + partialTick;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        float scale = limbs.scale() * (1.18F + intensity * 0.28F);

        renderLeg(poseStack, consumer, packedLight, limbs.leftFrontLeg(), -1.0F, phase, intensity, scale);
        renderLeg(poseStack, consumer, packedLight, limbs.rightFrontLeg(), 1.0F, phase, intensity, scale);
    }

    private static void renderLeg(
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        ModelPart leg,
        float side,
        float phase,
        float intensity,
        float scale
    ) {
        leg.resetPose();

        float rake = (float) Math.max(0.0D, Math.sin(phase * 4.2D + (side > 0 ? 0.85D : 0.0D)));
        float alt = (float) Math.sin(phase * 2.1D + (side > 0 ? 0.0D : Math.PI));
        float strike = (float) Math.max(0.0D, Math.sin(phase * 4.2D + (side > 0 ? 2.1D : 0.0D)));

        leg.xRot = Mth.lerp(intensity, 0.0F, 1.15F + rake * 0.75F + strike * 0.2F);
        leg.yRot = side * alt * 0.28F * intensity;
        leg.zRot = side * (0.18F + rake * 0.12F) * intensity;

        float lateral = 0.58F + strike * 0.06F * intensity;
        float forward = -0.66F - rake * 0.14F * intensity + side * strike * 0.04F;
        float vertical = -0.48F + rake * 0.09F * intensity;

        poseStack.pushPose();
        poseStack.translate(side * lateral, vertical, forward);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-32.0F - rake * 26.0F * intensity - strike * 8.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (10.0F + alt * 8.0F) * intensity));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (rake * 14.0F + 6.0F) * intensity));
        leg.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
