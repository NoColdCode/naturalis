package dev.naturalis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class NeoForge121ScentVisionMobRenderer {

    private static final float OUTLINE_SCALE = 1.034F;

    private NeoForge121ScentVisionMobRenderer() {
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
        RenderType outlineType = RenderType.entitySmoothCutout(texture);

        poseStack.pushPose();
        poseStack.scale(OUTLINE_SCALE, OUTLINE_SCALE, OUTLINE_SCALE);
        VertexConsumer outline = new ScentVisionFlatColorVertexConsumer(buffer.getBuffer(outlineType), outlineArgb);
        model.renderToBuffer(poseStack, outline, packedLight, overlay, -1);
        poseStack.popPose();
        endBatch(buffer, outlineType);

        VertexConsumer fill = new ScentVisionFlatColorVertexConsumer(buffer.getBuffer(cutout), fillArgb);
        model.renderToBuffer(poseStack, fill, packedLight, overlay, -1);
        endBatch(buffer, cutout);
    }

    private static void endBatch(MultiBufferSource buffer, RenderType type) {
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }
}
