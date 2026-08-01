package dev.naturalis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.naturalis.client.ClientPartialTick;
import dev.naturalis.client.MorphQuadrupedMouthRender;
import dev.tocraft.walkers.api.PlayerShape;
import dev.tocraft.walkers.impl.ShapeRenderStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Walkers draws morph shapes inside {@link PlayerRenderer#render}; mouth-held items are drawn right after the shape body.
 */
@Mixin(value = PlayerRenderer.class, priority = 1100)
public abstract class NaturalisQuadrupedMouthPlayerMixin {

    @Inject(
        method = "render(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            shift = At.Shift.BEFORE
        )
    )
    private void naturalis$clearShapeHandsBeforeRender(
        PlayerRenderState state,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        CallbackInfo ci
    ) {
        LivingEntity shape = ((ShapeRenderStateProvider) state).walkers$getShape();
        if (shape == null) {
            return;
        }
        AbstractClientPlayer player = resolveOwner(shape);
        if (player != null && MorphQuadrupedMouthRender.shouldShowMouthCarry(player, Minecraft.getInstance())) {
            MorphQuadrupedMouthRender.hideShapeHandItems(shape);
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            shift = At.Shift.AFTER
        )
    )
    private void naturalis$renderMouthHeldItem(
        PlayerRenderState state,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        CallbackInfo ci
    ) {
        if (state.isSpectator || state.isInvisible || state.isInvisibleToPlayer) {
            return;
        }

        LivingEntity shape = ((ShapeRenderStateProvider) state).walkers$getShape();
        if (shape == null) {
            return;
        }

        AbstractClientPlayer player = resolveOwner(shape);
        if (player == null || !MorphQuadrupedMouthRender.shouldShowMouthCarry(player, Minecraft.getInstance())) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }

        MorphQuadrupedMouthRender.renderInEntitySpace(
            poseStack,
            bufferSource,
            packedLight,
            player,
            shape,
            held,
            ClientPartialTick.get(Minecraft.getInstance())
        );
    }

    private static AbstractClientPlayer resolveOwner(LivingEntity shape) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            LivingEntity current = PlayerShape.getCurrentShape(player);
            if (current == shape) {
                return player;
            }
        }
        return null;
    }
}
