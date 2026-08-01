package dev.naturalis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.naturalis.client.ForgeScentVisionMobRenderer;
import dev.naturalis.client.ForgeScentVisionTintState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ForgeNaturalisScentEntityTintMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    @Final
    protected M model;

    @Shadow
    protected abstract ResourceLocation getTextureLocation(T entity);

    @Shadow
    protected abstract float getWhiteOverlayProgress(T entity, float partialTicks);

    @Shadow
    protected abstract boolean isBodyVisible(T entity, float partialTicks);

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void naturalis$renderScentedMob(
        T entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        var outline = ForgeScentVisionTintState.outlineForEntity(entity.getId());
        if (outline.isEmpty() || !isBodyVisible(entity, partialTicks)) {
            return;
        }
        ci.cancel();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, getWhiteOverlayProgress(entity, partialTicks));
        ForgeScentVisionMobRenderer.render(
            model,
            poseStack,
            buffer,
            getTextureLocation(entity),
            outline.get(),
            ForgeScentVisionTintState.filterFillArgb(),
            packedLight,
            overlay
        );
    }
}
