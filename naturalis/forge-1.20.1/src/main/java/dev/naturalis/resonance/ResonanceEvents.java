package dev.naturalis.resonance;

import dev.naturalis.Naturalis;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.metabolism.MetabolismManager;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.worldgen.NaturalDimensionKeys;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class ResonanceEvents {

    private static final net.minecraft.world.effect.MobEffect SPEED_EFFECT =
        CompatAccess.resolveMobEffect("MOVEMENT_SPEED", "SPEED");
    private static final net.minecraft.world.effect.MobEffect NAUSEA_EFFECT =
        CompatAccess.resolveMobEffect("CONFUSION", "NAUSEA");
    private static final net.minecraft.world.effect.MobEffect STRENGTH_EFFECT =
        CompatAccess.resolveMobEffect("DAMAGE_BOOST", "STRENGTH");
    private static final net.minecraft.world.effect.MobEffect RESISTANCE_EFFECT =
        CompatAccess.resolveMobEffect("DAMAGE_RESISTANCE", "RESISTANCE");
    private static final net.minecraft.world.effect.MobEffect SLOWNESS_EFFECT =
        CompatAccess.resolveMobEffect("MOVEMENT_SLOWDOWN", "SLOWNESS");
    private static final net.minecraft.world.effect.MobEffect JUMP_BOOST_EFFECT =
        CompatAccess.resolveMobEffect("JUMP", "JUMP_BOOST");

    private static final ResourceLocation ADV_ROOT = new ResourceLocation(Naturalis.MOD_ID, "root");
    private static final ResourceLocation ADV_FIRST_BOND = new ResourceLocation(Naturalis.MOD_ID, "resonance/first_bond");
    private static final ResourceLocation ADV_RESONANCE_ONLINE = new ResourceLocation(Naturalis.MOD_ID, "resonance/resonance_online");
    private static final ResourceLocation ADV_MILESTONE_ONE = new ResourceLocation(Naturalis.MOD_ID, "resonance/milestone_one");
    private static final ResourceLocation ADV_MILESTONE_TWO = new ResourceLocation(Naturalis.MOD_ID, "resonance/milestone_two");
    private static final ResourceLocation ADV_MILESTONE_THREE = new ResourceLocation(Naturalis.MOD_ID, "resonance/milestone_three");
    private static final ResourceLocation ADV_HUMANITY_DRIFTING = new ResourceLocation(Naturalis.MOD_ID, "resonance/humanity_drifting");
    private static final ResourceLocation ADV_HUMANITY_SPLIT = new ResourceLocation(Naturalis.MOD_ID, "resonance/humanity_split");
    private static final ResourceLocation ADV_HUMANITY_FERAL = new ResourceLocation(Naturalis.MOD_ID, "resonance/humanity_feral");
    private static final ResourceLocation ADV_HUMANITY_PRIMAL = new ResourceLocation(Naturalis.MOD_ID, "resonance/humanity_primal");
    private static final ResourceLocation ADV_HUMANITY_LOST = new ResourceLocation(Naturalis.MOD_ID, "resonance/humanity_lost");
    private static final ResourceLocation ADV_REBIRTH_ACCEPTED = new ResourceLocation(Naturalis.MOD_ID, "resonance/rebirth_accepted");
    private static final ResourceLocation ADV_TOKEN_MINOR = new ResourceLocation(Naturalis.MOD_ID, "resonance/token_minor");
    private static final ResourceLocation ADV_TOKEN_GREATER = new ResourceLocation(Naturalis.MOD_ID, "resonance/token_greater");
    private static final ResourceLocation ADV_TOOL_DECAY = new ResourceLocation(Naturalis.MOD_ID, "resonance/tool_decay");
    private static final ResourceLocation ADV_TOOL_REJECTED = new ResourceLocation(Naturalis.MOD_ID, "resonance/tool_rejected");
    private static final ResourceLocation ADV_MORPH_SLEEPLESS = new ResourceLocation(Naturalis.MOD_ID, "resonance/morph_sleepless");
    private static final String ROOT_SLEEP_TAG = "naturalis_resonance_sleep";
    private static final String WAS_SLEEPING = "was_sleeping";
    private static final String SLEPT_MORPHED = "slept_morphed";
    private static final String SLEPT_CONSECRATED = "slept_consecrated";
    private static final String COMBAT_HIT_STACKS = "combat_hit_stacks";
    private static final String LAST_RESTLESS_TICK = "last_restless_tick";
    private static final String LAST_BLINDNESS_TICK = "last_blindness_tick";
    private static final String LAST_NAUSEA_TICK = "last_nausea_tick";
    private static final String LAST_THREAT_SENSE_TICK = "last_threat_sense_tick";
    private static final String LAST_PACK_READING_TICK = "last_pack_reading_tick";
    private static final String LAST_PREY_RECOGNITION_TICK = "last_prey_recognition_tick";
    private static final String LAST_TERRITORIAL_PING_TICK = "last_territorial_ping_tick";
    private static final String LAST_APEX_AURA_TICK = "last_apex_aura_tick";
    private static final String LAST_REGEN_TICK = "last_regen_tick";
    private static final String LAST_RITE_TARGET = "last_rite_target";
    private static final String LAST_RITE_ENGAGE_TICK = "last_rite_engage_tick";
    private static final String LAST_RITE_FULL_HEALTH_HIT = "last_rite_full_health_hit";
    private static final String LAST_HUMANITY_STAGE_ANNOUNCED = "last_humanity_stage_announced";
    private static final String LOST_PROMPT_UNTIL_TICK = "lost_prompt_until_tick";
    private static final String REBIRTH_LOOT_TAG = "naturalis_rebirth_loot";
    private static final String REBIRTH_LOOT_SLOTS_TAG = "naturalis_rebirth_slots";
    private static final String REBIRTH_OWNER_TAG = "naturalis_rebirth_owner";
    private static final String REBIRTH_MORPH_TAG = "naturalis_rebirth_morph";
    private static final TagKey<Block> CONSECRATED_BEDS = TagKey.create(
        net.minecraft.core.registries.Registries.BLOCK,
        new ResourceLocation(Naturalis.MOD_ID, "consecrated_beds")
    );

    private ResonanceEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 == 0 && ResonanceManager.maybeAutoBondMostUsedMorph(player)) {
            ResourceLocation bondedAuto = ResonanceManager.getBondedMorph(player);
            if (bondedAuto != null) {
                player.displayClientMessage(Component.translatable("message.naturalis.resonance.auto_bonded", bondedAuto.toString()), true);
            }
        }

        if (player.tickCount % 20 == 0) {
            dev.naturalis.network.NaturalisNetwork.sendToPlayer(
                player,
                new dev.naturalis.network.HumanityPayload(
                    ResonanceManager.getHumanity(player),
                    ResonanceManager.isResonanceEnabled(player)
                )
            );
        }

        handleSleepHumanity(player);

        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }

        tickLostRebirthPrompt(player);

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        if (bonded == null) {
            ResonanceManager.setResonanceEnabled(player, false);
            return;
        }

        grantAdvancement(player, ADV_RESONANCE_ONLINE);

        long now = player.level().getGameTime();
        if (player.tickCount % 20 == 0) {
            ResonanceManager.HumanityStage stageBefore = ResonanceManager.getHumanityStage(player);
            ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
            boolean morphed = current != null;
            boolean aligned = ResonanceManager.isAligned(player);

            if (aligned && morphed) {
                applyMorphHumanityProgression(player);
            } else if (!morphed) {
                // Very slow passive recovery while consciously living as human.
                if (player.tickCount % 120 == 0) {
                    ResonanceManager.addHumanityRecoveryProgress(player, 1);
                }

                if (ResonanceManager.getHumanity(player) >= 60 && ResonanceManager.getHumanity(player) <= 79 && player.tickCount % 40 == 0) {
                    // Drifting: hunger pressure when trying to remain human.
                    player.causeFoodExhaustion(0.08F);
                }

                // Below primal threshold, human form slowly slips unless actively anchored by recovery effects.
                if (ResonanceManager.getHumanity(player) <= 19
                    && !ResonanceManager.isRecoveryWindowActive(player, now)
                    && !ResonanceManager.isHumanFormLockActive(player, now)
                    && player.tickCount % 200 == 0) {
                    ResonanceManager.addHumanity(player, -1);
                }
            }

            applyHumanityStateEffects(player);
            applyStageRewards(player, aligned, morphed);
            grantHumanityAdvancements(player);
            handleHumanityNarrativeTransition(player, stageBefore, ResonanceManager.getHumanityStage(player));
        }

        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        if (current == null || !current.equals(bonded)) {
            return;
        }

        applyArchetypeEffects(player, ResonanceManager.getBondedArchetype(player));

        // Mastery makes shape-binding incidents less likely, but never impossible.
        if (player.tickCount % 40 == 0 && !player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get())) {
            int level = MorphKnowledgeManager.getLevel(player, bonded);
            double chance = getBindingSurgeChance(level);
            if (ResonanceManager.getHumanity(player) <= 20) {
                chance += 0.03D;
            }
            if (player.getRandom().nextDouble() < chance) {
                player.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING.get(), 8 * 20, 0, true, false, true));
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        if (bonded == null || current == null || !bonded.equals(current)) {
            if (current == null) {
                applyHumanToolDamagePenalty(player, event);
            }
            return;
        }

        if (ResonanceManager.getHumanityStage(player).ordinal() >= ResonanceManager.HumanityStage.PRIMAL.ordinal()) {
            LivingEntity victim = event.getEntity();
            float victimHealthBefore = victim.getHealth();
            boolean fullHealthHit = victimHealthBefore >= victim.getMaxHealth() - 0.01F;

            CompoundTag tag = ResonanceManager.getOrCreateTag(player);
            tag.putString(LAST_RITE_TARGET, victim.getStringUUID());
            tag.putLong(LAST_RITE_ENGAGE_TICK, player.level().getGameTime());
            tag.putBoolean(LAST_RITE_FULL_HEALTH_HIT, fullHealthHit || event.getAmount() >= victimHealthBefore);
        }

        float multiplier = ResonanceManager.getDamageMultiplier(player);

        if (ResonanceManager.getHumanityStage(player).ordinal() >= ResonanceManager.HumanityStage.FERAL.ordinal()) {
            // Feral predator apex: shield block/parry is broken through.
            if (event.getEntity() instanceof Player defendingPlayer && defendingPlayer.isBlocking()) {
                defendingPlayer.stopUsingItem();
                CompatAccess.addItemCooldown(defendingPlayer, defendingPlayer.getUseItem(), 20);
            }
        }

        event.setAmount(event.getAmount() * multiplier);

        // Sustained combat as your bonded body slowly deepens identity loss.
        if (event.getAmount() > 0.0F) {
            CompoundTag tag = ResonanceManager.getOrCreateTag(player);
            int hitStacks = Math.max(0, CompatAccess.getInt(tag, COMBAT_HIT_STACKS)) + 1;
            if (hitStacks >= 4) {
                applyHumanityActionLoss(player, 1);
                hitStacks = 0;
            }
            tag.putInt(COMBAT_HIT_STACKS, hitStacks);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getEntity().level() instanceof ServerLevel level) {
            releaseRebirthLoot(victim, level);
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || victim == player) {
            return;
        }

        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        if (bonded == null || current == null || !bonded.equals(current)) {
            return;
        }

        int before = ResonanceManager.getMasteryTier(player);
        int gain = 12;
        if (ResonanceManager.getHumanity(player) <= 40) {
            gain += 3;
        }
        if (ResonanceManager.getHumanity(player) <= 20) {
            gain += 4;
        }

        ResonanceManager.addMasteryXp(player, gain);
        applyHumanityActionLoss(player, 2);

        if (ResonanceManager.getHumanity(player) <= 79) {
            player.getFoodData().eat(1, 0.1F);
        }

        tryApplyLastRite(player, victim);

        int after = ResonanceManager.getMasteryTier(player);

        if (after > before) {
            if (after >= 1) {
                grantAdvancement(player, ADV_MILESTONE_ONE);
            }
            if (after >= 2) {
                grantAdvancement(player, ADV_MILESTONE_TWO);
            }
            if (after >= 3) {
                grantAdvancement(player, ADV_MILESTONE_THREE);
            }
        }

        // Hard recovery path while locked: dangerous enemies can trigger memory returns.
        if (ResonanceManager.getHumanity(player) <= 0) {
            int restore = getHardRecoveryFromKill(victim, player);
            if (restore > 0) {
                ResonanceManager.addHumanity(player, restore);
                player.displayClientMessage(Component.translatable("message.naturalis.resonance.humanity_restore", restore), true);
            }
        }
    }

    public static void onBondSet(ServerPlayer player) {
        grantAdvancement(player, ADV_FIRST_BOND);
    }

    public static ActiveInstinctResult triggerActiveInstinct(ServerPlayer player) {
        if (ResonanceManager.getBondedMorph(player) == null) {
            return ActiveInstinctResult.NO_BOND;
        }
        if (!ResonanceManager.isResonanceEnabled(player)) {
            return ActiveInstinctResult.NOT_ACTIVE;
        }
        if (!ResonanceManager.isAligned(player)) {
            return ActiveInstinctResult.NOT_ALIGNED;
        }

        long now = player.level().getGameTime();
        if (!ResonanceManager.canUseActiveInstinct(player, now)) {
            return ActiveInstinctResult.COOLDOWN;
        }

        int humanity = ResonanceManager.getHumanity(player);
        int tier = ResonanceManager.getMasteryTier(player);
        applyActiveInstinct(player, ResonanceManager.getBondedArchetype(player), humanity, tier);
        ResonanceManager.markActiveInstinctUsed(player, now);
        applyHumanityActionLoss(player, 1);
        return ActiveInstinctResult.OK;
    }

    public static void applyHumanityActionLoss(ServerPlayer player, int amount) {
        ResonanceManager.applyHumanityActionLoss(player, amount);
    }

    @SubscribeEvent
    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }
        if (!event.getItem().getItem().isEdible()) {
            return;
        }
        // Human-only meal grounding: each meal contributes +0.2 humanity.
        if (CurrentMorphUtil.getCurrentMorphId(player) == null) {
            ResonanceManager.addHumanityRecoveryProgress(player, 2);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (isHumanToolLocked(player) && isToolLike(stack)) {
            grantAdvancement(player, ADV_TOOL_REJECTED);
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.tool_fumble"), true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (stack.is(NaturalisItems.HUMANITY_TOKEN_5.get()) || stack.is(NaturalisItems.HUMANITY_TOKEN_10.get())) {
            if (ResonanceManager.getHumanityStage(player) == ResonanceManager.HumanityStage.LOST) {
                player.displayClientMessage(Component.translatable("message.naturalis.resonance.humanity_token.blocked_lost"), true);
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }

            int amount = stack.is(NaturalisItems.HUMANITY_TOKEN_10.get()) ? 10 : 5;
            ResonanceManager.addHumanity(player, amount);
            grantAdvancement(player, amount >= 10 ? ADV_TOKEN_GREATER : ADV_TOKEN_MINOR);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.humanity_token", amount), true);

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (stack.is(NaturalisItems.MEMORY_TOKEN.get())) {
            if (!ResonanceManager.isResonanceEnabled(player) || !ResonanceManager.isAligned(player)) {
                return;
            }

            long now = player.level().getGameTime();
            ResonanceManager.openRecoveryWindow(player, now, 5 * 20, 5 * 20);
            ResonanceManager.addHumanity(player, 15);
            MorphEffectEvents.forceHuman(player);
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.memory_token"), true);

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!ResonanceManager.isResonanceEnabled(player) || !ResonanceManager.isAligned(player)) {
            return;
        }

        if (!stack.is(NaturalisItems.MORPH_ORB.get())) {
            return;
        }

        // Intentional ritual: crouch + use Echo Gem to reclaim humanity.
        if (!player.isCrouching()) {
            return;
        }

        ItemStack orbStack = new ItemStack(NaturalisItems.MORPH_ORB.get());
        if (!CompatAccess.isItemOnCooldown(player, orbStack)) {
            stack.shrink(1);
            CompatAccess.addItemCooldown(player, orbStack, 20 * 15);
            ResonanceManager.addHumanity(player, 8);
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.humanity_ritual", 8), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
        // Cooldown: do not cancel — allow normal morph-orb use (consume / acquire morph).
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer helper)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(NaturalisItems.REHUMANIZER.get())) {
            return;
        }

        if (ResonanceManager.getHumanity(helper) < 80) {
            helper.displayClientMessage(Component.translatable("message.naturalis.resonance.grounding_requirements"), true);
            return;
        }
        if (!ResonanceManager.isResonanceEnabled(target)) {
            return;
        }

        long now = target.level().getGameTime();
        ResonanceManager.openRecoveryWindow(target, now, 30 * 20, 30 * 20);
        ResonanceManager.addHumanity(target, 20);
        MorphEffectEvents.forceHuman(target);
        stack.shrink(1);

        helper.displayClientMessage(Component.translatable("message.naturalis.resonance.grounding_helper"), true);
        target.displayClientMessage(Component.translatable("message.naturalis.resonance.grounding_target"), true);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerDamaged(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }

        if (CurrentMorphUtil.getCurrentMorphId(player) == null) {
            return;
        }

        ResonanceManager.HumanityStage stage = ResonanceManager.getHumanityStage(player);
        if (stage.ordinal() < ResonanceManager.HumanityStage.SPLIT.ordinal()) {
            return;
        }

        // Split+: Wound fury on low health.
        if (player.getHealth() <= player.getMaxHealth() * 0.30F) {
            player.addEffect(new MobEffectInstance(SPEED_EFFECT, 8 * 20, 1, true, false, true));
            player.addEffect(new MobEffectInstance(STRENGTH_EFFECT, 8 * 20, 0, true, false, true));
        }

        // Feral survivor apex: prevent one-shot deaths while aligned in morph.
        ResonanceManager.ResonanceArchetype currentArchetype = ResonanceManager.getArchetype(CurrentMorphUtil.getCurrentMorphId(player));
        if (stage.ordinal() >= ResonanceManager.HumanityStage.FERAL.ordinal()
            && currentArchetype == ResonanceManager.ResonanceArchetype.SURVIVOR) {
            float incoming = event.getAmount();
            if (incoming >= player.getHealth()) {
                event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            BlockPos pos = event.getPos();
            BlockState state = player.level().getBlockState(pos);
            if (state.getBlock() instanceof BedBlock) {
                player.level().removeBlock(pos, false);
                player.level().explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5.0F, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }

        if (shouldToolFumble(player, event.getItemStack())) {
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.tool_fumble"), true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (shouldToolFumble(player, event.getItemStack())) {
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.tool_fumble"), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (shouldToolFumble(player, player.getMainHandItem())) {
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.tool_fumble"), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }
        if (CurrentMorphUtil.getCurrentMorphId(player) != null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!isToolLike(held)) {
            return;
        }

        if (isHumanToolLocked(player)) {
            event.setNewSpeed(0.0F);
            return;
        }

        float multiplier = getHumanToolEfficiencyMultiplier(player, held);
        if (multiplier < 1.0F) {
            event.setNewSpeed(Math.max(0.1F, event.getNewSpeed() * multiplier));
        }
    }

    private static double getBindingSurgeChance(int knowledgeLevel) {
        return switch (Math.max(0, Math.min(MorphKnowledgeManager.getMaxLevel(), knowledgeLevel))) {
            case 0, 1 -> 0.18D;
            case 2 -> 0.12D;
            case 3 -> 0.07D;
            case 4 -> 0.03D;
            default -> 0.01D;
        };
    }

    private static void applyArchetypeEffects(ServerPlayer player, ResonanceManager.ResonanceArchetype archetype) {
        int humanity = ResonanceManager.getHumanity(player);
        ResonanceManager.HumanityStage stage = ResonanceManager.getHumanityStage(humanity);
        if (stage == ResonanceManager.HumanityStage.GROUNDED) {
            return;
        }

        int tier = ResonanceManager.getMasteryTier(player);
        boolean primal = humanity <= 20;

        switch (archetype) {
            case PREDATOR -> {
                int amp = primal ? 1 : 0;
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 40, amp, true, false, true));
                if (tier >= 1) {
                    player.addEffect(new MobEffectInstance(STRENGTH_EFFECT, 40, 0, true, false, true));
                }
            }
            case SURVIVOR -> {
                int amp = primal ? 1 : 0;
                player.addEffect(new MobEffectInstance(RESISTANCE_EFFECT, 40, amp, true, false, true));
                if (tier >= 2 && player.getHealth() <= player.getMaxHealth() * 0.30F) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0, true, false, true));
                }
            }
            case AQUATIC -> {
                if (CompatAccess.isInWaterOrBubble(player)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true));
                } else {
                    player.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 40, 0, true, false, true));
                }
            }
            case OTHER -> {
                int amp = primal ? 1 : 0;
                player.addEffect(new MobEffectInstance(STRENGTH_EFFECT, 40, amp, true, false, true));
                if (tier >= 2) {
                    player.addEffect(new MobEffectInstance(SPEED_EFFECT, 40, 0, true, false, true));
                }
            }
        }
    }

    private static void applyMorphHumanityProgression(ServerPlayer player) {
        // Long-form bleed over long sessions.
        if (player.tickCount % 72000 == 0) {
            ResonanceManager.addHumanity(player, -3);
        }

        // Noticeable in normal play sessions too.
        if (player.tickCount % 12000 == 0) {
            ResonanceManager.addHumanity(player, -1);
        }

        int humanity = ResonanceManager.getHumanity(player);
        if (humanity < 50 && player.tickCount % 72000 == 0) {
            ResonanceManager.addHumanity(player, -1);
        }
    }

    private static void applyHumanityStateEffects(ServerPlayer player) {
        int humanity = ResonanceManager.getHumanity(player);
        ResonanceManager.HumanityStage stage = ResonanceManager.getHumanityStage(humanity);
        boolean morphed = CurrentMorphUtil.getCurrentMorphId(player) != null;
        CompoundTag tag = ResonanceManager.getOrCreateTag(player);

        if (!morphed) {
            if (stage == ResonanceManager.HumanityStage.SPLIT) {
                player.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 50, 0, true, false, true));
            }

            if (stage == ResonanceManager.HumanityStage.FERAL) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false, true));
                player.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 50, 0, true, false, true));
                enforceArmorRestrictions(player, false);
                maybePulseEffect(player, tag, LAST_BLINDNESS_TICK, 90 * 20, MobEffects.BLINDNESS, 3 * 20, 0);
            }

            if (stage == ResonanceManager.HumanityStage.PRIMAL || stage == ResonanceManager.HumanityStage.LOST) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 1, true, false, true));
                player.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 50, 1, true, false, true));
                enforceArmorRestrictions(player, true);
                maybePulseEffect(player, tag, LAST_NAUSEA_TICK, 120 * 20, NAUSEA_EFFECT, 8 * 20, 0);
            }

            return;
        }

        // Morph side progression mirrors the RP inversion stages.
        switch (stage) {
            case GROUNDED -> {
                // No forced penalties or boosts at 100-80 humanity.
            }
            case DRIFTING -> {
                // Neutral: no forced penalties or special boosts.
            }
            case SPLIT -> {
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 50, 0, true, false, true));
            }
            case FERAL -> {
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 50, 1, true, false, true));
                ResourceLocation morphNvId = CurrentMorphUtil.getCurrentMorphId(player);
                if (morphNvId == null || !EnvironmentalSusceptibilityManager.shouldSuppressMorphNightVision(player.level(), player.blockPosition(), morphNvId)) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true));
                }
                if (ResonanceManager.getBondedArchetype(player) == ResonanceManager.ResonanceArchetype.AQUATIC && CompatAccess.isInWaterOrBubble(player)) {
                    // Feral aquatic apex: keep full sprint-like movement underwater.
                    player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 50, 1, true, false, true));
                }
                if (ResonanceManager.getBondedArchetype(player) == ResonanceManager.ResonanceArchetype.OTHER) {
                    shortenNegativeEffects(player, 8);
                }
            }
            case PRIMAL, LOST -> {
                ResourceLocation morphNvId2 = CurrentMorphUtil.getCurrentMorphId(player);
                if (morphNvId2 == null || !EnvironmentalSusceptibilityManager.shouldSuppressMorphNightVision(player.level(), player.blockPosition(), morphNvId2)) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true));
                }
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 50, 1, true, false, true));
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(SLOWNESS_EFFECT);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(NAUSEA_EFFECT);
                player.removeEffect(MobEffects.DARKNESS);

                if (stage == ResonanceManager.HumanityStage.LOST) {
                    long now = player.level().getGameTime();
                    long lastRegen = CompatAccess.getLong(tag, LAST_REGEN_TICK);
                    if (now - lastRegen >= 80) {
                        tag.putLong(LAST_REGEN_TICK, now);
                        player.heal(1.0F);
                    }
                }
            }
        }
    }

    private static void handleSleepHumanity(ServerPlayer player) {
        var root = player.getPersistentData();
        if (!root.contains(ROOT_SLEEP_TAG)) {
            root.put(ROOT_SLEEP_TAG, new net.minecraft.nbt.CompoundTag());
        }
        net.minecraft.nbt.CompoundTag sleep = CompatAccess.getCompound(root, ROOT_SLEEP_TAG);

        boolean sleeping = player.isSleeping();
        boolean wasSleeping = CompatAccess.getBoolean(sleep, WAS_SLEEPING);
        if (sleeping && !wasSleeping) {
            if (ResonanceManager.isResonanceEnabled(player) && CurrentMorphUtil.getCurrentMorphId(player) == null) {
                ResonanceManager.HumanityStage stage = ResonanceManager.getHumanityStage(player);
                long now = player.level().getGameTime();
                boolean anchoredRecovery = ResonanceManager.isRecoveryWindowActive(player, now)
                    || ResonanceManager.isHumanFormLockActive(player, now);

                if (stage == ResonanceManager.HumanityStage.SPLIT && !anchoredRecovery && player.getRandom().nextBoolean()) {
                    // Split: sleep often fails as restless jolts.
                    sleep.putLong(LAST_RESTLESS_TICK, player.level().getGameTime());
                    player.stopSleeping();
                    player.displayClientMessage(Component.translatable("message.naturalis.resonance.restless_sleep"), true);
                    return;
                }

                if ((stage == ResonanceManager.HumanityStage.PRIMAL || stage == ResonanceManager.HumanityStage.LOST)
                    && !anchoredRecovery) {
                    // Primal/Lost: human sleep is rejected.
                    sleep.putLong(LAST_RESTLESS_TICK, player.level().getGameTime());
                    player.stopSleeping();
                    player.displayClientMessage(Component.translatable("message.naturalis.resonance.sleep_blocked"), true);
                    return;
                }
            } else if (ResonanceManager.getHumanity(player) <= 20) {
                grantAdvancement(player, ADV_MORPH_SLEEPLESS);
                sleep.putLong(LAST_RESTLESS_TICK, player.level().getGameTime());
                player.stopSleeping();
                player.displayClientMessage(Component.translatable("message.naturalis.resonance.sleep_blocked_morph"), true);
                return;
            }

            sleep.putBoolean(WAS_SLEEPING, true);
            sleep.putBoolean(SLEPT_MORPHED, CurrentMorphUtil.getCurrentMorphId(player) != null);
            sleep.putBoolean(SLEPT_CONSECRATED, isSleepingOnConsecratedBed(player));
            return;
        }

        if (!sleeping && wasSleeping) {
            boolean sleptMorphed = CompatAccess.getBoolean(sleep, SLEPT_MORPHED);
            boolean sleptConsecrated = CompatAccess.getBoolean(sleep, SLEPT_CONSECRATED);
            sleep.putBoolean(WAS_SLEEPING, false);
            sleep.putBoolean(SLEPT_MORPHED, false);
            sleep.putBoolean(SLEPT_CONSECRATED, false);

            if (!ResonanceManager.isResonanceEnabled(player)) {
                return;
            }

            if (sleptMorphed) {
                applyHumanityActionLoss(player, 5);
                if (ResonanceManager.getHumanityStage(player).ordinal() >= ResonanceManager.HumanityStage.SPLIT.ordinal()) {
                    // Split+: sleeping as morph fully resets your body.
                    player.setHealth(player.getMaxHealth());
                    clearNegativeEffects(player);
                }
            } else {
                ResonanceManager.addHumanity(player, 5);

                if (sleptConsecrated) {
                    long now = player.level().getGameTime();
                    if (ResonanceManager.isRecoveryWindowActive(player, now)
                        || ResonanceManager.isHumanFormLockActive(player, now)) {
                        ResonanceManager.addHumanity(player, 25);
                        ResonanceManager.extendHumanFormLock(player, now, 2 * 60 * 20);
                        player.displayClientMessage(Component.translatable("message.naturalis.resonance.consecrated_rest"), true);
                    }
                }
            }
        }
    }

    private static boolean isSleepingOnConsecratedBed(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        var sleepingPosOpt = player.getSleepingPos();
        if (sleepingPosOpt.isEmpty()) {
            return false;
        }

        BlockPos pos = sleepingPosOpt.get();
        return level.getBlockState(pos).is(CONSECRATED_BEDS);
    }

    private static int getHardRecoveryFromKill(LivingEntity victim, ServerPlayer player) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id == null) {
            return 0;
        }

        String path = id.getPath();
        if ("ender_dragon".equals(path) || "wither".equals(path)) {
            return 40;
        }
        if ("warden".equals(path) || "evoker".equals(path) || "elder_guardian".equals(path)) {
            return 12;
        }
        if (victim instanceof Enemy && victim.getMaxHealth() >= 40.0F) {
            return 4;
        }
        if (victim instanceof Enemy && player.getRandom().nextDouble() < 0.005D) {
            return 1;
        }
        return 0;
    }

    private static void grantHumanityAdvancements(ServerPlayer player) {
        int humanity = ResonanceManager.getHumanity(player);
        if (humanity <= 79) {
            grantAdvancement(player, ADV_HUMANITY_DRIFTING);
        }
        if (humanity <= 59) {
            grantAdvancement(player, ADV_HUMANITY_SPLIT);
        }
        if (humanity <= 39) {
            grantAdvancement(player, ADV_HUMANITY_FERAL);
        }
        if (humanity <= 19) {
            grantAdvancement(player, ADV_HUMANITY_PRIMAL);
        }
        if (humanity <= 0) {
            grantAdvancement(player, ADV_HUMANITY_LOST);
        }
    }

    private static void handleHumanityNarrativeTransition(
        ServerPlayer player,
        ResonanceManager.HumanityStage before,
        ResonanceManager.HumanityStage after
    ) {
        CompoundTag tag = ResonanceManager.getOrCreateTag(player);
        int rememberedOrdinal = CompatAccess.getInt(tag, LAST_HUMANITY_STAGE_ANNOUNCED);
        int normalizedRemembered = Math.max(0, Math.min(ResonanceManager.HumanityStage.values().length - 1, rememberedOrdinal));

        // First observation in a session: initialize without sending retroactive text.
        if (!tag.contains(LAST_HUMANITY_STAGE_ANNOUNCED)) {
            tag.putInt(LAST_HUMANITY_STAGE_ANNOUNCED, after.ordinal());
            return;
        }

        // Respect explicit stage transition when available, otherwise recover from remembered state.
        ResonanceManager.HumanityStage effectiveBefore = before != after
            ? before
            : ResonanceManager.HumanityStage.values()[normalizedRemembered];

        if (after.ordinal() >= effectiveBefore.ordinal()) {
            if (effectiveBefore.ordinal() < ResonanceManager.HumanityStage.FERAL.ordinal()
                && after.ordinal() >= ResonanceManager.HumanityStage.FERAL.ordinal()) {
                player.sendSystemMessage(Component.translatable("message.naturalis.resonance.lore.feral"));
            }

            if (effectiveBefore.ordinal() < ResonanceManager.HumanityStage.PRIMAL.ordinal()
                && after.ordinal() >= ResonanceManager.HumanityStage.PRIMAL.ordinal()) {
                player.sendSystemMessage(Component.translatable("message.naturalis.resonance.lore.primal"));
            }

            if (effectiveBefore.ordinal() < ResonanceManager.HumanityStage.LOST.ordinal()
                && after == ResonanceManager.HumanityStage.LOST) {
                // Explicit RP callout at the exact moment humanity falls to zero.
                player.sendSystemMessage(Component.translatable("message.naturalis.resonance.lore.lost"));
                player.displayClientMessage(Component.translatable("message.naturalis.resonance.lore.lost.overlay"), true);
                tag.putLong(LOST_PROMPT_UNTIL_TICK, player.level().getGameTime() + (10L * 20L));
            }
        }

        tag.putInt(LAST_HUMANITY_STAGE_ANNOUNCED, after.ordinal());
    }

    private static void applyStageRewards(ServerPlayer player, boolean aligned, boolean morphed) {
        if (!aligned || !morphed) {
            return;
        }

        ResonanceManager.HumanityStage stage = ResonanceManager.getHumanityStage(player);
        CompoundTag tag = ResonanceManager.getOrCreateTag(player);
        long now = player.level().getGameTime();

        if (stage.ordinal() >= ResonanceManager.HumanityStage.DRIFTING.ordinal()) {
            applyThreatSense(player, tag, now);
        }

        if (stage.ordinal() >= ResonanceManager.HumanityStage.SPLIT.ordinal()) {
            applyPackReading(player);
        }

        if (stage.ordinal() >= ResonanceManager.HumanityStage.FERAL.ordinal()) {
            applyTerritorialMap(player, tag, now);
            applyPreyRecognition(player);
            applyArchetypeApexPassives(player);
        }

        if (stage.ordinal() >= ResonanceManager.HumanityStage.PRIMAL.ordinal()) {
            applyPrimalAura(player, tag, now);
        }
    }

    private static void applyThreatSense(ServerPlayer player, CompoundTag tag, long now) {
        List<LivingEntity> hostiles = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(32.0D),
            entity -> entity.isAlive() && entity instanceof Enemy && entity instanceof Mob mob && mob.getTarget() == player
        );
        if (hostiles.isEmpty()) {
            return;
        }

        long last = CompatAccess.getLong(tag, LAST_THREAT_SENSE_TICK);
        if (now - last < 40) {
            return;
        }
        tag.putLong(LAST_THREAT_SENSE_TICK, now);

        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.05F, 0.40F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.15D, 0.1D, 0.15D, 0.005D);
        }
    }

    private static void applyPackReading(ServerPlayer player) {
        CompoundTag tag = ResonanceManager.getOrCreateTag(player);
        long now = player.level().getGameTime();
        long last = CompatAccess.getLong(tag, LAST_PACK_READING_TICK);
        if (now - last < 80) {
            return;
        }
        tag.putLong(LAST_PACK_READING_TICK, now);

        ResonanceManager.ResonanceArchetype archetype = ResonanceManager.getBondedArchetype(player);

        if (archetype == ResonanceManager.ResonanceArchetype.PREDATOR) {
            List<Mob> wolves = player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(18.0D),
                wolf -> wolf.isAlive()
                    && wolf.getType() == EntityType.WOLF
                    && (
                    wolf.getTarget() == player
                        || wolf.getLastHurtByMob() == player
                        || (wolf instanceof NeutralMob neutralMob && player.getUUID().equals(neutralMob.getPersistentAngerTarget()))
                )
            );
            for (Mob wolf : wolves) {
                wolf.setTarget(null);
                wolf.setLastHurtByMob(null);
                if (wolf instanceof NeutralMob neutralMob) {
                    neutralMob.setPersistentAngerTarget(null);
                    neutralMob.setRemainingPersistentAngerTime(0);
                }
            }
        }

        if (archetype == ResonanceManager.ResonanceArchetype.AQUATIC) {
            List<Dolphin> dolphins = player.level().getEntitiesOfClass(
                Dolphin.class,
                player.getBoundingBox().inflate(18.0D),
                dolphin -> dolphin.isAlive() && dolphin.getTarget() == player
            );
            for (Dolphin dolphin : dolphins) {
                dolphin.setTarget(null);
                dolphin.setLastHurtByMob(null);
            }
        }
    }

    private static void applyTerritorialMap(ServerPlayer player, CompoundTag tag, long now) {
        long last = CompatAccess.getLong(tag, LAST_TERRITORIAL_PING_TICK);
        if (now - last < 20) {
            return;
        }
        tag.putLong(LAST_TERRITORIAL_PING_TICK, now);

        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(24.0D),
            entity -> entity.isAlive() && entity != player
        );
        if (nearby.isEmpty()) {
            return;
        }

        LivingEntity nearest = nearby.stream()
            .min((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b)))
            .orElse(null);
        if (nearest == null) {
            return;
        }

        if (player.level() instanceof ServerLevel level) {
            double distance = Math.sqrt(player.distanceToSqr(nearest));
            float volume = (float) Math.max(0.12D, 0.28D - (distance / 160.0D));
            float pitch = (float) Math.max(0.45D, 0.62D - (distance / 90.0D));
            level.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, volume, pitch);
        }
    }

    private static void applyPreyRecognition(ServerPlayer player) {
        if (ResonanceManager.getBondedArchetype(player) != ResonanceManager.ResonanceArchetype.PREDATOR) {
            return;
        }

        CompoundTag tag = ResonanceManager.getOrCreateTag(player);
        long now = player.level().getGameTime();
        long last = CompatAccess.getLong(tag, LAST_PREY_RECOGNITION_TICK);
        if (now - last < 50) {
            return;
        }
        tag.putLong(LAST_PREY_RECOGNITION_TICK, now);

        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(24.0D),
            entity -> entity.isAlive()
                && entity != player
                && !(entity instanceof Player)
                && entity.getMaxHealth() < 100.0F
                && entity.getHealth() <= entity.getMaxHealth() * 0.50F
        );

        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false, true));
        }
    }

    private static void applyArchetypeApexPassives(ServerPlayer player) {
        ResonanceManager.ResonanceArchetype archetype = ResonanceManager.getBondedArchetype(player);
        if (archetype == ResonanceManager.ResonanceArchetype.AQUATIC && CompatAccess.isInWaterOrBubble(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 2, true, false, true));
        }
        if (archetype == ResonanceManager.ResonanceArchetype.OTHER) {
            shortenNegativeEffects(player, 8);
        }
    }

    private static void applyPrimalAura(ServerPlayer player, CompoundTag tag, long now) {
        long last = CompatAccess.getLong(tag, LAST_APEX_AURA_TICK);
        if (now - last < 10) {
            return;
        }
        tag.putLong(LAST_APEX_AURA_TICK, now);

        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(18.0D),
            entity -> entity.isAlive() && entity instanceof Enemy && entity.getMaxHealth() <= 40.0F
        );

        for (LivingEntity enemy : nearby) {
            if (player.getRandom().nextFloat() < 0.60F) {
                if (enemy instanceof Mob mobEnemy) {
                    mobEnemy.setTarget(null);
                    mobEnemy.setLastHurtByMob(null);
                }
                enemy.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 30, 1, true, false, true));
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
            }
        }
    }

    private static void tryApplyLastRite(ServerPlayer player, LivingEntity victim) {
        if (ResonanceManager.getHumanityStage(player).ordinal() < ResonanceManager.HumanityStage.PRIMAL.ordinal()) {
            return;
        }

        CompoundTag tag = ResonanceManager.getOrCreateTag(player);
        String target = CompatAccess.getString(tag, LAST_RITE_TARGET);
        if (!victim.getStringUUID().equals(target)) {
            return;
        }

        long now = player.level().getGameTime();
        long engageTick = CompatAccess.getLong(tag, LAST_RITE_ENGAGE_TICK);
        boolean fullHit = CompatAccess.getBoolean(tag, LAST_RITE_FULL_HEALTH_HIT);

        if (fullHit || (now - engageTick) <= 30L) {
            player.heal(4.0F);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + 0.8D, victim.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.01D);
            }
        }
    }

    private static void applyActiveInstinct(ServerPlayer player, ResonanceManager.ResonanceArchetype archetype, int humanity, int tier) {
        int stage = humanityToStage(humanity);
        double morphScalar = getMorphCharacteristicScalar(player);

        if (tryApplyMorphSpecificActiveInstinct(player, stage, tier, morphScalar)) {
            return;
        }

        switch (archetype) {
            case PREDATOR -> applyPredatorInstinct(player, stage, tier, morphScalar);
            case SURVIVOR -> applySurvivorInstinct(player, stage, tier, morphScalar);
            case AQUATIC -> applyAquaticInstinct(player, stage, tier, morphScalar);
            case OTHER -> applyOtherInstinct(player, stage, tier, morphScalar);
        }
    }

    private static boolean tryApplyMorphSpecificActiveInstinct(ServerPlayer player, int stage, int tier, double morphScalar) {
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return false;
        }

        String path = morphId.getPath();
        var look = player.getLookAngle().normalize();

        switch (path) {
            case "wolf", "fox", "cat", "ocelot" -> {
                double pounce = (1.05D + stage * 0.18D + tier * 0.08D) * morphScalar;
                player.push(look.x * pounce, 0.30D + stage * 0.03D, look.z * pounce);
                player.fallDistance = 0.0F;
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 80 + stage * 20, 0, true, false, true));
                return true;
            }
            case "spider", "cave_spider" -> {
                List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(4.0D + stage),
                    entity -> entity != player && entity.isAlive() && entity instanceof Enemy
                );
                for (LivingEntity target : nearby) {
                    target.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 120 + stage * 20, 1, true, false, true));
                }
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 60 + stage * 20, 1, true, false, true));
                return true;
            }
            case "horse", "skeleton_horse", "zombie_horse", "camel", "llama" -> {
                double charge = (1.25D + stage * 0.20D + tier * 0.08D) * morphScalar;
                player.push(look.x * charge, 0.12D, look.z * charge);
                List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(2.8D),
                    entity -> entity != player && entity.isAlive() && entity instanceof Enemy
                );
                for (LivingEntity target : nearby) {
                    double dx = target.getX() - player.getX();
                    double dz = target.getZ() - player.getZ();
                    target.knockback(0.65D + (0.08D * stage), dx, dz);
                }
                return true;
            }
            case "rabbit", "goat" -> {
                double jumpBoost = 0.65D + stage * 0.08D;
                player.push(look.x * 0.30D, jumpBoost, look.z * 0.30D);
                player.fallDistance = 0.0F;
                player.addEffect(new MobEffectInstance(JUMP_BOOST_EFFECT, 80 + stage * 20, 1, true, false, true));
                return true;
            }
            case "dolphin", "cod", "salmon", "tropical_fish", "pufferfish", "axolotl" -> {
                if (CompatAccess.isInWaterOrBubble(player)) {
                    double surge = (1.35D + stage * 0.22D) * morphScalar;
                    player.push(look.x * surge, 0.08D, look.z * surge);
                    player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 120 + stage * 20, 1, true, false, true));
                } else {
                    player.push(look.x * 0.40D, 0.34D, look.z * 0.40D);
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
                }
                return true;
            }
            case "bat", "parrot", "phantom", "vex", "allay", "blaze", "ghast" -> {
                player.push(look.x * 0.50D, 0.46D + stage * 0.05D, look.z * 0.50D);
                player.fallDistance = 0.0F;
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100 + stage * 20, 0, true, false, true));
                player.addEffect(new MobEffectInstance(SPEED_EFFECT, 70 + stage * 20, 0, true, false, true));
                return true;
            }
            default -> {
                if (matchesAny(path, "bird", "crow", "raven", "eagle", "hawk", "falcon", "owl", "vulture", "gull", "duck", "goose", "swan")) {
                    player.push(look.x * 0.52D, 0.44D + stage * 0.05D, look.z * 0.52D);
                    player.fallDistance = 0.0F;
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100 + stage * 20, 0, true, false, true));
                    player.addEffect(new MobEffectInstance(SPEED_EFFECT, 70 + stage * 20, 0, true, false, true));
                    return true;
                }

                if (matchesAny(path, "shark", "ray", "whale", "dolphin", "eel", "fish", "squid", "octopus", "jelly")) {
                    if (CompatAccess.isInWaterOrBubble(player)) {
                        double surge = (1.35D + stage * 0.20D) * morphScalar;
                        player.push(look.x * surge, 0.08D, look.z * surge);
                        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 110 + stage * 20, 1, true, false, true));
                    } else {
                        player.push(look.x * 0.38D, 0.30D, look.z * 0.38D);
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, true, false, true));
                    }
                    return true;
                }

                if (matchesAny(path, "deer", "elk", "moose", "bison", "buffalo", "boar", "bull", "yak", "ram", "antelope", "gazelle")) {
                    double charge = (1.20D + stage * 0.18D + tier * 0.08D) * morphScalar;
                    player.push(look.x * charge, 0.10D, look.z * charge);
                    List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0D),
                        entity -> entity != player && entity.isAlive() && entity instanceof Enemy
                    );
                    for (LivingEntity target : nearby) {
                        double dx = target.getX() - player.getX();
                        double dz = target.getZ() - player.getZ();
                        target.knockback(0.75D + (0.08D * stage), dx, dz);
                    }
                    return true;
                }

                if (matchesAny(path, "snake", "serpent", "lizard", "gecko", "iguana", "scorpion", "spider")) {
                    List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(4.0D + stage),
                        entity -> entity != player && entity.isAlive() && entity instanceof Enemy
                    );
                    for (LivingEntity target : nearby) {
                        target.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 100 + stage * 20, 1, true, false, true));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80 + stage * 20, 0, true, false, true));
                    }
                    player.addEffect(new MobEffectInstance(SPEED_EFFECT, 60 + stage * 20, 0, true, false, true));
                    return true;
                }

                return false;
            }
        }
    }

    private static boolean matchesAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static void applyPredatorInstinct(ServerPlayer player, int stage, int tier, double morphScalar) {
        var look = player.getLookAngle().normalize();
        double horizontal = (0.95D + (stage * 0.15D) + (tier * 0.08D)) * morphScalar;
        double vertical = 0.24D + (stage * 0.03D);

        player.push(look.x * horizontal, vertical, look.z * horizontal);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(SPEED_EFFECT, 60 + stage * 20, 0, true, false, true));
        player.addEffect(new MobEffectInstance(STRENGTH_EFFECT, 60 + stage * 20, stage >= 3 ? 1 : 0, true, false, true));
    }

    private static void applySurvivorInstinct(ServerPlayer player, int stage, int tier, double morphScalar) {
        player.clearFire();
        player.addEffect(new MobEffectInstance(RESISTANCE_EFFECT, 140 + stage * 40, stage >= 3 ? 1 : 0, true, false, true));
        int absorbAmp = Math.min(3, stage + (tier >= 2 ? 1 : 0) + (morphScalar > 1.06D ? 1 : 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120 + stage * 20, absorbAmp, true, false, true));
        if (stage >= 2) {
            player.removeEffect(SLOWNESS_EFFECT);
            player.removeEffect(MobEffects.WEAKNESS);
        }
    }

    private static void applyAquaticInstinct(ServerPlayer player, int stage, int tier, double morphScalar) {
        var look = player.getLookAngle().normalize();
        if (CompatAccess.isInWaterOrBubble(player)) {
            double waterDash = (1.15D + stage * 0.18D) * morphScalar;
            player.push(look.x * waterDash, 0.10D + (stage * 0.03D), look.z * waterDash);
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100 + stage * 30, tier >= 2 ? 1 : 0, true, false, true));
        } else {
            double landDash = (0.45D + stage * 0.08D) * morphScalar;
            player.push(look.x * landDash, 0.32D + (stage * 0.03D), look.z * landDash);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50 + stage * 10, 0, true, false, true));
        }
    }

    private static void applyOtherInstinct(ServerPlayer player, int stage, int tier, double morphScalar) {
        double radius = (5.0D + (stage * 1.2D)) * morphScalar;
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(radius),
            entity -> entity != player && entity.isAlive() && entity instanceof Enemy
        );

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100 + stage * 30, 0, true, false, true));
            target.addEffect(new MobEffectInstance(SLOWNESS_EFFECT, 80 + stage * 20, 0, true, false, true));

            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            target.knockback(0.20D + (0.06D * stage), dx, dz);

            if (tier >= 3) {
                target.hurt(player.damageSources().mobAttack(player), 1.5F + stage);
            }
        }
    }

    private static double getMorphCharacteristicScalar(ServerPlayer player) {
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return 1.0D;
        }

        double mass = MetabolismManager.getMass(morphId);
        int smell = InstinctManager.getSmellStrength(morphId);
        double massScalar = Math.max(0.90D, Math.min(1.12D, 0.92D + (mass * 0.02D)));
        double smellScalar = 1.0D + (smell * 0.03D);
        return Math.max(0.90D, Math.min(1.20D, (massScalar * 0.65D) + (smellScalar * 0.35D)));
    }

    private static void maybePulseEffect(ServerPlayer player, CompoundTag tag, String lastTickKey, int intervalTicks,
                                         net.minecraft.world.effect.MobEffect effect,
                                         int durationTicks, int amplifier) {
        long now = player.level().getGameTime();
        long last = CompatAccess.getLong(tag, lastTickKey);
        if (now - last < intervalTicks) {
            return;
        }
        tag.putLong(lastTickKey, now);
        player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, true, false, true));
    }

    private static void enforceArmorRestrictions(ServerPlayer player, boolean allSlots) {
        if (allSlots) {
            moveArmorToInventory(player, EquipmentSlot.HEAD);
            moveArmorToInventory(player, EquipmentSlot.CHEST);
            moveArmorToInventory(player, EquipmentSlot.LEGS);
            moveArmorToInventory(player, EquipmentSlot.FEET);
        } else {
            moveArmorToInventory(player, EquipmentSlot.CHEST);
        }
    }

    private static void moveArmorToInventory(ServerPlayer player, EquipmentSlot slot) {
        ItemStack equipped = player.getItemBySlot(slot);
        if (equipped.isEmpty()) {
            return;
        }

        ItemStack copy = equipped.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    private static boolean shouldToolFumble(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isHumanToolLocked(player) && isToolLike(stack)) {
            grantAdvancement(player, ADV_TOOL_REJECTED);
            return true;
        }

        if (!ResonanceManager.isResonanceEnabled(player)) {
            return false;
        }

        if (CurrentMorphUtil.getCurrentMorphId(player) != null) {
            return false;
        }

        if (!isToolLike(stack)) {
            return false;
        }

        int humanity = ResonanceManager.getHumanity(player);
        if (humanity > 60) {
            return false;
        }

        // Human tool handling degrades as humanity drops.
        float chance;
        if (humanity <= 20) {
            chance = 1.0F;
        } else if (humanity <= 40) {
            chance = 0.55F;
        } else {
            chance = 0.30F;
        }
        return player.getRandom().nextFloat() < chance;
    }

    private static boolean isHumanToolLocked(ServerPlayer player) {
        if (!ResonanceManager.isResonanceEnabled(player)) {
            return false;
        }
        if (CurrentMorphUtil.getCurrentMorphId(player) != null) {
            return false;
        }
        return ResonanceManager.getHumanity(player) <= 20;
    }

    private static void applyHumanToolDamagePenalty(ServerPlayer player, LivingHurtEvent event) {
        ItemStack held = player.getMainHandItem();
        if (!isToolLike(held)) {
            return;
        }

        if (isHumanToolLocked(player)) {
            event.setAmount(1.0F);
            return;
        }

        float multiplier = getHumanToolEfficiencyMultiplier(player, held);
        if (multiplier < 1.0F) {
            event.setAmount(Math.max(1.0F, event.getAmount() * multiplier));
        }
    }

    private static float getHumanToolEfficiencyMultiplier(ServerPlayer player, ItemStack stack) {
        if (CurrentMorphUtil.getCurrentMorphId(player) != null) {
            return 1.0F;
        }

        int humanity = ResonanceManager.getHumanity(player);
        if (humanity > 60) {
            return 1.0F;
        }

        grantAdvancement(player, ADV_TOOL_DECAY);

        if (humanity <= 20) {
            return 0.0F;
        }

        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return 0.68F;
        }

        String path = itemId.getPath();
        if (path.contains("netherite")) {
            return 0.88F;
        }
        if (path.contains("diamond")) {
            return 0.75F;
        }
        if (path.contains("iron")) {
            return 0.67F;
        }
        if (path.contains("stone")) {
            return 0.50F;
        }
        if (path.contains("gold") || path.contains("wood")) {
            return 0.35F;
        }
        return 0.68F;
    }

    private static void shortenNegativeEffects(ServerPlayer player, int extraTicksPerSecond) {
        if (player.tickCount % 20 != 0) {
            return;
        }

        List<MobEffectInstance> active = List.copyOf(player.getActiveEffects());
        for (MobEffectInstance instance : active) {
            if (!instance.getEffect().isBeneficial()) {
                int reduced = Math.max(1, instance.getDuration() - extraTicksPerSecond);
                player.addEffect(new MobEffectInstance(instance.getEffect(), reduced, instance.getAmplifier(), true, false, true));
            }
        }
    }

    private static void clearNegativeEffects(ServerPlayer player) {
        List<MobEffectInstance> active = List.copyOf(player.getActiveEffects());
        for (MobEffectInstance instance : active) {
            if (!instance.getEffect().isBeneficial()) {
                player.removeEffect(instance.getEffect());
            }
        }
    }

    private static boolean isToolLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        var item = stack.getItem();
        String className = item.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if (className.contains("tiered")
            || className.contains("digger")
            || item instanceof ShearsItem
            || item instanceof BowItem
            || item instanceof CrossbowItem
            || item instanceof TridentItem) {
            return true;
        }

        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return false;
        }

        String path = itemId.getPath();
        return path.contains("pickaxe")
            || path.contains("axe")
            || path.contains("shovel")
            || path.contains("hoe")
            || path.contains("sword")
            || path.contains("shears")
            || path.contains("bow")
            || path.contains("crossbow")
            || path.contains("trident");
    }

    private static int humanityToStage(int humanity) {
        if (humanity >= 80) {
            return 0;
        }
        if (humanity >= 60) {
            return 1;
        }
        if (humanity >= 40) {
            return 2;
        }
        if (humanity >= 20) {
            return 3;
        }
        return 4;
    }

    public enum ActiveInstinctResult {
        OK,
        NO_BOND,
        NOT_ACTIVE,
        NOT_ALIGNED,
        COOLDOWN
    }

    public enum RebirthResult {
        OK,
        NOT_LOST,
        NOT_MORPHED,
        SPAWN_FAILED,
        FORBIDDEN_IN_NATURAL
    }

    public static RebirthResult triggerHumanRebirth(ServerPlayer player) {
        if (player.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return RebirthResult.FORBIDDEN_IN_NATURAL;
        }

        if (ResonanceManager.getHumanityStage(player) != ResonanceManager.HumanityStage.LOST) {
            return RebirthResult.NOT_LOST;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return RebirthResult.NOT_MORPHED;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return RebirthResult.SPAWN_FAILED;
        }

        LivingEntity rebornBody = spawnRebirthBody(level, player, morphId);
        if (rebornBody == null) {
            return RebirthResult.SPAWN_FAILED;
        }

        BlockPos oldBodyPos = rebornBody.blockPosition();

        if (!stashPlayerInventoryIntoBody(player, rebornBody, level)) {
            rebornBody.discard();
            return RebirthResult.SPAWN_FAILED;
        }

        ResonanceManager.setHumanity(player, ResonanceManager.MAX_HUMANITY);
        ResonanceManager.openRecoveryWindow(player, level.getGameTime(), 5 * 20, 10 * 20);
        MorphEffectEvents.forceHuman(player);

        BlockPos respawnPos = findRebirthRespawnPos(level, oldBodyPos, 50, player);
        player.teleportTo(respawnPos.getX() + 0.5D, respawnPos.getY() + 0.1D, respawnPos.getZ() + 0.5D);

        player.sendSystemMessage(Component.translatable("message.naturalis.resonance.rebirth.done", morphId.toString()));
        grantAdvancement(player, ADV_REBIRTH_ACCEPTED);
        return RebirthResult.OK;
    }

    public static boolean tryTriggerRebirthFromCurlKey(ServerPlayer player) {
        if (ResonanceManager.getHumanityStage(player) != ResonanceManager.HumanityStage.LOST) {
            return false;
        }

        RebirthResult result = triggerHumanRebirth(player);
        if (result == RebirthResult.OK) {
            player.displayClientMessage(Component.translatable("command.naturalis.resonance.rebirth.success"), true);
            return true;
        }

        if (result == RebirthResult.NOT_MORPHED) {
            player.displayClientMessage(Component.translatable("command.naturalis.resonance.rebirth.not_morphed"), true);
            return true;
        }

        if (result == RebirthResult.SPAWN_FAILED) {
            player.displayClientMessage(Component.translatable("command.naturalis.resonance.rebirth.spawn_failed"), true);
            return true;
        }

        if (result == RebirthResult.FORBIDDEN_IN_NATURAL) {
            player.displayClientMessage(Component.translatable("message.naturalis.natural_dimension.rebirth_blocked"), true);
            return true;
        }

        return false;
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            MorphEffectEvents.forceHuman(player);
        }
    }

    private static LivingEntity spawnRebirthBody(ServerLevel level, ServerPlayer player, ResourceLocation morphId) {
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return null;
        }

        var created = CompatAccess.createEntity(type, level);
        if (!(created instanceof LivingEntity body)) {
            return null;
        }

        LivingEntity currentShape = getCurrentShapeCompat(player);
        copyKnownVariantData(currentShape, body);

        CompatAccess.moveEntity(body, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        body.setDeltaMovement(0.0D, 0.0D, 0.0D);
        body.setYHeadRot(player.getYHeadRot());
        body.setYBodyRot(player.getYRot());
        body.setHealth(body.getMaxHealth());

        if (body instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
        }

        if (!level.addFreshEntity(body)) {
            return null;
        }

        return body;
    }

    private static LivingEntity getCurrentShapeCompat(ServerPlayer player) {
        String[] classNames = new String[]{
            "tocraft.walkers.api.PlayerShape",
            "dev.tocraft.walkers.api.PlayerShape"
        };

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Object raw = clazz.getMethod("getCurrentShape", Player.class).invoke(null, player);
                if (raw instanceof LivingEntity living) {
                    return living;
                }
            } catch (Throwable ignored) {
                // Try next API namespace.
            }
        }

        return null;
    }

    private static void copyKnownVariantData(LivingEntity from, LivingEntity to) {
        if (from == null || to == null || from.getType() != to.getType()) {
            return;
        }

        try {
            Object variant = from.getClass().getMethod("getVariant").invoke(from);
            if (variant == null) {
                return;
            }

            java.lang.reflect.Method setter = null;
            for (java.lang.reflect.Method method : to.getClass().getMethods()) {
                if ("setVariant".equals(method.getName()) && method.getParameterCount() == 1) {
                    setter = method;
                    break;
                }
            }

            if (setter == null) {
                return;
            }

            setter.setAccessible(true);
            setter.invoke(to, variant);
        } catch (Throwable ignored) {
            // Variant API differs by version or entity type; keep spawned defaults.
        }
    }

    private static boolean stashPlayerInventoryIntoBody(ServerPlayer player, LivingEntity body, ServerLevel level) {
        int slots = player.getInventory().getContainerSize() + 1;
        ItemStackHandler stash = new ItemStackHandler(slots);
        boolean hadAnyItems = false;

        int inventorySlots = player.getInventory().getContainerSize();
        for (int i = 0; i < inventorySlots; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            hadAnyItems = true;
            stash.setStackInSlot(i, stack.copy());
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            hadAnyItems = true;
            stash.setStackInSlot(inventorySlots, carried.copy());
        }

        try {
            CompoundTag lootTag = CompatAccess.serializeItemHandler(stash, level.registryAccess());
            if (hadAnyItems && lootTag.isEmpty()) {
                // Serialization failed silently on this runtime; keep inventory instead of losing it.
                return true;
            }

            body.getPersistentData().put(REBIRTH_LOOT_TAG, lootTag);
            body.getPersistentData().putInt(REBIRTH_LOOT_SLOTS_TAG, slots);
            body.getPersistentData().putString(REBIRTH_OWNER_TAG, player.getStringUUID());

            ResourceLocation currentMorph = CurrentMorphUtil.getCurrentMorphId(player);
            if (currentMorph != null) {
                body.getPersistentData().putString(REBIRTH_MORPH_TAG, currentMorph.toString());
            }

            for (int i = 0; i < inventorySlots; i++) {
                if (!player.getInventory().getItem(i).isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
            if (!carried.isEmpty()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }

            player.inventoryMenu.broadcastChanges();
            return true;
        } catch (Throwable ignored) {
            // Keep inventory intact instead of cancelling rebirth on serializer differences.
            return true;
        }
    }

    private static void restorePlayerInventoryFromStash(ServerPlayer player, ItemStackHandler stash) {
        int limit = Math.min(player.getInventory().getContainerSize(), stash.getSlots());
        for (int i = 0; i < limit; i++) {
            ItemStack stack = stash.getStackInSlot(i);
            if (!stack.isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
            }
        }
        player.inventoryMenu.broadcastChanges();
    }

    private static void releaseRebirthLoot(LivingEntity victim, ServerLevel level) {
        CompoundTag data = victim.getPersistentData();
        if (!data.contains(REBIRTH_LOOT_TAG)) {
            return;
        }

        CompoundTag lootTag = CompatAccess.getCompound(data, REBIRTH_LOOT_TAG);
        int slots = Math.max(1, CompatAccess.getInt(data, REBIRTH_LOOT_SLOTS_TAG));
        ItemStackHandler stash = new ItemStackHandler(slots);
        CompatAccess.deserializeItemHandler(stash, level.registryAccess(), lootTag);

        for (int i = 0; i < stash.getSlots(); i++) {
            ItemStack stack = stash.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompatAccess.spawnEntityItemDrop(victim, level, stack.copy());
            }
        }

        data.remove(REBIRTH_LOOT_TAG);
        data.remove(REBIRTH_LOOT_SLOTS_TAG);
        data.remove(REBIRTH_OWNER_TAG);
        data.remove(REBIRTH_MORPH_TAG);
    }

    private static void tickLostRebirthPrompt(ServerPlayer player) {
        CompoundTag tag = ResonanceManager.getOrCreateTag(player);
        long until = CompatAccess.getLong(tag, LOST_PROMPT_UNTIL_TICK);
        if (until <= 0L) {
            return;
        }

        long now = player.level().getGameTime();
        if (now >= until) {
            tag.remove(LOST_PROMPT_UNTIL_TICK);
            return;
        }

        player.displayClientMessage(
            Component.translatable("message.naturalis.resonance.rebirth.prompt").withStyle(ChatFormatting.RED),
            true
        );
    }

    private static BlockPos findRebirthRespawnPos(ServerLevel level, BlockPos origin, int radius, ServerPlayer player) {
        BlockPos fallback = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin).above();
        if (radius <= 0) {
            return fallback;
        }

        for (int i = 0; i < 24; i++) {
            double angle = player.getRandom().nextDouble() * (Math.PI * 2.0D);
            int distance = 1 + player.getRandom().nextInt(radius);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos candidate = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, origin.getY(), z)).above();
            if (!level.getBlockState(candidate.below()).isAir()) {
                return candidate;
            }
        }

        return fallback;
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        var server = CompatAccess.getServer(player);
        if (server == null) {
            return;
        }
        Advancement root = server.getAdvancements().getAdvancement(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        Advancement advancement = server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }

        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : advancement.getCriteria().keySet()) {
            if (!progress.isDone()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }
}
