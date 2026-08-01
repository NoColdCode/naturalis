package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.chat.MorphComprehensionProfile;
import dev.naturalis.util.TranslationDeviceUtil;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphAudioPerceptionClient {

    private static final SoundSource[] BOOSTED_SOURCES = new SoundSource[] {
        SoundSource.HOSTILE,
        SoundSource.NEUTRAL,
        SoundSource.AMBIENT,
        SoundSource.PLAYERS
    };

    private static final Map<SoundSource, Float> BASE_VOLUMES = new EnumMap<>(SoundSource.class);
    private static boolean boostedApplied;

    private MorphAudioPerceptionClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            clearBoost(mc);
            return;
        }

        Player player = mc.player;
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        boolean translator = TranslationDeviceUtil.isTranslationCoreHeld(player);

        if (morphId == null || translator) {
            clearBoost(mc);
            return;
        }

        float hearingMultiplier = MorphComprehensionProfile.hearingMultiplier(morphId);
        if (hearingMultiplier > 1.01F) {
            applyBoost(mc, hearingMultiplier);
            // Hearing cue: periodic far-listening pulse.
            if (!NaturalisClientPrefs.isMuteMorphPerceptionSounds() && player.tickCount % 80 == 0) {
                float pitch = 0.7F + (player.getRandom().nextFloat() * 0.5F);
                player.playSound(SoundEvents.BEACON_AMBIENT, 0.08F, pitch);
            }
        } else {
            clearBoost(mc);
        }

        // Distort music perception for feral forms by interrupting and overlaying warped tones.
        if (!NaturalisClientPrefs.isMuteMorphPerceptionSounds() && player.tickCount % 120 == 0) {
            float pitch = 0.45F + (player.getRandom().nextFloat() * 0.9F);
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.18F, pitch);
            if (player.getRandom().nextFloat() < 0.30F) {
                invokeIfPresent(mc.getMusicManager(), "stopPlaying");
            }
        }
    }

    private static void applyBoost(Minecraft mc, float multiplier) {
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
        // Keep this reflective for minor API differences across mapped versions.
        if (invokeIfPresent(mc.options, "setSoundCategoryVolume", source, volume)) {
            return;
        }
        invokeIfPresent(mc.options, "setSoundSourceVolume", source, volume);
    }

    private static boolean invokeIfPresent(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }

        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            try {
                method.invoke(target, args);
                return true;
            } catch (ReflectiveOperationException ignored) {
                // Try next overload.
            }
        }

        return false;
    }

}
