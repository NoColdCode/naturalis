package dev.naturalis.mixin;

import tocraft.remorphed.handler.LivingDeathHandler;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Remorphed from granting morphs when a mob is killed.
 * Naturalis will provide its own morph acquisition system instead.
 */
@Mixin(value = LivingDeathHandler.class, remap = false)
public class DisableKillMorphMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void naturalis$disableKillMorph(LivingEntity entity, DamageSource source,
                                             CallbackInfoReturnable<InteractionResult> cir) {
        // Never touch player death flow; let vanilla/mod respawn handling run normally.
        if (entity instanceof Player) {
            return;
        }

        // Cancel: no morph is awarded for kills; Naturalis handles unlock logic.
        cir.setReturnValue(InteractionResult.PASS);
    }
}
