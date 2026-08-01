package dev.naturalis.mixin;

import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.content.NaturalisMobEffects;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tocraft.remorphed.tick.KeyPressHandler;

@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableRemorphedMenuWhenBoundMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void naturalis$disableMenuWhenBound(Minecraft client, CallbackInfo ci) {
        if (client.player == null) {
            return;
        }

        boolean stormAttuned = client.player.hasEffect(NaturalisMobEffects.STORM_ATTUNEMENT.get());

        // Keep shape menu inaccessible while forcibly bound or fully locked at 0 humanity.
        if ((client.player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get()) && !stormAttuned)
            || ((HumanityClientCache.isActive() && HumanityClientCache.getHumanity() <= 0) && !stormAttuned)) {
            ci.cancel();
        }
    }
}