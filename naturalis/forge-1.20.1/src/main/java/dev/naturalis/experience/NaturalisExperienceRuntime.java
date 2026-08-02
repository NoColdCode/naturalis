package dev.naturalis.experience;

import net.minecraft.server.level.ServerPlayer;

/**
 * Forge 1.20.1 stub — full experience sync UI is NeoForge / Fabric 1.21+.
 */
public final class NaturalisExperienceRuntime {

    private NaturalisExperienceRuntime() {
    }

    public static void requestChoiceScreen(ServerPlayer player) {
        NaturalisExperienceEvents.requestChoiceScreen(player);
    }

    public static boolean applyChoice(ServerPlayer player, NaturalisExperienceMode mode) {
        return NaturalisExperienceEvents.applyChoice(player, mode);
    }
}
