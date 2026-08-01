package dev.naturalis.mixin;

import dev.naturalis.client.MorphQuickSlotClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tocraft.walkers.WalkersClient;
import tocraft.walkers.impl.tick.KeyPressHandler;
import tocraft.walkers.network.impl.SwapPackets;

@Mixin(value = KeyPressHandler.class, remap = false)
public class MorphQuickSlotTransformMixin {

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void naturalis$suppressSwapWhileWheelOpen(Minecraft client, CallbackInfo ci) {
        if (!MorphQuickSlotClient.shouldBlockTransformKey()) {
            return;
        }
        while (WalkersClient.TRANSFORM_KEY.consumeClick()) {
            // Swallow G while the quick-slot wheel is opening/open or G is held.
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ltocraft/walkers/network/impl/SwapPackets;sendSwapRequest()V"
        ),
        remap = false
    )
    private void naturalis$blockSwapWhileWheelOpen() {
        if (!MorphQuickSlotClient.shouldBlockTransformKey()) {
            SwapPackets.sendSwapRequest();
        }
    }
}
