package dev.naturalis.mixin;

import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.content.NaturalisMobEffects;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tocraft.walkers.WalkersClient;
import dev.tocraft.walkers.impl.tick.KeyPressHandler;

@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableWalkersTransformWhenBoundMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void naturalis$disableTransformKeyWhenBound(Minecraft client, CallbackInfo ci) {
        if (client.player == null) {
            return;
        }
        if (!SurvivalAsClientCache.isLocked() && !client.player.hasEffect(NaturalisMobEffects.MORPH_BINDING)) {
            return;
        }

        while (WalkersClient.TRANSFORM_KEY.consumeClick()) {
            // Swallow pending G-key transform clicks while Survival-as locked or potion-bound.
        }
    }
}
