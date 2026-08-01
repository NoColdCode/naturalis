package dev.naturalis.instinct;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Diagnostics for wander / instinct steering. Enable with {@code -Dnaturalis.instinct.debug=true}
 * (JVM arg in your launcher), or dev client: {@code gradlew :forge-1.20.1:runClient -PinstinctDebug=true}.
 */
public final class InstinctDebug {

    private static final Logger LOGGER = LoggerFactory.getLogger("naturalis-instinct");
    private static final Deque<String> HUD_LINES = new ArrayDeque<>();
    private static final int HUD_MAX = 20;

    private static final boolean ENABLED = "true".equalsIgnoreCase(
        System.getProperty("naturalis.instinct.debug", "false")
    );

    private InstinctDebug() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void event(ServerPlayer player, String category, String message) {
        if (!ENABLED) {
            return;
        }
        String line = "[" + category + "] " + message;
        LOGGER.info("[{}] {}: {}", player.getGameProfile().getName(), category, message);
        synchronized (HUD_LINES) {
            HUD_LINES.addLast(line);
            while (HUD_LINES.size() > HUD_MAX) {
                HUD_LINES.removeFirst();
            }
        }
    }

    public static void tickSummary(
        ServerPlayer player,
        ResourceLocation morphId,
        boolean afk,
        boolean activeThisTick,
        boolean effectiveFeral,
        int instinctRank,
        int wanderRank,
        boolean wanderMastered,
        boolean shouldWander,
        boolean wanderActive,
        int idleTicks,
        @Nullable String action
    ) {
        if (!ENABLED || player.tickCount % 40 != 0) {
            return;
        }
        event(player, "tick",
            "morph=" + morphId
                + " afk=" + afk
                + " idle=" + idleTicks
                + " active=" + activeThisTick
                + " feral=" + effectiveFeral
                + " instinctRank=" + instinctRank
                + " wanderRank=" + wanderRank
                + " wanderMastered=" + wanderMastered
                + " wanderActive=" + wanderActive
                + " shouldWander=" + shouldWander
                + (action != null ? " last=" + action : "")
        );
    }

    public static List<String> hudLines() {
        synchronized (HUD_LINES) {
            return new ArrayList<>(HUD_LINES);
        }
    }
}
