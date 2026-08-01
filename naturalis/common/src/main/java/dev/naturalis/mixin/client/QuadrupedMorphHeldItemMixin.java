package dev.naturalis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.naturalis.client.MorphQuadrupedShapeCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides vanilla hand-item layers on Walkers morph shapes; {@link dev.naturalis.client.MorphQuadrupedMouthRender}
 * draws the held stack at the mouth instead.
 */
@Mixin(ItemInHandLayer.class)
public abstract class QuadrupedMorphHeldItemMixin {

    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void naturalis$cancelHandLayerOnMorphShape(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        ArmedEntityRenderState state,
        float limbSwing,
        float limbSwingAmount,
        CallbackInfo ci
    ) {
        if (state != null && MorphQuadrupedShapeCache.shouldHideHandItems(state)) {
            ci.cancel();
        }
    }
}
