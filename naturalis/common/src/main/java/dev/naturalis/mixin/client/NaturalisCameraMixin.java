package dev.naturalis.mixin.client;

import dev.naturalis.client.perception.MorphThirdPersonCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pulls third-person cameras out of oversized morph meshes (Leviathan, dragons, whales, …).
 */
@Mixin(Camera.class)
public abstract class NaturalisCameraMixin {

    @Inject(method = "setup", at = @At("RETURN"))
    private void naturalis$adjustThirdPersonMorphCamera(
        BlockGetter level,
        Entity entity,
        boolean thirdPerson,
        boolean thirdPersonReverse,
        float partialTick,
        CallbackInfo ci
    ) {
        if (!thirdPerson) {
            return;
        }
        MorphThirdPersonCamera.adjustAfterSetup((Camera) (Object) this, entity, thirdPersonReverse, partialTick);
    }
}
