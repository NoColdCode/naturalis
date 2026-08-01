package dev.naturalis.survivalas;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;

/**
 * Extended create-world game modes. Vanilla only has Survival / Hardcore / Creative;
 * {@link #SURVIVAL_AS} is Naturalis-specific and still stores as Survival under the hood.
 */
public enum NaturalisCreateGameMode {
    SURVIVAL(WorldCreationUiState.SelectedGameMode.SURVIVAL, false,
        Component.translatable("selectWorld.gameMode.survival"),
        Component.translatable("selectWorld.gameMode.survival.info")),
    SURVIVAL_AS(WorldCreationUiState.SelectedGameMode.SURVIVAL, true,
        Component.translatable("gui.naturalis.survival_as.game_mode"),
        Component.translatable("gui.naturalis.survival_as.game_mode.info")),
    HARDCORE(WorldCreationUiState.SelectedGameMode.HARDCORE, false,
        Component.translatable("selectWorld.gameMode.hardcore"),
        Component.translatable("selectWorld.gameMode.hardcore.info")),
    CREATIVE(WorldCreationUiState.SelectedGameMode.CREATIVE, false,
        Component.translatable("selectWorld.gameMode.creative"),
        Component.translatable("selectWorld.gameMode.creative.info"));

    public final WorldCreationUiState.SelectedGameMode vanilla;
    public final boolean survivalAs;
    public final Component displayName;
    public final Component info;

    NaturalisCreateGameMode(
        WorldCreationUiState.SelectedGameMode vanilla,
        boolean survivalAs,
        Component displayName,
        Component info
    ) {
        this.vanilla = vanilla;
        this.survivalAs = survivalAs;
        this.displayName = displayName;
        this.info = info;
    }

    public static NaturalisCreateGameMode fromVanilla(
        WorldCreationUiState.SelectedGameMode mode,
        boolean survivalAsSelected
    ) {
        if (mode == WorldCreationUiState.SelectedGameMode.HARDCORE) {
            return HARDCORE;
        }
        if (mode == WorldCreationUiState.SelectedGameMode.CREATIVE) {
            return CREATIVE;
        }
        if (mode == WorldCreationUiState.SelectedGameMode.DEBUG) {
            return SURVIVAL;
        }
        return survivalAsSelected ? SURVIVAL_AS : SURVIVAL;
    }
}
