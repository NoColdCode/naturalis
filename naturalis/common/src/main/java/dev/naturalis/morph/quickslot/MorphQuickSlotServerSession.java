package dev.naturalis.morph.quickslot;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side guard while the client owns the transform key for the quick-slot wheel. */
public final class MorphQuickSlotServerSession {

    private static final int MAX_SESSION_TICKS = 200;
    /** Keep blocking walkers swaps briefly after session end to absorb queued packets. */
    private static final int RELEASE_GRACE_TICKS = 25;

    private static final Map<UUID, SessionState> SESSIONS = new ConcurrentHashMap<>();

    private MorphQuickSlotServerSession() {
    }

    public static void setActive(ServerPlayer player, boolean active) {
        if (active) {
            SESSIONS.put(player.getUUID(), SessionState.active(player.tickCount));
            MorphQuickSlotDebug.event("server", "quick-slot session ACTIVE for " + player.getGameProfile().getName());
            return;
        }

        SessionState state = SESSIONS.get(player.getUUID());
        if (state == null) {
            return;
        }
        SESSIONS.put(player.getUUID(), state.withGraceUntil(player.tickCount + RELEASE_GRACE_TICKS));
        MorphQuickSlotDebug.event(
            "server",
            "quick-slot session GRACE until tick " + (player.tickCount + RELEASE_GRACE_TICKS)
                + " for " + player.getGameProfile().getName()
        );
    }

    public static void clear(ServerPlayer player) {
        if (SESSIONS.remove(player.getUUID()) != null) {
            MorphQuickSlotDebug.event("server", "quick-slot session CLEARED for " + player.getGameProfile().getName());
        }
    }

    public static boolean isBlockingWalkersSwap(ServerPlayer player) {
        SessionState state = SESSIONS.get(player.getUUID());
        if (state == null) {
            return false;
        }
        int tick = player.tickCount;
        if (tick - state.startTick > MAX_SESSION_TICKS || tick > state.blockUntilTick) {
            SESSIONS.remove(player.getUUID());
            if (tick - state.startTick > MAX_SESSION_TICKS) {
                MorphQuickSlotDebug.event("server", "quick-slot session TIMED OUT for " + player.getGameProfile().getName());
            }
            return false;
        }
        return true;
    }

    private record SessionState(int startTick, int blockUntilTick) {

        private static SessionState active(int startTick) {
            return new SessionState(startTick, Integer.MAX_VALUE);
        }

        private SessionState withGraceUntil(int blockUntilTick) {
            return new SessionState(startTick, blockUntilTick);
        }
    }
}
