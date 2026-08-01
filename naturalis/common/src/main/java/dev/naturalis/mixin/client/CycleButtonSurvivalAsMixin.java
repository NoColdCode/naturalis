package dev.naturalis.mixin.client;

import dev.naturalis.survivalas.NaturalisCreateGameMode;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * When the Create World game-mode cycle is patched to {@link NaturalisCreateGameMode},
 * vanilla UI listeners still call {@code setValue(SelectedGameMode)}. Map that back.
 */
@Mixin(CycleButton.class)
public abstract class CycleButtonSurvivalAsMixin {

    @ModifyVariable(method = "setValue", at = @At("HEAD"), argsOnly = true)
    private Object naturalis$mapSelectedGameMode(Object value) {
        CycleButton<?> self = (CycleButton<?>) (Object) this;
        if (value instanceof WorldCreationUiState.SelectedGameMode vanilla
            && self.getValue() instanceof NaturalisCreateGameMode) {
            return NaturalisCreateGameMode.fromVanilla(
                vanilla,
                vanilla == WorldCreationUiState.SelectedGameMode.SURVIVAL
                    && self.getValue() == NaturalisCreateGameMode.SURVIVAL_AS
            );
        }
        return value;
    }
}
