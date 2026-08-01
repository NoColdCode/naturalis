package dev.naturalis.instinct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Client-side instinct / wander look diagnostics (same flag as {@link InstinctDebug}). */
public final class InstinctClientDebug {

    private static final Logger LOGGER = LoggerFactory.getLogger("naturalis-instinct-client");
    private static final Deque<String> HUD_LINES = new ArrayDeque<>();
    private static final int HUD_MAX = 16;

    private InstinctClientDebug() {
    }

    public static boolean enabled() {
        return InstinctDebug.enabled();
    }

    public static void log(String message) {
        if (!enabled()) {
            return;
        }
        LOGGER.info("[client] {}", message);
        synchronized (HUD_LINES) {
            HUD_LINES.addLast(message);
            while (HUD_LINES.size() > HUD_MAX) {
                HUD_LINES.removeFirst();
            }
        }
    }

    public static List<String> hudLines() {
        synchronized (HUD_LINES) {
            return new ArrayList<>(HUD_LINES);
        }
    }
}
