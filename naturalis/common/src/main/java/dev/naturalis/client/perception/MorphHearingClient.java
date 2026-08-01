package dev.naturalis.client.perception;

import dev.naturalis.chat.MorphComprehensionProfile;
import dev.naturalis.client.NaturalisClientPrefs;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loader-neutral morph hearing tick (volume boost, entity scan, directional cues).
 * Invoke from loader client tick events.
 */
public final class MorphHearingClient {

    private static final SoundEvent WOLF_GROWL = registrySound("entity.wolf.growl");

    /** Do not boost NEUTRAL — farm animals would sound like alarms at 1.85×. */
    private static final SoundSource[] BOOSTED_SOURCES = {
        SoundSource.HOSTILE,
        SoundSource.AMBIENT,
        SoundSource.PLAYERS
    };

    private static final Map<SoundSource, Float> BASE_VOLUMES = new EnumMap<>(SoundSource.class);
    private static boolean boostedApplied;
    private static int scanCooldown;
    /** Minimum ticks between threat growls while the same hostile stays in range. */
    private static final int THREAT_GROWL_COOLDOWN = 100;
    private static int threatGrowlCooldown;
    private static int lastThreatEntityId = -1;

    private MorphHearingClient() {
    }

    public static void clientTick(Minecraft mc) {
        MorphHearingClientState.tickHudPulse();

        LocalPlayer player = mc.player;
        if (player == null) {
            clearBoost(mc);
            MorphHearingClientState.setCue(null, 0);
            lastThreatEntityId = -1;
            return;
        }

        if (threatGrowlCooldown > 0) {
            threatGrowlCooldown--;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || TranslationDeviceUtil.isTranslationCoreHeld(player)) {
            clearBoost(mc);
            MorphHearingClientState.setCue(null, 0);
            return;
        }

        MorphHearingProfile profile = MorphPerceptionScaling.hearing(MorphHearingProfiles.resolve(morphId));
        if (!profile.hasEnhancedHearing()) {
            clearBoost(mc);
            MorphHearingClientState.setCue(null, 0);
            return;
        }

        applyBoost(mc, profile.volumeMultiplier());

        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }
        scanCooldown = profile.scanIntervalTicks();

        if (!profile.directionalCues()) {
            return;
        }

        Optional<MorphHearingCue> cue = MorphHearingLogic.scanStrongestCue(player, profile);
        if (cue.isEmpty() || cue.get().intensity() < profile.minCueIntensity()) {
            lastThreatEntityId = -1;
            return;
        }

        MorphHearingCue active = cue.get();
        MorphHearingClientState.setCue(active, 50);

        if (!NaturalisClientPrefs.isMuteMorphPerceptionSounds() && active.kind() == MorphHearingCueKind.THREAT) {
            int threatId = active.source() != null ? active.source().getId() : -1;
            boolean newThreat = threatId >= 0 && threatId != lastThreatEntityId;
            if (newThreat || threatGrowlCooldown <= 0) {
                playThreatCue(player, active);
                lastThreatEntityId = threatId;
                threatGrowlCooldown = THREAT_GROWL_COOLDOWN;
            }
        } else {
            lastThreatEntityId = -1;
        }

        if (MorphComprehensionProfile.getLiteracy(morphId) == MorphComprehensionProfile.Literacy.GARBLED
            && !NaturalisClientPrefs.isMuteMorphPerceptionSounds()
            && player.tickCount % 140 == 0) {
            float pitch = 0.45F + player.getRandom().nextFloat() * 0.4F;
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.12F, pitch);
        }
    }

    private static void playThreatCue(LocalPlayer player, MorphHearingCue cue) {
        if (cue.source() == null) {
            return;
        }

        float volume = Mth.clamp(0.04F + cue.intensity() * 0.08F, 0.04F, 0.12F);
        float pitch = 0.75F + player.getRandom().nextFloat() * 0.12F;

        player.level().playLocalSound(
            cue.source().getX(),
            cue.source().getY(),
            cue.source().getZ(),
            WOLF_GROWL,
            SoundSource.HOSTILE,
            volume,
            pitch,
            false
        );
    }

    private static void applyBoost(Minecraft mc, float multiplier) {
        if (boostedApplied) {
            return;
        }
        for (SoundSource source : BOOSTED_SOURCES) {
            float current = mc.options.getSoundSourceVolume(source);
            BASE_VOLUMES.putIfAbsent(source, current);
            float base = BASE_VOLUMES.get(source);
            setSoundSourceVolume(mc, source, Math.min(1.0F, base * multiplier));
        }
        boostedApplied = true;
    }

    private static void clearBoost(Minecraft mc) {
        if (!boostedApplied) {
            BASE_VOLUMES.clear();
            return;
        }
        for (Map.Entry<SoundSource, Float> entry : BASE_VOLUMES.entrySet()) {
            setSoundSourceVolume(mc, entry.getKey(), entry.getValue());
        }
        BASE_VOLUMES.clear();
        boostedApplied = false;
    }

    private static void setSoundSourceVolume(Minecraft mc, SoundSource source, float volume) {
        if (invokeIfPresent(mc.options, "setSoundCategoryVolume", source, volume)) {
            return;
        }
        invokeIfPresent(mc.options, "setSoundSourceVolume", source, volume);
    }

    private static SoundEvent registrySound(String id) {
        return BuiltInRegistries.SOUND_EVENT
            .getOptional(ResourceLocation.withDefaultNamespace(id))
            .orElse(SoundEvents.AMBIENT_CAVE.value());
    }

    private static boolean invokeIfPresent(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            try {
                method.invoke(target, args);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }
}
