package dev.naturalis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.naturalis.NaturalisMod;
import dev.naturalis.client.perception.MorphSniffClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

@EventBusSubscriber(modid = NaturalisMod.ID, value = Dist.CLIENT)
public final class ScentTrailWorldRenderer {

    private static final int STRAND_COUNT = 3;
    private static final float COLOR_BOOST = 1.35F;

    private ScentTrailWorldRenderer() {
    }

    private static boolean shouldRender() {
        if (!ScentTrailClient.hasRibbonGeometry()) {
            return false;
        }
        return MorphSniffClientState.isScentVisionActive() || ScentTrailClient.hasDeepRibbons();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !shouldRender()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        float partialTick = ClientPartialTick.get(mc);
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        renderToBuffer(buffers, poseStack.last().pose(), camera, partialTick, 1.0F);
        buffers.endBatch(ScentRibbonRenderTypes.ribbon());
        poseStack.popPose();
    }

    private static void renderToBuffer(
        MultiBufferSource.BufferSource buffers,
        Matrix4f matrix,
        Camera camera,
        float partialTick,
        float alphaScale
    ) {
        VertexConsumer consumer = buffers.getBuffer(ScentRibbonRenderTypes.ribbon());
        int light = ScentRibbonRenderTypes.fullBrightLight();
        int overlay = ScentRibbonRenderTypes.noOverlay();
        Vec3 cam = camera.getPosition();
        Minecraft mc = Minecraft.getInstance();

        for (ScentTrailClient.ScentRibbon ribbon : ScentTrailClient.activeRibbons()) {
            List<Vec3> path = ScentTrailClient.renderPath(ribbon, mc, partialTick);
            if (path.size() < 2) {
                continue;
            }
            float alpha = ribbon.alpha() * alphaScale;
            int argb = ScentTrailClient.ribbonColor(ribbon.category(), alpha);
            float a = ((argb >> 24) & 0xFF) / 255.0F;
            float r = Math.min(1.0F, ((argb >> 16) & 0xFF) / 255.0F * COLOR_BOOST);
            float g = Math.min(1.0F, ((argb >> 8) & 0xFF) / 255.0F * COLOR_BOOST);
            float b = Math.min(1.0F, (argb & 0xFF) / 255.0F * COLOR_BOOST);
            drawMultiStrandRibbon(
                consumer,
                matrix,
                path,
                cam,
                ribbon.key(),
                path.size() - 1,
                ScentTrailClient.ribbonHalfWidth(ribbon),
                r,
                g,
                b,
                a,
                light,
                overlay
            );
        }
    }

    private static void drawMultiStrandRibbon(
        VertexConsumer consumer,
        Matrix4f matrix,
        List<Vec3> path,
        Vec3 camera,
        int ribbonSeed,
        int segmentCount,
        float baseHalfWidth,
        float r,
        float g,
        float b,
        float alpha,
        int light,
        int overlay
    ) {
        int segments = path.size() - 1;
        for (int i = 0; i < segments; i++) {
            Vec3 from = path.get(i);
            Vec3 to = path.get(i + 1);
            if (shouldSkipSegment(camera, from, to)) {
                continue;
            }
            Vec3 delta = to.subtract(from);
            double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            double vert = Math.abs(delta.y);
            if (horiz < 0.03D || vert > horiz * 0.55D) {
                continue;
            }
            float along0 = i / (float) Math.max(1, segments);
            float along1 = (i + 1) / (float) Math.max(1, segments);
            for (int strand = 0; strand < STRAND_COUNT; strand++) {
                float strandAlpha = alpha * (0.7F + 0.3F * (1.0F - Math.abs(strand - 1) / 1.5F));
                drawStrandSegment(
                    consumer,
                    matrix,
                    from,
                    to,
                    camera,
                    ribbonSeed,
                    segmentCount,
                    i,
                    strand,
                    along0,
                    along1,
                    baseHalfWidth,
                    r,
                    g,
                    b,
                    strandAlpha,
                    light,
                    overlay
                );
            }
        }
    }

    private static boolean shouldSkipSegment(Vec3 camera, Vec3 from, Vec3 to) {
        Vec3 mid = from.add(to).scale(0.5D);
        if (camera.distanceTo(mid) >= 1.25D) {
            return false;
        }
        Vec3 dir = mid.subtract(camera);
        if (dir.lengthSqr() < 1.0E-4D) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getViewVector(1.0F).dot(dir.normalize()) > 0.93D;
    }

    private static void drawStrandSegment(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3 from,
        Vec3 to,
        Vec3 camera,
        int ribbonSeed,
        int pathSegments,
        int segmentIndex,
        int strandIndex,
        float along0,
        float along1,
        float baseHalfWidth,
        float r,
        float g,
        float b,
        float alpha,
        int light,
        int overlay
    ) {
        Vec3 tangent = to.subtract(from);
        double segLen = tangent.length();
        if (segLen < 0.03D) {
            return;
        }
        tangent = tangent.scale(1.0D / segLen);

        Vec3 mid = from.add(to).scale(0.5D);
        Vec3 viewDir = camera.subtract(mid);
        if (viewDir.lengthSqr() < 1.0E-6D) {
            viewDir = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            viewDir = viewDir.normalize();
        }

        Vec3 binormal = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (binormal.lengthSqr() < 1.0E-5D) {
            binormal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        binormal = binormal.normalize();

        float weaveAmp = baseHalfWidth * 1.4F;
        Vec3 lateral0 = weaveLateral(binormal, ribbonSeed, strandIndex, along0, pathSegments, weaveAmp);
        Vec3 lateral1 = weaveLateral(binormal, ribbonSeed, strandIndex, along1, pathSegments, weaveAmp);

        Vec3 billboard = viewDir.cross(tangent);
        if (billboard.lengthSqr() < 1.0E-5D) {
            billboard = binormal;
        }
        billboard = billboard.normalize();

        float faceView = Mth.abs((float) billboard.dot(viewDir));
        float stretch = Mth.clamp(0.45F + faceView * 0.75F, 0.45F, 1.0F);
        float halfWide = baseHalfWidth * stretch * (0.9F + 0.1F * Mth.sin((along0 + along1) * 6.0F + strandIndex));
        float halfThin = baseHalfWidth * 0.16F;

        Vec3 c0 = from.add(lateral0);
        Vec3 c1 = to.add(lateral1);

        Vec3 w = billboard.scale(halfWide);
        Vec3 t = tangent.cross(billboard).normalize().scale(halfThin);

        float edgeA = alpha * 0.12F;
        float coreA = alpha * (0.78F + 0.22F * faceView);

        addFlatQuad(consumer, matrix, c0.subtract(w).subtract(t), c0.add(w).subtract(t), c1.add(w).subtract(t), c1.subtract(w).subtract(t),
            r, g, b, edgeA, 0.0F, 0.0F, 1.0F, 1.0F, light, overlay);
        addFlatQuad(consumer, matrix, c0.subtract(w).add(t), c0.add(w).add(t), c1.add(w).add(t), c1.subtract(w).add(t),
            r, g, b, coreA, 0.25F, 0.0F, 0.75F, 1.0F, light, overlay);
    }

    private static Vec3 weaveLateral(
        Vec3 binormal,
        int ribbonSeed,
        int strandIndex,
        float along,
        int pathSegments,
        float amplitude
    ) {
        float phase = ribbonSeed * 0.17F + strandIndex * 2.09F;
        float freq = 5.8F + strandIndex * 0.65F;
        float alongWave = (float) Math.sin(along * freq + phase);
        float pathWave = (float) Math.sin((along * pathSegments + strandIndex * 1.7F) * 0.42F + ribbonSeed * 0.13F);
        float mix = alongWave * 0.72F + pathWave * 0.28F;
        float strandOffset = (strandIndex - 1) * amplitude * 0.38F;
        return binormal.scale(mix * amplitude + strandOffset);
    }

    private static void addFlatQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3 v0,
        Vec3 v1,
        Vec3 v2,
        Vec3 v3,
        float r,
        float g,
        float b,
        float alpha,
        float u0,
        float vStart,
        float u1,
        float vEnd,
        int light,
        int overlay
    ) {
        consumer.addVertex(matrix, (float) v0.x, (float) v0.y, (float) v0.z)
            .setColor(r, g, b, alpha).setUv(u0, vStart).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z)
            .setColor(r, g, b, alpha).setUv(u0, vEnd).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z)
            .setColor(r, g, b, alpha).setUv(u1, vEnd).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z)
            .setColor(r, g, b, alpha).setUv(u1, vStart).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
    }
}
