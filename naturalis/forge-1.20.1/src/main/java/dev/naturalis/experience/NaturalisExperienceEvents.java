package dev.naturalis.experience;

import net.minecraft.server.level.ServerPlayer;

public final class NaturalisExperienceEvents {

    private NaturalisExperienceEvents() {
    }

    public static boolean applyChoice(ServerPlayer player, NaturalisExperienceMode mode) {
        NaturalisWorldExperienceStorage.setMode(player.getServer(), mode);
        return true;
    }

    /** Choice screen is NeoForge-only; Forge uses {@code /morph experience realistic|softened} directly. */
    public static void requestChoiceScreen(ServerPlayer player) {
    }
}
