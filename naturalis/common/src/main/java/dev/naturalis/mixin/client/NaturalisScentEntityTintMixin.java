package dev.naturalis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.naturalis.client.EntityRenderStateScentAccess;
import dev.naturalis.client.ScentVisionMobRenderer;
import dev.naturalis.client.ScentVisionTintState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
public abstract class NaturalisScentEntityTintMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Shadow
    @Final
    private M model;

    @Shadow
    protected abstract ResourceLocation getTextureLocation(S state);

    @Shadow
    protected abstract boolean isBodyVisible(S state);

    @Shadow
    protected abstract float getWhiteOverlayProgress(S state);

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("HEAD")
    )
    private void naturalis$trackScentEntity(T entity, S state, float partialTick, CallbackInfo ci) {
        EntityRenderStateScentAccess access = (EntityRenderStateScentAccess) state;
        access.naturalis$setScentEntityId(entity.getId());
        access.naturalis$setScentTintArgb(-1);
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("RETURN")
    )
    private void naturalis$bindScentTint(T entity, S state, float partialTick, CallbackInfo ci) {
        ScentVisionTintState.applyTint((EntityRenderStateScentAccess) state, state);
    }

    @Inject(
        method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void naturalis$refreshScentTintAtRender(S state, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        ScentVisionTintState.applyTint((EntityRenderStateScentAccess) state, state);
    }

    private static Optional<Integer> naturalis$resolvedOutline(LivingEntityRenderState state) {
        return ScentVisionTintState.resolveOutline((EntityRenderStateScentAccess) state, state);
    }

    private static boolean naturalis$useScentStyle(LivingEntityRenderState state) {
        return naturalis$resolvedOutline(state).isPresent();
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
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
        int packedColor,
        S state
    ) {
        if (naturalis$useScentStyle(state)) {
            return;
        }
        entityModel.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, packedColor);
    }

    @Inject(
        method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void naturalis$skipLayersWhenScented(S state, CallbackInfoReturnable<Boolean> cir) {
        if (naturalis$useScentStyle(state)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.BEFORE)
    )
    private void naturalis$renderScentMob(S state, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        Optional<Integer> outline = naturalis$resolvedOutline(state);
        if (!isBodyVisible(state) || outline.isEmpty()) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(state, getWhiteOverlayProgress(state));
        ScentVisionMobRenderer.render(
            model,
            poseStack,
            buffer,
            getTextureLocation(state),
            outline.get(),
            ScentVisionTintState.filterFillArgb(),
            light,
            overlay
        );
    }
}
