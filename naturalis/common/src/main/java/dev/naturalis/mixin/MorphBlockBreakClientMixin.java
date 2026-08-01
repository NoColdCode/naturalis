package dev.naturalis.mixin;

import dev.naturalis.client.perception.MorphAnimalInteractionClient;
import dev.naturalis.client.perception.MorphDigClientState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MorphBlockBreakClientMixin {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void naturalis$guardStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (MorphAnimalInteractionClient.shouldSuppressBlockMining(net.minecraft.client.Minecraft.getInstance())) {
            MorphDigClientState.pulseScratch();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void naturalis$guardContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (MorphAnimalInteractionClient.shouldSuppressBlockMining(net.minecraft.client.Minecraft.getInstance())) {
            cir.setReturnValue(false);
        }
    }
}
