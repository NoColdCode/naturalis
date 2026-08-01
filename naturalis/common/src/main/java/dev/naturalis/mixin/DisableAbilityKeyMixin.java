package dev.naturalis.mixin;

import tocraft.walkers.impl.tick.KeyPressHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables Woodwalkers' ability keybind (R by default).
 * Naturalis will implement its own ability system in the future.
 */
@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableAbilityKeyMixin {

    @Inject(method = "handleAbilityKey", at = @At("HEAD"), cancellable = true)
    private void naturalis$disableAbility(Minecraft client, CallbackInfo ci) {
        ci.cancel();
    }
}
