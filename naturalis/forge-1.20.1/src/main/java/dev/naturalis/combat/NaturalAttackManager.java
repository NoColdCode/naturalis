package dev.naturalis.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NaturalAttackManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ROOT_TAG = "naturalis_natural_attack";
    private static final String COOLDOWN_UNTIL_TAG = "cooldown_until";

    private static volatile boolean loaded;
    private static final Map<ResourceLocation, NaturalAttackConfig> CONFIGS = new HashMap<>();

    private NaturalAttackManager() {
    }

    public static boolean tryUseNaturalAttack(ServerPlayer player, LivingEntity target, ResourceLocation morphId, float fallbackDamage) {
        ensureLoaded();

        long gameTime = player.level().getGameTime();
        int remaining = getRemainingCooldownTicks(player, gameTime);
        if (remaining > 0) {
            if (player.tickCount % 10 == 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.naturalis.natural_attack.cooldown",
                    String.format(Locale.ROOT, "%.1f", remaining / 20.0D)), true);
            }
            return true;
        }

        NaturalAttackConfig cfg = resolveConfig(morphId, fallbackDamage);
        boolean performed = executeAttack(player, target, cfg);
        if (!performed) {
            return false;
        }

        setCooldown(player, gameTime + cfg.cooldown());
        return cfg.replaceNormalAttack();
    }

    public static NaturalAttackConfig getConfig(ResourceLocation morphId, float fallbackDamage) {
        ensureLoaded();
        return resolveConfig(morphId, fallbackDamage);
    }

    public static int getRemainingCooldownTicks(ServerPlayer player, long gameTime) {
        long until = CompatAccess.getLong(CompatAccess.getCompound(player.getPersistentData(), ROOT_TAG), COOLDOWN_UNTIL_TAG);
        return (int) Math.max(0L, until - gameTime);
    }

    public static void reload() {
        loaded = false;
        ensureLoaded();
    }

    private static void setCooldown(ServerPlayer player, long until) {
        var root = player.getPersistentData();
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        var tag = CompatAccess.getCompound(root, ROOT_TAG);
        tag.putLong(COOLDOWN_UNTIL_TAG, until);
        root.put(ROOT_TAG, tag);
    }

    private static boolean executeAttack(ServerPlayer player, LivingEntity target, NaturalAttackConfig cfg) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        Vec3 look = player.getLookAngle();
        switch (cfg.type()) {
            case FIREBALL -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                // 1.20.1: LargeFireball(Level, LivingEntity, double xPower, double yPower, double zPower, int explosionPower)
                Vec3 power = look.scale(cfg.velocity());
                LargeFireball fireball = new LargeFireball(level, player,
                    power.x, power.y, power.z,
                    Math.max(1, (int) Math.round(cfg.damage() / 3.0D)));
                fireball.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
                fireball.setOwner(player);
                level.addFreshEntity(fireball);
                level.playSound(null, player, SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
            case SMALL_FIREBALL -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                // 1.20.1: SmallFireball(Level, LivingEntity, double xPower, double yPower, double zPower)
                Vec3 sPower = look.scale(cfg.velocity());
                SmallFireball fireball = new SmallFireball(level, player, sPower.x, sPower.y, sPower.z);
                fireball.setPos(player.getX(), player.getEyeY(), player.getZ());
                fireball.setOwner(player);
                level.addFreshEntity(fireball);
                level.playSound(null, player, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.1F);
                return true;
            }
            case WITHER_SKULL -> {
                Entity raw = createEntity(level, new ResourceLocation("minecraft", "wither_skull"));
                if (!(raw instanceof Projectile projectile)) {
                    return false;
                }

                projectile.setOwner(player);
                raw.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
                raw.setYRot(player.getYRot());
                raw.setXRot(player.getXRot());
                projectile.shoot(look.x, look.y, look.z, (float) Math.max(0.8D, cfg.velocity()), 1.0F);
                if (raw instanceof AbstractHurtingProjectile hurting) {
                    hurting.setDeltaMovement(look.normalize().scale(Math.max(0.35D, cfg.velocity() * 0.24D)));
                }
                level.addFreshEntity(raw);
                level.playSound(null, player, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 0.95F);
                return true;
            }
            case ARROW -> {
                ArrowItem arrowItem = (ArrowItem) Items.ARROW;
                // 1.20.1: createArrow(Level, ItemStack, LivingEntity) — no weapon param
                AbstractArrow arrow = arrowItem.createArrow(level, new ItemStack(Items.ARROW), player);
                arrow.setBaseDamage(cfg.damage());
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) cfg.velocity(), 1.0F);
                level.addFreshEntity(arrow);
                level.playSound(null, player, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F + ((player.getRandom().nextFloat() - 0.5F) * 0.12F));
                return true;
            }
            case DRAGON_BREATH_ZONE -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                Vec3 center = target.position();

                Entity raw = createEntity(level, new ResourceLocation("minecraft", "area_effect_cloud"));
                if (raw instanceof AreaEffectCloud cloud) {
                    cloud.setPos(center.x, center.y + 0.1D, center.z);
                    cloud.setOwner(player);
                    cloud.setRadius((float) Math.max(2.5D, cfg.range() * 0.45D));
                    cloud.setDuration(80);
                    cloud.setWaitTime(2);
                    level.addFreshEntity(cloud);
                }

                level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y + 0.2D, center.z, 45, 1.8D, 0.35D, 1.8D, 0.03D);

                for (LivingEntity victim : level.getEntitiesOfClass(
                    LivingEntity.class,
                    target.getBoundingBox().inflate(Math.max(2.4D, cfg.range() * 0.45D)),
                    e -> e.isAlive() && e != player
                )) {
                    victim.hurt(level.damageSources().playerAttack(player), (float) Math.max(2.0D, cfg.damage() * 0.55D));
                }

                level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
                return true;
            }
            case GUARDIAN_BEAM -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                Vec3 from = player.getEyePosition();
                Vec3 to = target.getEyePosition();
                Vec3 dir = to.subtract(from);
                if (dir.lengthSqr() <= 1.0E-5D || player.distanceTo(target) > cfg.range()) {
                    return false;
                }

                Vec3 step = dir.normalize().scale(0.55D);
                Vec3 p = from;
                int hops = Math.min(42, Math.max(8, (int) (dir.length() / 0.55D)));
                for (int i = 0; i < hops; i++) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                    p = p.add(step);
                }

                target.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                level.playSound(null, player, SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
            case DASH_BITE -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                Vec3 dash = new Vec3(look.x, 0.0D, look.z).normalize().scale(cfg.velocity());
                player.setDeltaMovement(player.getDeltaMovement().add(dash.x, 0.08D, dash.z));
                player.hurtMarked = true;

                if (player.distanceTo(target) <= cfg.range() + 0.5D) {
                    target.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                    target.knockback(0.45D, player.getX() - target.getX(), player.getZ() - target.getZ());
                }

                level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.8D, player.getZ(), 10, 0.25D, 0.2D, 0.25D, 0.02D);
                return true;
            }
            case LEAP_ATTACK -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                double leapDamage = cfg.damage() > 0.0D ? cfg.damage() : 3.0D;
                double leapRange = cfg.range() > 0.0D ? cfg.range() : 2.4D;

                if (player.distanceTo(target) <= leapRange) {
                    target.hurt(level.damageSources().playerAttack(player), (float) leapDamage);
                    target.knockback(0.35D, player.getX() - target.getX(), player.getZ() - target.getZ());
                }

                Vec3 leap = new Vec3(look.x, 0.0D, look.z).normalize().scale(cfg.velocity());
                player.setDeltaMovement(player.getDeltaMovement().add(leap.x, 0.42D, leap.z));
                player.hurtMarked = true;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.6D, player.getZ(), 10, 0.3D, 0.2D, 0.3D, 0.02D);
                return true;
            }
            case MELEE_ENHANCED -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                if (player.distanceTo(target) > cfg.range()) {
                    return false;
                }
                target.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                target.knockback(0.8D, player.getX() - target.getX(), player.getZ() - target.getZ());
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8D, target.getZ(), 12, 0.25D, 0.35D, 0.25D, 0.05D);
                return true;
            }
            case POISON_BITE -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                if (player.distanceTo(target) > cfg.range()) {
                    return false;
                }
                target.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                target.knockback(0.35D, player.getX() - target.getX(), player.getZ() - target.getZ());
                level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 0.7D, target.getZ(), 14, 0.2D, 0.2D, 0.2D, 0.02D);
                level.playSound(null, player, SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.4F);
                return true;
            }
            case FREEZE_ZONE -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                Vec3 center = target.position();
                double radius = Math.max(2.2D, cfg.range() * 0.4D);
                for (LivingEntity victim : level.getEntitiesOfClass(
                    LivingEntity.class,
                    target.getBoundingBox().inflate(radius),
                    e -> e.isAlive() && e != player
                )) {
                    victim.hurt(level.damageSources().playerAttack(player), (float) Math.max(2.0D, cfg.damage() * 0.7D));
                    victim.setTicksFrozen(Math.min(200, victim.getTicksFrozen() + 80));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.4D, center.z, 50, radius * 0.6D, 0.4D, radius * 0.6D, 0.02D);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.85F);
                return true;
            }
            case LIGHTNING_STRIKE -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                if (player.distanceTo(target) > cfg.range()) {
                    return false;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setCause(player);
                    level.addFreshEntity(bolt);
                }
                target.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                level.playSound(null, target, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.2F);
                return true;
            }
            case CHARGE -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                Vec3 dash = new Vec3(look.x, 0.0D, look.z).normalize().scale(Math.max(1.1D, cfg.velocity()));
                player.setDeltaMovement(player.getDeltaMovement().add(dash.x, 0.12D, dash.z));
                player.hurtMarked = true;
                if (player.distanceTo(target) <= cfg.range() + 1.0D) {
                    target.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                    target.knockback(1.2D, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3D, player.getZ(), 16, 0.4D, 0.1D, 0.4D, 0.02D);
                level.playSound(null, player, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.7F, 1.15F);
                return true;
            }
            case SONIC_BLAST -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                double radius = Math.max(3.0D, cfg.range());
                for (LivingEntity victim : level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(radius),
                    e -> e.isAlive() && e != player
                )) {
                    if (player.distanceTo(victim) > radius) {
                        continue;
                    }
                    victim.hurt(level.damageSources().playerAttack(player), (float) cfg.damage());
                    victim.knockback(0.9D, victim.getX() - player.getX(), victim.getZ() - player.getZ());
                }
                level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getEyeY(), player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.playSound(null, player, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.1F);
                return true;
            }
        }
        return false;
    }

    private static Entity createEntity(ServerLevel level, ResourceLocation entityId) {
        var type = CompatAccess.getEntityType(entityId);
        return type != null ? CompatAccess.createEntity(type, level) : null;
    }

    private static NaturalAttackConfig resolveConfig(ResourceLocation morphId, float fallbackDamage) {
        NaturalAttackConfig cfg = CONFIGS.get(morphId);
        if (cfg != null) {
            return cfg;
        }
        return new NaturalAttackConfig(AttackType.MELEE_ENHANCED, fallbackDamage, 2.8D, 0.5D, 10, true);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        synchronized (NaturalAttackManager.class) {
            if (loaded) {
                return;
            }
            CONFIGS.clear();
            CONFIGS.putAll(defaultConfigs());
            loadConfigOverrides();
            loaded = true;
        }
    }

    private static void loadConfigOverrides() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("naturalis").resolve("natural_attacks.json");
        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                Files.writeString(configPath, createDefaultConfigJson(), StandardCharsets.UTF_8);
                return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("capacities") || !root.get("capacities").isJsonObject()) {
                return;
            }

            JsonObject capacities = root.getAsJsonObject("capacities");
            for (Map.Entry<String, JsonElement> entry : capacities.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject capObj = entry.getValue().getAsJsonObject();
                if (!capObj.has("natural_attack") || !capObj.get("natural_attack").isJsonObject()) {
                    continue;
                }

                ResourceLocation morphId = ResourceLocation.tryParse(entry.getKey());
                if (morphId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(morphId)) {
                    continue;
                }

                NaturalAttackConfig parsed = parseAttack(capObj.getAsJsonObject("natural_attack"));
                if (parsed != null) {
                    CONFIGS.put(morphId, parsed);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static NaturalAttackConfig parseAttack(JsonObject obj) {
        if (!obj.has("type")) {
            return null;
        }

        AttackType type = AttackType.fromId(obj.get("type").getAsString());
        if (type == null) {
            return null;
        }

        double damage = obj.has("damage") ? obj.get("damage").getAsDouble() : type.defaultDamage;
        double range = obj.has("range") ? obj.get("range").getAsDouble() : type.defaultRange;
        double velocity = obj.has("velocity") ? obj.get("velocity").getAsDouble() : type.defaultVelocity;
        int cooldown = obj.has("cooldown") ? obj.get("cooldown").getAsInt() : type.defaultCooldown;
        boolean replace = !obj.has("replace_normal_attack") || obj.get("replace_normal_attack").getAsBoolean();

        return new NaturalAttackConfig(type, damage, range, velocity, Math.max(1, cooldown), replace);
    }

    private static String createDefaultConfigJson() {
        JsonObject root = new JsonObject();
        JsonObject capacities = new JsonObject();

        for (Map.Entry<ResourceLocation, NaturalAttackConfig> entry : defaultConfigs().entrySet()) {
            JsonObject morphObj = new JsonObject();
            morphObj.add("natural_attack", entry.getValue().toJson());
            capacities.add(entry.getKey().toString(), morphObj);
        }

        root.add("capacities", capacities);
        return GSON.toJson(root);
    }

    private static Map<ResourceLocation, NaturalAttackConfig> defaultConfigs() {
        Map<ResourceLocation, NaturalAttackConfig> defaults = new HashMap<>();

        defaults.put(new ResourceLocation("minecraft", "ghast"), new NaturalAttackConfig(AttackType.FIREBALL, 9.5D, 22.0D, 0.14D, 36, true));
        defaults.put(new ResourceLocation("minecraft", "wither"), new NaturalAttackConfig(AttackType.WITHER_SKULL, 7.0D, 24.0D, 1.35D, 36, true));
        defaults.put(new ResourceLocation("minecraft", "blaze"), new NaturalAttackConfig(AttackType.SMALL_FIREBALL, 5.0D, 20.0D, 0.15D, 30, true));
        defaults.put(new ResourceLocation("minecraft", "ender_dragon"), new NaturalAttackConfig(AttackType.DRAGON_BREATH_ZONE, 9.0D, 7.0D, 0.0D, 55, true));
        defaults.put(new ResourceLocation("minecraft", "guardian"), new NaturalAttackConfig(AttackType.GUARDIAN_BEAM, 5.0D, 18.0D, 0.0D, 32, true));
        defaults.put(new ResourceLocation("minecraft", "elder_guardian"), new NaturalAttackConfig(AttackType.GUARDIAN_BEAM, 8.0D, 24.0D, 0.0D, 36, true));
        defaults.put(new ResourceLocation("minecraft", "evoker"), new NaturalAttackConfig(AttackType.MELEE_ENHANCED, 6.0D, 4.5D, 0.0D, 18, true));
        defaults.put(new ResourceLocation("minecraft", "shulker"), new NaturalAttackConfig(AttackType.MELEE_ENHANCED, 4.0D, 5.0D, 0.0D, 20, true));

        defaults.put(new ResourceLocation("minecraft", "skeleton"), new NaturalAttackConfig(AttackType.ARROW, 2.5D, 20.0D, 3.0D, 20, true));
        defaults.put(new ResourceLocation("minecraft", "stray"), new NaturalAttackConfig(AttackType.ARROW, 2.5D, 20.0D, 3.0D, 20, true));
        defaults.put(new ResourceLocation("minecraft", "wither_skeleton"), new NaturalAttackConfig(AttackType.ARROW, 3.0D, 20.0D, 3.0D, 18, true));
        defaults.put(new ResourceLocation("minecraft", "pillager"), new NaturalAttackConfig(AttackType.ARROW, 2.5D, 20.0D, 3.0D, 20, true));

        defaults.put(new ResourceLocation("minecraft", "wolf"), new NaturalAttackConfig(AttackType.DASH_BITE, 4.0D, 2.5D, 0.8D, 15, true));
        defaults.put(new ResourceLocation("minecraft", "fox"), new NaturalAttackConfig(AttackType.DASH_BITE, 4.0D, 2.5D, 0.8D, 15, true));

        defaults.put(new ResourceLocation("minecraft", "spider"), new NaturalAttackConfig(AttackType.LEAP_ATTACK, 3.5D, 2.6D, 0.5D, 25, true));
        defaults.put(new ResourceLocation("minecraft", "cave_spider"), new NaturalAttackConfig(AttackType.LEAP_ATTACK, 3.0D, 2.4D, 0.5D, 25, true));
        defaults.put(new ResourceLocation("minecraft", "rabbit"), new NaturalAttackConfig(AttackType.LEAP_ATTACK, 2.0D, 2.2D, 0.5D, 25, true));

        defaults.put(new ResourceLocation("minecraft", "enderman"), new NaturalAttackConfig(AttackType.MELEE_ENHANCED, 7.0D, 4.0D, 0.0D, 10, true));
        defaults.put(new ResourceLocation("minecraft", "iron_golem"), new NaturalAttackConfig(AttackType.MELEE_ENHANCED, 7.0D, 4.0D, 0.0D, 10, true));
        defaults.put(new ResourceLocation("minecraft", "zombie"), new NaturalAttackConfig(AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 10, true));

        putModNaturalAttacks(defaults);
        return defaults;
    }

    private static void putModNaturalAttacks(Map<ResourceLocation, NaturalAttackConfig> defaults) {
        // Ice and Fire — signature breath / gaze / charge attacks
        put(defaults, "iceandfire:fire_dragon", AttackType.FIREBALL, 12.0D, 24.0D, 0.16D, 40);
        put(defaults, "iceandfire:ice_dragon", AttackType.FREEZE_ZONE, 11.0D, 8.0D, 0.0D, 42);
        put(defaults, "iceandfire:lightning_dragon", AttackType.LIGHTNING_STRIKE, 12.0D, 20.0D, 0.0D, 48);
        put(defaults, "iceandfire:black_frost_dragon", AttackType.FREEZE_ZONE, 13.0D, 9.0D, 0.0D, 44);
        put(defaults, "iceandfire:amphithere", AttackType.DASH_BITE, 6.0D, 3.0D, 1.0D, 16);
        put(defaults, "iceandfire:hippogryph", AttackType.LEAP_ATTACK, 7.0D, 3.2D, 0.7D, 22);
        put(defaults, "iceandfire:cockatrice", AttackType.GUARDIAN_BEAM, 6.0D, 14.0D, 0.0D, 36);
        put(defaults, "iceandfire:cyclops", AttackType.MELEE_ENHANCED, 14.0D, 5.0D, 0.0D, 18);
        put(defaults, "iceandfire:gorgon", AttackType.GUARDIAN_BEAM, 8.0D, 12.0D, 0.0D, 40);
        put(defaults, "iceandfire:hydra", AttackType.POISON_BITE, 9.0D, 4.0D, 0.0D, 20);
        put(defaults, "iceandfire:deathworm", AttackType.CHARGE, 10.0D, 4.0D, 1.5D, 30);
        put(defaults, "iceandfire:sea_serpent", AttackType.DASH_BITE, 11.0D, 4.5D, 1.1D, 18);
        put(defaults, "iceandfire:siren", AttackType.SONIC_BLAST, 5.0D, 6.0D, 0.0D, 45);
        put(defaults, "iceandfire:stymphalian_bird", AttackType.ARROW, 4.0D, 18.0D, 2.8D, 16);
        put(defaults, "iceandfire:troll", AttackType.MELEE_ENHANCED, 10.0D, 4.0D, 0.0D, 14);
        put(defaults, "iceandfire:pixie", AttackType.GUARDIAN_BEAM, 3.0D, 10.0D, 0.0D, 22);
        put(defaults, "iceandfire:ghost", AttackType.MELEE_ENHANCED, 6.0D, 3.5D, 0.0D, 12);
        put(defaults, "iceandfire:dread_lich", AttackType.WITHER_SKULL, 8.0D, 20.0D, 1.2D, 34);
        put(defaults, "iceandfire:dread_thrall", AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 12);
        put(defaults, "iceandfire:dread_ghoul", AttackType.DASH_BITE, 6.5D, 2.8D, 0.9D, 14);
        put(defaults, "iceandfire:dread_beast", AttackType.DASH_BITE, 8.0D, 3.0D, 1.0D, 14);
        put(defaults, "iceandfire:dread_scuttler", AttackType.LEAP_ATTACK, 5.0D, 2.6D, 0.6D, 20);
        put(defaults, "iceandfire:dread_knight", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "iceandfire:dread_queen", AttackType.WITHER_SKULL, 10.0D, 22.0D, 1.3D, 32);

        // Naturalist — animal signature attacks
        put(defaults, "naturalist:alligator", AttackType.DASH_BITE, 7.0D, 3.0D, 0.9D, 16);
        put(defaults, "naturalist:bear", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "naturalist:boar", AttackType.CHARGE, 6.5D, 3.2D, 1.3D, 24);
        put(defaults, "naturalist:coral_snake", AttackType.POISON_BITE, 4.0D, 2.4D, 0.0D, 18);
        put(defaults, "naturalist:deer", AttackType.CHARGE, 4.0D, 3.0D, 1.2D, 22);
        put(defaults, "naturalist:elephant", AttackType.CHARGE, 12.0D, 4.5D, 1.5D, 32);
        put(defaults, "naturalist:giraffe", AttackType.MELEE_ENHANCED, 6.0D, 4.0D, 0.0D, 16);
        put(defaults, "naturalist:hippo", AttackType.DASH_BITE, 10.0D, 3.5D, 1.0D, 18);
        put(defaults, "naturalist:lion", AttackType.DASH_BITE, 9.0D, 3.2D, 1.1D, 14);
        put(defaults, "naturalist:ostrich", AttackType.MELEE_ENHANCED, 5.5D, 3.2D, 0.0D, 14);
        put(defaults, "naturalist:rattlesnake", AttackType.POISON_BITE, 5.0D, 2.6D, 0.0D, 18);
        put(defaults, "naturalist:rhino", AttackType.CHARGE, 11.0D, 4.0D, 1.6D, 28);
        put(defaults, "naturalist:snake", AttackType.POISON_BITE, 4.5D, 2.5D, 0.0D, 16);
        put(defaults, "naturalist:vulture", AttackType.LEAP_ATTACK, 5.0D, 2.8D, 0.6D, 20);
        put(defaults, "naturalist:zebra", AttackType.MELEE_ENHANCED, 5.0D, 3.0D, 0.0D, 14);

        // Aquamirae — abyss combat
        put(defaults, "aquamirae:anglerfish", AttackType.POISON_BITE, 6.0D, 2.8D, 0.0D, 16);
        put(defaults, "aquamirae:maw", AttackType.LEAP_ATTACK, 8.0D, 3.2D, 0.7D, 22);
        put(defaults, "aquamirae:tortured_soul", AttackType.MELEE_ENHANCED, 7.0D, 3.5D, 0.0D, 12);
        put(defaults, "aquamirae:maze_mother", AttackType.DASH_BITE, 10.0D, 4.0D, 1.0D, 20);
        put(defaults, "aquamirae:eel", AttackType.SONIC_BLAST, 9.0D, 5.5D, 0.0D, 40);
        put(defaults, "aquamirae:captain_cornelia", AttackType.CHARGE, 12.0D, 4.5D, 1.4D, 28);
        put(defaults, "aquamirae:abyssal_scyphoid", AttackType.POISON_BITE, 5.5D, 3.0D, 0.0D, 18);

        // Friends & Foes
        put(defaults, "friendsandfoes:iceologer", AttackType.FREEZE_ZONE, 7.0D, 7.0D, 0.0D, 36);
        put(defaults, "friendsandfoes:illusioner", AttackType.ARROW, 4.0D, 20.0D, 3.0D, 18);
        put(defaults, "friendsandfoes:wildfire", AttackType.SMALL_FIREBALL, 7.0D, 18.0D, 0.16D, 22);
        put(defaults, "friendsandfoes:mauler", AttackType.DASH_BITE, 8.0D, 2.8D, 1.0D, 16);
        put(defaults, "friendsandfoes:copper_golem", AttackType.MELEE_ENHANCED, 5.0D, 3.0D, 0.0D, 12);
        put(defaults, "friendsandfoes:tuff_golem", AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 14);

        // Cataclysm bosses / elites
        put(defaults, "cataclysm:netherite_monstrosity", AttackType.FIREBALL, 16.0D, 22.0D, 0.14D, 36);
        put(defaults, "cataclysm:ender_guardian", AttackType.WITHER_SKULL, 12.0D, 22.0D, 1.3D, 30);
        put(defaults, "cataclysm:the_harbinger", AttackType.GUARDIAN_BEAM, 11.0D, 24.0D, 0.0D, 28);
        put(defaults, "cataclysm:ancient_remnant", AttackType.CHARGE, 14.0D, 5.0D, 1.6D, 30);
        put(defaults, "cataclysm:the_leviathan", AttackType.SONIC_BLAST, 13.0D, 7.0D, 0.0D, 40);
        put(defaults, "cataclysm:scylla", AttackType.LIGHTNING_STRIKE, 13.0D, 18.0D, 0.0D, 36);
        put(defaults, "cataclysm:maledictus", AttackType.WITHER_SKULL, 11.0D, 20.0D, 1.25D, 32);
        put(defaults, "cataclysm:ignis", AttackType.FIREBALL, 14.0D, 20.0D, 0.15D, 28);
        put(defaults, "cataclysm:ender_golem", AttackType.MELEE_ENHANCED, 12.0D, 4.5D, 0.0D, 16);
        put(defaults, "cataclysm:the_prowler", AttackType.GUARDIAN_BEAM, 9.0D, 18.0D, 0.0D, 24);
        put(defaults, "cataclysm:kobolediator", AttackType.MELEE_ENHANCED, 10.0D, 4.0D, 0.0D, 14);
        put(defaults, "cataclysm:wadjet", AttackType.POISON_BITE, 9.0D, 3.5D, 0.0D, 16);
        put(defaults, "cataclysm:amethyst_crab", AttackType.ARROW, 7.0D, 16.0D, 2.5D, 18);
        put(defaults, "cataclysm:clawdian", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "cataclysm:aptrgangr", AttackType.MELEE_ENHANCED, 10.0D, 4.0D, 0.0D, 14);
        put(defaults, "cataclysm:ignited_revenant", AttackType.SMALL_FIREBALL, 8.0D, 16.0D, 0.15D, 20);
        put(defaults, "cataclysm:ignited_berserker", AttackType.CHARGE, 9.0D, 3.5D, 1.4D, 20);
        put(defaults, "cataclysm:coralssus", AttackType.MELEE_ENHANCED, 11.0D, 4.5D, 0.0D, 16);
        put(defaults, "cataclysm:koboleton", AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 12);
        put(defaults, "cataclysm:deepling", AttackType.DASH_BITE, 6.0D, 3.0D, 0.9D, 14);
        put(defaults, "cataclysm:deepling_angler", AttackType.POISON_BITE, 6.5D, 3.0D, 0.0D, 16);
        put(defaults, "cataclysm:deepling_brute", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "cataclysm:deepling_priest", AttackType.GUARDIAN_BEAM, 7.0D, 14.0D, 0.0D, 28);
        put(defaults, "cataclysm:deepling_warlock", AttackType.WITHER_SKULL, 7.5D, 16.0D, 1.1D, 26);
        put(defaults, "cataclysm:coral_golem", AttackType.MELEE_ENHANCED, 8.0D, 3.8D, 0.0D, 14);
        put(defaults, "cataclysm:lionfish", AttackType.ARROW, 5.0D, 14.0D, 2.4D, 16);
        put(defaults, "cataclysm:endermaptera", AttackType.LEAP_ATTACK, 6.0D, 2.8D, 0.6D, 18);
        put(defaults, "cataclysm:draugr", AttackType.MELEE_ENHANCED, 6.5D, 3.2D, 0.0D, 12);
        put(defaults, "cataclysm:elite_draugr", AttackType.MELEE_ENHANCED, 8.5D, 3.6D, 0.0D, 12);
        put(defaults, "cataclysm:royal_draugr", AttackType.MELEE_ENHANCED, 10.0D, 4.0D, 0.0D, 12);
        put(defaults, "cataclysm:symbiocto", AttackType.POISON_BITE, 7.0D, 3.2D, 0.0D, 16);

        // Born in Chaos — undead / horror signature attacks
        put(defaults, "born_in_chaos_v1:decrepit_skeleton", AttackType.ARROW, 3.0D, 18.0D, 2.8D, 20);
        put(defaults, "born_in_chaos_v1:baby_skeleton", AttackType.ARROW, 2.5D, 16.0D, 2.8D, 16);
        put(defaults, "born_in_chaos_v1:skeleton_thrasher", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 12);
        put(defaults, "born_in_chaos_v1:skeleton_demoman", AttackType.FIREBALL, 8.0D, 14.0D, 0.12D, 36);
        put(defaults, "born_in_chaos_v1:bonescaller", AttackType.WITHER_SKULL, 6.0D, 16.0D, 1.1D, 30);
        put(defaults, "born_in_chaos_v1:supreme_bonescaller", AttackType.WITHER_SKULL, 9.0D, 20.0D, 1.25D, 28);
        put(defaults, "born_in_chaos_v1:decaying_zombie", AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 12);
        put(defaults, "born_in_chaos_v1:zombie_bruiser", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "born_in_chaos_v1:door_knight", AttackType.MELEE_ENHANCED, 8.0D, 3.5D, 0.0D, 14);
        put(defaults, "born_in_chaos_v1:fallen_chaos_knight", AttackType.MELEE_ENHANCED, 10.0D, 4.0D, 0.0D, 14);
        put(defaults, "born_in_chaos_v1:missioner", AttackType.CHARGE, 8.0D, 3.5D, 1.3D, 22);
        put(defaults, "born_in_chaos_v1:nightmare_stalker", AttackType.DASH_BITE, 8.5D, 3.0D, 1.1D, 14);
        put(defaults, "born_in_chaos_v1:lifestealer", AttackType.DASH_BITE, 9.0D, 3.0D, 1.0D, 16);
        put(defaults, "born_in_chaos_v1:mother_spider", AttackType.LEAP_ATTACK, 7.0D, 3.0D, 0.6D, 20);
        put(defaults, "born_in_chaos_v1:baby_spider", AttackType.LEAP_ATTACK, 3.5D, 2.4D, 0.5D, 18);
        put(defaults, "born_in_chaos_v1:bloody_gadfly", AttackType.DASH_BITE, 4.5D, 2.6D, 0.9D, 12);
        put(defaults, "born_in_chaos_v1:glutton_fish", AttackType.DASH_BITE, 6.0D, 2.8D, 1.0D, 14);
        put(defaults, "born_in_chaos_v1:thornshell_crab", AttackType.MELEE_ENHANCED, 6.5D, 3.0D, 0.0D, 14);
        put(defaults, "born_in_chaos_v1:dread_hound", AttackType.DASH_BITE, 7.0D, 2.8D, 1.0D, 14);
        put(defaults, "born_in_chaos_v1:dire_hound_leader", AttackType.DASH_BITE, 9.0D, 3.2D, 1.1D, 14);
        put(defaults, "born_in_chaos_v1:restless_spirit", AttackType.MELEE_ENHANCED, 5.5D, 3.2D, 0.0D, 12);
        put(defaults, "born_in_chaos_v1:infernal_spirit", AttackType.SMALL_FIREBALL, 6.5D, 16.0D, 0.15D, 22);
        put(defaults, "born_in_chaos_v1:phantom_creeper", AttackType.SONIC_BLAST, 10.0D, 4.0D, 0.0D, 45);
        put(defaults, "born_in_chaos_v1:lord_pumpkinhead", AttackType.FIREBALL, 11.0D, 18.0D, 0.14D, 32);
        put(defaults, "born_in_chaos_v1:sir_pumpkinhead", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "born_in_chaos_v1:krampus", AttackType.MELEE_ENHANCED, 11.0D, 4.2D, 0.0D, 16);

        // Alias namespace for older Born in Chaos builds
        copyNamespaceAttacks(defaults, "born_in_chaos_v1", "born_in_chaos");

        // Critters and Companions — light combat pets
        put(defaults, "crittersandcompanions:jumping_spider", AttackType.LEAP_ATTACK, 3.5D, 2.4D, 0.55D, 16);
        put(defaults, "crittersandcompanions:stag_beetle", AttackType.MELEE_ENHANCED, 4.0D, 2.8D, 0.0D, 14);
        put(defaults, "crittersandcompanions:dumbo_octopus", AttackType.POISON_BITE, 3.5D, 2.6D, 0.0D, 18);
        put(defaults, "crittersandcompanions:otter", AttackType.DASH_BITE, 4.0D, 2.6D, 0.9D, 14);

        // Twilight Forest — bosses and signature hostiles
        put(defaults, "twilightforest:naga", AttackType.CHARGE, 12.0D, 4.5D, 1.5D, 28);
        put(defaults, "twilightforest:lich", AttackType.WITHER_SKULL, 10.0D, 22.0D, 1.25D, 30);
        put(defaults, "twilightforest:minoshroom", AttackType.CHARGE, 11.0D, 4.0D, 1.4D, 26);
        put(defaults, "twilightforest:hydra", AttackType.FIREBALL, 14.0D, 20.0D, 0.14D, 32);
        put(defaults, "twilightforest:knight_phantom", AttackType.MELEE_ENHANCED, 9.0D, 3.8D, 0.0D, 14);
        put(defaults, "twilightforest:ur_ghast", AttackType.FIREBALL, 13.0D, 24.0D, 0.12D, 36);
        put(defaults, "twilightforest:alpha_yeti", AttackType.FREEZE_ZONE, 12.0D, 7.0D, 0.0D, 36);
        put(defaults, "twilightforest:snow_queen", AttackType.FREEZE_ZONE, 11.0D, 8.0D, 0.0D, 34);
        put(defaults, "twilightforest:minotaur", AttackType.CHARGE, 8.0D, 3.5D, 1.35D, 22);
        put(defaults, "twilightforest:fire_beetle", AttackType.SMALL_FIREBALL, 6.0D, 14.0D, 0.15D, 22);
        put(defaults, "twilightforest:slime_beetle", AttackType.POISON_BITE, 5.5D, 3.0D, 0.0D, 16);
        put(defaults, "twilightforest:pinch_beetle", AttackType.MELEE_ENHANCED, 7.0D, 3.2D, 0.0D, 14);
        put(defaults, "twilightforest:king_spider", AttackType.LEAP_ATTACK, 7.5D, 3.0D, 0.65D, 18);
        put(defaults, "twilightforest:hedge_spider", AttackType.LEAP_ATTACK, 5.0D, 2.6D, 0.55D, 16);
        put(defaults, "twilightforest:swarm_spider", AttackType.POISON_BITE, 3.5D, 2.4D, 0.0D, 14);
        put(defaults, "twilightforest:mist_wolf", AttackType.DASH_BITE, 7.0D, 3.0D, 1.05D, 14);
        put(defaults, "twilightforest:winter_wolf", AttackType.FREEZE_ZONE, 7.5D, 5.0D, 0.0D, 30);
        put(defaults, "twilightforest:hostile_wolf", AttackType.DASH_BITE, 5.5D, 2.8D, 1.0D, 14);
        put(defaults, "twilightforest:yeti", AttackType.FREEZE_ZONE, 8.0D, 5.5D, 0.0D, 32);
        put(defaults, "twilightforest:skeleton_druid", AttackType.POISON_BITE, 6.0D, 3.0D, 0.0D, 18);
        put(defaults, "twilightforest:death_tome", AttackType.WITHER_SKULL, 5.5D, 16.0D, 1.1D, 28);
        put(defaults, "twilightforest:wraith", AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 12);
        put(defaults, "twilightforest:carminite_ghastguard", AttackType.FIREBALL, 9.0D, 20.0D, 0.12D, 30);
        put(defaults, "twilightforest:carminite_ghastling", AttackType.SMALL_FIREBALL, 5.0D, 16.0D, 0.15D, 22);
        put(defaults, "twilightforest:mosquito_swarm", AttackType.DASH_BITE, 4.0D, 2.5D, 0.85D, 12);
        put(defaults, "twilightforest:troll", AttackType.MELEE_ENHANCED, 9.0D, 4.0D, 0.0D, 16);
        put(defaults, "twilightforest:ice_crystal", AttackType.FREEZE_ZONE, 5.5D, 5.0D, 0.0D, 28);
        put(defaults, "twilightforest:snow_guardian", AttackType.FREEZE_ZONE, 6.0D, 5.0D, 0.0D, 30);
        put(defaults, "twilightforest:block_and_chain_goblin", AttackType.MELEE_ENHANCED, 7.0D, 4.5D, 0.0D, 16);

        // Goety — bosses and signature casters / beasts
        put(defaults, "goety:apostle", AttackType.FIREBALL, 14.0D, 22.0D, 0.14D, 32);
        put(defaults, "goety:vizier", AttackType.WITHER_SKULL, 11.0D, 20.0D, 1.25D, 30);
        put(defaults, "goety:heresiarch", AttackType.WITHER_SKULL, 12.0D, 22.0D, 1.3D, 28);
        put(defaults, "goety:skull_lord", AttackType.WITHER_SKULL, 10.0D, 18.0D, 1.2D, 28);
        put(defaults, "goety:ender_keeper", AttackType.GUARDIAN_BEAM, 12.0D, 22.0D, 0.0D, 30);
        put(defaults, "goety:malghast", AttackType.FIREBALL, 11.0D, 22.0D, 0.12D, 34);
        put(defaults, "goety:grave_golem", AttackType.MELEE_ENHANCED, 12.0D, 4.5D, 0.0D, 16);
        put(defaults, "goety:redstone_monstrosity", AttackType.FIREBALL, 14.0D, 20.0D, 0.13D, 36);
        put(defaults, "goety:hostile_redstone_monstrosity", AttackType.FIREBALL, 14.0D, 20.0D, 0.13D, 36);
        put(defaults, "goety:black_beast", AttackType.DASH_BITE, 10.0D, 3.5D, 1.2D, 16);
        put(defaults, "goety:brood_mother", AttackType.LEAP_ATTACK, 9.0D, 3.2D, 0.65D, 20);
        put(defaults, "goety:endersent", AttackType.WITHER_SKULL, 10.0D, 20.0D, 1.25D, 28);
        put(defaults, "goety:squall_golem", AttackType.LIGHTNING_STRIKE, 10.0D, 16.0D, 0.0D, 36);
        put(defaults, "goety:ice_golem", AttackType.FREEZE_ZONE, 9.0D, 6.5D, 0.0D, 34);
        put(defaults, "goety:cryologer", AttackType.FREEZE_ZONE, 7.0D, 6.0D, 0.0D, 32);
        put(defaults, "goety:storm_caster", AttackType.LIGHTNING_STRIKE, 8.0D, 16.0D, 0.0D, 36);
        put(defaults, "goety:warlock", AttackType.FIREBALL, 7.5D, 16.0D, 0.14D, 28);
        put(defaults, "goety:heretic", AttackType.FIREBALL, 7.0D, 16.0D, 0.14D, 28);
        put(defaults, "goety:sorcerer", AttackType.WITHER_SKULL, 6.5D, 16.0D, 1.1D, 28);
        put(defaults, "goety:envioker", AttackType.WITHER_SKULL, 7.0D, 16.0D, 1.15D, 28);
        put(defaults, "goety:necromancer", AttackType.WITHER_SKULL, 7.0D, 16.0D, 1.1D, 30);
        put(defaults, "goety:wither_necromancer", AttackType.WITHER_SKULL, 8.5D, 18.0D, 1.2D, 28);
        put(defaults, "goety:reaper", AttackType.DASH_BITE, 7.0D, 3.0D, 1.0D, 14);
        put(defaults, "goety:wraith", AttackType.MELEE_ENHANCED, 6.0D, 3.2D, 0.0D, 12);
        put(defaults, "goety:hellhound", AttackType.DASH_BITE, 7.5D, 3.0D, 1.1D, 14);
        put(defaults, "goety:stormhound", AttackType.LIGHTNING_STRIKE, 7.0D, 12.0D, 0.0D, 32);
        put(defaults, "goety:winter_wolf", AttackType.FREEZE_ZONE, 7.0D, 5.0D, 0.0D, 30);
        put(defaults, "goety:bone_spider", AttackType.LEAP_ATTACK, 6.0D, 2.8D, 0.55D, 16);
        put(defaults, "goety:icy_spider", AttackType.FREEZE_ZONE, 6.0D, 4.5D, 0.0D, 28);
        put(defaults, "goety:inferno", AttackType.SMALL_FIREBALL, 7.0D, 14.0D, 0.15D, 20);
        put(defaults, "goety:whisperer", AttackType.SONIC_BLAST, 7.5D, 5.0D, 0.0D, 40);
        put(defaults, "goety:wavewhisperer", AttackType.SONIC_BLAST, 8.0D, 5.5D, 0.0D, 38);
        put(defaults, "goety:crusher", AttackType.MELEE_ENHANCED, 8.5D, 3.8D, 0.0D, 14);
        put(defaults, "goety:trampler", AttackType.CHARGE, 8.0D, 3.5D, 1.35D, 22);
        put(defaults, "goety:ravaged", AttackType.CHARGE, 10.0D, 4.0D, 1.4D, 24);
        put(defaults, "goety:redstone_golem", AttackType.MELEE_ENHANCED, 11.0D, 4.2D, 0.0D, 16);
        put(defaults, "goety:hostile_redstone_golem", AttackType.MELEE_ENHANCED, 11.0D, 4.2D, 0.0D, 16);
        put(defaults, "goety:mini_ghast", AttackType.SMALL_FIREBALL, 5.0D, 16.0D, 0.15D, 22);
        put(defaults, "goety:poison_quill_vine", AttackType.POISON_BITE, 5.0D, 8.0D, 0.0D, 18);
        put(defaults, "goety:poison_anemone", AttackType.POISON_BITE, 5.5D, 3.0D, 0.0D, 16);

        // The Aether — sky realm signature attacks
        put(defaults, "aether:slider", AttackType.CHARGE, 12.0D, 5.0D, 1.5D, 28);
        put(defaults, "aether:sun_spirit", AttackType.FIREBALL, 13.0D, 22.0D, 0.14D, 32);
        put(defaults, "aether:valkyrie_queen", AttackType.MELEE_ENHANCED, 11.0D, 4.0D, 0.0D, 14);
        put(defaults, "aether:valkyrie", AttackType.MELEE_ENHANCED, 8.0D, 3.5D, 0.0D, 12);
        put(defaults, "aether:zephyr", AttackType.SONIC_BLAST, 8.0D, 6.0D, 0.0D, 36);
        put(defaults, "aether:cockatrice", AttackType.POISON_BITE, 6.5D, 3.0D, 0.0D, 16);
        put(defaults, "aether:aechor_plant", AttackType.POISON_BITE, 5.0D, 8.0D, 0.0D, 20);
        put(defaults, "aether:fire_minion", AttackType.SMALL_FIREBALL, 6.0D, 14.0D, 0.15D, 20);
        put(defaults, "aether:sentry", AttackType.CHARGE, 5.5D, 3.0D, 1.2D, 22);
        put(defaults, "aether:mimic", AttackType.MELEE_ENHANCED, 6.0D, 3.0D, 0.0D, 12);
        put(defaults, "aether:whirlwind", AttackType.SONIC_BLAST, 5.0D, 4.0D, 0.0D, 40);
        put(defaults, "aether:evil_whirlwind", AttackType.SONIC_BLAST, 6.5D, 4.5D, 0.0D, 36);
        put(defaults, "aether:blue_swet", AttackType.LEAP_ATTACK, 4.0D, 2.5D, 0.5D, 18);
        put(defaults, "aether:golden_swet", AttackType.LEAP_ATTACK, 4.5D, 2.5D, 0.5D, 18);
    }

    private static void put(Map<ResourceLocation, NaturalAttackConfig> map, String id, AttackType type,
                            double damage, double range, double velocity, int cooldown) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key != null) {
            map.put(key, new NaturalAttackConfig(type, damage, range, velocity, cooldown, true));
        }
    }

    private static void copyNamespaceAttacks(Map<ResourceLocation, NaturalAttackConfig> map, String fromNs, String toNs) {
        Map<ResourceLocation, NaturalAttackConfig> copies = new HashMap<>();
        for (Map.Entry<ResourceLocation, NaturalAttackConfig> entry : map.entrySet()) {
            if (!fromNs.equals(entry.getKey().getNamespace())) {
                continue;
            }
            ResourceLocation alias = new ResourceLocation(toNs, entry.getKey().getPath());
            copies.put(alias, entry.getValue());
        }
        map.putAll(copies);
    }

    public enum AttackType {
        FIREBALL("fireball", 6.0D, 20.0D, 0.1D, 40),
        SMALL_FIREBALL("small_fireball", 5.0D, 20.0D, 0.15D, 30),
        WITHER_SKULL("wither_skull", 7.0D, 24.0D, 1.35D, 36),
        ARROW("arrow", 2.5D, 20.0D, 3.0D, 20),
        DRAGON_BREATH_ZONE("dragon_breath_zone", 9.0D, 7.0D, 0.0D, 55),
        GUARDIAN_BEAM("guardian_beam", 5.0D, 18.0D, 0.0D, 32),
        DASH_BITE("dash_bite", 4.0D, 2.5D, 0.8D, 15),
        LEAP_ATTACK("leap_attack", 0.0D, 0.0D, 0.5D, 25),
        MELEE_ENHANCED("melee_enhanced", 7.0D, 4.0D, 0.0D, 10),
        POISON_BITE("poison_bite", 5.0D, 2.8D, 0.0D, 18),
        FREEZE_ZONE("freeze_zone", 7.0D, 6.0D, 0.0D, 40),
        LIGHTNING_STRIKE("lightning_strike", 8.0D, 16.0D, 0.0D, 45),
        CHARGE("charge", 8.0D, 3.5D, 1.4D, 28),
        SONIC_BLAST("sonic_blast", 7.0D, 5.0D, 0.0D, 50);

        private final String id;
        private final double defaultDamage;
        private final double defaultRange;
        private final double defaultVelocity;
        private final int defaultCooldown;

        AttackType(String id, double defaultDamage, double defaultRange, double defaultVelocity, int defaultCooldown) {
            this.id = id;
            this.defaultDamage = defaultDamage;
            this.defaultRange = defaultRange;
            this.defaultVelocity = defaultVelocity;
            this.defaultCooldown = defaultCooldown;
        }

        public static AttackType fromId(String id) {
            for (AttackType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }
    }

    public record NaturalAttackConfig(AttackType type, double damage, double range, double velocity, int cooldown,
                                      boolean replaceNormalAttack) {
        private JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", type.id);
            obj.addProperty("damage", damage);
            obj.addProperty("range", range);
            obj.addProperty("velocity", velocity);
            obj.addProperty("cooldown", cooldown);
            obj.addProperty("replace_normal_attack", replaceNormalAttack);
            return obj;
        }
    }
}
