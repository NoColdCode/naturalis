package dev.naturalis.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * Sends mod payloads to the local player's client; implementation is loader-specific
 * (NeoForge {@code PacketDistributor} vs Forge {@code SimpleChannel}).
 */
public final class PlayToClientSender {

    private static volatile BiConsumer<ServerPlayer, Object> impl = (p, o) -> {};

    private PlayToClientSender() {
    }

    public static void register(BiConsumer<ServerPlayer, Object> handler) {
        impl = handler != null ? handler : (p, o) -> {};
    }

    public static void send(ServerPlayer player, Object payload) {
        if (player == null || payload == null) {
            return;
        }
        impl.accept(player, payload);
    }
}
