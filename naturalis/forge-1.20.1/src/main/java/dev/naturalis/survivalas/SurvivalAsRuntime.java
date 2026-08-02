package dev.naturalis.survivalas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge 1.20.1 stub — Survival as… is NeoForge / Fabric 1.21+ only.
 */
public final class SurvivalAsRuntime {

    private SurvivalAsRuntime() {
    }

    public static boolean isMorphAllowed(ResourceLocation requestedOrNull) {
        return true;
    }

    public static boolean isAcquireAllowed(ResourceLocation morphId) {
        return true;
    }

    public static void onServerStarting(MinecraftServer server) {
        SurvivalAsWorldStorage.load(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
    }

    public static void onPlayerTick(ServerPlayer player) {
    }

    public static boolean unlock(MinecraftServer server, ServerPlayer actor) {
        return false;
    }

    public static boolean changeIdentity(ServerPlayer player, ResourceLocation newMorph) {
        return false;
    }
}
