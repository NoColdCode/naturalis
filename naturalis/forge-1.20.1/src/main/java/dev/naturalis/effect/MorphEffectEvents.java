package dev.naturalis.effect;

import dev.naturalis.Naturalis;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.morph.quickslot.MorphQuickSlotServerSession;
import dev.naturalis.item.BrewedMorphPotionItem;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.util.MorphShapeUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.TickEvent;
import tocraft.walkers.api.PlayerAbilities;
import tocraft.walkers.api.PlayerShape;
import tocraft.walkers.api.PlayerShapeChanger;
import tocraft.walkers.api.events.ShapeEvents;
import tocraft.walkers.api.variant.ShapeType;
import tocraft.walkers.impl.PlayerDataProvider;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class MorphEffectEvents {

    private static final String ROOT_TAG = "naturalis_effects";
    private static final String LAST_MORPH_ID = "last_morph_id";
    private static final String BINDING_TARGET_MORPH_ID = "binding_target_morph_id";
    private static final String BINDING_ACTIVE = "binding_active";
    private static final String BREWED_MORPH_ID = "brewed_morph_id";
    private static final String LAST_TRANSMUTATION_GIFT_TICK = "last_transmutation_gift_tick";

    private static final int DEFAULT_BREWED_DURATION = 20 * 60;
    private static final int MIN_BREWED_DURATION = 20;
    private static final int BINDING_POTION_DURATION = 8 * 20 * 60;
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
        if (player == null || morphId == null || durationTicks < MIN_BREWED_DURATION) {
            return false;
        }

        if (!isValidLivingMorph(morphId)) {
            return false;
        }

        CompoundTag effectTag = getOrCreateEffectTag(player);
        // Brewed morph has priority: binding must be removed instantly to avoid state fights.
        if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get())) {
            player.removeEffect(NaturalisMobEffects.MORPH_BINDING.get());
        }
        effectTag.remove(BINDING_TARGET_MORPH_ID);
        effectTag.remove(BINDING_ACTIVE);
        effectTag.putString(BREWED_MORPH_ID, morphId.toString());

        player.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH.get(), durationTicks, 0, false, true, true));
        return forceMorph(player, morphId);
    }

    /**
     * Applies a boss-ring morph effect to a player — pins the morph via BREWED_MORPH_ID and
     * adds the BREWED_MORPH effect so MorphEffectEvents enforces it each tick.
     */
    public static void applyBossRingMorphEffect(ServerPlayer player, ResourceLocation morphId, int durationTicks) {
        if (!isValidLivingMorph(morphId)) {
            return;
        }
        CompoundTag effectTag = getOrCreateEffectTag(player);
        effectTag.putString(BREWED_MORPH_ID, morphId.toString());
        player.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH.get(), durationTicks, 0, false, true, true));
        forceMorph(player, morphId);
    }

    /**
     * Removes the beacon-applied BREWED_MORPH effect and reverts the player to human form.
     * Called when a player leaves a MorphBeacon's range.
     */
    public static void clearBeaconMorph(ServerPlayer player) {
        player.removeEffect(NaturalisMobEffects.BREWED_MORPH.get());
        CompoundTag tag = getOrCreateEffectTag(player);
        tag.remove(BREWED_MORPH_ID);
        forceHuman(player);
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
            if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get())) {
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
            boolean stormAttuned = player.hasEffect(NaturalisMobEffects.STORM_ATTUNEMENT.get());

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

            if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get()) && !stormAttuned) {
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag tag = getOrCreateEffectTag(player);
        ResourceLocation currentMorphId = CurrentMorphUtil.getCurrentMorphId(player);
        MorphShapeUtil.enforceCurrentShape(player);

        if (player.hasEffect(NaturalisMobEffects.BREWED_MORPH.get())) {
            if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get())) {
                player.removeEffect(NaturalisMobEffects.MORPH_BINDING.get());
                tag.remove(BINDING_TARGET_MORPH_ID);
                tag.remove(BINDING_ACTIVE);
            }
            currentMorphId = enforceBrewedMorph(player, tag, currentMorphId);
        }

        if (player.hasEffect(NaturalisMobEffects.MORPH_BINDING.get()) && NaturalisConfig.morphBindingEnabled()) {
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
        if (!player.hasEffect(NaturalisMobEffects.BREWED_MORPH.get()) && tag.contains(BREWED_MORPH_ID)) {
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
            applyBrewedMorphFromStack(player, stack, true);
            return;
        }

        if (stack.is(NaturalisItems.MORPH_BINDING_POTION.get())) {
            player.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING.get(), BINDING_POTION_DURATION, 0, true, false, true));
            return;
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
            NaturalisMobEffects.MORPH_BINDING.get(),
            Integer.MAX_VALUE,
            0,
            true,
            false,
            true
        ));
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent event) {
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
    public static void onWitchDamagedWithBrewedMorph(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Witch witch)) {
            return;
        }
        if (!(witch.level() instanceof ServerLevel level)) {
            return;
        }
        if (!witch.hasEffect(NaturalisMobEffects.BREWED_MORPH.get())) {
            return;
        }
        if (event.getAmount() <= 0.0F || !witch.isAlive()) {
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

    private static Entity resolveTransmutationAttacker(ServerPlayer player, LivingDamageEvent event) {
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
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(root, ROOT_TAG);
    }

    private static CompoundTag getOrCreateEffectTag(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(root, ROOT_TAG);
    }

    private static int readBrewedDuration(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return DEFAULT_BREWED_DURATION;

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

        CompoundTag tag = stack.getTag();
        if (tag == null) return null;

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
        player.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH.get(), durationTicks, 0, false, true, true));
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
        entity.addEffect(new MobEffectInstance(NaturalisMobEffects.BREWED_MORPH.get(), durationTicks, 0, false, true, true));
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
        if (PlayerShape.getCurrentShape(player) == null) {
            return true;
        }

        try {
            boolean swapped = PlayerShape.updateShapes(player, null);
            if (!swapped) {
                PlayerDataProvider provider = (PlayerDataProvider) player;
                provider.walkers$updateShapes(null);
                swapped = true;
            }
            player.refreshDimensions();
            PlayerShape.sync(player);
            return swapped;
        } catch (Throwable ignored) {
            return false;
        }
    }
}