package dev.naturalis.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.naturalis.world.MorphBeaconBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;

/**
 * Renders a cyan beacon beam above a MorphBeacon when its pyramid is active (level >= 1).
 * Forge 1.20.1 — uses float[] color array and the 6-arg render signature (no Vec3 cameraPos).
 */
public class MorphBeaconRenderer implements BlockEntityRenderer<MorphBeaconBlockEntity> {

    /** Cyan-tinted beam color matching the echo/morph theme. */
    private static final float[] BEAM_COLOR = { 0x2E / 255f, 0xB8 / 255f, 0xE0 / 255f };

    public MorphBeaconRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MorphBeaconBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null || be.getPyramidLevel() <= 0) return;

        long gameTime = level.getGameTime();
        int maxHeight = level.getMaxBuildHeight() + 1 - be.getBlockPos().getY();

        BeaconRenderer.renderBeaconBeam(
            poseStack, bufferSource,
            BeaconRenderer.BEAM_LOCATION,
            partialTick, 1.0F, gameTime,
            0, maxHeight,
            BEAM_COLOR,
            0.15F, 0.25F
        );
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
