package dev.naturalis.mixin;

import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.content.NaturalisMobEffects;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tocraft.remorphed.tick.KeyPressHandler;

@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableRemorphedMenuWhenBoundMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void naturalis$disableMenuWhenBound(Minecraft client, CallbackInfo ci) {
        if (client.player == null) {
            return;
        }

        // Survival as…: world-level identity lock (no Morph Binding effect).
        if (SurvivalAsClientCache.isLocked()) {
            ci.cancel();
            return;
        }

        boolean stormAttuned = client.player.hasEffect(NaturalisMobEffects.STORM_ATTUNEMENT);

        // Potion Morph Binding / humanity lock still use the effect-based path.
        if ((client.player.hasEffect(NaturalisMobEffects.MORPH_BINDING) && !stormAttuned)
            || ((HumanityClientCache.isActive() && HumanityClientCache.getHumanity() <= 0) && !stormAttuned)) {
            ci.cancel();
        }
    }
}
