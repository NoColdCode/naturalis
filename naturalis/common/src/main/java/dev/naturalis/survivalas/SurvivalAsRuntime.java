package dev.naturalis.survivalas;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.network.HumanityPayload;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.network.SurvivalAsLockPayload;
import dev.naturalis.network.SurvivalAsTraitsPayload;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphAcquisition;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

/**
 * Applies Survival-as identity: morph lock via world rules (not Morph Binding),
 * spawn relocation, and first-join lore.
 */
public final class SurvivalAsRuntime {

    private SurvivalAsRuntime() {
    }

    public static boolean isMorphAllowed(ResourceLocation requestedOrNull) {
        if (!SurvivalAsWorldStorage.isLocked()) {
            return true;
        }
        ResourceLocation locked = SurvivalAsWorldStorage.getMorphId();
        if (locked == null) {
            return true;
        }
        if (requestedOrNull == null) {
            return false;
        }
        return locked.equals(requestedOrNull);
    }

    public static boolean isAcquireAllowed(ResourceLocation morphId) {
        if (!SurvivalAsWorldStorage.isLocked()) {
            return true;
        }
        ResourceLocation locked = SurvivalAsWorldStorage.getMorphId();
        return locked != null && locked.equals(morphId);
    }

    public static void onServerStarting(MinecraftServer server) {
        if (SurvivalAsClientCreateState.isActive()) {
            SurvivalAsWorldStorage.enable(server, SurvivalAsClientCreateState.getMorphId());
            SurvivalAsClientCreateState.clear();
        } else {
            SurvivalAsWorldStorage.load(server);
        }
        // Survival as… never uses the Realistic/Softened choice UI.
        if (SurvivalAsWorldStorage.isEnabled() && server != null) {
            if (!dev.naturalis.experience.NaturalisWorldExperienceStorage.isChosen()) {
                dev.naturalis.experience.NaturalisWorldExperienceStorage.setMode(
                    server, dev.naturalis.experience.NaturalisExperienceMode.REALISTIC);
            }
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!SurvivalAsWorldStorage.isEnabled()) {
            PlayToClientSender.send(player, new SurvivalAsLockPayload(false));
            return;
        }
        ResourceLocation morphId = SurvivalAsWorldStorage.getMorphId();
        if (morphId == null) {
            PlayToClientSender.send(player, new SurvivalAsLockPayload(false));
            return;
        }

        clearLegacyBinding(player);
        applyIdentity(player, morphId);
        PlayToClientSender.send(player, new SurvivalAsLockPayload(SurvivalAsWorldStorage.isLocked()));

        if (!SurvivalAsWorldStorage.isSpawnDone()) {
            SurvivalAsSpawnFinder.relocatePlayer(player, morphId);
            SurvivalAsWorldStorage.markSpawnDone(player.getServer());
        }

        if (!SurvivalAsWorldStorage.isLoreSent()) {
            var type = CompatAccess.getEntityType(morphId);
            player.sendSystemMessage(SurvivalAsMessages.firstSpawnLore(type, morphId));
            player.sendSystemMessage(SurvivalAsMessages.lockedNotice(type, morphId));
            SurvivalAsWorldStorage.markLoreSent(player.getServer());
        }

        // Humanity gauge is disabled for Survival-as worlds.
        PlayToClientSender.send(player, new HumanityPayload(0, false));

        if (!SurvivalAsWorldStorage.isTraitsShown()) {
            SurvivalAsTraitSummary.Sheet sheet = SurvivalAsTraitSummary.build(player, morphId);
            PlayToClientSender.send(player, new SurvivalAsTraitsPayload(
                sheet.morphId().toString(),
                sheet.mass(),
                sheet.dietId(),
                sheet.traitIds(),
                sheet.traitExtras()
            ));
            SurvivalAsWorldStorage.markTraitsShown(player.getServer());
        }
    }

    public static void applyIdentity(ServerPlayer player, ResourceLocation morphId) {
        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        if (current == null || !current.equals(morphId)) {
            MorphAcquisition.acquire(player, morphId);
        }
    }

    public static void tickEnforce(ServerPlayer player) {
        if (!SurvivalAsWorldStorage.isLocked()) {
            return;
        }
        ResourceLocation morphId = SurvivalAsWorldStorage.getMorphId();
        if (morphId == null) {
            return;
        }
        // Strip leftover Morph Binding from older Survival-as saves.
        Holder<MobEffect> binding = CompatAccess.naturalisMobEffectHolder("morph_binding");
        if (player.hasEffect(binding)) {
            clearLegacyBinding(player);
        }
        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        if (current == null || !current.equals(morphId)) {
            MorphAcquisition.acquire(player, morphId);
        }
    }

    public static boolean unlock(MinecraftServer server, ServerPlayer actor) {
        if (!SurvivalAsWorldStorage.isEnabled()) {
            return false;
        }
        SurvivalAsWorldStorage.unlock(server);
        if (actor != null) {
            clearLegacyBinding(actor);
            PlayToClientSender.send(actor, new SurvivalAsLockPayload(false));
            actor.sendSystemMessage(SurvivalAsMessages.unlockNotice());
        }
        return true;
    }

    /**
     * Survival-as only: rewrite the world's locked morph and rebind the player.
     * Forces a habit reset (hunger crash + traits popup) so diet/circadian must be relearned.
     */
    public static boolean changeIdentity(ServerPlayer player, ResourceLocation newMorph) {
        if (player == null || newMorph == null || player.getServer() == null) {
            return false;
        }
        if (!SurvivalAsWorldStorage.isEnabled() || !SurvivalAsWorldStorage.isLocked()) {
            return false;
        }
        if (CompatAccess.getEntityType(newMorph) == null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("command.naturalis.invalid_id", newMorph.toString()),
                true);
            return false;
        }

        SurvivalAsWorldStorage.changeMorph(player.getServer(), newMorph);

        clearLegacyBinding(player);
        boolean acquired = MorphAcquisition.acquire(player, newMorph);
        if (!acquired) {
            return false;
        }

        // Habit shock: new stomach, new clock — you must feed and adapt again.
        player.getFoodData().setFoodLevel(Math.min(player.getFoodData().getFoodLevel(), 6));
        player.getFoodData().setSaturation(0.0F);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.HUNGER, 200, 0, false, true, true));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            CompatAccess.resolveMobEffect("NAUSEA", "CONFUSION"), 160, 0, false, true, true));

        var type = CompatAccess.getEntityType(newMorph);
        player.sendSystemMessage(SurvivalAsMessages.habitChanged(type, newMorph));

        SurvivalAsTraitSummary.Sheet sheet = SurvivalAsTraitSummary.build(player, newMorph);
        PlayToClientSender.send(player, new SurvivalAsTraitsPayload(
            sheet.morphId().toString(),
            sheet.mass(),
            sheet.dietId(),
            sheet.traitIds(),
            sheet.traitExtras()
        ));
        SurvivalAsWorldStorage.markTraitsShown(player.getServer());
        PlayToClientSender.send(player, new SurvivalAsLockPayload(true));
        PlayToClientSender.send(player, new HumanityPayload(0, false));
        return true;
    }

    /** Older Survival-as worlds applied Morph Binding; strip it so the HUD stays clean. */
    private static void clearLegacyBinding(ServerPlayer player) {
        Holder<MobEffect> binding = CompatAccess.naturalisMobEffectHolder("morph_binding");
        if (player.hasEffect(binding)) {
            player.removeEffect(binding);
        }
    }
}
