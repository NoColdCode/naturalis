package dev.naturalis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class ForgeScentVisionMobRenderer {

    private static final float OUTLINE_SCALE = 1.034F;

    private ForgeScentVisionMobRenderer() {
    }

    public static void render(
        EntityModel<?> model,
        PoseStack poseStack,
        MultiBufferSource buffer,
        ResourceLocation texture,
        int outlineArgb,
        int fillArgb,
        int packedLight,
        int overlay
    ) {
        RenderType cutout = RenderType.entityCutout(texture);
        RenderType outlineType = RenderType.entityCutoutNoCull(texture);

        poseStack.pushPose();
        poseStack.scale(OUTLINE_SCALE, OUTLINE_SCALE, OUTLINE_SCALE);
        VertexConsumer outline = new ForgeScentVisionFlatColorVertexConsumer(buffer.getBuffer(outlineType), outlineArgb);
        model.renderToBuffer(poseStack, outline, packedLight, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        endBatch(buffer, outlineType);

        VertexConsumer fill = new ForgeScentVisionFlatColorVertexConsumer(buffer.getBuffer(cutout), fillArgb);
        model.renderToBuffer(poseStack, fill, packedLight, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        endBatch(buffer, cutout);
    }

    private static void endBatch(MultiBufferSource buffer, RenderType type) {
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }
}
