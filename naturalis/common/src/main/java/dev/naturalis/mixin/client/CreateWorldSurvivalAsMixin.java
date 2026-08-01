package dev.naturalis.mixin.client;

import dev.naturalis.client.screen.SurvivalAsMobSelectScreen;
import dev.naturalis.survivalas.SurvivalAsClientCreateState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldSurvivalAsMixin {

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void naturalis$requireSurvivalAsMob(CallbackInfo ci) {
        if (SurvivalAsClientCreateState.isModeSelected() && !SurvivalAsClientCreateState.isActive()) {
            Minecraft.getInstance().setScreen(new SurvivalAsMobSelectScreen((CreateWorldScreen) (Object) this));
            ci.cancel();
        }
    }

    @Inject(method = "popScreen", at = @At("HEAD"))
    private void naturalis$clearSurvivalAsOnCancel(CallbackInfo ci) {
        SurvivalAsClientCreateState.clear();
    }
}
