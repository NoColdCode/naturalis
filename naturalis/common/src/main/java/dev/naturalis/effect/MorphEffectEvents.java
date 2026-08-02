package dev.naturalis.effect;

import dev.naturalis.NaturalisMod;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.item.BrewedMorphPotionItem;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphAcquisition;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.util.MorphShapeUtil;
import dev.naturalis.morph.quickslot.MorphQuickSlotServerSession;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tocraft.walkers.api.PlayerAbilities;
import tocraft.walkers.api.PlayerShape;
import tocraft.walkers.api.PlayerShapeChanger;
import tocraft.walkers.api.events.ShapeEvents;
import tocraft.walkers.api.variant.ShapeType;
import tocraft.walkers.impl.PlayerDataProvider;

import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class MorphEffectEvents {

    private static final ResourceLocation ADV_POTION_BINDING = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "resonance/potion_binding");
    private static final ResourceLocation ADV_POTION_BREWED = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "resonance/potion_brewed");
    private static final ResourceLocation ADV_POTION_BREWED_SPLASH = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "resonance/potion_brewed_splash");

    private static final String ROOT_TAG = "naturalis_effects";
    private static final String LAST_MORPH_ID = "last_morph_id";
    private static final String BINDING_TARGET_MORPH_ID = "binding_target_morph_id";
    private static final String BINDING_ACTIVE = "binding_active";
    private static final String BREWED_MORPH_ID = "brewed_morph_id";
    private static final String LAST_TRANSMUTATION_GIFT_TICK = "last_transmutation_gift_tick";
    private static final String CLOUD_KIND_TAG = "naturalis_custom_cloud_kind";
    private static final String CLOUD_MORPH_ID_TAG = "naturalis_custom_cloud_morph";
    private static final String CLOUD_DURATION_TAG = "naturalis_custom_cloud_duration";
    private static final String CLOUD_LAST_APPLY_TICK_TAG = "naturalis_custom_cloud_last_apply";

    private static final String CLOUD_KIND_BINDING = "binding";
    private static final String CLOUD_KIND_BREWED = "brewed";

    private static final Map<ResourceKey<net.minecraft.world.level.Level>, Set<UUID>> CUSTOM_LINGERING_CLOUDS = new HashMap<>();

    private static final int DEFAULT_BREWED_DURATION = 20 * 60;
    private static final int MIN_BREWED_DURATION = 20;
    private static final int BINDING_POTION_DURATION = 8 * 20 * 60;
    private static final double SPLASH_POTION_RADIUS = 4.0D;
    private static final double LINGERING_POTION_RADIUS = 6.0D;
    private static final int WITCH_GIFT_COOLDOWN = 20 * 14;
    private static final int EVOKER_GIFT_COOLDOWN = 20 * 9;
    private static final double WITCH_GIFT_CHANCE = 0.10D;
    private static final double EVOKER_GIFT_CHANCE = 0.22D;
    // Weighted pools by duplication: dangerous forms are uncommon but no longer ultra-rare.
    private static final String[] WITCH_GIFT_POOL = {
        "minecraft:frog",
        "minecraft:cow",
        "minecraft:sheep",
        "minecraft:goat",
        "minecraft:pig",
        "minecraft:chicken",
        "minecraft:rabbit",
        "minecraft:turtle",
        "minecraft:axolotl",
        // Keep dangerous options limited to wolf/fox.
        "minecraft:wolf", "minecraft:wolf", "minecraft:wolf",
        "minecraft:fox", "minecraft:fox"
    };
    private static final String[] EVOKER_GIFT_POOL = {
        "minecraft:frog",
        "minecraft:cow",
        "minecraft:sheep",
        "minecraft:goat",
        "minecraft:pig",
        "minecraft:chicken",
        "minecraft:rabbit",
        "minecraft:turtle",
        "minecraft:axolotl",
        "minecraft:armadillo",
        "minecraft:sniffer",
        // Evoker is better at transmutation and rolls dangerous outcomes more often.
        "minecraft:wolf", "minecraft:wolf", "minecraft:wolf", "minecraft:wolf",
        "minecraft:fox", "minecraft:fox", "minecraft:fox"
    };
    private static boolean shapeGuardRegistered;

    private MorphEffectEvents() {
    }

    public static boolean applyBrewedMorph(ServerPlayer player, ResourceLocation morphId, int durationTicks) {
        if (!NaturalisConfig.brewedMorphEnabled()) {
            return false;
        }
        if (player == null || morphId == null || durationTicks < MIN_BREWED_DURATION) {
            return false;
        }

        if (!isValidLivingMorph(morphId)) {
            return false;
        }

        CompoundTag effectTag = getOrCreateEffectTag(player);
        // Brewed morph has priority: binding must be removed instantly to avoid state fights.
        if (NaturalisConfig.brewedMorphOverridesBinding() && player.hasEffect(NaturalisMobEffects.MORPH_BINDING)) {
            player.removeEffect(NaturalisMobEffects.MORPH_BINDING);
        }
        effectTag.remove(BINDING_TARGET_MORPH_ID);
        effectTag.remove(BINDING_ACTIVE);
        effectTag.putString(BREWED_MORPH_ID, morphId.toString());

        player.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH, durationTicks, 0, false, true, true));
        return forceMorph(player, morphId);
    }

    public static boolean applyBrewedMorphFromStack(ServerPlayer player, ItemStack stack, boolean notifyOnFailure) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation mobId = resolveBrewedMobId(stack);
        if (mobId == null) {
            if (notifyOnFailure) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("item.naturalis.brewed_morph_potion.no_target"),
                    true
                );
            }
            return false;
        }

        int duration = readBrewedDuration(stack);
        boolean applied = applyBrewedMorph(player, mobId, duration);
        if (!applied && notifyOnFailure) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("item.naturalis.brewed_morph_potion.no_target"),
                true
            );
        }
        return applied;
    }

    /**
     * Applies brewed morph from a splash/lingering potion stack to any living entity (players get full morph; mobs get effect-only).
     */
    public static void applyBrewedMorphPotionToLiving(LivingEntity target, ItemStack stack) {
        if (target == null || stack == null || stack.isEmpty()) {
            return;
        }
        if (target instanceof ServerPlayer serverPlayer) {
            applyBrewedMorphFromStack(serverPlayer, stack, false);
            return;
        }
        ResourceLocation mobId = resolveBrewedMobId(stack);
        if (mobId == null) {
            return;
        }
        int duration = readBrewedDuration(stack);
        applyBrewedMorphToMob(target, mobId, duration);
    }

    public static void registerShapeGuards() {
        if (shapeGuardRegistered) {
            return;
        }
        shapeGuardRegistered = true;

        ShapeEvents.UNLOCK_SHAPE.register((player, type) -> {
            if (player instanceof ServerPlayer serverPlayer
                && !dev.naturalis.survivalas.SurvivalAsRuntime.isAcquireAllowed(
                    type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type.getEntityType()))) {
                return InteractionResult.FAIL;
            }
            if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING)) {
                ResourceLocation targetId = getBindingTarget(player);
                if (targetId == null || type == null) {
                    return InteractionResult.FAIL;
                }

                ResourceLocation requested = BuiltInRegistries.ENTITY_TYPE.getKey(type.getEntityType());
                if (requested == null || !requested.equals(targetId)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        ShapeEvents.SWAP_SHAPE.register((player, to) -> {
            long now = player.level().getGameTime();
            boolean stormAttuned = player.hasEffect(NaturalisMobEffects.STORM_ATTUNEMENT);

            ResourceLocation requestedId = null;
            if (to != null) {
                requestedId = BuiltInRegistries.ENTITY_TYPE.getKey(to.getType());
            }
            if (player instanceof ServerPlayer
                && !dev.naturalis.survivalas.SurvivalAsRuntime.isMorphAllowed(requestedId)) {
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(dev.naturalis.survivalas.SurvivalAsMessages.cannotChange(), true);
                }
                return InteractionResult.FAIL;
            }

            if (stormAttuned && to == null) {
                return InteractionResult.FAIL;
            }

            if (to == null
                && ResonanceManager.isHumanityLocked(player)
                && !ResonanceManager.isRecoveryWindowActive(player, now)) {
                if (ResonanceManager.shouldSendLockWarning(player, now)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.resonance.locked"));
                }
                return InteractionResult.FAIL;
            }

            if (to != null && ResonanceManager.isHumanFormLockActive(player, now)) {
                if (MorphQuickSlotServerSession.isBlockingWalkersSwap(player)) {
                    return InteractionResult.PASS;
                }
                if (ResonanceManager.shouldSendLockWarning(player, now)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.resonance.human_lock"));
                }
                return InteractionResult.FAIL;
            }

            if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING) && !stormAttuned) {
                if (MorphQuickSlotServerSession.isBlockingWalkersSwap(player)) {
                    return InteractionResult.PASS;
                }
                ResourceLocation targetId = getBindingTarget(player);
                if (targetId == null || to == null) {
                    return InteractionResult.FAIL;
                }

                ResourceLocation requested = BuiltInRegistries.ENTITY_TYPE.getKey(to.getType());
                if (requested == null || !requested.equals(targetId)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag tag = getOrCreateEffectTag(player);
        ResourceLocation currentMorphId = CurrentMorphUtil.getCurrentMorphId(player);
        MorphShapeUtil.enforceCurrentShape(player);

        if (player.hasEffect(NaturalisMobEffects.BREWED_MORPH)) {
            if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING)) {
                player.removeEffect(NaturalisMobEffects.MORPH_BINDING);
                tag.remove(BINDING_TARGET_MORPH_ID);
                tag.remove(BINDING_ACTIVE);
            }
            currentMorphId = enforceBrewedMorph(player, tag, currentMorphId);
        }

        if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING) && NaturalisConfig.morphBindingEnabled()) {
            if (MorphQuickSlotServerSession.isBlockingWalkersSwap(player)) {
                return;
            }
            // First tick of this binding effect: freeze the target once.
            if (!CompatAccess.getBoolean(tag, BINDING_ACTIVE)) {
                ResourceLocation initialTarget = currentMorphId != null
                    ? currentMorphId
                    : parseMobId(CompatAccess.getString(tag, LAST_MORPH_ID));

                if (initialTarget != null) {
                    tag.putString(BINDING_TARGET_MORPH_ID, initialTarget.toString());
                }
                tag.putBoolean(BINDING_ACTIVE, true);
            }

            ResourceLocation bindingTarget = parseMobId(CompatAccess.getString(tag, BINDING_TARGET_MORPH_ID));

            // Hard-enforce every tick while binding is active.
            if (bindingTarget != null) {
                if (forceMorph(player, bindingTarget)) {
                    tag.putString(LAST_MORPH_ID, bindingTarget.toString());
                }
            }

            return;
        }

        if (currentMorphId != null) {
            tag.putString(LAST_MORPH_ID, currentMorphId.toString());
        }

        if (tag.contains(BINDING_TARGET_MORPH_ID)) {
            tag.remove(BINDING_TARGET_MORPH_ID);
        }
        if (tag.contains(BINDING_ACTIVE)) {
            tag.remove(BINDING_ACTIVE);
        }
        if (!player.hasEffect(NaturalisMobEffects.BREWED_MORPH) && tag.contains(BREWED_MORPH_ID)) {
            tag.remove(BREWED_MORPH_ID);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItem();
        if (stack.getItem() instanceof BrewedMorphPotionItem) {
            boolean applied = applyBrewedMorphFromStack(player, stack, true);
            if (applied) {
                grantAdvancement(player, ADV_POTION_BREWED);
            }
            return;
        }

        if (stack.is(NaturalisItems.MORPH_BINDING_POTION.get())) {
            player.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING, BINDING_POTION_DURATION, 0, true, false, true));
            grantAdvancement(player, ADV_POTION_BINDING);
        }
    }

    @SubscribeEvent
    public static void onThrownPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion thrownPotion)) {
            return;
        }
        if (!(thrownPotion.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = thrownPotion.getItem();
        if (stack.isEmpty()) {
            return;
        }

        boolean isBrewedSplash = stack.is(NaturalisItems.BREWED_MORPH_SPLASH_POTION.get());
        boolean isBrewedLingering = stack.is(NaturalisItems.BREWED_MORPH_LINGERING_POTION.get());
        boolean isBindingSplash = stack.is(NaturalisItems.MORPH_BINDING_SPLASH_POTION.get());
        boolean isBindingLingering = stack.is(NaturalisItems.MORPH_BINDING_LINGERING_POTION.get());

        if (!isBrewedSplash && !isBrewedLingering && !isBindingSplash && !isBindingLingering) {
            return;
        }

        double radius = (isBrewedLingering || isBindingLingering) ? LINGERING_POTION_RADIUS : SPLASH_POTION_RADIUS;

        if (isBindingLingering || isBrewedLingering) {
            spawnCustomLingeringCloud(level, thrownPotion, stack, isBrewedLingering ? CLOUD_KIND_BREWED : CLOUD_KIND_BINDING);
        }

        if (isBindingSplash || isBindingLingering) {
            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                thrownPotion.getBoundingBox().inflate(radius),
                entity -> entity.isAlive())) {
                target.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING, BINDING_POTION_DURATION, 0, true, false, true));
            }
        }

        if (isBrewedSplash || isBrewedLingering) {
            ResourceLocation targetMorph = resolveBrewedMobId(stack);
            if (targetMorph == null || !isValidLivingMorph(targetMorph)) {
                return;
            }

            int brewedDuration = readBrewedDuration(stack);

            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                thrownPotion.getBoundingBox().inflate(radius),
                entity -> entity.isAlive())) {
                if (target instanceof ServerPlayer targetPlayer) {
                    applyBrewedMorph(targetPlayer, targetMorph, brewedDuration);
                } else if (canTransformMobTarget(target)) {
                    transformMobTarget((Mob) target, targetMorph);
                }
            }

            if (thrownPotion.getOwner() instanceof ServerPlayer owner) {
                grantAdvancement(owner, ADV_POTION_BREWED_SPLASH);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (now % 10 != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            Set<UUID> ids = CUSTOM_LINGERING_CLOUDS.get(level.dimension());
            if (ids == null || ids.isEmpty()) {
                continue;
            }

            Set<UUID> toRemove = new HashSet<>();
            for (UUID id : ids) {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof AreaEffectCloud cloud) || !cloud.isAlive()) {
                    toRemove.add(id);
                    continue;
                }

                CompoundTag cloudTag = CompatAccess.getPersistentData(cloud);
                String kind = CompatAccess.getString(cloudTag, CLOUD_KIND_TAG);
                if (kind.isBlank()) {
                    toRemove.add(id);
                    continue;
                }

                long lastApply = CompatAccess.getLong(cloudTag, CLOUD_LAST_APPLY_TICK_TAG);
                if (now - lastApply < 10) {
                    continue;
                }
                cloudTag.putLong(CLOUD_LAST_APPLY_TICK_TAG, now);

                if (CLOUD_KIND_BINDING.equals(kind)) {
                    applyBindingCloud(cloud, level);
                } else if (CLOUD_KIND_BREWED.equals(kind)) {
                    ResourceLocation morphId = parseMobId(CompatAccess.getString(cloudTag, CLOUD_MORPH_ID_TAG));
                    if (morphId != null && isValidLivingMorph(morphId)) {
                        applyBrewedCloud(cloud, level, morphId);
                    }
                }
            }

            ids.removeAll(toRemove);
            if (ids.isEmpty()) {
                CUSTOM_LINGERING_CLOUDS.remove(level.dimension());
            }
        }
    }

    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Easter egg only applies when the player is currently morphed.
        if (CurrentMorphUtil.getCurrentMorphId(player) == null) {
            return;
        }

        player.addEffect(new MobEffectInstance(
            NaturalisMobEffects.MORPH_BINDING,
            Integer.MAX_VALUE,
            0,
            true,
            false,
            true
        ));
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity attacker = resolveTransmutationAttacker(player, event);
        if (attacker == null) {
            return;
        }

        if (attacker instanceof Witch) {
            maybeGrantAndLaunchTransmutationPotion(player, attacker, WITCH_GIFT_POOL, WITCH_GIFT_CHANCE, WITCH_GIFT_COOLDOWN, 70 * 20);
        } else if (attacker instanceof Evoker) {
            maybeGrantAndLaunchTransmutationPotion(player, attacker, EVOKER_GIFT_POOL, EVOKER_GIFT_CHANCE, EVOKER_GIFT_COOLDOWN, 90 * 20);
        }
    }

    @SubscribeEvent
    public static void onWitchDamagedWithBrewedMorph(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Witch witch)) {
            return;
        }
        if (!(witch.level() instanceof ServerLevel level)) {
            return;
        }
        if (!witch.hasEffect(NaturalisMobEffects.BREWED_MORPH)) {
            return;
        }
        if (event.getNewDamage() <= 0.0F || !witch.isAlive()) {
            return;
        }

        CompoundTag tag = getOrCreateEffectTag(witch);
        ResourceLocation morphId = parseMobId(CompatAccess.getString(tag, BREWED_MORPH_ID));
        if (morphId == null || !isValidLivingMorph(morphId)) {
            return;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        Entity created = CompatAccess.createEntity(type, level);
        if (!(created instanceof LivingEntity transformed)) {
            return;
        }

        CompatAccess.moveEntity(transformed, witch.getX(), witch.getY(), witch.getZ(), witch.getYRot(), witch.getXRot());
        transformed.setCustomName(witch.getCustomName());
        transformed.setCustomNameVisible(witch.isCustomNameVisible());
        transformed.setHealth(Math.min(transformed.getMaxHealth(), Math.max(1.0F, witch.getHealth())));

        witch.discard();
        level.addFreshEntity(transformed);
    }

    private static Entity resolveTransmutationAttacker(ServerPlayer player, LivingDamageEvent.Post event) {
        Entity source = event.getSource().getEntity();
        if ((source instanceof Witch || source instanceof Evoker) && isCasterAggroOnPlayer(source, player)) {
            return source;
        }

        Entity direct = event.getSource().getDirectEntity();
        if ((direct instanceof Witch || direct instanceof Evoker) && isCasterAggroOnPlayer(direct, player)) {
            return direct;
        }

        if (direct instanceof EvokerFangs fangs
            && fangs.getOwner() instanceof Evoker evoker
            && isCasterAggroOnPlayer(evoker, player)) {
            return evoker;
        }

        if (direct instanceof Projectile projectile
            && projectile.getOwner() instanceof Witch witch
            && isCasterAggroOnPlayer(witch, player)) {
            return witch;
        }

        LivingEntity last = player.getLastHurtByMob();
        if ((last instanceof Witch || last instanceof Evoker)
            && last.isAlive()
            && last.distanceToSqr(player) <= 16.0D * 16.0D) {
            return last;
        }

        // Some magic damage paths do not preserve attacker ownership in the damage source.
        Entity nearbyAggroCaster = findNearbyAggroCaster(player);
        if (nearbyAggroCaster != null) {
            return nearbyAggroCaster;
        }

        return null;
    }

    private static boolean isCasterAggroOnPlayer(Entity caster, ServerPlayer player) {
        if (!(caster instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }

        if (living instanceof Witch witch) {
            return witch.getTarget() == player || witch.getLastHurtByMob() == player;
        }

        if (living instanceof Evoker evoker) {
            return evoker.getTarget() == player || evoker.getLastHurtByMob() == player;
        }

        return false;
    }

    private static Entity findNearbyAggroCaster(ServerPlayer player) {
        double maxRange = 24.0D;
        double bestDistance = maxRange * maxRange;
        Entity best = null;

        List<Witch> witches = player.level().getEntitiesOfClass(
            Witch.class,
            player.getBoundingBox().inflate(maxRange),
            witch -> witch.isAlive() && (witch.getTarget() == player || witch.getLastHurtByMob() == player)
        );

        for (Witch witch : witches) {
            double d = witch.distanceToSqr(player);
            if (d < bestDistance) {
                bestDistance = d;
                best = witch;
            }
        }

        List<Evoker> evokers = player.level().getEntitiesOfClass(
            Evoker.class,
            player.getBoundingBox().inflate(maxRange),
            evoker -> evoker.isAlive() && (evoker.getTarget() == player || evoker.getLastHurtByMob() == player)
        );

        for (Evoker evoker : evokers) {
            double d = evoker.distanceToSqr(player);
            if (d < bestDistance) {
                bestDistance = d;
                best = evoker;
            }
        }

        return best;
    }

    private static ResourceLocation enforceBrewedMorph(ServerPlayer player, CompoundTag tag, ResourceLocation currentMorphId) {
        ResourceLocation brewedMorph = parseMobId(CompatAccess.getString(tag, BREWED_MORPH_ID));
        if (brewedMorph == null) {
            return currentMorphId;
        }

        if (currentMorphId != null && currentMorphId.equals(brewedMorph)) {
            return currentMorphId;
        }

        if (!forceMorph(player, brewedMorph)) {
            return currentMorphId;
        }

        return brewedMorph;
    }

    private static CompoundTag getOrCreateEffectTag(ServerPlayer player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(root, ROOT_TAG);
    }

    private static CompoundTag getOrCreateEffectTag(LivingEntity entity) {
        CompoundTag root = CompatAccess.getPersistentData(entity);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(root, ROOT_TAG);
    }

    private static int readBrewedDuration(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (tag.contains("BrewedMorphDuration")) {
            return Math.max(MIN_BREWED_DURATION, CompatAccess.getInt(tag, "BrewedMorphDuration"));
        }
        if (tag.contains("MorphDuration")) {
            return Math.max(MIN_BREWED_DURATION, CompatAccess.getInt(tag, "MorphDuration"));
        }
        if (tag.contains("EffectDuration")) {
            return Math.max(MIN_BREWED_DURATION, CompatAccess.getInt(tag, "EffectDuration"));
        }

        return DEFAULT_BREWED_DURATION;
    }

    private static ResourceLocation resolveBrewedMobId(ItemStack stack) {
        ResourceLocation direct = MorphDataUtil.resolveMobId(stack);
        if (direct != null) {
            return direct;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        ResourceLocation brewed = parseMobId(CompatAccess.getString(tag, "BrewedMorphId"));
        if (brewed != null) {
            return brewed;
        }

        return parseMobId(CompatAccess.getString(tag, "TargetMorph"));
    }

    private static void maybeGrantAndLaunchTransmutationPotion(ServerPlayer player, Entity attacker, String[] pool, double chance, int cooldownTicks, int durationTicks) {
        if (pool.length == 0) {
            return;
        }

        long now = player.level().getGameTime();
        CompoundTag tag = getOrCreateEffectTag(player);
        long last = CompatAccess.getLong(tag, LAST_TRANSMUTATION_GIFT_TICK);
        if (now - last < cooldownTicks) {
            return;
        }

        if (player.getRandom().nextDouble() > chance) {
            return;
        }

        String selected = pool[player.getRandom().nextInt(pool.length)];
        ResourceLocation mobId = parseMobId(selected);
        if (mobId == null || !isValidLivingMorph(mobId)) {
            return;
        }

        // They also cast/launch the transmutation on hit, not only gift an item.
        // Keep this explicitly NBT-driven like Morph Binding: target id is always pinned in effect data.
        CompoundTag effectTag = getOrCreateEffectTag(player);
        effectTag.putString(BREWED_MORPH_ID, mobId.toString());
        player.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH, durationTicks, 0, false, true, true));
        forceMorph(player, mobId);

        if (attacker instanceof LivingEntity livingAttacker) {
            applyBrewedMorphToMob(livingAttacker, mobId, durationTicks);
        }

        tag.putLong(LAST_TRANSMUTATION_GIFT_TICK, now);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.naturalis.brewed_morph_potion.cast", mobId.toString()), true);
    }

    private static void applyBrewedMorphToMob(LivingEntity entity, ResourceLocation morphId, int durationTicks) {
        if (entity == null || morphId == null || durationTicks < MIN_BREWED_DURATION) {
            return;
        }
        if (!isValidLivingMorph(morphId)) {
            return;
        }

        CompoundTag effectTag = getOrCreateEffectTag(entity);
        effectTag.putString(BREWED_MORPH_ID, morphId.toString());
        entity.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH, durationTicks, 0, false, true, true));
    }

    private static boolean canTransformMobTarget(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        if (mob instanceof Witch || mob instanceof Evoker || mob instanceof Ravager || mob instanceof Warden
            || mob instanceof EnderDragon || mob instanceof WitherBoss) {
            return false;
        }
        if (mob.getType() == EntityType.ELDER_GUARDIAN) {
            return false;
        }

        // Treat high-health hostiles as mini-bosses and keep them immune.
        if (mob.getMaxHealth() >= 80.0F && mob.getType().getCategory() == MobCategory.MONSTER) {
            return false;
        }

        return true;
    }

    private static void transformMobTarget(Mob source, ResourceLocation morphId) {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return;
        }

        Entity created = CompatAccess.createEntity(type, level);
        if (!(created instanceof LivingEntity transformed)) {
            return;
        }

        CompatAccess.moveEntity(transformed, source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        transformed.setCustomName(source.getCustomName());
        transformed.setCustomNameVisible(source.isCustomNameVisible());
        if (source instanceof Mob sourceMob && transformed instanceof Mob transformedMob) {
            transformedMob.setNoAi(sourceMob.isNoAi());
        }

        float ratio = source.getMaxHealth() <= 0.0F ? 1.0F : source.getHealth() / source.getMaxHealth();
        float targetHealth = Math.max(1.0F, transformed.getMaxHealth() * Math.max(0.1F, Math.min(1.0F, ratio)));
        transformed.setHealth(Math.min(transformed.getMaxHealth(), targetHealth));

        source.discard();
        level.addFreshEntity(transformed);
    }

    private static void spawnCustomLingeringCloud(ServerLevel level, ThrownPotion thrownPotion, ItemStack stack, String kind) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, thrownPotion.getX(), thrownPotion.getY(), thrownPotion.getZ());
        cloud.setRadius((float) LINGERING_POTION_RADIUS);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
        cloud.setWaitTime(0);

        CompoundTag cloudTag = CompatAccess.getPersistentData(cloud);
        cloudTag.putString(CLOUD_KIND_TAG, kind);
        cloudTag.putLong(CLOUD_LAST_APPLY_TICK_TAG, 0L);

        if (CLOUD_KIND_BREWED.equals(kind)) {
            ResourceLocation morphId = resolveBrewedMobId(stack);
            if (morphId != null) {
                cloudTag.putString(CLOUD_MORPH_ID_TAG, morphId.toString());
                cloudTag.putInt(CLOUD_DURATION_TAG, readBrewedDuration(stack));
            }
        }

        if (level.addFreshEntity(cloud)) {
            CUSTOM_LINGERING_CLOUDS
                .computeIfAbsent(level.dimension(), key -> new HashSet<>())
                .add(cloud.getUUID());
        }
    }

    private static void applyBindingCloud(AreaEffectCloud cloud, ServerLevel level) {
        float radius = cloud.getRadius();
        for (LivingEntity target : level.getEntitiesOfClass(
            LivingEntity.class,
            cloud.getBoundingBox().inflate(radius),
            entity -> entity.isAlive() && entity.distanceToSqr(cloud) <= radius * radius)) {
            target.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING, 60, 0, true, false, true));
        }
    }

    private static void applyBrewedCloud(AreaEffectCloud cloud, ServerLevel level, ResourceLocation morphId) {
        CompoundTag cloudTag = CompatAccess.getPersistentData(cloud);
        int durationTicks = Math.max(MIN_BREWED_DURATION, CompatAccess.getInt(cloudTag, CLOUD_DURATION_TAG));
        float radius = cloud.getRadius();
        for (LivingEntity target : level.getEntitiesOfClass(
            LivingEntity.class,
            cloud.getBoundingBox().inflate(radius),
            entity -> entity.isAlive() && entity.distanceToSqr(cloud) <= radius * radius)) {
            if (target instanceof ServerPlayer player) {
                applyBrewedMorph(player, morphId, durationTicks);
            } else if (canTransformMobTarget(target)) {
                transformMobTarget((Mob) target, morphId);
            }
        }
    }

    private static ResourceLocation parseMobId(String rawMobId) {
        if (rawMobId == null || rawMobId.isBlank()) {
            return null;
        }

        String normalized = rawMobId.trim().toLowerCase(Locale.ROOT);
        if ("daulphin".equals(normalized)) {
            normalized = "dolphin";
        }

        ResourceLocation parsed = ResourceLocation.tryParse(normalized);
        if (parsed != null && BuiltInRegistries.ENTITY_TYPE.containsKey(parsed)) {
            return parsed;
        }

        ResourceLocation fallback = ResourceLocation.tryParse("minecraft:" + normalized);
        if (fallback != null && BuiltInRegistries.ENTITY_TYPE.containsKey(fallback)) {
            return fallback;
        }

        return null;
    }

    private static boolean isValidLivingMorph(ResourceLocation morphId) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(morphId)) {
            return false;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return false;
        }
        if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
            return true;
        }

        // Fallback for environments where getBaseClass is too generic.
        return type.getCategory() != MobCategory.MISC;
    }

    @SuppressWarnings("unchecked")
    private static boolean forceMorph(ServerPlayer player, ResourceLocation morphId) {
        if (!isValidLivingMorph(morphId)) {
            return false;
        }

        LivingEntity currentShape = PlayerShape.getCurrentShape(player);
        if (currentShape != null) {
            ResourceLocation currentShapeId = BuiltInRegistries.ENTITY_TYPE.getKey(currentShape.getType());
            if (morphId.equals(currentShapeId)) {
                CompoundTag stored = MorphShapeUtil.getForPlayer(player, morphId);
                if (!stored.isEmpty() && !MorphShapeUtil.matches(currentShape, stored)) {
                    MorphShapeUtil.applyToEntity(currentShape, stored);
                    PlayerShape.sync(player);
                    return true;
                }
                return false;
            }
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return false;
        }
        ShapeType<? extends LivingEntity> shapeType =
            ShapeType.from((EntityType<? extends LivingEntity>) type);

        if (shapeType == null) {
            return false;
        }

        // Always pin the target as the player's 2nd shape while bound.
        PlayerDataProvider provider = (PlayerDataProvider) player;
        provider.walkers$set2ndShape(shapeType);
        PlayerShapeChanger.sync(player);
        PlayerAbilities.sync(player);

        // If player is currently human, remorph via the same swap path used by G key:
        // updateShapes(player, provider.walkers$get2ndShape().create(...)).
        if (PlayerShape.getCurrentShape(player) == null) {
            ShapeType<?> secondShape = provider.walkers$get2ndShape();
            if (secondShape == null) {
                return false;
            }

            LivingEntity created = secondShape.create(player.level(), player);
            if (created == null) {
                return false;
            }

            MorphShapeUtil.applyStoredShape(player, created, morphId);

            boolean swapped = PlayerShape.updateShapes(player, created);
            player.refreshDimensions();
            if (swapped) {
                PlayerShape.sync(player);
            }
            return swapped;
        }

        LivingEntity shape = shapeType.create(player.level(), player);
        if (shape == null) {
            return false;
        }

        MorphShapeUtil.applyStoredShape(player, shape, morphId);

        // While already morphed, hard-force the bound target.
        provider.walkers$set2ndShape(shapeType);
        provider.walkers$updateShapes(shape);
        PlayerShape.sync(player);
        player.refreshDimensions();
        return true;
    }

    private static ResourceLocation getBindingTarget(ServerPlayer player) {
        CompoundTag tag = getOrCreateEffectTag(player);
        return parseMobId(CompatAccess.getString(tag, BINDING_TARGET_MORPH_ID));
    }

    public static boolean forceHuman(ServerPlayer player) {
        return MorphAcquisition.forceHuman(player);
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        var server = CompatAccess.getServer(player);
        if (server == null) {
            return;
        }

        AdvancementHolder root = server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "root"));
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        AdvancementHolder advancement = server.getAdvancements().get(id);
        if (advancement == null) {
            return;
        }

        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : advancement.value().criteria().keySet()) {
            if (!progress.isDone()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }
}