package dev.naturalis.mixin;

import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.config.NaturalisConfig;
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

        if (SurvivalAsClientCache.isLocked()) {
            ci.cancel();
            return;
        }

        boolean stormAttuned = client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("storm_attunement"));

        boolean bound = NaturalisConfig.morphBindingEnabled()
            && NaturalisConfig.morphBindingBlockRemorphedMenu()
            && client.player.hasEffect(CompatAccess.naturalisMobEffectHolder("morph_binding"))
            && !stormAttuned;
        boolean humanityLocked = NaturalisConfig.humanityEnabled()
            && HumanityClientCache.isActive()
            && HumanityClientCache.getHumanity() <= 0
            && !stormAttuned;
        if (bound || humanityLocked) {
            ci.cancel();
        }
    }
}
