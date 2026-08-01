package dev.naturalis.mixin.client;



import com.mojang.blaze3d.vertex.PoseStack;

import dev.naturalis.client.MorphQuadrupedMouthRender;

import dev.naturalis.client.MorphQuadrupedShapeCache;

import net.minecraft.client.Minecraft;

import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.entity.EntityRenderer;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



/**

 * Walkers renders morph shapes via {@link EntityRenderer#render}; hide copied hand stacks and draw mouth carry there.

 * Cannot mixin Walkers' {@code PlayerEntityRendererMixin} directly — Mixin rejects mixin-on-mixin targets.

 */

@Mixin(EntityRenderer.class)

public abstract class ForgeWalkersShapeHandItemsMixin {



    @Inject(

        method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

        at = @At("HEAD")

    )

    private void naturalis$clearShapeHandsBeforeRender(

        Entity entity,

        float entityYaw,

        float partialTicks,

        PoseStack poseStack,

        MultiBufferSource buffer,

        int packedLight,

        CallbackInfo ci

    ) {

        if (!(entity instanceof LivingEntity living) || !MorphQuadrupedShapeCache.isMouthCarryShape(entity.getId())) {

            return;

        }

        MorphQuadrupedMouthRender.hideShapeHandItems(living);

    }



    @Inject(

        method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

        at = @At("RETURN")

    )

    private void naturalis$renderMouthAfterShape(

        Entity entity,

        float entityYaw,

        float partialTicks,

        PoseStack poseStack,

        MultiBufferSource buffer,

        int packedLight,

        CallbackInfo ci

    ) {

        if (!(entity instanceof LivingEntity shape) || !MorphQuadrupedShapeCache.isMouthCarryShape(entity.getId())) {

            return;

        }

        AbstractClientPlayer player = MorphQuadrupedShapeCache.getOwner(entity.getId());

        Minecraft mc = Minecraft.getInstance();

        if (player == null || !MorphQuadrupedMouthRender.shouldShowMouthCarry(player, mc)) {

            return;

        }

        ItemStack held = player.getMainHandItem();

        if (held.isEmpty()) {

            return;

        }

        MorphQuadrupedMouthRender.renderInEntitySpace(

            poseStack,

            buffer,

            packedLight,

            player,

            shape,

            held,

            partialTicks

        );

    }

}


