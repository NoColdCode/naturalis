package dev.naturalis.mixin;

import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.morph.quickslot.MorphQuickSlotDebug;
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
        if (client.player == null
            || !NaturalisConfig.morphBindingEnabled()
            || !NaturalisConfig.morphBindingBlockTransformKey()
            || !client.player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get())) {
            return;
        }

        // Hold G for the quick-slot wheel — only block quick-tap walkers swap.
        if (WalkersClient.TRANSFORM_KEY.isDown()) {
            return;
        }

        while (WalkersClient.TRANSFORM_KEY.consumeClick()) {
            MorphQuickSlotDebug.event("bound", "swallowed G (morph binding active)");
        }
    }
}