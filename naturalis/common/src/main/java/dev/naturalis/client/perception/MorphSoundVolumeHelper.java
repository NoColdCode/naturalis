package dev.naturalis.client.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.lang.reflect.Method;

/**
 * Applies per-category volume through {@link net.minecraft.client.sounds.SoundManager}
 * (required for live music) and mirrors it in client options when possible.
 */
public final class MorphSoundVolumeHelper {

    private MorphSoundVolumeHelper() {
    }

    public static void applyCategoryVolume(Minecraft mc, SoundSource source, float volume) {
        mc.getSoundManager().updateSourceVolume(source, volume);
        setOptionVolume(mc, source, volume);
    }

    private static void setOptionVolume(Minecraft mc, SoundSource source, float volume) {
        Object options = mc.options;
        for (Method method : options.getClass().getMethods()) {
            if (method.getParameterCount() != 2 || method.getParameterTypes()[0] != SoundSource.class) {
                continue;
            }
            if (!"setSoundSourceVolume".equals(method.getName()) && !"setSoundCategoryVolume".equals(method.getName())) {
                continue;
            }
            try {
                method.invoke(options, source, volume);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
