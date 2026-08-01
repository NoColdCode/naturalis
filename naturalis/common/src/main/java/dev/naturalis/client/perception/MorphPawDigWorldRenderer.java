package dev.naturalis.client.perception;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders both front paws in camera space during dig (avoids hand-pass overlap that hid one paw).
 */
public final class MorphPawDigWorldRenderer {

    private MorphPawDigWorldRenderer() {
    }

    public static void render(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Camera camera,
        float partialTick,
        @Nullable ResourceLocation textureOverride
    ) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) {
            return;
        }

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
        float scale = limbs.scale() * (1.22F + intensity * 0.32F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        renderLeg(poseStack, consumer, camera, limbs.leftFrontLeg(), -1.0F, phase, intensity, scale);
        renderLeg(poseStack, consumer, camera, limbs.rightFrontLeg(), 1.0F, phase, intensity, scale);

        bufferSource.endBatch(RenderType.entityCutoutNoCull(texture));
    }

    private static void renderLeg(
        PoseStack poseStack,
        VertexConsumer consumer,
        Camera camera,
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

        leg.xRot = Mth.lerp(intensity, 0.0F, 1.2F + rake * 0.8F + strike * 0.25F);
        leg.yRot = side * alt * 0.32F * intensity;
        leg.zRot = side * (0.22F + rake * 0.14F) * intensity;

        float yaw = camera.getYRot();
        float pitch = camera.getXRot();
        Quaternionf rotation = new Quaternionf()
            .rotateY((float) Math.toRadians(yaw + 180.0F))
            .rotateX((float) Math.toRadians(pitch));

        Vector3f offset = new Vector3f(
            side * (0.46F + strike * 0.08F * intensity),
            -0.38F + rake * 0.12F * intensity,
            -0.58F - rake * 0.16F * intensity - strike * 0.05F
        );
        rotation.transform(offset);

        poseStack.pushPose();
        poseStack.translate(
            camera.getPosition().x + offset.x,
            camera.getPosition().y + offset.y,
            camera.getPosition().z + offset.z
        );
        poseStack.mulPose(rotation);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-34.0F - rake * 28.0F * intensity - strike * 10.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (12.0F + alt * 10.0F) * intensity));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (rake * 16.0F + 8.0F) * intensity));

        int light = 0xF000F0;
        leg.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
