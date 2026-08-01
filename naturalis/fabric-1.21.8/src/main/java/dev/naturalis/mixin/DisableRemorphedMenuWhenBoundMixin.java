package dev.naturalis.mixin;

import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.compat.CompatAccess;
import dev.tocraft.remorphed.tick.KeyPressHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyPressHandler.class, remap = false)
public class DisableRemorphedMenuWhenBoundMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void naturalis$disableMenuWhenBound(Minecraft client, CallbackInfo ci) {
        if (client.player == null) {
            return;
        }

        boolean stormAttuned = client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("storm_attunement"));

        // Keep shape menu inaccessible while forcibly bound or fully locked at 0 humanity.
        if ((client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("morph_binding")) && !stormAttuned)
            || ((HumanityClientCache.isActive() && HumanityClientCache.getHumanity() <= 0) && !stormAttuned)) {
            ci.cancel();
        }
    }
}
