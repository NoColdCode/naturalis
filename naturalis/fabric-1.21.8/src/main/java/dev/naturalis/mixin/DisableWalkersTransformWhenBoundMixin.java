package dev.naturalis.mixin;

import dev.naturalis.compat.CompatAccess;
import dev.tocraft.walkers.WalkersClient;
import dev.tocraft.walkers.impl.tick.KeyPressHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableWalkersTransformWhenBoundMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void naturalis$disableTransformKeyWhenBound(Minecraft client, CallbackInfo ci) {
        if (client.player == null || !client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("morph_binding"))) {
            return;
        }

        while (WalkersClient.TRANSFORM_KEY.consumeClick()) {
            // Swallow pending G-key transform clicks while bound.
        }
    }
}
