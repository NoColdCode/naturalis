package dev.naturalis.knowledge;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Pushes morph hotbar / inventory restriction state to the client immediately after
 * knowledge changes. Periodic sync still runs from {@code KnowledgeLevelEvents}.
 */
public final class KnowledgeClientSync {

    private static volatile Consumer<ServerPlayer> handler = p -> {};

    private KnowledgeClientSync() {
    }

    public static void register(Consumer<ServerPlayer> sender) {
        handler = sender != null ? sender : p -> {};
    }

    public static void flush(ServerPlayer player) {
        if (player == null) {
            return;
        }
        handler.accept(player);
    }
}
