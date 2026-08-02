package dev.naturalis.util;

import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-specific clear-to-human morph. NeoForge wires {@link MorphAcquisition#forceHuman};
 * Fabric wires {@code FabricMorphEffects} / Walkers shape clear.
 */
public final class ForceHumanBridge {

    @FunctionalInterface
    public interface Handler {
        boolean forceHuman(ServerPlayer player);
    }

    private static Handler handler = p -> false;

    private ForceHumanBridge() {
    }

    public static void register(Handler impl) {
        handler = impl != null ? impl : p -> false;
    }

    public static boolean forceHuman(ServerPlayer player) {
        return handler.forceHuman(player);
    }
}
