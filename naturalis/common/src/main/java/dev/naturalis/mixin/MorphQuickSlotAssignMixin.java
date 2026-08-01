package dev.naturalis.mixin;

import dev.naturalis.client.MorphQuickSlotAssignSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
    "tocraft.remorphed.screen.widget.ShapeWidget",
    "dev.tocraft.remorphed.screen.widget.ShapeWidget",
    "tocraft.remorphed.screen.widget.EntityWidget",
    "dev.tocraft.remorphed.screen.widget.EntityWidget"
}, remap = false)
public abstract class MorphQuickSlotAssignMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void naturalis$assignOnShiftRightClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MorphQuickSlotAssignSupport.handleShiftRightClick(this, button, cir);
    }
}
