package dev.naturalis.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.naturalis.NaturalisMod;
import dev.naturalis.experience.NaturalisExperienceEvents;
import dev.naturalis.experience.NaturalisExperienceMode;
import dev.naturalis.experience.NaturalisWorldExperienceStorage;
import dev.naturalis.combat.NaturalAttackManager;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.metabolism.MassInertiaManager;
import dev.naturalis.metabolism.MetabolismManager;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.world.menu.MorphKnowledgeMenu;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.naturalis.network.ClientSoundPrefsPayload;
import dev.naturalis.network.HumanityPayload;
import dev.naturalis.network.PlayToClientSender;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import dev.naturalis.util.MorphAcquisition;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class MorphCommand {

    private MorphCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("morph")
            .then(Commands.literal("knowledge")
                .executes(commandContext -> executeKnowledge(commandContext.getSource()))
                .then(Commands.literal("stats")
                    .executes(commandContext -> executeKnowledgeStats(commandContext.getSource())))
                .then(Commands.literal("spend")
                    .then(Commands.literal("vitality").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_VITALITY)))
                    .then(Commands.literal("handling").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_HANDLING)))
                    .then(Commands.literal("instinct").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_INSTINCT)))
                    .then(Commands.literal("wander").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_WANDER)))
                    .then(Commands.literal("connection").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION)))
                    .then(Commands.literal("resonance").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION)))
                    .then(Commands.literal("damage").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_DAMAGE)))
                    .then(Commands.literal("resistance").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE)))
                    .then(Commands.literal("morph_resistance").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE)))
                    .then(Commands.literal("utilities").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_UTILITIES)))
                    .then(Commands.literal("social").executes(commandContext -> executeKnowledgeSpend(commandContext.getSource(), MorphKnowledgeManager.BRANCH_SOCIAL))))
                .then(Commands.literal("addxp")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(commandContext -> executeAddXp(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "amount")))))
                .then(Commands.literal("addlevel")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                        .executes(commandContext -> executeAddLevel(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "levels")))))
                .then(Commands.literal("setxp")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(commandContext -> executeSetXp(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "amount")))))
                .then(Commands.literal("setlevel")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, MorphKnowledgeManager.getMaxPointLevel()))
                        .executes(commandContext -> executeSetLevel(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "level")))))
            )
            .then(Commands.literal("experience")
                .then(Commands.literal("choose")
                    .executes(ctx -> executeExperienceChoose(ctx.getSource())))
                .then(Commands.literal("realistic")
                    .executes(ctx -> executeExperience(ctx.getSource(), NaturalisExperienceMode.REALISTIC)))
                .then(Commands.literal("softened")
                    .executes(ctx -> executeExperience(ctx.getSource(), NaturalisExperienceMode.SOFTENED)))
                .then(Commands.literal("status")
                    .executes(ctx -> executeExperienceStatus(ctx.getSource()))))
            .then(Commands.literal("survival_as")
                .then(Commands.literal("status")
                    .executes(ctx -> executeSurvivalAsStatus(ctx.getSource())))
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("mob", ResourceLocationArgument.id())
                        .suggests((commandContext, suggestionsBuilder) ->
                            net.minecraft.commands.SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), suggestionsBuilder))
                        .executes(ctx -> executeSurvivalAsSet(ctx.getSource(), ResourceLocationArgument.getId(ctx, "mob")))))
                .then(Commands.literal("unlock")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> executeSurvivalAsUnlock(ctx.getSource())))
                .then(Commands.literal("clear")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> executeSurvivalAsClear(ctx.getSource()))))
            .then(Commands.literal("acquire")
                .then(Commands.argument("mob", ResourceLocationArgument.id())
                    .suggests((commandContext, suggestionsBuilder) ->
                        net.minecraft.commands.SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), suggestionsBuilder))
                    .executes(commandContext -> executeAcquire(commandContext.getSource(), ResourceLocationArgument.getId(commandContext, "mob")))))
            .then(Commands.literal("brewed")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mob", ResourceLocationArgument.id())
                    .suggests((commandContext, suggestionsBuilder) ->
                        net.minecraft.commands.SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), suggestionsBuilder))
                    .executes(commandContext -> executeBrewed(commandContext.getSource(), ResourceLocationArgument.getId(commandContext, "mob"), 120))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                        .executes(commandContext -> executeBrewed(
                            commandContext.getSource(),
                            ResourceLocationArgument.getId(commandContext, "mob"),
                            IntegerArgumentType.getInteger(commandContext, "seconds"))))))
            .then(Commands.literal("resonance")
                .executes(commandContext -> executeResonanceStatus(commandContext.getSource()))
                .then(Commands.literal("status")
                    .executes(commandContext -> executeResonanceStatus(commandContext.getSource())))
                .then(Commands.literal("bond")
                    .executes(commandContext -> executeResonanceBondCurrent(commandContext.getSource()))
                    .then(Commands.argument("mob", ResourceLocationArgument.id())
                        .suggests((commandContext, suggestionsBuilder) ->
                            net.minecraft.commands.SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), suggestionsBuilder))
                        .executes(commandContext -> executeResonanceBond(
                            commandContext.getSource(),
                            ResourceLocationArgument.getId(commandContext, "mob")))))
                .then(Commands.literal("clearbond")
                    .executes(commandContext -> executeResonanceClearBond(commandContext.getSource())))
                .then(Commands.literal("activate")
                    .executes(commandContext -> executeResonanceActivate(commandContext.getSource())))
                .then(Commands.literal("deactivate")
                    .executes(commandContext -> executeResonanceDeactivate(commandContext.getSource())))
                .then(Commands.literal("rebirth")
                    .executes(commandContext -> executeResonanceRebirth(commandContext.getSource())))
                .then(Commands.literal("instinct")
                    .executes(commandContext -> executeResonanceInstinct(commandContext.getSource()))))
            .then(Commands.literal("client")
                .then(Commands.literal("mutePerceptionSounds")
                    .then(Commands.argument("muted", BoolArgumentType.bool())
                        .executes(commandContext -> executeClientMutePerceptionSounds(
                            commandContext.getSource(),
                            BoolArgumentType.getBool(commandContext, "muted"))))))
            .then(Commands.literal("admin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("humanity")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                            .executes(commandContext -> executeAdminHumanity(
                                commandContext.getSource(),
                                EntityArgument.getPlayer(commandContext, "target"),
                                IntegerArgumentType.getInteger(commandContext, "value")))))))
            .then(Commands.literal("debug")
                .then(Commands.literal("metabolism")
                    .executes(commandContext -> executeDebugMetabolism(commandContext.getSource())))
                .then(Commands.literal("environment")
                    .executes(commandContext -> executeDebugEnvironment(commandContext.getSource())))
                .then(Commands.literal("diet")
                    .executes(commandContext -> executeDebugDiet(commandContext.getSource())))
                .then(Commands.literal("naturalattack")
                    .executes(commandContext -> executeDebugNaturalAttack(commandContext.getSource()))
                    .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(commandContext -> executeReloadNaturalAttack(commandContext.getSource()))))
                .then(Commands.literal("config")
                    .requires(source -> source.hasPermission(2))
                    .executes(commandContext -> executeReloadConfig(commandContext.getSource())))));
    }

    private static int executeAcquire(CommandSourceStack source, ResourceLocation id) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        boolean acquired = MorphAcquisition.acquire(player, id);
        if (acquired) {
            source.sendSuccess(() -> MorphAcquisition.formatAcquireSuccess(id), false);
            return 1;
        }

        source.sendFailure(MorphAcquisition.formatAcquireFailed(id));
        return 0;
    }

    private static int executeBrewed(CommandSourceStack source, ResourceLocation id, int seconds) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        int durationTicks = seconds * 20;
        boolean applied = MorphCommandBridge.applyBrewedMorph(player, id, durationTicks);
        if (!applied) {
            source.sendFailure(Component.translatable("command.naturalis.invalid_id", id.toString()));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.naturalis.brewed.applied", id.toString(), seconds), false);
        return 1;
    }

    private static int executeResonanceBond(CommandSourceStack source, ResourceLocation id) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation resolved = resolveResonanceBondTarget(player, id);
        if (resolved == null) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.bond.current_required"));
            return 0;
        }

        if (!ResonanceManager.isValidLivingMorph(resolved)) {
            source.sendFailure(Component.translatable("command.naturalis.invalid_id", resolved.toString()));
            return 0;
        }

        if (!ResonanceManager.setBondedMorph(player, resolved)) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.bond.requirement_knowledge"));
            return 0;
        }

        MorphCommandBridge.resonanceOnBondSet(player);
        source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.bond.set", resolved.toString()), false);

        // Bonding is the intentional opt-in point; bring resonance online immediately.
        if (ResonanceManager.setResonanceEnabled(player, true)) {
            source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.activated"), false);
        }

        return 1;
    }

    private static int executeResonanceBondCurrent(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation currentMorph = CurrentMorphUtil.getCurrentMorphId(player);
        if (currentMorph == null) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.bond.current_required"));
            return 0;
        }

        return executeResonanceBond(source, currentMorph);
    }

    private static ResourceLocation resolveResonanceBondTarget(ServerPlayer player, ResourceLocation requested) {
        if (requested == null) {
            return null;
        }

        if ("minecraft".equals(requested.getNamespace())) {
            String path = requested.getPath();
            if ("mob".equals(path) || "current".equals(path)) {
                return CurrentMorphUtil.getCurrentMorphId(player);
            }
        }

        return requested;
    }

    private static int executeResonanceClearBond(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResonanceManager.clearBond(player);
        source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.bond.cleared"), false);
        return 1;
    }

    private static int executeResonanceActivate(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        if (!ResonanceManager.setResonanceEnabled(player, true)) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.no_bond"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.activated"), false);
        return 1;
    }

    private static int executeResonanceDeactivate(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        if (!ResonanceManager.setResonanceEnabled(player, false)) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.locked_active"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.deactivated"), false);
        return 1;
    }

    private static int executeResonanceRebirth(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        MorphCommandBridge.RebirthOutcome result = MorphCommandBridge.resonanceRebirth(player);
        if (result != MorphCommandBridge.RebirthOutcome.OK) {
            return switch (result) {
                case NOT_LOST -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.rebirth.not_lost"));
                    yield 0;
                }
                case NOT_MORPHED -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.rebirth.not_morphed"));
                    yield 0;
                }
                case SPAWN_FAILED -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.rebirth.spawn_failed"));
                    yield 0;
                }
                case FORBIDDEN_IN_NATURAL -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.rebirth.forbidden_in_natural"));
                    yield 0;
                }
                case OK -> 1;
            };
        }

        source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.rebirth.success"), false);
        return 1;
    }

    private static int executeResonanceInstinct(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        if (ResonanceManager.getBondedMorph(player) == null) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.no_bond"));
            return 0;
        }

        if (!ResonanceManager.isResonanceEnabled(player)) {
            source.sendFailure(Component.translatable("command.naturalis.resonance.not_active"));
            return 0;
        }

        MorphCommandBridge.InstinctOutcome result = MorphCommandBridge.resonanceInstinct(player);
        if (result != MorphCommandBridge.InstinctOutcome.OK) {
            return switch (result) {
                case NO_BOND -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.no_bond"));
                    yield 0;
                }
                case NOT_ACTIVE -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.not_active"));
                    yield 0;
                }
                case NOT_ALIGNED -> {
                    source.sendFailure(Component.translatable("command.naturalis.resonance.instinct.requires_bonded_form"));
                    yield 0;
                }
                case COOLDOWN -> {
                    long cooldown = ResonanceManager.getActiveInstinctCooldownRemaining(player, player.level().getGameTime()) / 20L;
                    source.sendFailure(Component.translatable("command.naturalis.resonance.instinct.cooldown", cooldown));
                    yield 0;
                }
                case OK -> 1;
            };
        }

        source.sendSuccess(() -> Component.translatable("command.naturalis.resonance.instinct.triggered"), false);
        return 1;
    }

    private static int executeResonanceStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        String bondedText = bonded == null ? "none" : bonded.toString();
        boolean active = ResonanceManager.isResonanceEnabled(player);
        long cooldown = ResonanceManager.getActiveInstinctCooldownRemaining(player, player.level().getGameTime()) / 20L;
        int masteryXp = ResonanceManager.getMasteryXp(player);
        int masteryTier = ResonanceManager.getMasteryTier(player);
        int humanity = ResonanceManager.getHumanity(player);
        boolean locked = ResonanceManager.isHumanityLocked(player);
        String stage = ResonanceManager.getHumanityStage(player).name().toLowerCase(Locale.ROOT);
        String archetype = ResonanceManager.getBondedArchetype(player).name().toLowerCase(Locale.ROOT);

        source.sendSuccess(() -> Component.translatable(
            "command.naturalis.resonance.status",
            bondedText,
            active,
            cooldown,
            masteryTier,
            masteryXp,
            humanity,
            locked,
            stage,
            archetype), false);
        return 1;
    }

    private static int executeKnowledge(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        Component boundDisplay = bonded == null
            ? Component.translatable("gui.naturalis.knowledge.bound.none")
            : Component.literal(bonded.toString());

        player.openMenu(new SimpleMenuProvider((containerId, inventory, p) ->
            new MorphKnowledgeMenu(containerId, inventory, player),
            Component.empty()
                .append(Component.translatable("gui.naturalis.knowledge.title"))
                .append(Component.literal(" | "))
                .append(Component.translatable("gui.naturalis.knowledge.bound", boundDisplay))));
        return 1;
    }

    private static int executeKnowledgeStats(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            int globalXp = MorphKnowledgeManager.getGlobalXp(player);
            int pointLevel = MorphKnowledgeManager.getGlobalPointLevelForXp(globalXp);
            int totalPoints = MorphKnowledgeManager.getGlobalKnowledgePointsForXp(globalXp);
            int spentPoints = MorphKnowledgeManager.getSpentKnowledgePoints(player, null);
            int unspentPoints = MorphKnowledgeManager.getUnspentKnowledgePoints(player, null);
            source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.stats", pointLevel, globalXp, MorphKnowledgeManager.getGlobalMaxXp(), "global"), false);
            source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.points", spentPoints, totalPoints, unspentPoints), false);
            return 1;
        }

        int xp = MorphKnowledgeManager.getXp(player, morphId);
        int level = MorphKnowledgeManager.getLevelForXp(xp);
        int totalPoints = MorphKnowledgeManager.getTotalKnowledgePoints(player, morphId);
        int spentPoints = MorphKnowledgeManager.getSpentKnowledgePoints(player, morphId);
        int unspentPoints = MorphKnowledgeManager.getUnspentKnowledgePoints(player, morphId);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.stats", level, xp, MorphKnowledgeManager.getMaxXp(), morphId), false);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.points", spentPoints, totalPoints, unspentPoints), false);
        return 1;
    }

    private static int executeKnowledgeSpend(CommandSourceStack source, String branch) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            morphId = null;
        }

        boolean spent = MorphKnowledgeManager.spendKnowledgePoint(player, morphId, branch);
        if (!spent) {
            source.sendFailure(Component.translatable("command.naturalis.knowledge.spend.failed", branch));
            return 0;
        }

        int rank = MorphKnowledgeManager.getBranchRank(player, morphId, branch);
        int max = MorphKnowledgeManager.getMaxRankForBranch(branch);
        int left = MorphKnowledgeManager.getUnspentKnowledgePoints(player, morphId);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.spend.success", branch, rank, max, left), false);
        return 1;
    }

    private static int executeAddXp(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            MorphKnowledgeManager.addGlobalXp(player, amount);
            int xp = MorphKnowledgeManager.getGlobalXp(player);
            int level = MorphKnowledgeManager.getGlobalPointLevelForXp(xp);
            source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", level, xp, MorphKnowledgeManager.getGlobalMaxXp()), false);
            return 1;
        }

        MorphKnowledgeManager.addXp(player, morphId, amount);
        int xp = MorphKnowledgeManager.getXp(player, morphId);
        int level = MorphKnowledgeManager.getPointLevelForXp(xp);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", level, xp, MorphKnowledgeManager.getMaxXp()), false);
        return 1;
    }

    private static int executeAddLevel(CommandSourceStack source, int levels) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            int currentLevel = MorphKnowledgeManager.getGlobalPointLevelForXp(MorphKnowledgeManager.getGlobalXp(player));
            int targetLevel = Math.min(MorphKnowledgeManager.getMaxPointLevel(), currentLevel + levels);
            int targetXp = MorphKnowledgeManager.getRequiredGlobalXpForPointLevel(targetLevel);
            MorphKnowledgeManager.setGlobalXp(player, targetXp);
            source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", targetLevel, targetXp, MorphKnowledgeManager.getGlobalMaxXp()), false);
            return 1;
        }

        int currentLevel = MorphKnowledgeManager.getPointLevelForXp(MorphKnowledgeManager.getXp(player, morphId));
        int targetLevel = Math.min(MorphKnowledgeManager.getMaxPointLevel(), currentLevel + levels);
        int targetXp = MorphKnowledgeManager.getRequiredXpForPointLevel(targetLevel);
        MorphKnowledgeManager.setXp(player, morphId, targetXp);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", targetLevel, targetXp, MorphKnowledgeManager.getMaxXp()), false);
        return 1;
    }

    private static int executeSetXp(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            MorphKnowledgeManager.setGlobalXp(player, amount);
            int xp = MorphKnowledgeManager.getGlobalXp(player);
            int level = MorphKnowledgeManager.getGlobalPointLevelForXp(xp);
            source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", level, xp, MorphKnowledgeManager.getGlobalMaxXp()), false);
            return 1;
        }

        MorphKnowledgeManager.setXp(player, morphId, amount);
        int xp = MorphKnowledgeManager.getXp(player, morphId);
        int level = MorphKnowledgeManager.getPointLevelForXp(xp);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", level, xp, MorphKnowledgeManager.getMaxXp()), false);
        return 1;
    }

    private static int executeSetLevel(CommandSourceStack source, int level) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        int targetLevel = Math.max(0, Math.min(MorphKnowledgeManager.getMaxPointLevel(), level));
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            int targetXp = MorphKnowledgeManager.getRequiredGlobalXpForPointLevel(targetLevel);
            MorphKnowledgeManager.setGlobalXp(player, targetXp);
            source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", targetLevel, targetXp, MorphKnowledgeManager.getGlobalMaxXp()), false);
            return 1;
        }

        int targetXp = MorphKnowledgeManager.getRequiredXpForPointLevel(targetLevel);
        MorphKnowledgeManager.setXp(player, morphId, targetXp);
        source.sendSuccess(() -> Component.translatable("command.naturalis.knowledge.updated", targetLevel, targetXp, MorphKnowledgeManager.getMaxXp()), false);
        return 1;
    }

    private static int executeDebugMetabolism(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            source.sendFailure(Component.translatable("command.naturalis.knowledge.no_morph"));
            return 0;
        }

        double mass = MetabolismManager.getMass(morphId);
        double metabolism = MetabolismManager.getMetabolismMultiplierForPlayer(player, morphId);
        double speed = MassInertiaManager.getMovementSpeedMultiplier(mass);
        double gravity = MassInertiaManager.getGravityMultiplier(mass);
        double knockback = MassInertiaManager.getKnockbackResistance(mass);
        double step = MassInertiaManager.getStepHeightMultiplier(mass);
        double fall = MassInertiaManager.getFallDamageMultiplier(mass);

        source.sendSuccess(() -> Component.translatable(
            "command.naturalis.debug.metabolism",
            morphId,
            fmt(mass),
            fmt(metabolism),
            fmt(speed),
            fmt(gravity),
            fmt(knockback),
            fmt(step),
            fmt(fall)), false);
        return 1;
    }

    private static int executeDebugEnvironment(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            source.sendFailure(Component.translatable("command.naturalis.knowledge.no_morph"));
            return 0;
        }

        EnvironmentalSusceptibilityManager.EnvironmentType environmentType =
            EnvironmentalSusceptibilityManager.getEnvironmentType(player.level(), player.position(), player.isInWater());
        EnvironmentalSusceptibilityManager.EnvironmentalVulnerability vulnerability =
            EnvironmentalSusceptibilityManager.checkVulnerability(morphId, environmentType);

        boolean dryVulnerable = EnvironmentalSusceptibilityManager.isDryVulnerable(morphId);
        boolean drySuffering = EnvironmentalSusceptibilityManager.isDrySuffering(morphId, player.level(), player.position());
        boolean awayFromWater = EnvironmentalSusceptibilityManager.isAwayFromWater(player.level(), player.position(), 16.0D);
        boolean raining = player.level().isRaining();

        source.sendSuccess(() -> Component.translatable(
            "command.naturalis.debug.environment",
            morphId,
            environmentType.name(),
            vulnerability.name(),
            dryVulnerable,
            drySuffering,
            player.isInWater(),
            awayFromWater,
            raining), false);
        return 1;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static int executeDebugDiet(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            source.sendFailure(Component.translatable("command.naturalis.knowledge.no_morph"));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        String debug = MorphCommandBridge.debugDiet(morphId, itemId);
        source.sendSuccess(() -> Component.translatable("command.naturalis.debug.diet", debug), false);
        return 1;
    }

    private static int executeDebugNaturalAttack(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            source.sendFailure(Component.translatable("command.naturalis.knowledge.no_morph"));
            return 0;
        }

        NaturalAttackManager.NaturalAttackConfig config = NaturalAttackManager.getConfig(morphId, 1.0F);
        int cooldown = NaturalAttackManager.getRemainingCooldownTicks(player, player.level().getGameTime());

        source.sendSuccess(() -> Component.translatable(
            "command.naturalis.debug.natural_attack",
            morphId,
            config.type().name(),
            fmt(config.damage()),
            fmt(config.range()),
            fmt(config.velocity()),
            config.cooldown(),
            config.replaceNormalAttack(),
            cooldown), false);
        return 1;
    }

    private static int executeReloadNaturalAttack(CommandSourceStack source) {
        NaturalAttackManager.reload();
        source.sendSuccess(() -> Component.translatable("command.naturalis.debug.natural_attack.reloaded"), true);
        return 1;
    }

    private static int executeExperienceChoose(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }
        NaturalisExperienceEvents.requestChoiceScreen(player);
        return 1;
    }

    private static int executeExperience(CommandSourceStack source, NaturalisExperienceMode mode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.player_only"));
            return 0;
        }
        if (!NaturalisExperienceEvents.applyChoice(player, mode)) {
            source.sendFailure(Component.translatable("command.naturalis.experience.denied"));
            return 0;
        }
        return 1;
    }

    private static int executeExperienceStatus(CommandSourceStack source) {
        NaturalisExperienceMode mode = NaturalisWorldExperienceStorage.getMode();
        boolean chosen = NaturalisWorldExperienceStorage.isChosen();
        source.sendSuccess(() -> chosen
            ? Component.translatable(
                mode.isRealistic()
                    ? "command.naturalis.experience.status_realistic"
                    : "command.naturalis.experience.status_softened")
            : Component.translatable("command.naturalis.experience.status_unset"), false);
        return 1;
    }

    private static int executeSurvivalAsStatus(CommandSourceStack source) {
        if (!dev.naturalis.survivalas.SurvivalAsWorldStorage.isEnabled()) {
            source.sendSuccess(() -> Component.translatable("command.naturalis.survival_as.status_off"), false);
            return 1;
        }
        ResourceLocation morph = dev.naturalis.survivalas.SurvivalAsWorldStorage.getMorphId();
        boolean locked = dev.naturalis.survivalas.SurvivalAsWorldStorage.isLocked();
        source.sendSuccess(() -> Component.translatable(
            "command.naturalis.survival_as.status_on",
            morph == null ? "?" : morph.toString(),
            locked
        ), false);
        return 1;
    }

    private static int executeSurvivalAsSet(CommandSourceStack source, ResourceLocation morphId) {
        if (source.getServer() == null) {
            return 0;
        }
        dev.naturalis.survivalas.SurvivalAsWorldStorage.enable(source.getServer(), morphId);
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            dev.naturalis.survivalas.SurvivalAsRuntime.onPlayerJoin(player);
        }
        source.sendSuccess(() -> Component.translatable("command.naturalis.survival_as.set", morphId.toString()), true);
        return 1;
    }

    private static int executeSurvivalAsUnlock(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (!dev.naturalis.survivalas.SurvivalAsRuntime.unlock(source.getServer(), player)) {
            source.sendFailure(Component.translatable("command.naturalis.survival_as.not_active"));
            return 0;
        }
        return 1;
    }

    private static int executeSurvivalAsClear(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            player.removeEffect(dev.naturalis.content.NaturalisMobEffects.MORPH_BINDING);
        }
        dev.naturalis.survivalas.SurvivalAsWorldStorage.disable(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.naturalis.survival_as.cleared"), true);
        return 1;
    }

    private static int executeReloadConfig(CommandSourceStack source) {
        NaturalAttackManager.reload();
        source.sendSuccess(() -> Component.translatable(
            "command.naturalis.debug.config.reloaded",
            NaturalisMod.ID + "-common.toml",
            NaturalisMod.ID + "-client.toml"
        ), true);
        return 1;
    }

    private static int executeClientMutePerceptionSounds(CommandSourceStack source, boolean muted) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.naturalis.client.mute_perception.player_only"));
            return 0;
        }
        PlayToClientSender.send(player, new ClientSoundPrefsPayload(muted));
        source.sendSuccess(() -> muted
            ? Component.translatable("command.naturalis.client.mute_perception.success_muted")
            : Component.translatable("command.naturalis.client.mute_perception.success_unmuted"), true);
        return 1;
    }

    private static int executeAdminHumanity(CommandSourceStack source, ServerPlayer target, int value) {
        ResonanceManager.setHumanity(target, value);
        PlayToClientSender.send(target, new HumanityPayload(
            ResonanceManager.getHumanity(target),
            ResonanceManager.isResonanceEnabled(target)
        ));
        source.sendSuccess(() -> Component.translatable("command.naturalis.admin.humanity.success", target.getName().getString(), value), true);
        return 1;
    }
}
