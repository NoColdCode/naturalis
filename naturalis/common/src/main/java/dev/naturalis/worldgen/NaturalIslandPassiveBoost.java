package dev.naturalis.worldgen;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Soft spawn assist for the Natural island: only harmless morph-friendly mobs (no hostile morph / debuff
 * bait like wolves or pufferfish). Pools mirror each biome's peaceful lineup and stay aligned with datapack
 * {@code spawners.creature} / {@code ambient} in {@code data/naturalis/worldgen/biome}.
 */
public final class NaturalIslandPassiveBoost {

    /** Soft cap — boost spawns used to be persistence-required and flooded the island (TPS death). */
    private static final int MAX_PASSIVE_MOBS = 96;
    private static final int SPAWN_ATTEMPTS = 4;
    private static final int SPAWN_INTERVAL_TICKS = 600;
    private static final double PLAYER_SCAN_RADIUS = 96.0D;

    private NaturalIslandPassiveBoost() {
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    /** Land / air / water mob ids safe as chill morph targets (no intentional poison/slow/binding identity). */
    private static java.util.List<ResourceLocation> peacefulMorphSpawnPool(Holder<Biome> biome) {
        if (biome.is(NaturalDimensionKeys.NATURAL_PLAIN)) {
            return java.util.List.of(mc("cow"), mc("sheep"), mc("pig"), mc("chicken"), mc("rabbit"), mc("horse"));
        }
        if (biome.is(NaturalDimensionKeys.NATURAL_BEACH)) {
            return java.util.List.of(mc("turtle"), mc("squid"));
        }
        if (biome.is(NaturalDimensionKeys.DENSE_FOREST)) {
            return java.util.List.of(mc("fox"), mc("rabbit"), mc("chicken"));
        }
        if (biome.is(NaturalDimensionKeys.HIGH_PEAK)) {
            return java.util.List.of(mc("parrot"), mc("goat"), mc("rabbit"));
        }
        if (biome.is(NaturalDimensionKeys.SNOWY_MOUNTAIN)) {
            return java.util.List.of(mc("rabbit"), mc("goat"));
        }
        if (biome.is(NaturalDimensionKeys.DEEP_WATER)) {
            return java.util.List.of(mc("cod"), mc("squid"));
        }
        if (biome.is(NaturalDimensionKeys.CORAL_WATER)) {
            return java.util.List.of(mc("tropical_fish"), mc("squid"));
        }
        if (biome.is(NaturalDimensionKeys.VOLCANO)) {
            return java.util.List.of(mc("bat"));
        }
        if (biome.is(NaturalDimensionKeys.ENDER_FOREST)) {
            return java.util.List.of(mc("bat"));
        }
        if (biome.is(NaturalDimensionKeys.JUNGLE_REAL)) {
            return java.util.List.of(mc("parrot"), mc("panda"), mc("chicken"));
        }
        if (biome.is(NaturalDimensionKeys.ARID_SAVANNA)) {
            return java.util.List.of(mc("sheep"), mc("llama"), mc("horse"), mc("rabbit"), mc("cow"));
        }
        if (biome.is(NaturalDimensionKeys.DARK_CAVES)) {
            return java.util.List.of(mc("rabbit"), mc("bat"));
        }
        if (biome.is(NaturalDimensionKeys.NATURAL_ECHO)) {
            return java.util.List.of(mc("bat"));
        }
        return java.util.List.of(mc("pig"), mc("cow"), mc("sheep"));
    }

    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }
        long time = level.getGameTime();
        // Existing worlds may already be flooded with persistence-required boost mobs.
        if (time % 200L == 0L) {
            cullExcessPassives(level);
        }
        if (time % SPAWN_INTERVAL_TICKS != 0L) {
            return;
        }
        IslandHeightmap hm = IslandHeightmap.tryGet();
        if (hm == null) {
            return;
        }
        if (countPassives(level) >= MAX_PASSIVE_MOBS) {
            return;
        }
        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            if (countPassives(level) >= MAX_PASSIVE_MOBS) {
                return;
            }
            trySpawnPack(level, hm);
        }
    }

    private static void cullExcessPassives(ServerLevel level) {
        java.util.List<Mob> passives = collectPassivesNearPlayers(level);
        int excess = passives.size() - MAX_PASSIVE_MOBS;
        if (excess <= 0) {
            return;
        }
        int removed = 0;
        for (Mob mob : passives) {
            if (removed >= excess) {
                break;
            }
            if (mob.isPersistenceRequired() && !mob.hasCustomName()) {
                mob.discard();
                removed++;
            }
        }
    }

    private static int countPassives(ServerLevel level) {
        return collectPassivesNearPlayers(level).size();
    }

    private static java.util.List<Mob> collectPassivesNearPlayers(ServerLevel level) {
        java.util.LinkedHashMap<java.util.UUID, Mob> byId = new java.util.LinkedHashMap<>();
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            AABB near = player.getBoundingBox().inflate(PLAYER_SCAN_RADIUS);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, near, NaturalIslandPassiveBoost::isBoostCandidate)) {
                byId.putIfAbsent(mob.getUUID(), mob);
            }
        }
        return new java.util.ArrayList<>(byId.values());
    }

    private static boolean isBoostCandidate(Mob mob) {
        if (!mob.isAlive()) {
            return false;
        }
        MobCategory cat = mob.getType().getCategory();
        return cat == MobCategory.CREATURE
            || cat == MobCategory.AMBIENT
            || cat == MobCategory.WATER_AMBIENT
            || cat == MobCategory.WATER_CREATURE
            || mob instanceof Animal;
    }

    private static void trySpawnPack(ServerLevel level, IslandHeightmap hm) {
        RandomSource rnd = level.random;
        int bx = rnd.nextInt(1041) - 520;
        int bz = rnd.nextInt(1041) - 520;
        if (!hm.isInsideIsland(bx, bz)) {
            return;
        }
        level.getChunk(bx >> 4, bz >> 4);
        int sy = hm.getSurfaceY(bx, bz);
        int sampleY = Mth.clamp(sy + 1, CompatAccess.getMinBuildHeight(level) + 2, CompatAccess.getMaxBuildHeight(level) - 4);
        Holder<Biome> biome = level.getBiome(new BlockPos(bx, sampleY, bz));
        java.util.List<ResourceLocation> pool = peacefulMorphSpawnPool(biome);
        if (pool.isEmpty()) {
            return;
        }
        ResourceLocation pick = pool.get(rnd.nextInt(pool.size()));
        EntityType<?> entityType = CompatAccess.getEntityType(pick);
        if (entityType == null) {
            return;
        }

        int pack = Mth.clamp(1 + rnd.nextInt(2), 1, 2);
        for (int n = 0; n < pack; n++) {
            BlockPos feet = resolveSpawnFeet(level, bx, bz, hm, entityType, rnd);
            if (feet == null) {
                return;
            }
            spawnMob(level, entityType, feet, rnd);
        }
    }

    private static BlockPos resolveSpawnFeet(ServerLevel level, int bx, int bz, IslandHeightmap hm, EntityType<?> type, RandomSource rnd) {
        MobCategory cat = type.getCategory();
        if (cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT) {
            int surface = hm.getSurfaceY(bx, bz);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(bx, surface, bz);
            for (int dy = 0; dy < 28; dy++) {
                m.setY(surface - dy);
                if (level.getFluidState(m).is(FluidTags.WATER) && level.getFluidState(m.above()).is(FluidTags.WATER)) {
                    return m.immutable();
                }
            }
            return null;
        }
        if (cat == MobCategory.AMBIENT) {
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            int y = Mth.clamp(
                ground + 2 + rnd.nextInt(10),
                CompatAccess.getMinBuildHeight(level) + 2,
                CompatAccess.getMaxBuildHeight(level) - 4);
            return new BlockPos(bx, y, bz);
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return new BlockPos(bx, y + 1, bz);
    }

    private static void spawnMob(ServerLevel level, EntityType<?> type, BlockPos feet, RandomSource rnd) {
        Entity created = CompatAccess.createEntity(type, level);
        if (!(created instanceof Mob mob)) {
            return;
        }
        float yaw = rnd.nextFloat() * 360.0F;
        CompatAccess.moveEntity(mob, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0.0F);
        // Allow natural despawn — persistence flooded the island and tanked TPS.
        if (!level.noCollision(null, mob.getBoundingBox())) {
            return;
        }
        level.addFreshEntity(mob);
    }
}
