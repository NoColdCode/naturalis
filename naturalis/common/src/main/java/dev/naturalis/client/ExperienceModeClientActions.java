package dev.naturalis.client;

import java.util.function.IntConsumer;

public final class ExperienceModeClientActions {

    private static IntConsumer choiceSender = modeId -> {
    };

    private ExperienceModeClientActions() {
    }

    public static void registerChoiceSender(IntConsumer sender) {
        choiceSender = sender != null ? sender : modeId -> {
        };
    }

    public static void sendChoice(int modeId) {
        choiceSender.accept(modeId);
    }
}
