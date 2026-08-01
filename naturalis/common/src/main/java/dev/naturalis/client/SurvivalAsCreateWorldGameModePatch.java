package dev.naturalis.client;

import com.google.common.collect.ImmutableList;
import dev.naturalis.client.screen.SurvivalAsMobSelectScreen;
import dev.naturalis.survivalas.NaturalisCreateGameMode;
import dev.naturalis.survivalas.SurvivalAsClientCreateState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

/**
 * Patches the Create World game-mode cycle to include {@link NaturalisCreateGameMode#SURVIVAL_AS}.
 */
public final class SurvivalAsCreateWorldGameModePatch {

    private static final List<NaturalisCreateGameMode> MODES = ImmutableList.copyOf(NaturalisCreateGameMode.values());

    private SurvivalAsCreateWorldGameModePatch() {
    }

    public static boolean isPatched(CycleButton<?> button) {
        return button.getValue() instanceof NaturalisCreateGameMode;
    }

    public static boolean isVanillaGameModeCycle(CycleButton<?> button) {
        return button.getValue() instanceof WorldCreationUiState.SelectedGameMode;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void patch(CycleButton<?> button, CreateWorldScreen screen) {
        if (isPatched(button)) {
            syncDisplay(button);
            return;
        }
        if (!isVanillaGameModeCycle(button)) {
            return;
        }

        WorldCreationUiState uiState = readUiState(screen);
        NaturalisCreateGameMode initial = NaturalisCreateGameMode.fromVanilla(
            uiState != null ? uiState.getGameMode() : WorldCreationUiState.SelectedGameMode.SURVIVAL,
            SurvivalAsClientCreateState.isModeSelected()
        );

        try {
            Field valuesField = CycleButton.class.getDeclaredField("values");
            valuesField.setAccessible(true);
            valuesField.set(button, CycleButton.ValueListSupplier.create(MODES));

            Field stringifierField = CycleButton.class.getDeclaredField("valueStringifier");
            stringifierField.setAccessible(true);
            stringifierField.set(button, (Function) (Object value) -> ((NaturalisCreateGameMode) value).displayName);

            Field onChangeField = CycleButton.class.getDeclaredField("onValueChange");
            onChangeField.setAccessible(true);
            onChangeField.set(button, (CycleButton.OnValueChange) (btn, value) -> {
                NaturalisCreateGameMode mode = (NaturalisCreateGameMode) value;
                applyMode(screen, mode);
                btn.setTooltip(Tooltip.create(mode.info));
            });

            Field valueField = CycleButton.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(button, initial);

            Field indexField = CycleButton.class.getDeclaredField("index");
            indexField.setAccessible(true);
            indexField.setInt(button, MODES.indexOf(initial));

            Method updateValue = CycleButton.class.getDeclaredMethod("updateValue", Object.class);
            updateValue.setAccessible(true);
            updateValue.invoke(button, initial);

            button.setTooltip(Tooltip.create(initial.info));
            applyMode(screen, initial);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to patch Create World game-mode cycle for Survival as…", e);
        }
    }

    public static void syncFromUiState(CycleButton<?> button, CreateWorldScreen screen) {
        if (!isPatched(button)) {
            return;
        }
        WorldCreationUiState uiState = readUiState(screen);
        if (uiState == null) {
            return;
        }
        NaturalisCreateGameMode mapped = NaturalisCreateGameMode.fromVanilla(
            uiState.getGameMode(),
            SurvivalAsClientCreateState.isModeSelected()
        );
        if (button.getValue() != mapped) {
            ((CycleButton<NaturalisCreateGameMode>) (CycleButton<?>) button).setValue(mapped);
            button.setTooltip(Tooltip.create(mapped.info));
        }
    }

    private static void syncDisplay(CycleButton<?> button) {
        Object value = button.getValue();
        if (value instanceof NaturalisCreateGameMode mode) {
            button.setTooltip(Tooltip.create(mode.info));
        }
    }

    private static void applyMode(CreateWorldScreen screen, NaturalisCreateGameMode mode) {
        WorldCreationUiState uiState = readUiState(screen);
        if (uiState != null) {
            uiState.setGameMode(mode.vanilla);
        }
        if (mode.survivalAs) {
            SurvivalAsClientCreateState.selectMode();
        } else {
            SurvivalAsClientCreateState.clear();
        }
    }

    public static void openMobPicker(Screen parent) {
        Minecraft.getInstance().setScreen(new SurvivalAsMobSelectScreen(parent));
    }

    private static WorldCreationUiState readUiState(CreateWorldScreen screen) {
        return screen.getUiState();
    }
}
