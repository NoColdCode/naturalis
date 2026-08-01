package dev.naturalis.experience;

import net.minecraft.server.MinecraftServer;

public final class NaturalisWorldExperienceStorage {

    private NaturalisWorldExperienceStorage() {
    }

    public static void load(MinecraftServer server) {
    }

    public static NaturalisExperienceMode getMode() {
        return NaturalisExperienceMode.REALISTIC;
    }

    public static boolean isChosen() {
        return true;
    }

    public static boolean shouldPrompt() {
        return false;
    }

    public static void setMode(MinecraftServer server, NaturalisExperienceMode mode) {
    }
}
