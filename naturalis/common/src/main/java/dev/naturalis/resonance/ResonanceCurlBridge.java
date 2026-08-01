package dev.naturalis.resonance;

import net.minecraft.server.level.ServerPlayer;

/** Curl-key rebirth hook; NeoForge wires real logic, Fabric uses a stub until resonance parity lands. */
public final class ResonanceCurlBridge {

    @FunctionalInterface
    public interface Handler {
        boolean tryTriggerRebirthFromCurlKey(ServerPlayer player);
    }

    private static Handler handler = p -> false;

    private ResonanceCurlBridge() {
    }

    public static void register(Handler impl) {
        handler = impl != null ? impl : p -> false;
    }

    public static boolean tryTriggerRebirthFromCurlKey(ServerPlayer player) {
        return handler.tryTriggerRebirthFromCurlKey(player);
    }
}
