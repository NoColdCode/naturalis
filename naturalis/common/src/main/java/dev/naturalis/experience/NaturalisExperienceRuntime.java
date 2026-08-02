package dev.naturalis.experience;

import dev.naturalis.network.ExperienceModePayload;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.network.SetExperienceModePayload;
import dev.naturalis.survivalas.SurvivalAsWorldStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-neutral experience-mode logic shared by NeoForge event subscribers and Fabric hooks.
 */
public final class NaturalisExperienceRuntime {

    private NaturalisExperienceRuntime() {
    }

    public static void onServerStarting(MinecraftServer server) {
        NaturalisWorldExperienceStorage.load(server);
    }

    public static void syncPlayer(ServerPlayer player) {
        // Survival as… locks you into one morph — skip the Realistic/Softened choice screen.
        if (SurvivalAsWorldStorage.isEnabled()) {
            ensureRealisticWithoutPrompt(player.getServer());
            PlayToClientSender.send(player,
                new ExperienceModePayload(NaturalisWorldExperienceStorage.getMode(), false));
            return;
        }

        boolean prompt = NaturalisWorldExperienceStorage.shouldPrompt();
        NaturalisExperienceMode mode = NaturalisWorldExperienceStorage.getMode();
        PlayToClientSender.send(player, new ExperienceModePayload(mode, prompt));
        if (prompt) {
            player.sendSystemMessage(NaturalisExperienceMessages.welcome());
            player.sendSystemMessage(NaturalisExperienceMessages.chooseHint());
        }
    }

    private static void ensureRealisticWithoutPrompt(MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (!NaturalisWorldExperienceStorage.isChosen()
            || NaturalisWorldExperienceStorage.getMode() == NaturalisExperienceMode.UNSET) {
            NaturalisWorldExperienceStorage.setMode(server, NaturalisExperienceMode.REALISTIC);
        }
    }

    public static void requestChoiceScreen(ServerPlayer player) {
        if (SurvivalAsWorldStorage.isEnabled()) {
            player.sendSystemMessage(Component.translatable("message.naturalis.survival_as.no_experience_choice"));
            return;
        }
        if (NaturalisWorldExperienceStorage.isChosen() && !player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("command.naturalis.experience.denied"));
            return;
        }
        PlayToClientSender.send(player, new ExperienceModePayload(NaturalisWorldExperienceStorage.getMode(), true));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    public static boolean applyChoice(ServerPlayer player, NaturalisExperienceMode mode) {
        if (mode == NaturalisExperienceMode.UNSET) {
            return false;
        }
        if (SurvivalAsWorldStorage.isEnabled() && !player.hasPermissions(2)) {
            return false;
        }
        if (NaturalisWorldExperienceStorage.isChosen() && !player.hasPermissions(2)) {
            return false;
        }
        NaturalisWorldExperienceStorage.setMode(player.getServer(), mode);
        syncAll(player.getServer());
        player.sendSystemMessage(NaturalisExperienceMessages.chosen(mode));
        return true;
    }

    public static void handleSetExperiencePayload(SetExperienceModePayload payload, ServerPlayer player) {
        NaturalisExperienceMode mode = NaturalisExperienceMode.fromId(payload.modeId());
        if (mode == NaturalisExperienceMode.UNSET) {
            return;
        }
        applyChoice(player, mode);
    }
}
