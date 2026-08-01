package dev.naturalis.mixin.client;

import dev.naturalis.client.NaturalisShapeRenderSync;
import dev.naturalis.client.perception.MorphEmbodimentLogic;
import dev.naturalis.client.perception.MorphEmbodimentProfile;
import dev.tocraft.walkers.api.PlayerShape;
import dev.tocraft.walkers.impl.ShapeRenderStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Walkers hides the morph shape in first person; Naturalis re-enables it for deep embodiment profiles.
 */
@Mixin(value = PlayerRenderer.class, priority = 1100)
public abstract class NaturalisFirstPersonMorphMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
        at = @At("RETURN")
    )
    private void naturalis$firstPersonMorphBody(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        MorphEmbodimentProfile profile = MorphEmbodimentLogic.profileFor(player);
        if (!MorphEmbodimentLogic.shouldRenderFirstPersonMorphBody(profile)) {
            return;
        }

        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape == null) {
            return;
        }

        ((ShapeRenderStateProvider) state).walkers$setShape(() -> {
            NaturalisShapeRenderSync.syncShapeFromPlayer(player, shape);
            return shape;
        });
    }
}
