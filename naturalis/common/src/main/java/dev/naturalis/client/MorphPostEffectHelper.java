package dev.naturalis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Applies morph vision post effects on 1.21.x via {@code setPostEffect} only.
 * Legacy {@code loadEffect(shaders/post/...)} paths are invalid on 1.21.8 and trigger reload errors.
 */
public final class MorphPostEffectHelper {

    private MorphPostEffectHelper() {
    }

    public static boolean apply(Minecraft mc, ResourceLocation shaderId) {
        if (mc == null || mc.gameRenderer == null || shaderId == null) {
            return false;
        }
        ResourceLocation postEffectId = shaderId.getPath().endsWith("_scent")
            ? shaderId
            : MorphVisionPostEffects.resolveForSetPostEffect(shaderId);
        return invokeSetPostEffect(mc.gameRenderer, postEffectId);
    }

    public static void clear(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) {
            return;
        }
        Object renderer = mc.gameRenderer;
        if (invokeNoArg(renderer, "shutdownEffect")) {
            return;
        }
        invokeNoArg(renderer, "clearPostEffect");
    }

    public static boolean hasActivePostEffect(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) {
            return false;
        }
        return hasActivePostEffect(mc.gameRenderer);
    }

    private static boolean hasActivePostEffect(Object renderer) {
        for (String name : new String[] {
            "currentPostEffect",
            "currentEffect",
            "getPostEffect",
            "getPostProcessor",
            "postEffect"
        }) {
            try {
                Object raw = renderer.getClass().getMethod(name).invoke(renderer);
                if (raw instanceof Optional<?> opt) {
                    raw = opt.orElse(null);
                }
                if (raw instanceof ResourceLocation id) {
                    return !id.getPath().isEmpty();
                }
                if (raw != null) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }

    private static boolean invokeSetPostEffect(Object renderer, ResourceLocation id) {
        try {
            renderer.getClass().getMethod("setPostEffect", ResourceLocation.class).invoke(renderer, id);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeNoArg(Object renderer, String method) {
        try {
            renderer.getClass().getMethod(method).invoke(renderer);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
