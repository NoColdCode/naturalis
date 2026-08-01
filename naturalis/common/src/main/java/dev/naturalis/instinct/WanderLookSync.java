package dev.naturalis.instinct;

import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.network.WanderLookPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Sends wander look to the client. Loaders may register an immediate client mirror for integrated server
 * (packet can arrive after the client tick in the same frame).
 */
public final class WanderLookSync {

    private static final Set<UUID> LOOK_ACTIVE = ConcurrentHashMap.newKeySet();

    private static volatile Consumer<WanderLookPayload> immediateClientMirror = payload -> {};

    private WanderLookSync() {
    }

    public static void registerImmediateClientMirror(Consumer<WanderLookPayload> mirror) {
        immediateClientMirror = mirror != null ? mirror : payload -> {};
    }

    public static void send(ServerPlayer player, WanderLookPayload payload) {
        if (payload.active()) {
            LOOK_ACTIVE.add(player.getUUID());
        }
        if (InstinctDebug.enabled()) {
            InstinctDebug.event(
                player,
                "wander-look-send",
                "active=" + payload.active()
                    + " yaw=" + String.format("%.1f", payload.yaw())
                    + " pitch=" + String.format("%.1f", payload.pitch())
                    + " body=" + String.format("%.1f", payload.bodyYaw())
            );
        }
        PlayToClientSender.send(player, payload);
        immediateClientMirror.accept(payload);
    }

    /** Sends clear only once after a prior active look for this player. */
    public static void sendClear(ServerPlayer player) {
        if (!LOOK_ACTIVE.remove(player.getUUID())) {
            return;
        }
        if (InstinctDebug.enabled()) {
            InstinctDebug.event(player, "wander-look-send", "active=false (clear)");
        }
        WanderLookPayload payload = new WanderLookPayload(false, 0.0F, 0.0F, 0.0F);
        PlayToClientSender.send(player, payload);
        immediateClientMirror.accept(payload);
    }
}
