package dev.naturalis.client.perception;

import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Distant, mood-preserving music for feral morphs via {@code SoundManager.updateSourceVolume}.
 */
public final class MorphMusicPerceptionClient {

    private static final Map<SoundSource, Float> STORED_BASE = new EnumMap<>(SoundSource.class);
    private static boolean active;

    private MorphMusicPerceptionClient() {
    }

    public static void clientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            clear(mc);
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || TranslationDeviceUtil.isTranslationCoreHeld(player)) {
            clear(mc);
            return;
        }

        MorphMusicProfile profile = MorphPerceptionScaling.music(MorphMusicProfiles.resolve(morphId));
        if (!profile.altersMusicPerception()) {
            clear(mc);
            return;
        }

        captureBaseIfNeeded(mc, SoundSource.MUSIC);
        captureBaseIfNeeded(mc, SoundSource.RECORDS);

        float music = STORED_BASE.get(SoundSource.MUSIC) * profile.musicVolumeMultiplier();
        float records = STORED_BASE.get(SoundSource.RECORDS) * profile.recordVolumeMultiplier();

        MorphSoundVolumeHelper.applyCategoryVolume(mc, SoundSource.MUSIC, Mth.clamp(music, 0.0F, 1.0F));
        MorphSoundVolumeHelper.applyCategoryVolume(mc, SoundSource.RECORDS, Mth.clamp(records, 0.0F, 1.0F));
        active = true;
    }

    private static void captureBaseIfNeeded(Minecraft mc, SoundSource source) {
        STORED_BASE.putIfAbsent(source, mc.options.getSoundSourceVolume(source));
    }

    private static void clear(Minecraft mc) {
        if (!active) {
            STORED_BASE.clear();
            return;
        }
        for (Map.Entry<SoundSource, Float> entry : STORED_BASE.entrySet()) {
            MorphSoundVolumeHelper.applyCategoryVolume(mc, entry.getKey(), entry.getValue());
        }
        STORED_BASE.clear();
        active = false;
    }
}
