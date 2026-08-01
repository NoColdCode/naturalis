package dev.naturalis.mixin;

import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.config.NaturalisConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tocraft.walkers.WalkersClient;
import tocraft.walkers.impl.tick.KeyPressHandler;

@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableWalkersTransformWhenBoundMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void naturalis$disableTransformKeyWhenBound(Minecraft client, CallbackInfo ci) {
        if (client.player == null) {
            return;
        }

        boolean survivalAsLock = SurvivalAsClientCache.isLocked();
        boolean potionBound = NaturalisConfig.morphBindingEnabled()
            && NaturalisConfig.morphBindingBlockTransformKey()
            && client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("morph_binding"));
        if (!survivalAsLock && !potionBound) {
            return;
        }

        // Hold G for the quick-slot wheel — only block quick-tap walkers swap.
        if (WalkersClient.TRANSFORM_KEY.isDown()) {
            return;
        }

        while (WalkersClient.TRANSFORM_KEY.consumeClick()) {
            // Swallow pending G-key transform clicks while Survival-as locked or potion-bound.
        }
    }
}
