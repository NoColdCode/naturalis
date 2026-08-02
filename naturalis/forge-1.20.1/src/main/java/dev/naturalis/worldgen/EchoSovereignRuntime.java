package dev.naturalis.worldgen;

import dev.naturalis.Naturalis;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EchoSovereignRuntime {

    private static final Map<UUID, ServerBossEvent> BOSS_BARS = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> KNOWN_SOVEREIGNS = ConcurrentHashMap.newKeySet();

    private static final double BOSS_BAR_RANGE = 100.0D;
    private static final double SOVEREIGN_SCAN_RADIUS = 96.0D;

    private static final String TAG_BOSS = "naturalis_echo_sovereign";
    private static final String TAG_PHASE_DAMAGE = "naturalis_echo_phase_damage";
    private static final String TAG_SPECIAL_CD = "naturalis_echo_special_cd";
    private static final String TAG_LIGHTNING_CD = "naturalis_echo_lightning_cd";

    private static final float DAMAGE_THRESHOLD = 20.0F;

    private static final int PHASE_SHIFT_PLAYER_MORPH_DURATION_TICKS = 20 * 60 * 15;

    private static final ResourceLocation HARMLESS_RING_MORPH = new ResourceLocation("minecraft", "bat");
    private static final int RING_MORPH_DURATION = 12 * 20;

    /** Boss HP stays flat across phase morphs; phase shifts still accumulate every {@link #DAMAGE_THRESHOLD} damage taken. */
    private static final double MIN_SOVEREIGN_HEALTH = 200.0D;

    private EchoSovereignRuntime() {
    }

    public static void initializeBoss(Mob boss) {
        boss.getPersistentData().putBoolean(TAG_BOSS, true);
        boss.getPersistentData().putFloat(TAG_PHASE_DAMAGE, 0.0F);
        boss.getPersistentData().putInt(TAG_SPECIAL_CD, 90);
        boss.getPersistentData().putInt(TAG_LIGHTNING_CD, 200);
        KNOWN_SOVEREIGNS.add(boss.getUUID());

        boss.setPersistenceRequired();
        boss.setCustomName(Component.translatable("entity.naturalis.echo_sovereign"));
        boss.setCustomNameVisible(true);

        applySovereignScaledCombatStats(boss, 1.0F);
        ensureBossBar(boss);
    }

    public static boolean isEchoSovereign(LivingEntity livingEntity) {
        return livingEntity instanceof Mob mob && mob.getPersistentData().getBoolean(TAG_BOSS);
    }

    public static List<Mob> knownSovereigns(ServerLevel level) {
        java.util.ArrayList<Mob> list = new java.util.ArrayList<>(KNOWN_SOVEREIGNS.size());
        for (UUID id : List.copyOf(KNOWN_SOVEREIGNS)) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob && isEchoSovereign(mob) && mob.isAlive()) {
                list.add(mob);
            } else if (entity != null) {
                KNOWN_SOVEREIGNS.remove(id);
                ServerBossEvent orphan = BOSS_BARS.remove(id);
                if (orphan != null) {
                    orphan.removeAllPlayers();
                }
            }
        }
        if (!list.isEmpty()) {
            return list;
        }
        for (ServerPlayer player : level.players()) {
            for (Mob mob : level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(SOVEREIGN_SCAN_RADIUS),
                EchoSovereignRuntime::isEchoSovereign
            )) {
                KNOWN_SOVEREIGNS.add(mob.getUUID());
                if (!list.contains(mob)) {
                    list.add(mob);
                }
            }
        }
        return list;
    }

    public static boolean hasTrackedSovereign() {
        return !KNOWN_SOVEREIGNS.isEmpty();
    }

    public static void clearTrackedSovereigns() {
        for (UUID id : List.copyOf(KNOWN_SOVEREIGNS)) {
            KNOWN_SOVEREIGNS.remove(id);
            ServerBossEvent orphan = BOSS_BARS.remove(id);
            if (orphan != null) {
                orphan.removeAllPlayers();
            }
        }
    }

    /** Held-weapon / projectile hits — cancel before mitigation; phase meter uses {@link #onBossDamagedFinal}. */
    public static void onBossHurtPre(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob boss) || !isEchoSovereign(boss)) {
            return;
        }
        if (isBlockedWeaponDamage(event.getSource())) {
            event.setAmount(0.0F);
        }
    }

    /**
     * Phase teleport every {@link #DAMAGE_THRESHOLD} HP actually removed (after armor / resist).
     * {@link LivingDamageEvent} runs before {@code setHealth}; {@code min(amount, currentHealth)} matches real bar loss on the flat 200 pool.
     */
    public static void onBossDamagedFinal(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob boss) || !isEchoSovereign(boss)) {
            return;
        }
        if (isBlockedWeaponDamage(event.getSource())) {
            return;
        }
        float before = boss.getHealth();
        float raw = Math.max(0.0F, event.getAmount());
        float lost = Math.min(raw, before);
        accumulatePhaseMeterFromHpLost(boss, lost, event.getSource());
    }

    private static void accumulatePhaseMeterFromHpLost(Mob boss, float dealtToHealth, DamageSource source) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (dealtToHealth <= 0.0F || !boss.isAlive()) {
            return;
        }
        ServerPlayer partner = resolvePhaseShiftPartner(serverLevel, boss, source.getEntity());
        Mob sovereign = boss;
        float acc = sovereign.getPersistentData().getFloat(TAG_PHASE_DAMAGE) + dealtToHealth;
        if (partner != null) {
            while (acc >= DAMAGE_THRESHOLD && sovereign.isAlive()) {
                acc -= DAMAGE_THRESHOLD;
                sovereign = phaseShift(serverLevel, sovereign, partner, acc);
            }
        }
        sovereign.getPersistentData().putFloat(TAG_PHASE_DAMAGE, acc);
    }

    private static ServerPlayer resolvePhaseShiftPartner(ServerLevel level, Mob boss, Entity damagingEntity) {
        if (damagingEntity instanceof ServerPlayer sp) {
            return sp;
        }
        if (boss.getTarget() instanceof ServerPlayer sp) {
            return sp;
        }
        Player nearest = level.getNearestPlayer(boss, BOSS_BAR_RANGE);
        return nearest instanceof ServerPlayer sp ? sp : null;
    }

    /**
     * Vanilla tools/weapons in main hand are blocked; empty hand and Echo morph forged tools damage normally.
     * Player-fired projectiles stay blocked (align with morph ranged identities).
     */
    private static boolean isBlockedWeaponDamage(DamageSource source) {
        if (source.getDirectEntity() instanceof Projectile) {
            return true;
        }
        if (source.getEntity() instanceof Player player) {
            ItemStack main = player.getMainHandItem();
            if (main.isEmpty()) {
                return false;
            }
            return !isEchoMorphTool(main);
        }
        return false;
    }

    private static boolean isEchoMorphTool(ItemStack stack) {
        var item = stack.getItem();
        return item == NaturalisItems.ECHO_MORPH_BLADE.get()
            || item == NaturalisItems.ECHO_MORPH_PICK.get()
            || item == NaturalisItems.ECHO_MORPH_AXE.get()
            || item == NaturalisItems.ECHO_MORPH_SHOVEL.get();
    }

    public static void tick(ServerLevel level) {
        List<Mob> sovereigns = knownSovereigns(level);
        if (sovereigns.size() > 1) {
            sovereigns.sort(Comparator.comparing(Entity::getUUID));
            for (int i = 1; i < sovereigns.size(); i++) {
                Mob extra = sovereigns.get(i);
                ServerBossEvent orphan = BOSS_BARS.remove(extra.getUUID());
                if (orphan != null) {
                    orphan.removeAllPlayers();
                }
                KNOWN_SOVEREIGNS.remove(extra.getUUID());
                extra.discard();
            }
            sovereigns = sovereigns.subList(0, 1);
        }

        for (Mob boss : sovereigns) {

            enforceFlatSovereignHealth(boss);

            ServerBossEvent bar = BOSS_BARS.computeIfAbsent(boss.getUUID(), id -> {
                ServerBossEvent b = new ServerBossEvent(
                    Component.translatable("entity.naturalis.echo_sovereign"),
                    BossEvent.BossBarColor.BLUE,
                    BossEvent.BossBarOverlay.NOTCHED_10);
                b.setCreateWorldFog(false);
                return b;
            });
            bar.setProgress(Mth.clamp(boss.getHealth() / boss.getMaxHealth(), 0.0F, 1.0F));
            for (ServerPlayer player : level.players()) {
                if (boss.distanceTo(player) <= BOSS_BAR_RANGE) {
                    bar.addPlayer(player);
                } else {
                    bar.removePlayer(player);
                }
            }

            int special = Math.max(0, CompatAccess.getInt(boss.getPersistentData(), TAG_SPECIAL_CD) - 1);
            int lightning = Math.max(0, CompatAccess.getInt(boss.getPersistentData(), TAG_LIGHTNING_CD) - 1);

            Player nearest = level.getNearestPlayer(boss, 56.0D);
            ServerPlayer target = nearest instanceof ServerPlayer sp ? sp : null;
            if (target != null) {
                boss.setTarget(target);

                if (special <= 0) {
                    throwPotionRing(level, boss);
                    special = 140 + level.random.nextInt(70);
                }

                if (lightning <= 0 && level.random.nextDouble() < 0.055D) {
                    strikeLightning(level, boss, target);
                    lightning = 260 + level.random.nextInt(160);
                }
            }

            boss.getPersistentData().putInt(TAG_SPECIAL_CD, special);
            boss.getPersistentData().putInt(TAG_LIGHTNING_CD, lightning);

            maybeKeepNearEchoArena(level, boss);
        }
    }

    private static void maybeKeepNearEchoArena(ServerLevel level, Mob boss) {
        if (!level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }
        net.minecraft.core.BlockPos feet = boss.blockPosition();
        if (!level.getBiome(feet).is(NaturalDimensionKeys.NATURAL_ECHO)) {
            return;
        }
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm == null) {
            return;
        }
        int ax = hm.getEchoArenaBlockX();
        int az = hm.getEchoArenaBlockZ();
        double dx = boss.getX() - (ax + 0.5D);
        double dz = boss.getZ() - (az + 0.5D);
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 14.0D && boss.getTarget() == null) {
            int y = hm.getSurfaceY(ax, az) + 1;
            boss.teleportTo(ax + 0.5D, y, az + 0.5D);
        }
    }

    private static net.minecraft.core.BlockPos pickPhaseShiftColumn(ServerLevel level) {
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm != null) {
            for (int attempt = 0; attempt < 160; attempt++) {
                int bx = level.random.nextInt(1041) - 520;
                int bz = level.random.nextInt(1041) - 520;
                if (!hm.isInsideIsland(bx, bz)) {
                    continue;
                }
                if (isNearStaticHutFootprint(bx, bz)) {
                    continue;
                }
                return new net.minecraft.core.BlockPos(bx, 0, bz);
            }
            return new net.minecraft.core.BlockPos(hm.getEchoArenaBlockX(), 0, hm.getEchoArenaBlockZ());
        }
        return net.minecraft.core.BlockPos.ZERO;
    }

    private static boolean isNearStaticHutFootprint(int bx, int bz) {
        for (net.minecraft.core.BlockPos h : NaturalDimensionRuntime.NATURAL_HUT_CENTERS) {
            int dx = bx - h.getX();
            int dz = bz - h.getZ();
            if (dx * dx + dz * dz <= 45 * 45) {
                return true;
            }
        }
        return false;
    }

    private static int resolveStandOnGroundY(ServerLevel level, int blockX, int blockZ, double halfWidth, double entityHeight) {
        level.getChunk(blockX >> 4, blockZ >> 4);
        IslandHeightmap hm = IslandHeightmap.tryGet();
        int yGuess;
        if (hm != null && level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION) && hm.isInsideIsland(blockX, blockZ)) {
            yGuess = hm.getSurfaceY(blockX, blockZ) + 1;
        } else {
            int motionTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
            int surfaceTop = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            yGuess = Math.max(level.getSeaLevel() + 2, Math.max(motionTop, surfaceTop) + 1);
        }
        int minY = CompatAccess.getMinBuildHeight(level);
        int maxY = CompatAccess.getMaxBuildHeight(level);
        double px = blockX + 0.5;
        double pz = blockZ + 0.5;
        int topCap = maxY - Mth.ceil(entityHeight) - 2;
        for (int dy = -8; dy <= 72; dy++) {
            int y = yGuess + dy;
            int bodyTop = Mth.floor(y + entityHeight);
            if (y <= minY + 1 || bodyTop >= maxY - 1) {
                continue;
            }
            AABB box = new AABB(px - halfWidth, y, pz - halfWidth, px + halfWidth, y + entityHeight, pz + halfWidth);
            if (!level.noCollision(null, box)) {
                continue;
            }
            BlockPos below = new BlockPos(blockX, y - 1, blockZ);
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            return y;
        }
        return Mth.clamp(yGuess, minY + 2, Math.max(minY + 2, topCap));
    }

    private static Mob phaseShift(ServerLevel level, Mob boss, ServerPlayer player, float persistedPhaseDamageAcc) {
        net.minecraft.core.BlockPos targetXZ = pickPhaseShiftColumn(level);
        int bx = targetXZ.getX();
        int bz = targetXZ.getZ();
        int playerX = bx + 2;
        int playerZ = bz;

        double halfW = 0.5 * Math.max(player.getBbWidth(), boss.getBbWidth()) + 0.06;
        double bodyH = Math.max(player.getBbHeight(), boss.getBbHeight()) + 0.125;

        int yBoss = resolveStandOnGroundY(level, bx, bz, halfW, bodyH);
        int yPlayer = resolveStandOnGroundY(level, playerX, playerZ, halfW, bodyH);

        boss.teleportTo(bx + 0.5D, yBoss, bz + 0.5D);
        player.teleportTo(playerX + 0.5D, yPlayer, playerZ + 0.5D);

        Mob sovereign = applyBiomeMorphShift(level, boss, player, new BlockPos(bx, yBoss, bz), persistedPhaseDamageAcc);

        level.playSound(null, sovereign.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.9F);
        player.displayClientMessage(Component.translatable("message.naturalis.echo_sovereign.shift").withStyle(ChatFormatting.DARK_AQUA), true);
        return sovereign;
    }

    private record BiomeMorphPair(ResourceLocation weakest, ResourceLocation strongest) {
    }

    private static BiomeMorphPair morphPairForBiome(Holder<Biome> biomeHolder) {
        if (biomeHolder.is(NaturalDimensionKeys.NATURAL_ECHO)) {
            return morphPair("bat", "warden");
        }
        if (biomeHolder.is(NaturalDimensionKeys.DARK_CAVES)) {
            return morphPair("silverfish", "creeper");
        }
        if (biomeHolder.is(NaturalDimensionKeys.VOLCANO)) {
            return morphPair("magma_cube", "wither_skeleton");
        }
        if (biomeHolder.is(NaturalDimensionKeys.DEEP_WATER)) {
            return morphPair("cod", "guardian");
        }
        if (biomeHolder.is(NaturalDimensionKeys.CORAL_WATER)) {
            return morphPair("tropical_fish", "drowned");
        }
        if (biomeHolder.is(NaturalDimensionKeys.ENDER_FOREST)) {
            return morphPair("endermite", "enderman");
        }
        if (biomeHolder.is(NaturalDimensionKeys.SNOWY_MOUNTAIN)) {
            return morphPair("rabbit", "stray");
        }
        if (biomeHolder.is(NaturalDimensionKeys.HIGH_PEAK)) {
            return morphPair("parrot", "ghast");
        }
        if (biomeHolder.is(NaturalDimensionKeys.ARID_SAVANNA)) {
            return morphPair("sheep", "husk");
        }
        if (biomeHolder.is(NaturalDimensionKeys.NATURAL_BEACH)) {
            return morphPair("turtle", "skeleton");
        }
        if (biomeHolder.is(NaturalDimensionKeys.NATURAL_PLAIN)) {
            return morphPair("cow", "vindicator");
        }
        if (biomeHolder.is(NaturalDimensionKeys.DENSE_FOREST)) {
            return morphPair("wolf", "ravager");
        }
        if (biomeHolder.is(NaturalDimensionKeys.JUNGLE_REAL)) {
            return morphPair("parrot", "vex");
        }
        return morphPair("pig", "zombie");
    }

    private static BiomeMorphPair morphPair(String weakestPath, String strongestPath) {
        return new BiomeMorphPair(
            new ResourceLocation("minecraft", weakestPath),
            new ResourceLocation("minecraft", strongestPath));
    }

    private static Mob applyBiomeMorphShift(ServerLevel level, Mob boss, ServerPlayer player, net.minecraft.core.BlockPos pos, float persistedPhaseDamageAcc) {
        Holder<Biome> biome = level.getBiome(pos);

        BiomeMorphPair pair = morphPairForBiome(biome);
        MorphEffectEvents.applyBrewedMorph(player, pair.weakest(), PHASE_SHIFT_PLAYER_MORPH_DURATION_TICKS);

        return morphBossInto(level, boss, pair.strongest(), persistedPhaseDamageAcc);
    }

    private static Mob morphBossInto(ServerLevel level, Mob source, ResourceLocation morphId, float persistedPhaseDamageAcc) {
        UUID oldId = source.getUUID();
        ServerBossEvent oldBar = BOSS_BARS.remove(oldId);
        if (oldBar != null) {
            oldBar.removeAllPlayers();
        }

        float hpFrac = source.getMaxHealth() <= 0.0F ? 1.0F : source.getHealth() / source.getMaxHealth();
        int specialCd = CompatAccess.getInt(source.getPersistentData(), TAG_SPECIAL_CD);
        int lightningCd = CompatAccess.getInt(source.getPersistentData(), TAG_LIGHTNING_CD);

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            source.getPersistentData().putFloat(TAG_PHASE_DAMAGE, persistedPhaseDamageAcc);
            ensureBossBar(source);
            return source;
        }

        Entity created = CompatAccess.createEntity(type, level);
        if (!(created instanceof Mob transformed)) {
            source.getPersistentData().putFloat(TAG_PHASE_DAMAGE, persistedPhaseDamageAcc);
            ensureBossBar(source);
            return source;
        }

        CompatAccess.moveEntity(transformed, source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        transformed.setCustomName(Component.translatable("entity.naturalis.echo_sovereign"));
        transformed.setCustomNameVisible(true);
        transformed.setPersistenceRequired();
        transformed.getPersistentData().putBoolean(TAG_BOSS, true);
        transformed.getPersistentData().putFloat(TAG_PHASE_DAMAGE, persistedPhaseDamageAcc);
        transformed.getPersistentData().putInt(TAG_SPECIAL_CD, specialCd);
        transformed.getPersistentData().putInt(TAG_LIGHTNING_CD, lightningCd);

        applySovereignScaledCombatStats(transformed, hpFrac);

        source.discard();
        level.addFreshEntity(transformed);
        ensureBossBar(transformed);
        return transformed;
    }

    private static void applySovereignScaledCombatStats(Mob mob, float healthFraction) {
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MIN_SOVEREIGN_HEALTH);
        }
        mob.setHealth(Mth.clamp((float) (MIN_SOVEREIGN_HEALTH * healthFraction), 1.0F, (float) MIN_SOVEREIGN_HEALTH));

        if (mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(
                Math.max(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue(), 0.55D));
        }
    }

    /** Morph bodies (e.g. dragon) may reset max health after spawn — keep the sovereign on {@link #MIN_SOVEREIGN_HEALTH}. */
    private static void enforceFlatSovereignHealth(Mob boss) {
        if (boss.getAttribute(Attributes.MAX_HEALTH) == null) {
            return;
        }
        float max = boss.getMaxHealth();
        if (Math.abs(max - MIN_SOVEREIGN_HEALTH) <= 0.05F) {
            return;
        }
        float frac = max <= 0.0F ? 1.0F : Mth.clamp(boss.getHealth() / max, 0.0F, 1.0F);
        applySovereignScaledCombatStats(boss, frac);
    }

    /**
     * Bat morph from the ring attack only while the boss still uses an Evoker-class shell.
     * After phase shifts ({@link #morphBossInto}) the body is a warden, blaze, etc., and bat morph felt wrong everywhere.
     */
    private static boolean isEvokerFormSovereign(Mob boss) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        return id != null && (
            ("minecraft".equals(id.getNamespace()) && "evoker".equals(id.getPath()))
                || (Naturalis.MOD_ID.equals(id.getNamespace()) && "echo_sovereign".equals(id.getPath()))
        );
    }

    private static void ensureBossBar(Mob boss) {
        ServerBossEvent bar = new ServerBossEvent(
            Component.translatable("entity.naturalis.echo_sovereign"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setProgress(Mth.clamp(boss.getHealth() / boss.getMaxHealth(), 0.0F, 1.0F));
        bar.setCreateWorldFog(false);
        ServerBossEvent prev = BOSS_BARS.put(boss.getUUID(), bar);
        if (prev != null) {
            prev.removeAllPlayers();
        }
    }

    private static void throwPotionRing(ServerLevel level, Mob boss) {
        Vec3 origin = boss.position().add(0.0D, 0.35D, 0.0D);
        int count = 8;
        double ringRadius = 9.5D;
        String[] debuffCycle = {"MOVEMENT_SLOWDOWN", "WEAKNESS", "POISON"};

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) count);
            double x = origin.x + Math.cos(angle) * ringRadius;
            double z = origin.z + Math.sin(angle) * ringRadius;

            AreaEffectCloud cloud = new AreaEffectCloud(level, x, origin.y, z);
            cloud.setOwner(boss);
            cloud.setRadius(5.2F);
            cloud.setDuration(220);
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
            String debuffKey = debuffCycle[i % debuffCycle.length];
            cloud.addEffect(new MobEffectInstance(resolveVanillaEffect(debuffKey), 140, 0));
            level.addFreshEntity(cloud);
        }

        if (isEvokerFormSovereign(boss)) {
            List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class,
                boss.getBoundingBox().inflate(14.0D), p -> p.isAlive() && !p.isSpectator());
            for (ServerPlayer target : targets) {
                MorphEffectEvents.applyBrewedMorph(target, HARMLESS_RING_MORPH, RING_MORPH_DURATION);
            }
        }
    }

    private static void strikeLightning(ServerLevel level, Mob boss, ServerPlayer player) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) {
            return;
        }
        lightning.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(lightning);
        player.displayClientMessage(Component.translatable("message.naturalis.echo_sovereign.lightning").withStyle(ChatFormatting.GOLD), true);
    }

    private static MobEffect resolveVanillaEffect(String name) {
        try {
            Object raw = MobEffects.class.getField(name).get(null);
            if (raw instanceof MobEffect effect) {
                return effect;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return NaturalisMobEffects.BREWED_MORPH.get();
    }

    public static void onBossDrops(ServerLevel level, Mob boss, net.minecraft.world.damagesource.DamageSource source) {
        if (!isEchoSovereign(boss)) {
            return;
        }

        ServerBossEvent bar = BOSS_BARS.remove(boss.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }

        CompatAccess.spawnEntityItemDrop(boss, level, new ItemStack(NaturalisItems.NATURAL_STAR.get()));
        CompatAccess.spawnEntityItemDrop(boss, level, new ItemStack(NaturalisItems.SOVEREIGN_AMULET.get()));

        int shards = 8 + level.random.nextInt(9);
        CompatAccess.spawnEntityItemDrop(boss, level, new ItemStack(net.minecraft.world.item.Items.ECHO_SHARD, shards));

        ItemStack[] echoTools = new ItemStack[] {
            new ItemStack(NaturalisItems.ECHO_MORPH_BLADE.get()),
            new ItemStack(NaturalisItems.ECHO_MORPH_PICK.get()),
            new ItemStack(NaturalisItems.ECHO_MORPH_AXE.get()),
            new ItemStack(NaturalisItems.ECHO_MORPH_SHOVEL.get())
        };
        CompatAccess.spawnEntityItemDrop(boss, level, echoTools[level.random.nextInt(echoTools.length)]);

        if (source.getEntity() instanceof ServerPlayer killer) {
            int currentXp = MorphKnowledgeManager.getGlobalXp(killer);
            int currentLevel = MorphKnowledgeManager.getGlobalPointLevelForXp(currentXp);
            int targetLevel = Math.min(MorphKnowledgeManager.getMaxPointLevel(), currentLevel + 10);
            int targetXp = MorphKnowledgeManager.getRequiredGlobalXpForPointLevel(targetLevel);
            MorphKnowledgeManager.addGlobalXp(killer, Math.max(0, targetXp - currentXp));

            killer.displayClientMessage(Component.translatable("message.naturalis.echo_sovereign.reward_knowledge", 10).withStyle(ChatFormatting.AQUA), false);
        }
    }
}
