package dev.naturalis.worldgen;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hard safety net for the Natural Dimension when biome spawn lists or boost spawns go wrong.
 * Scans near players only — never the whole ±520 island AABB.
 */
public final class NaturalDimensionEntityBudget {

    /** Soft ceiling for living mobs near players (players / bosses excluded from cull). */
    private static final int HARD_MOB_CAP = 100;
    private static final int CULL_INTERVAL_TICKS = 40;
    private static final double PLAYER_SCAN_RADIUS = 96.0D;

    private NaturalDimensionEntityBudget() {
    }

    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(NaturalDimensionKeys.NATURAL_DIMENSION)) {
            return;
        }
        if (level.getGameTime() % CULL_INTERVAL_TICKS != 0L) {
            return;
        }

        Set<UUID> seen = new HashSet<>();
        List<Mob> cullable = new ArrayList<>();
        int mobs = 0;
        for (ServerPlayer player : level.players()) {
            AABB near = player.getBoundingBox().inflate(PLAYER_SCAN_RADIUS);
            for (Entity entity : level.getEntities(null, near)) {
                if (!(entity instanceof Mob mob) || entity instanceof Player) {
                    continue;
                }
                if (!seen.add(entity.getUUID())) {
                    continue;
                }
                mobs++;
                if (EchoSovereignRuntime.isEchoSovereign(mob) || mob.hasCustomName()) {
                    continue;
                }
                cullable.add(mob);
            }
        }
        if (mobs <= HARD_MOB_CAP) {
            return;
        }

        int need = mobs - HARD_MOB_CAP;
        cullable.sort(Comparator
            .comparingInt((Mob m) -> cullPriority(m))
            .thenComparingDouble(m -> -distanceToNearestPlayerSq(level, m)));

        int removed = 0;
        for (Mob mob : cullable) {
            if (removed >= need) {
                break;
            }
            mob.discard();
            removed++;
        }
    }

    private static int cullPriority(Mob mob) {
        MobCategory cat = mob.getType().getCategory();
        if (cat == MobCategory.WATER_AMBIENT || cat == MobCategory.WATER_CREATURE) {
            return 0;
        }
        if (cat == MobCategory.AMBIENT) {
            return 1;
        }
        if (cat == MobCategory.MONSTER) {
            return 2;
        }
        if (cat == MobCategory.CREATURE) {
            return 3;
        }
        return 4;
    }

    private static double distanceToNearestPlayerSq(ServerLevel level, Mob mob) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            best = Math.min(best, mob.distanceToSqr(player));
        }
        return best;
    }
}
