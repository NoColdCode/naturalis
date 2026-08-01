package dev.naturalis.mixin.client;

import dev.naturalis.client.EntityRenderStateScentAccess;
import dev.naturalis.client.ScentVisionTintState;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WolfRenderer.class)
public abstract class NaturalisWolfScentTintMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/animal/wolf/Wolf;Lnet/minecraft/client/renderer/entity/state/WolfRenderState;F)V",
        at = @At("HEAD")
    )
    private void naturalis$trackWolfScentEntity(Wolf entity, WolfRenderState state, float partialTick, CallbackInfo ci) {
        ((EntityRenderStateScentAccess) state).naturalis$setScentEntityId(entity.getId());
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/animal/wolf/Wolf;Lnet/minecraft/client/renderer/entity/state/WolfRenderState;F)V",
        at = @At("RETURN")
    )
    private void naturalis$bindWolfScentState(Wolf entity, WolfRenderState state, float partialTick, CallbackInfo ci) {
        ScentVisionTintState.applyTint((EntityRenderStateScentAccess) state, state);
    }
}
