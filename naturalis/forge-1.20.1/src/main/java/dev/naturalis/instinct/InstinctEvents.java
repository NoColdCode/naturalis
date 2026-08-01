package dev.naturalis.instinct;

import dev.naturalis.Naturalis;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.network.ScentHintPayload;
import dev.naturalis.rule.NaturalisGameRules;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class InstinctEvents {

    private static final ResourceLocation ADV_ROOT = new ResourceLocation(Naturalis.MOD_ID, "root");
    private static final ResourceLocation ADV_WILD_WANDERER = new ResourceLocation(Naturalis.MOD_ID, "instinct/wild_wanderer");
    private static final ResourceLocation ADV_SURVIVAL_REFLEX = new ResourceLocation(Naturalis.MOD_ID, "instinct/survival_reflex");

    private static final String ROOT_TAG = "naturalis_instinct";
    private static final String LAST_ACTIVE_TICK = "last_active_tick";
    private static final String LAST_X = "last_x";
    private static final String LAST_Y = "last_y";
    private static final String LAST_Z = "last_z";
    private static final String LAST_YAW = "last_yaw";
    private static final String LAST_PITCH = "last_pitch";
    private static final String NEXT_WANDER_TICK = "next_wander_tick";
    private static final String WANDER_ANGLE = "wander_angle";
    static final String WANDER_UNTIL_TICK = "wander_until_tick";
    static final String WANDER_SYNC_YAW = "wander_sync_yaw";
    static final String WANDER_SYNC_PITCH = "wander_sync_pitch";
    private static final String WANDER_START_X = "wander_start_x";
    private static final String WANDER_START_Z = "wander_start_z";
    private static final String WANDER_TARGET_DISTANCE = "wander_target_distance";
    private static final String ACTIVE_THIS_TICK = "active_this_tick";
    private static final String HUNT_ATTACK_COOLDOWN_UNTIL = "hunt_attack_cooldown_until";
    private static final String HUNT_ACTIVE_UNTIL = "hunt_active_until";
    private static final String LAST_MORPH_ID = "last_morph_id";

    private static final double INSTINCT_PLAYER_MAX_HSPEED = 0.28D;
    private static final double INSTINCT_PREY_MAX_HSPEED = 0.26D;
    private static final double WANDER_MIN_DISTANCE = 5.0D;
    private static final double WANDER_MAX_DISTANCE = 10.0D;
    private static final float INPUT_DEADZONE = 0.02F;
    // Slightly above wander max speed (~0.062 blocks/tick => ~0.0038 sq),
    // but well below normal walking horizontal speed.
    private static final double MANUAL_MOVE_HSPEED_SQR = 0.0042D;
    // Strong movement while wander is active is treated as manual override.
    private static final double WANDER_OVERRIDE_HSPEED_SQR = 0.0070D;
    // Walking/sprinting while hunt is active overrides the instinct.
    // Must be above hunt-impulse residual speed (~0.07 b/t → 0.005 sqr) but below normal walking (0.01).
    private static final double HUNT_OVERRIDE_HSPEED_SQR = 0.009D;
    // AFK data older than this (game ticks) is treated as stale and reset to avoid instant-wander on relog.
    private static final long MAX_AFK_STALENESS_TICKS = 1200L; // 60 s
    private static final int SCENT_SCAN_INTERVAL = 14;
    private static final int SCENT_MAX_TARGETS = 16;
    private static final int DEEP_SCENT_HINT_CAP = 5;
    private static final int DEEP_SCENT_WINDOW_TICKS = 220;
    private static final String DEEP_SCENT_UNTIL = "deep_scent_until_tick";
    private static final int GROUP_ATTRACTION_INTERVAL = 20;
    private static final double GROUP_ATTRACTION_RADIUS = 20.0D;
    private static final double GROUP_SOFT_RADIUS = 8.0D;
    private static final String LAST_GROUP_CALL_TICK = "last_group_call_tick";

    private InstinctEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!NaturalisGameRules.isInstinctsEnabled(player.level())) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        int instinctRank = MorphKnowledgeManager.getBranchRank(player, morphId, MorphKnowledgeManager.BRANCH_INSTINCT);
        boolean feralIdentity = ResonanceManager.isResonanceEnabled(player)
            && ResonanceManager.isAligned(player)
            && ResonanceManager.getHumanityStage(player) == ResonanceManager.HumanityStage.LOST;
        boolean effectiveFeral = feralIdentity && !TranslationDeviceUtil.isTranslationCoreHeld(player);

        if (effectiveFeral) {
            applyFeralIdentityBuffs(player, level, morphId);
        }

        // Reset AFK timer whenever the morph changes (new morph or re-morph after a gap)
        // so stale LAST_ACTIVE_TICK data from a previous morph session never causes instant-wander.
        CompoundTag morphCheckTag = getOrCreateInstinctTag(player);
        String lastMorphStr = CompatAccess.getString(morphCheckTag, LAST_MORPH_ID);
        String currentMorphStr = morphId.toString();
        if (!currentMorphStr.equals(lastMorphStr)) {
            morphCheckTag.putLong(LAST_ACTIVE_TICK, now);
            morphCheckTag.putString(LAST_MORPH_ID, currentMorphStr);
            writePoseSnapshot(player, morphCheckTag);
            VanillaMobWanderDriver.discard(player);
        }

        // Always track AFK status (timers stay accurate regardless of suppression).
        boolean afk = updateAndCheckAfk(player, now, morphId);
        CompoundTag instinctTag = getOrCreateInstinctTag(player);
        boolean wanderActive = isWanderActive(instinctTag, now);
        boolean activeThisTick = CompatAccess.getBoolean(instinctTag, ACTIVE_THIS_TICK);
        if (!afk && !effectiveFeral) {
            clearWanderState(player);
        }

        // Any manual activity immediately stops all active instinct steering loops.
        if (activeThisTick && !effectiveFeral) {
            clearWanderState(player);
        }

        // Sessile morphs have no instincts at all (no hunt, fear, wander, smell, group steering).
        if (InstinctManager.isStaticMorph(morphId)) {
            clearWanderState(player);
            VanillaMobWanderDriver.discard(player);
            return;
        }

        // Smell sense is a passive biological trait, not a learned instinct.
        // It is always active regardless of mastery level.
        emitSmellHints(player, level, morphId);
        int socialRank = MorphKnowledgeManager.getSocialRank(player, morphId);
        applyGroupInstinct(player, level, morphId, now, socialRank);
        applySpeciesCommunicationCall(player, level, morphId, now, socialRank);

        if (activeThisTick && !effectiveFeral) {
            return;
        }

        boolean instinctsDisabled = !effectiveFeral && MorphKnowledgeManager.areInstinctsDisabled(instinctRank);
        int instinctInterval = effectiveFeral ? 6 : MorphKnowledgeManager.getInstinctCheckIntervalTicks(instinctRank);
        int wanderRank = MorphKnowledgeManager.getBranchRank(player, morphId, MorphKnowledgeManager.BRANCH_WANDER);
        boolean wanderMastered = wanderRank >= MorphKnowledgeManager.getMaxRankForBranch(MorphKnowledgeManager.BRANCH_WANDER);
        boolean shouldWander = InstinctManager.isWanderMorph(morphId)
            && !wanderMastered
            && afk
            && !hasDirectMovementInput(player);

        String lastAction = null;
        if (!instinctsDisabled && player.tickCount % instinctInterval == 0) {
            boolean reflexTriggered = applyFearReflex(player, level, morphId);
            if (reflexTriggered) {
                lastAction = "fear";
            }
            boolean huntFired = applyHuntInstinct(player, level, morphId, now);
            if (huntFired) {
                lastAction = "hunt";
            }
            reflexTriggered |= huntFired;
            reflexTriggered |= applyOtherInstincts(player, level, morphId, afk);
            if (reflexTriggered && lastAction == null) {
                lastAction = "reflex";
            }

            if (reflexTriggered) {
                grantAdvancement(player, ADV_SURVIVAL_REFLEX);
            }

            if (huntFired) {
                suppressWanderActivityDetection(player);
            }
        }

        if (shouldWander && !instinctsDisabled) {
            if (VanillaMobWanderDriver.tick(player, level, morphId, now)) {
                lastAction = "wander";
                grantAdvancement(player, ADV_WILD_WANDERER);
                suppressWanderActivityDetection(player);
            }
        } else {
            VanillaMobWanderDriver.discardIfActive(player);
        }

        InstinctDebug.tickSummary(
            player,
            morphId,
            afk,
            activeThisTick,
            effectiveFeral,
            instinctRank,
            wanderRank,
            wanderMastered,
            shouldWander,
            wanderActive,
            (int) (now - CompatAccess.getLong(instinctTag, LAST_ACTIVE_TICK)),
            lastAction
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VanillaMobWanderDriver.discard(player);
        }
    }

    private static void applyFeralIdentityBuffs(ServerPlayer player, ServerLevel level, ResourceLocation morphId) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (!EnvironmentalSusceptibilityManager.shouldSuppressMorphNightVision(level, player.blockPosition(), morphId)) {
            ensureEffect(player, "NIGHT_VISION", "NIGHT_VISION", 220, 0);
        }
        ensureEffect(player, "MOVEMENT_SPEED", "SPEED", 80, 1);
        ensureEffect(player, "DAMAGE_BOOST", "STRENGTH", 80, 1);
        ensureEffect(player, "DAMAGE_RESISTANCE", "RESISTANCE", 80, 0);
    }

    private static void ensureEffect(ServerPlayer player, String primary, String fallback, int duration, int amplifier) {
        var effect = CompatAccess.resolveMobEffect(primary, fallback);
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getDuration() > duration / 4) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static void emitSmellHints(ServerPlayer player, ServerLevel level, ResourceLocation morphId) {
        int smellStrength = InstinctManager.getSmellStrength(morphId);
        if (smellStrength <= 0 || player.tickCount % SCENT_SCAN_INTERVAL != 0) {
            return;
        }
        if (isDeepScentWindowActive(player)) {
            sendScentHints(player, level, morphId, smellStrength + 4, DEEP_SCENT_HINT_CAP);
        } else {
            sendScentHints(player, level, morphId, smellStrength, SCENT_MAX_TARGETS);
        }
    }

    private static void markDeepScentWindow(ServerPlayer player) {
        getOrCreateInstinctTag(player).putLong(DEEP_SCENT_UNTIL, player.level().getGameTime() + DEEP_SCENT_WINDOW_TICKS);
    }

    private static boolean isDeepScentWindowActive(ServerPlayer player) {
        long until = CompatAccess.getLong(getOrCreateInstinctTag(player), DEEP_SCENT_UNTIL);
        return until > 0L && player.level().getGameTime() < until;
    }

    public record DeepSniffResult(int scented, int prey, int hostile, int unknown) {
    }

    /** Active shift-right-click sniff: deep ribbons, glow marks, and client scent vision. */
    public static DeepSniffResult performDeepSniff(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return new DeepSniffResult(0, 0, 0, 0);
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || InstinctManager.getSmellStrength(morphId) <= 0) {
            return new DeepSniffResult(0, 0, 0, 0);
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return new DeepSniffResult(0, 0, 0, 0);
        }

        int smellStrength = InstinctManager.getSmellStrength(morphId);
        markDeepScentWindow(player);
        int[] counts = sendScentHints(player, level, morphId, smellStrength + 4, DEEP_SCENT_HINT_CAP);
        int sent = counts[0];
        int prey = counts[1];
        int hostile = counts[2];
        int unknown = counts[3];

        dev.naturalis.network.NaturalisNetwork.sendToPlayer(
            player,
            new dev.naturalis.network.SniffPulsePayload(smellStrength, sent, prey, hostile)
        );
        return new DeepSniffResult(sent, prey, hostile, unknown);
    }

    /**
     * @return int[4] — sent, prey, hostile, other
     */
    private static int[] sendScentHints(
        ServerPlayer player,
        ServerLevel level,
        ResourceLocation morphId,
        int hintStrength,
        int cap
    ) {
        int smellStrength = InstinctManager.getSmellStrength(morphId);
        double range = hintStrength >= smellStrength + 4 ? 12.0D + smellStrength * 8.0D : 8.0D + smellStrength * 6.0D;
        AABB scanBox = player.getBoundingBox().inflate(range);

        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            scanBox,
            e -> e.isAlive() && e != player && player.distanceToSqr(e) <= range * range
        );

        nearby.sort(Comparator.comparingDouble(player::distanceToSqr));

        int sent = 0;
        int prey = 0;
        int hostile = 0;
        int other = 0;
        int limit = Math.max(1, cap);
        boolean sendHints = dev.naturalis.config.NaturalisConfig.instinctsScentHints()
            && dev.naturalis.experience.NaturalisExperienceProfile.useInstinctScentHints(player.level());

        for (LivingEntity target : nearby) {
            if (sent >= limit) {
                break;
            }

            byte category = classifyScentTarget(morphId, target);
            if (category == ScentHintPayload.CATEGORY_UNKNOWN) {
                continue;
            }

            if (hintStrength >= smellStrength + 4) {
                switch (category) {
                    case ScentHintPayload.CATEGORY_PREY -> prey++;
                    case ScentHintPayload.CATEGORY_HOSTILE -> hostile++;
                    default -> other++;
                }
            }

            if (sendHints) {
                dev.naturalis.network.NaturalisNetwork.sendToPlayer(
                    player,
                    new dev.naturalis.network.ScentHintPayload(target.getId(), category, hintStrength)
                );
            }
            sent++;
        }
        return new int[] { sent, prey, hostile, other };
    }

    private static void applyGroupInstinct(ServerPlayer player, ServerLevel level, ResourceLocation morphId, long now, int socialRank) {
        if (player.tickCount % GROUP_ATTRACTION_INTERVAL != 0) {
            return;
        }

        EntityType<?> morphType = CompatAccess.getEntityType(morphId);
        if (morphType == null) {
            return;
        }

        double attractionRadius = socialRank >= 1 ? GROUP_ATTRACTION_RADIUS + 6.0D : GROUP_ATTRACTION_RADIUS;
        double softRadius = socialRank >= 1 ? GROUP_SOFT_RADIUS + 1.5D : GROUP_SOFT_RADIUS;

        List<Mob> allies = level.getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(attractionRadius),
            mob -> mob.isAlive()
                && mob.getType() == morphType
                && !mob.isLeashed()
                && player.distanceToSqr(mob) <= attractionRadius * attractionRadius
        );

        if (allies.isEmpty()) {
            return;
        }

        double maxDistanceSqr = attractionRadius * attractionRadius;
        for (Mob ally : allies) {
            if (ally.getTarget() != null && ally.getTarget().isAlive()) {
                continue;
            }

            double distSqr = ally.distanceToSqr(player);
            if (distSqr > maxDistanceSqr) {
                ally.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), socialRank >= 1 ? 1.15D : 1.0D);
                continue;
            }

            if (distSqr > softRadius * softRadius) {
                double angle = (now * 0.08D) + (ally.getId() * 0.47D);
                double radius = 3.5D + ((Math.abs(ally.getId()) % 30) / 10.0D);
                double targetX = player.getX() + Math.cos(angle) * radius;
                double targetZ = player.getZ() + Math.sin(angle) * radius;
                ally.getNavigation().moveTo(targetX, player.getY(), targetZ, socialRank >= 1 ? 1.08D : 0.95D);
            }

            ally.lookAt(player, 22.0F, 22.0F);
        }

        if (socialRank >= 3 && player.tickCount % 20 == 0) {
            var regen = CompatAccess.resolveMobEffect("REGENERATION", "HEAL");
            player.addEffect(new MobEffectInstance(regen, 60, 0, false, false, true));
            for (Mob ally : allies) {
                if (ally.distanceToSqr(player) <= 10.0D * 10.0D) {
                    ally.addEffect(new MobEffectInstance(regen, 60, 0, false, false, true));
                }
            }
        }
    }

    private static void applySpeciesCommunicationCall(ServerPlayer player, ServerLevel level, ResourceLocation morphId, long now, int socialRank) {
        if (!player.isShiftKeyDown()) {
            return;
        }

        CompoundTag tag = getOrCreateInstinctTag(player);
        long lastCall = CompatAccess.getLong(tag, LAST_GROUP_CALL_TICK);
        int callCooldown = MorphKnowledgeManager.getGroupCallCooldownTicks(socialRank);
        if (now - lastCall < callCooldown) {
            return;
        }

        EntityType<?> morphType = CompatAccess.getEntityType(morphId);
        if (morphType == null) {
            return;
        }

        List<Mob> allies = level.getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(24.0D),
            mob -> mob.isAlive() && mob.getType() == morphType
        );

        if (allies.isEmpty()) {
            return;
        }

        boolean hasNearbyAlly = allies.stream().anyMatch(mob -> mob.distanceToSqr(player) <= 12.0D * 12.0D);
        if (!hasNearbyAlly) {
            return;
        }

        int rallied = 0;
        for (Mob ally : allies) {
            ally.lookAt(player, 40.0F, 40.0F);
            if (ally.getTarget() == null) {
                double angle = ((ally.getId() * 0.61D) + (now * 0.04D));
                double radius = 2.5D + ((Math.abs(ally.getId()) % 20) / 10.0D);
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                ally.getNavigation().moveTo(x, player.getY(), z, 1.05D);
            }

            if (socialRank >= 5) {
                ally.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("MOVEMENT_SPEED", "SPEED"), 100, 0, false, false, true));
                ally.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("DAMAGE_BOOST", "STRENGTH"), 100, 0, false, false, true));
            }

            rallied++;
            if (rallied >= 20) {
                break;
            }
        }

        if (socialRank >= 5) {
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("MOVEMENT_SPEED", "SPEED"), 100, 0, false, false, true));
        }

        tag.putLong(LAST_GROUP_CALL_TICK, now);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 10, 0.55D, 0.35D, 0.55D, 0.02D);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 0.95F + player.getRandom().nextFloat() * 0.15F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.group_call"), true);
    }

    private static byte classifyScentTarget(ResourceLocation morphId, LivingEntity target) {
        ResourceLocation targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (targetId != null && InstinctManager.hunts(morphId, targetId)) {
            return ScentHintPayload.CATEGORY_PREY;
        }
        if (target instanceof Enemy) {
            return ScentHintPayload.CATEGORY_HOSTILE;
        }
        return ScentHintPayload.CATEGORY_UNKNOWN;
    }

    private static boolean applyFearReflex(ServerPlayer player, ServerLevel level, ResourceLocation morphId) {
        Vec3 steer = Vec3.ZERO;
        boolean reacted = false;

        if (InstinctManager.fearsWater(morphId)) {
            Vec3 awayWater = getWaterAvoidance(player, level, 6);
            if (awayWater.lengthSqr() > 0.0D) {
                steer = steer.add(awayWater.scale(player.isInWater() ? 0.10D : 0.05D));
                reacted = true;
            }
        }

        if (InstinctManager.fearsCats(morphId)) {
            Vec3 awayCat = getAvoidanceFromNearest(player, level.getEntitiesOfClass(Cat.class, player.getBoundingBox().inflate(10.0D)));
            if (awayCat.lengthSqr() > 0.0D) {
                steer = steer.add(awayCat.scale(0.07D));
                reacted = true;
            }
            Vec3 awayOcelot = getAvoidanceFromNearest(player, level.getEntitiesOfClass(Ocelot.class, player.getBoundingBox().inflate(10.0D)));
            if (awayOcelot.lengthSqr() > 0.0D) {
                steer = steer.add(awayOcelot.scale(0.06D));
                reacted = true;
            }
        }

        if (InstinctManager.fearsWolves(morphId)) {
            Vec3 awayWolf = getAvoidanceFromNearest(player, level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(12.0D),
                mob -> mob.getType() == EntityType.WOLF
            ));
            if (awayWolf.lengthSqr() > 0.0D) {
                steer = steer.add(awayWolf.scale(0.08D));
                reacted = true;
            }
        }

        if (InstinctManager.fearsIronGolem(morphId)) {
            Vec3 awayGolem = getAvoidanceFromNearest(player, level.getEntitiesOfClass(
                IronGolem.class,
                player.getBoundingBox().inflate(14.0D)
            ));
            if (awayGolem.lengthSqr() > 0.0D) {
                steer = steer.add(awayGolem.scale(0.07D));
                reacted = true;
            }
        }

        if (InstinctManager.fearsZoglin(morphId)) {
            Vec3 awayZoglin = getAvoidanceFromNearest(player, level.getEntitiesOfClass(
                Zoglin.class,
                player.getBoundingBox().inflate(12.0D)
            ));
            if (awayZoglin.lengthSqr() > 0.0D) {
                steer = steer.add(awayZoglin.scale(0.08D));
                reacted = true;
            }
        }

        if (InstinctManager.fearsBees(morphId)) {
            Vec3 awayBee = getAvoidanceFromNearest(player, level.getEntitiesOfClass(
                Bee.class,
                player.getBoundingBox().inflate(10.0D),
                bee -> bee.isAngry()
            ));
            if (awayBee.lengthSqr() > 0.0D) {
                steer = steer.add(awayBee.scale(0.06D));
                reacted = true;
            }
        }

        if (steer.lengthSqr() > 0.0D) {
            if (InstinctDebug.enabled()) {
                InstinctDebug.event(player, "fear", "steer " + String.format("%.2f,%.2f", steer.x, steer.z));
            }
            applySteering(player, level, steer.normalize(), 0.08D, true, true);
            return true;
        }

        return reacted;
    }

    private static boolean applyHuntInstinct(ServerPlayer player, ServerLevel level, ResourceLocation morphId, long now) {
        if (!InstinctManager.isHunterMorph(morphId)) {
            return false;
        }

        boolean reacted = false;

        List<LivingEntity> preyList = level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(20.0D),
            prey -> prey.isAlive() && prey != player && isPreyForMorph(morphId, prey)
        );

        LivingEntity nearestPrey = preyList.stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);

        if (nearestPrey != null) {
            Vec3 towardPrey = nearestPrey.position().subtract(player.position());
            if (towardPrey.lengthSqr() > 1.0E-5D) {
                applySteering(player, level, towardPrey.normalize(), 0.09D, true, true);
                // Mark hunt as active so its own velocity does not reset the AFK timer.
                getOrCreateInstinctTag(player).putLong(HUNT_ACTIVE_UNTIL, now + 120L);
                reacted = true;
            }

            if (player.distanceTo(nearestPrey) <= 1.8D && now >= getHuntAttackCooldownUntil(player)) {
                nearestPrey.hurt(level.damageSources().playerAttack(player), 4.5F);
                nearestPrey.knockback(0.55D, player.getX() - nearestPrey.getX(), player.getZ() - nearestPrey.getZ());
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                setHuntAttackCooldownUntil(player, now + 55L);
                reacted = true;
            }
        }

        // Make prey mobs flee from hunter morphs (throttled — pushing every entity every tick stalls the server).
        if (player.tickCount % 8 == 0) {
            for (LivingEntity prey : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(12.0D),
                e -> e.isAlive() && e != player && isPreyForMorph(morphId, e))) {
                Vec3 away = prey.position().subtract(player.position());
                Vec3 awayHorizontal = new Vec3(away.x, 0.0D, away.z);
                if (awayHorizontal.lengthSqr() <= 1.0E-5D) {
                    continue;
                }
                Vec3 flee = awayHorizontal.normalize().scale(0.09D);
                pushHorizontal(prey, flee, INSTINCT_PREY_MAX_HSPEED);
                faceEntityHorizontal(prey, flee);
                prey.hurtMarked = true;
                reacted = true;
            }
        }

        return reacted;
    }

    private static boolean applyOtherInstincts(ServerPlayer player, ServerLevel level, ResourceLocation morphId, boolean afk) {
        boolean reacted = false;

        // Aquatic panic: dry-vulnerable morphs nudge toward nearby water when dehydrated.
        if (EnvironmentalSusceptibilityManager.isDryVulnerable(morphId)
            && EnvironmentalSusceptibilityManager.isAwayFromWater(level, player.position(), 10.0D)) {
            Vec3 towardWater = getTowardNearestWater(player, level, 8);
            if (towardWater.lengthSqr() > 0.0D) {
                applySteering(player, level, towardWater.normalize(), 0.06D, true, true);
                reacted = true;
            }
        }

        // Nyctalop hostile morphs softly prefer darker spaces when in bright zones.
        if (InstinctManager.isNyctalopHostile(morphId) && level.getMaxLocalRawBrightness(player.blockPosition()) >= 10) {
            Vec3 darker = getDirectionToDarkerSpot(player, level);
            if (darker.lengthSqr() > 0.0D) {
                applySteering(player, level, darker.normalize(), 0.045D, true, true);
                reacted = true;
            }
        }

        return reacted;
    }

    /**
     * Called immediately after wander applies steering so that movement and look from the
     * vanilla proxy do not register as manual player activity on the next AFK check.
     */
    private static void suppressWanderActivityDetection(ServerPlayer player) {
        var root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        CompoundTag tag = CompatAccess.getCompound(root, ROOT_TAG);
        writePoseSnapshot(player, tag);
    }

    private static void clearWanderState(ServerPlayer player) {
        VanillaMobWanderDriver.discard(player);
        var root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            return;
        }

        CompoundTag tag = CompatAccess.getCompound(root, ROOT_TAG);
        tag.remove(NEXT_WANDER_TICK);
        tag.remove(WANDER_ANGLE);
        tag.remove(WANDER_UNTIL_TICK);
        tag.remove(WANDER_SYNC_YAW);
        tag.remove(WANDER_SYNC_PITCH);
        tag.remove(WANDER_START_X);
        tag.remove(WANDER_START_Z);
        tag.remove(WANDER_TARGET_DISTANCE);
    }

    static CompoundTag getOrCreateInstinctTag(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(root, ROOT_TAG);
    }

    private static boolean updateAndCheckAfk(ServerPlayer player, long now, ResourceLocation morphId) {
        var root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        CompoundTag tag = CompatAccess.getCompound(root, ROOT_TAG);

        if (!CompatAccess.contains(tag, LAST_ACTIVE_TICK)) {
            tag.putLong(LAST_ACTIVE_TICK, now);
            writePoseSnapshot(player, tag);
            return false;
        }

        // Stale data guard: if the stored tick is from a previous play session or a very
        // old morph cycle, treat the player as just-active to avoid instant-wander.
        long storedLastActive = CompatAccess.getLong(tag, LAST_ACTIVE_TICK);
        if (now - storedLastActive > MAX_AFK_STALENESS_TICKS) {
            tag.putLong(LAST_ACTIVE_TICK, now);
            writePoseSnapshot(player, tag);
            return false;
        }

        double lastX = CompatAccess.getDouble(tag, LAST_X);
        double lastY = CompatAccess.getDouble(tag, LAST_Y);
        double lastZ = CompatAccess.getDouble(tag, LAST_Z);
        float lastYaw = CompatAccess.getFloat(tag, LAST_YAW);
        float lastPitch = CompatAccess.getFloat(tag, LAST_PITCH);

        Vec3 pos = player.position();
        boolean directInput = hasDirectMovementInput(player);
        boolean wandering = isWanderActive(tag, now);
        boolean huntActive = isHuntActive(tag, now);
        double horizontalSpeedSqr = player.getDeltaMovement().horizontalDistanceSqr();
        boolean manualMovement = !wandering
            && horizontalSpeedSqr > MANUAL_MOVE_HSPEED_SQR
            && (!huntActive || horizontalSpeedSqr > HUNT_OVERRIDE_HSPEED_SQR);
        boolean moved = !wandering && horizontalMoved(pos, lastX, lastZ);
        boolean manualLook = false;
        if (wandering && CompatAccess.contains(tag, WANDER_SYNC_YAW)) {
            float syncYaw = CompatAccess.getFloat(tag, WANDER_SYNC_YAW);
            float syncPitch = CompatAccess.getFloat(tag, WANDER_SYNC_PITCH);
            manualLook = Math.abs(wrapDegrees(player.getYRot() - syncYaw)) > 4.0F
                || Math.abs(player.getXRot() - syncPitch) > 3.5F;
        }

        boolean looked = manualLook || (!wandering
            && (Math.abs(wrapDegrees(player.getYRot() - lastYaw)) > 3.5F
            || Math.abs(player.getXRot() - lastPitch) > 3.5F));

        boolean manualOverride = wandering
            && directInput
            && horizontalSpeedSqr > WANDER_OVERRIDE_HSPEED_SQR;

        boolean activeThisTick = directInput || manualOverride || manualMovement || moved || looked || player.swinging;
        tag.putBoolean(ACTIVE_THIS_TICK, activeThisTick);

        if (activeThisTick) {
            tag.putLong(LAST_ACTIVE_TICK, now);
        }

        if (!wandering) {
            writePoseSnapshot(player, tag);
        }

        long lastActive = CompatAccess.getLong(tag, LAST_ACTIVE_TICK);
        return now - lastActive >= MorphKnowledgeManager.getAfkThresholdTicks(player, morphId);
    }

    private static boolean isWanderActive(net.minecraft.nbt.CompoundTag tag, long now) {
        return CompatAccess.contains(tag, WANDER_UNTIL_TICK) && CompatAccess.getLong(tag, WANDER_UNTIL_TICK) > now;
    }

    private static boolean isHuntActive(net.minecraft.nbt.CompoundTag tag, long now) {
        return CompatAccess.contains(tag, HUNT_ACTIVE_UNTIL) && CompatAccess.getLong(tag, HUNT_ACTIVE_UNTIL) > now;
    }

    private static boolean horizontalMoved(Vec3 pos, double lastX, double lastZ) {
        double dx = pos.x - lastX;
        double dz = pos.z - lastZ;
        return dx * dx + dz * dz > 0.0025D;
    }

    private static boolean hasDirectMovementInput(ServerPlayer player) {
        return Math.abs(player.zza) > INPUT_DEADZONE
            || Math.abs(player.xxa) > INPUT_DEADZONE
            || player.isSprinting()
            || player.isShiftKeyDown();
    }

    private static void writePoseSnapshot(ServerPlayer player, net.minecraft.nbt.CompoundTag tag) {
        Vec3 pos = player.position();
        tag.putDouble(LAST_X, pos.x);
        tag.putDouble(LAST_Y, pos.y);
        tag.putDouble(LAST_Z, pos.z);
        tag.putFloat(LAST_YAW, player.getYRot());
        tag.putFloat(LAST_PITCH, player.getXRot());
    }

    private static float wrapDegrees(float deg) {
        float value = deg % 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }
        if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    private static Vec3 getAvoidanceFromNearest(ServerPlayer player, List<? extends LivingEntity> threats) {
        LivingEntity nearest = threats.stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (nearest == null) {
            return Vec3.ZERO;
        }

        Vec3 away = player.position().subtract(nearest.position());
        if (away.lengthSqr() <= 1.0E-5D) {
            return Vec3.ZERO;
        }
        return away.normalize();
    }

    private static Vec3 getWaterAvoidance(ServerPlayer player, ServerLevel level, int radius) {
        BlockPos nearest = findNearestWater(player.blockPosition(), level, radius);
        if (nearest == null) {
            return Vec3.ZERO;
        }

        Vec3 away = player.position().subtract(Vec3.atCenterOf(nearest));
        if (away.lengthSqr() <= 1.0E-5D) {
            return Vec3.ZERO;
        }
        return away.normalize();
    }

    private static Vec3 getTowardNearestWater(ServerPlayer player, ServerLevel level, int radius) {
        BlockPos nearest = findNearestWater(player.blockPosition(), level, radius);
        if (nearest == null) {
            return Vec3.ZERO;
        }

        Vec3 toward = Vec3.atCenterOf(nearest).subtract(player.position());
        if (toward.lengthSqr() <= 1.0E-5D) {
            return Vec3.ZERO;
        }
        return toward.normalize();
    }

    private static BlockPos findNearestWater(BlockPos center, ServerLevel level, int radius) {
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getFluidState(cursor).isSourceOfType(Fluids.WATER)) {
                        continue;
                    }

                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static Vec3 getDirectionToDarkerSpot(ServerPlayer player, ServerLevel level) {
        BlockPos origin = player.blockPosition();
        int base = level.getMaxLocalRawBrightness(origin);

        Vec3 bestVec = Vec3.ZERO;
        int bestLight = base;

        int[] offsets = new int[]{-4, 0, 4};
        for (int ox : offsets) {
            for (int oz : offsets) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                BlockPos sample = origin.offset(ox, 0, oz);
                int light = level.getMaxLocalRawBrightness(sample);
                if (light < bestLight) {
                    bestLight = light;
                    Vec3 vec = Vec3.atCenterOf(sample).subtract(player.position());
                    if (vec.lengthSqr() > 1.0E-5D) {
                        bestVec = vec.normalize();
                    }
                }
            }
        }

        return bestVec;
    }

    private static boolean isPreyForMorph(ResourceLocation morphId, LivingEntity entity) {
        ResourceLocation preyId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return preyId != null && InstinctManager.hunts(morphId, preyId);
    }

    private static long getHuntAttackCooldownUntil(ServerPlayer player) {
        var root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        return CompatAccess.getLong(CompatAccess.getCompound(root, ROOT_TAG), HUNT_ATTACK_COOLDOWN_UNTIL);
    }

    private static void setHuntAttackCooldownUntil(ServerPlayer player, long until) {
        var root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        CompoundTag tag = CompatAccess.getCompound(root, ROOT_TAG);
        tag.putLong(HUNT_ATTACK_COOLDOWN_UNTIL, until);
        root.put(ROOT_TAG, tag);
    }

    private static void applySteering(ServerPlayer player, ServerLevel level, Vec3 rawDirection, double speed, boolean faceDirection, boolean obstacleAware) {
        Vec3 horizontalRaw = new Vec3(rawDirection.x, 0.0D, rawDirection.z);
        if (horizontalRaw.lengthSqr() <= 1.0E-5D) {
            return;
        }

        Vec3 direction = horizontalRaw.normalize();

        if (obstacleAware && isBlockedAhead(player, level, direction)) {
            if (player.onGround()) {
                player.jumpFromGround();
            }
            Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).normalize();
            if (player.getRandom().nextBoolean()) {
                side = side.scale(-1.0D);
            }
            direction = direction.scale(0.55D).add(side.scale(0.75D)).normalize();
            speed *= 1.12D;
        }

        if (faceDirection) {
            float targetYaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0D);
            float newYaw = player.getYRot() + wrapDegrees(targetYaw - player.getYRot()) * 0.55F;
            player.setYRot(newYaw);
            player.setYHeadRot(newYaw);
            player.setYBodyRot(newYaw);

            float targetPitch = 0.0F;
            float newPitch = player.getXRot() + (targetPitch - player.getXRot()) * 0.35F;
            player.setXRot(newPitch);
        }

        pushHorizontal(player, direction.scale(speed), INSTINCT_PLAYER_MAX_HSPEED);
        player.hurtMarked = true;
    }

    private static void pushHorizontal(LivingEntity entity, Vec3 impulse, double maxHorizontalSpeed) {
        Vec3 current = entity.getDeltaMovement();
        Vec3 currentHorizontal = new Vec3(current.x, 0.0D, current.z);
        Vec3 blended = currentHorizontal.scale(0.82D).add(impulse.scale(0.78D));

        double len = blended.length();
        if (len > maxHorizontalSpeed) {
            blended = blended.scale(maxHorizontalSpeed / len);
        }

        entity.setDeltaMovement(blended.x, current.y, blended.z);
    }

    private static void faceEntityHorizontal(LivingEntity entity, Vec3 direction) {
        if (direction.lengthSqr() <= 1.0E-5D) {
            return;
        }

        float targetYaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0D);
        float newYaw = entity.getYRot() + wrapDegrees(targetYaw - entity.getYRot()) * 0.60F;
        entity.setYRot(newYaw);
        entity.setYHeadRot(newYaw);
        entity.setYBodyRot(newYaw);
        entity.setXRot(entity.getXRot() * 0.65F);
    }

    private static boolean isBlockedAhead(ServerPlayer player, ServerLevel level, Vec3 direction) {
        Vec3 lookAhead = player.position().add(direction.scale(0.9D));
        BlockPos feetAhead = BlockPos.containing(lookAhead.x, player.getY(), lookAhead.z);
        BlockPos headAhead = feetAhead.above();

        boolean feetBlocked = level.getBlockState(feetAhead).blocksMotion();
        boolean headBlocked = level.getBlockState(headAhead).blocksMotion();
        return feetBlocked || headBlocked;
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        Advancement root = player.getServer().getAdvancements().getAdvancement(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }
}
