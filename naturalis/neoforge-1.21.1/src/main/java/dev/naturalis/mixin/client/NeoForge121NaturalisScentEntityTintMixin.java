package dev.naturalis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.naturalis.client.NeoForge121ScentVisionMobRenderer;
import dev.naturalis.client.NeoForge121ScentVisionTintState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(LivingEntityRenderer.class)
public abstract class NeoForge121NaturalisScentEntityTintMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    @Final
    protected M model;

    @Unique
    private T naturalis$scentEntity;

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void naturalis$captureScentEntity(
        T entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        naturalis$scentEntity = entity;
    }

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
        )
    )
    private void naturalis$deferBodyWhenScented(
        EntityModel<?> entityModel,
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        T entity = naturalis$scentEntity;
        if (entity != null && NeoForge121ScentVisionTintState.outlineForEntity(entity.getId()).isPresent()) {
            return;
        }
        entityModel.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, packedColor);
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.BEFORE)
    )
    private void naturalis$renderScentMob(
        T entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        var outline = NeoForge121ScentVisionTintState.outlineForEntity(entity.getId());
        if (outline.isEmpty() || !naturalis$isBodyVisible(entity, partialTicks)) {
            return;
        }
        ResourceLocation texture = naturalis$getTextureLocation(entity);
        if (texture == null) {
            return;
        }
        float whiteOverlay = naturalis$getWhiteOverlayProgress(entity, partialTicks);
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, whiteOverlay);
        NeoForge121ScentVisionMobRenderer.render(
            model,
            poseStack,
            buffer,
            texture,
            outline.get(),
            NeoForge121ScentVisionTintState.filterFillArgb(),
            packedLight,
            overlay
        );
    }

    @Unique
    private ResourceLocation naturalis$getTextureLocation(T entity) {
        return naturalis$invokeRendererMethod("getTextureLocation", entity, ResourceLocation.class);
    }

    @Unique
    private boolean naturalis$isBodyVisible(T entity, float partialTicks) {
        Boolean visible = naturalis$invokeRendererMethod("isBodyVisible", entity, partialTicks, Boolean.class);
        return visible == null || visible;
    }

    @Unique
    private float naturalis$getWhiteOverlayProgress(T entity, float partialTicks) {
        Float progress = naturalis$invokeRendererMethod("getWhiteOverlayProgress", entity, partialTicks, Float.class);
        return progress == null ? 0.0F : progress;
    }

    @Unique
    private <R> R naturalis$invokeRendererMethod(String methodName, Object arg1, Class<R> returnType) {
        return naturalis$invokeRendererMethod(methodName, returnType, arg1.getClass(), arg1);
    }

    @Unique
    private <R> R naturalis$invokeRendererMethod(String methodName, Object arg1, float arg2, Class<R> returnType) {
        for (Class<?> type = this.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(methodName, arg1.getClass(), float.class);
                method.setAccessible(true);
                return returnType.cast(method.invoke(this, arg1, arg2));
            } catch (ReflectiveOperationException ignored) {
                try {
                    Method method = type.getDeclaredMethod(methodName, LivingEntity.class, float.class);
                    method.setAccessible(true);
                    return returnType.cast(method.invoke(this, arg1, arg2));
                } catch (ReflectiveOperationException ignoredAgain) {
                    // Try superclass.
                }
            }
        }
        return null;
    }

    @Unique
    private <R> R naturalis$invokeRendererMethod(String methodName, Class<R> returnType, Class<?> argType, Object arg) {
        for (Class<?> type = this.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(methodName, argType);
                method.setAccessible(true);
                return returnType.cast(method.invoke(this, arg));
            } catch (ReflectiveOperationException ignored) {
                try {
                    Method method = type.getDeclaredMethod(methodName, LivingEntity.class);
                    method.setAccessible(true);
                    return returnType.cast(method.invoke(this, arg));
                } catch (ReflectiveOperationException ignoredAgain) {
                    // Try superclass.
                }
            }
        }
        return null;
    }
}
