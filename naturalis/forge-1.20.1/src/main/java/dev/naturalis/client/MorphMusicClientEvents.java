package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphMusicClientEvents {

    private static float lastAppliedMusicVolume = -1.0F;
    private static float storedOptionMusicVolume = -1.0F;

    private MorphMusicClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            restoreMusicVolume(mc);
            return;
        }

        float currentOptionVolume = mc.options.getSoundSourceVolume(SoundSource.MUSIC);
        if (storedOptionMusicVolume < 0.0F) {
            storedOptionMusicVolume = currentOptionVolume;
        }

        float baseMusicVolume = storedOptionMusicVolume;
        float targetMusicVolume = baseMusicVolume;

        if (CurrentMorphUtil.getCurrentMorphId(mc.player) != null) {
            int humanity = HumanityClientCache.isActive() ? HumanityClientCache.getHumanity() : 100;
            float factor = getMorphMusicFactor(humanity);
            targetMusicVolume = Math.max(0.0F, Math.min(1.0F, baseMusicVolume * factor));
        } else {
            targetMusicVolume = baseMusicVolume;
        }

        applyOptionMusicVolume(mc, targetMusicVolume);
        applyMusicVolume(mc, targetMusicVolume);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restoreMusicVolume(Minecraft.getInstance());
    }

    private static float getMorphMusicFactor(int humanity) {
        if (humanity <= 20) {
            return 0.35F;
        }
        if (humanity <= 40) {
            return 0.50F;
        }
        if (humanity <= 60) {
            return 0.65F;
        }
        if (humanity <= 80) {
            return 0.80F;
        }
        return 0.90F;
    }

    private static void applyMusicVolume(Minecraft mc, float target) {
        if (Math.abs(lastAppliedMusicVolume - target) < 0.0001F) {
            return;
        }

        mc.getSoundManager().updateSourceVolume(SoundSource.MUSIC, target);
        lastAppliedMusicVolume = target;
    }

    private static void restoreMusicVolume(Minecraft mc) {
        float base = storedOptionMusicVolume >= 0.0F ? storedOptionMusicVolume : mc.options.getSoundSourceVolume(SoundSource.MUSIC);
        applyOptionMusicVolume(mc, base);
        applyMusicVolume(mc, base);
        storedOptionMusicVolume = -1.0F;
    }

    private static void applyOptionMusicVolume(Minecraft mc, float target) {
        float current = mc.options.getSoundSourceVolume(SoundSource.MUSIC);
        if (Math.abs(current - target) < 0.0001F) {
            return;
        }

        Object options = mc.options;
        for (Method method : options.getClass().getMethods()) {
            if (!"setSoundSourceVolume".equals(method.getName()) && !"setSoundCategoryVolume".equals(method.getName())) {
                continue;
            }
            if (method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != SoundSource.class || params[1] != float.class) {
                continue;
            }

            try {
                method.invoke(options, SoundSource.MUSIC, target);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Try other signature candidates.
            }
        }
    }
}
