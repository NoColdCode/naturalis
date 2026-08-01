package dev.naturalis.morph.quickslot;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Temporary diagnostics for G / quick-slot wheel issues. Disable with -Dnaturalis.quickslot.debug=false */
public final class MorphQuickSlotDebug {

    private static final Logger LOGGER = LoggerFactory.getLogger("naturalis-quickslot");
    private static final Deque<String> HUD_LINES = new ArrayDeque<>();
    private static final int HUD_MAX = 16;

    /** Enabled by default until the wheel bug is resolved. */
    private static final boolean ENABLED = !"false".equalsIgnoreCase(
        System.getProperty("naturalis.quickslot.debug", "true")
    );

    private MorphQuickSlotDebug() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void event(String source, String message) {
        if (!ENABLED) {
            return;
        }
        String line = "[" + source + "] " + message;
        LOGGER.info(line);
        synchronized (HUD_LINES) {
            HUD_LINES.addLast(line);
            while (HUD_LINES.size() > HUD_MAX) {
                HUD_LINES.removeFirst();
            }
        }
    }

    public static void event(String source, String message, @Nullable Throwable trace) {
        if (!ENABLED) {
            return;
        }
        event(source, message);
        if (trace != null) {
            LOGGER.info("[{}] stack trace:", source, trace);
        }
    }

    public static List<String> hudLines() {
        synchronized (HUD_LINES) {
            return new ArrayList<>(HUD_LINES);
        }
    }

    public static void clearHud() {
        synchronized (HUD_LINES) {
            HUD_LINES.clear();
        }
    }
}
