package dev.naturalis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.naturalis.client.MorphQuadrupedShapeCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class NeoForgeQuadrupedMorphHeldItemMixin {

    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFFF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void naturalis$cancelHandLayerOnMorphShape(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        LivingEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float animationPos,
        float scale,
        CallbackInfo ci
    ) {
        if (entity != null && MorphQuadrupedShapeCache.isMouthCarryShape(entity.getId())) {
            ci.cancel();
        }
    }
}
