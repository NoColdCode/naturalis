package dev.naturalis.client.perception;

import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Paw vibration sense: tactile action-bar whispers and camera tremor — no cue sounds, no hearing HUD.
 */
public final class MorphVibrationClient {

    private static int scanCooldown;

    private MorphVibrationClient() {
    }

    public static void clientTick(Minecraft mc) {
        MorphVibrationClientState.tickDecay();

        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || TranslationDeviceUtil.isTranslationCoreHeld(player)) {
            return;
        }

        MorphVibrationProfile profile = MorphPerceptionScaling.vibration(MorphVibrationProfiles.resolve(morphId));
        if (!profile.hasPawVibrationSense()) {
            return;
        }

        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }
        scanCooldown = profile.scanIntervalTicks();

        Optional<MorphVibrationCue> cue = MorphVibrationLogic.scanStrongestCue(player, profile, mc);
        if (cue.isEmpty() || cue.get().intensity() < profile.minIntensity()) {
            return;
        }

        MorphVibrationCue active = cue.get();
        MorphVibrationClientState.absorb(active);
    }
}
