package dev.naturalis.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.naturalis.world.MorphBeaconBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a cyan beacon beam above a MorphBeacon when its pyramid is active (level >= 1).
 * 1.21.8 version — uses float[] color API.
 */
public class MorphBeaconRenderer implements BlockEntityRenderer<MorphBeaconBlockEntity> {

    /** Cyan-tinted beam color matching the echo/morph theme (packed ARGB). */
    private static final int BEAM_COLOR = 0xFF2EB8E0;

    public MorphBeaconRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MorphBeaconBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
        Level level = be.getLevel();
        if (level == null || be.getPyramidLevel() <= 0) return;

        long gameTime = level.getGameTime();
        int maxHeight = level.getMaxY() + 1 - be.getBlockPos().getY();

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
