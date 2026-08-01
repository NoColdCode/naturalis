package dev.naturalis.client;

import dev.naturalis.client.perception.MorphSniffClientState;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.network.ScentHintPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One continuous meandering ribbon per scent (entity or nature block anchor).
 */
public final class ScentTrailClient {

    private static final int BASE_TTL = 40;
    private static final int DEEP_SNIFF_TTL = 160;
    private static final int MAX_TRACKED = 6;
    private static final double TARGET_MOVE_REBUILD_SQR = 0.04D;
    private static final double MAX_Y_STEP = 0.06D;

    private static final Map<Integer, ScentRibbon> RIBBONS = new HashMap<>();
    private static Vec3 lastSniffOrigin = Vec3.ZERO;

    private ScentTrailClient() {
    }

    public static void pushHint(int entityId, byte category, int strength) {
        pushHint(entityId, category, strength, false);
    }

    public static void pushHint(int entityId, byte category, int strength, boolean deep) {
        pushHint(new ScentHintPayload(entityId, category, strength), deep);
    }

    public static void pushDeepHint(int entityId, byte category, int strength) {
        pushHint(new ScentHintPayload(entityId, category, strength), true);
    }

    public static void pushHint(ScentHintPayload payload, boolean deep) {
        if (!deep || !isRibbonWorthyCategory(payload.category())) {
            return;
        }
        if (RIBBONS.size() >= MAX_TRACKED && !RIBBONS.containsKey(payload.entityId())) {
            Integer first = RIBBONS.keySet().stream().findFirst().orElse(null);
            if (first != null) {
                RIBBONS.remove(first);
            }
        }
        int ttl = deep ? DEEP_SNIFF_TTL : BASE_TTL;
        byte category = normalizeCategory(payload.category());
        ScentRibbon ribbon = RIBBONS.computeIfAbsent(
            payload.entityId(),
            id -> new ScentRibbon(id, category, Mth.clamp(payload.strength(), 1, 8), deep, payload.anchor())
        );
        ribbon.category = category;
        ribbon.strength = Mth.clamp(payload.strength(), 1, 8);
        ribbon.deep = deep;
        ribbon.ttl = Math.max(ribbon.ttl, ttl);
        if (payload.anchor().isPresent()) {
            ribbon.anchor = payload.anchor();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            if (deep) {
                lastSniffOrigin = mc.player.position();
            }
            ribbon.rebuildPath(mc, true);
        }
    }

    private static boolean isRibbonWorthyCategory(byte category) {
        return switch (category) {
            case ScentHintPayload.CATEGORY_HOSTILE,
                ScentHintPayload.CATEGORY_PREY,
                ScentHintPayload.CATEGORY_PLAYER,
                ScentHintPayload.CATEGORY_PASSIVE -> true;
            case ScentHintPayload.CATEGORY_NATURE -> true;
            default -> false;
        };
    }

    private static byte normalizeCategory(byte category) {
        return switch (category) {
            case ScentHintPayload.CATEGORY_PREY,
                ScentHintPayload.CATEGORY_HOSTILE,
                ScentHintPayload.CATEGORY_PLAYER,
                ScentHintPayload.CATEGORY_PASSIVE,
                ScentHintPayload.CATEGORY_NATURE -> category;
            default -> ScentHintPayload.CATEGORY_PASSIVE;
        };
    }

    public static boolean hasRibbonGeometry() {
        for (ScentRibbon ribbon : RIBBONS.values()) {
            if (ribbon.path != null && ribbon.path.size() >= 2) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDeepRibbons() {
        for (ScentRibbon ribbon : RIBBONS.values()) {
            if (ribbon.deep && ribbon.path != null && ribbon.path.size() >= 2) {
                return true;
            }
        }
        return false;
    }

    public static Collection<ScentRibbon> activeRibbons() {
        return List.copyOf(RIBBONS.values());
    }

    /** Entity id → trail color category while a ribbon is active (not block anchors). */
    public static Optional<Byte> scentCategoryForEntity(int entityId) {
        ScentRibbon ribbon = RIBBONS.get(entityId);
        if (ribbon == null || ribbon.ttl <= 0 || ribbon.anchor.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(ribbon.category);
    }

    /** Path with the tail snapped to the live mob position for smooth motion. */
    public static List<Vec3> renderPath(ScentRibbon ribbon, Minecraft mc, float partialTick) {
        List<Vec3> base = ribbon.path;
        if (base.size() < 2 || mc.level == null) {
            return base;
        }
        if (ribbon.anchor.isPresent()) {
            return base;
        }
        Entity entity = mc.level.getEntity(ribbon.key);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return base;
        }
        Vec3 live = living.getPosition(partialTick).add(0.0D, living.getBbHeight() * 0.12D, 0.0D);
        live = snapAboveGround(mc.level, live);
        List<Vec3> out = new ArrayList<>(base);
        int last = out.size() - 1;
        Vec3 prev = out.get(last - 1);
        double maxY = prev.y + MAX_Y_STEP;
        double minY = prev.y - MAX_Y_STEP;
        live = new Vec3(live.x, Mth.clamp(live.y, minY, maxY), live.z);
        out.set(last, live);
        if (out.size() >= 3) {
            int penult = out.size() - 2;
            Vec3 before = out.get(penult - 1);
            Vec3 mid = out.get(penult);
            Vec3 lerpedMid = new Vec3(
                Mth.lerp(0.45D, mid.x, Mth.lerp(0.65D, before.x, live.x)),
                Mth.clamp(Mth.lerp(0.55D, mid.y, Mth.lerp(0.65D, before.y, live.y)), before.y - MAX_Y_STEP, before.y + MAX_Y_STEP),
                Mth.lerp(0.45D, mid.z, Mth.lerp(0.65D, before.z, live.z))
            );
            out.set(penult, snapAboveGround(mc.level, lerpedMid));
        }
        return out;
    }

    public static void tick() {
        boolean sniffing = MorphSniffClientState.isScentVisionActive();
        if (!sniffing) {
            if (!dev.naturalis.experience.NaturalisExperienceProfile.useScentTrailParticlesClient()
                || !NaturalisConfig.clientHudScentTrails()) {
                clearAll();
                return;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            clearAll();
            return;
        }

        boolean playerMoved = sniffing
            && mc.player.position().distanceToSqr(lastSniffOrigin) > 0.55D;
        if (playerMoved) {
            lastSniffOrigin = mc.player.position();
        }

        Iterator<Map.Entry<Integer, ScentRibbon>> it = RIBBONS.entrySet().iterator();
        while (it.hasNext()) {
            ScentRibbon ribbon = it.next().getValue();
            if (--ribbon.ttl <= 0) {
                it.remove();
                continue;
            }

            if (!ribbon.resolveTarget(mc)) {
                it.remove();
                continue;
            }

            if (sniffing || ribbon.deep) {
                boolean targetMoved = ribbon.targetMoved(mc);
                if (playerMoved || targetMoved) {
                    ribbon.rebuildPath(mc, targetMoved);
                } else {
                    ribbon.maybeRebuildPath(mc, sniffing);
                }
            }
            if (ribbon.path.isEmpty()) {
                ribbon.rebuildPath(mc, false);
            }
        }
    }

    private static void clearAll() {
        RIBBONS.clear();
        lastSniffOrigin = Vec3.ZERO;
    }

    public static int ribbonColor(byte category, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24;
        return switch (category) {
            case ScentHintPayload.CATEGORY_PLAYER -> a | 0x0098D8FF;
            case ScentHintPayload.CATEGORY_PASSIVE -> a | 0x0068F0FF;
            case ScentHintPayload.CATEGORY_PREY -> a | 0x00FFD040;
            case ScentHintPayload.CATEGORY_HOSTILE -> a | 0x00FF6666;
            case ScentHintPayload.CATEGORY_NATURE -> a | 0x00F5F5F5;
            default -> a | 0x0068F0FF;
        };
    }

    public static float ribbonHalfWidth(ScentRibbon ribbon) {
        return 0.07F + ribbon.strength * 0.008F + (ribbon.deep ? 0.02F : 0.0F);
    }

    public static final class ScentRibbon {
        private final int key;
        private byte category;
        private int strength;
        private boolean deep;
        private Optional<BlockPos> anchor = Optional.empty();
        private List<Vec3> path = List.of();
        private long pathBuiltAt;
        private final long pathSeed;
        private int ttl;
        private Vec3 lastTargetPos = Vec3.ZERO;
        private boolean hasTargetPos;

        private ScentRibbon(int key, byte category, int strength, boolean deep, Optional<BlockPos> anchor) {
            this.key = key;
            this.category = category;
            this.strength = strength;
            this.deep = deep;
            this.anchor = anchor == null ? Optional.empty() : anchor;
            this.ttl = deep ? DEEP_SNIFF_TTL : BASE_TTL;
            this.pathSeed = key * 31L + category * 17L;
        }

        public int key() {
            return key;
        }

        public byte category() {
            return category;
        }

        public List<Vec3> path() {
            return path;
        }

        public float alpha() {
            return Mth.clamp(ttl / 80.0F, 0.35F, 1.0F);
        }

        private boolean resolveTarget(Minecraft mc) {
            if (anchor.isPresent()) {
                return true;
            }
            Entity entity = mc.level.getEntity(key);
            return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved();
        }

        private Vec3 targetPoint(Minecraft mc) {
            if (anchor.isPresent()) {
                return Vec3.atCenterOf(anchor.get()).add(0.0D, 0.05D, 0.0D);
            }
            Entity entity = mc.level.getEntity(key);
            if (entity instanceof LivingEntity living) {
                return living.position().add(0.0D, living.getBbHeight() * 0.12D, 0.0D);
            }
            return null;
        }

        private boolean targetMoved(Minecraft mc) {
            Vec3 target = targetPoint(mc);
            if (target == null) {
                return false;
            }
            if (!hasTargetPos) {
                lastTargetPos = target;
                hasTargetPos = true;
                return true;
            }
            boolean moved = target.distanceToSqr(lastTargetPos) > TARGET_MOVE_REBUILD_SQR;
            if (moved) {
                lastTargetPos = target;
            }
            return moved;
        }

        private void maybeRebuildPath(Minecraft mc, boolean scentVision) {
            long now = mc.level.getGameTime();
            long interval = scentVision ? 2L : 10L;
            if (path.isEmpty() || now - pathBuiltAt >= interval) {
                rebuildPath(mc, false);
            }
        }

        private void rebuildPath(Minecraft mc, boolean fastFollow) {
            Vec3 target = targetPoint(mc);
            if (target == null) {
                path = List.of();
                return;
            }
            lastTargetPos = target;
            hasTargetPos = true;
            pathBuiltAt = mc.level.getGameTime();
            List<Vec3> next = buildWanderingPath(mc, target, strength, pathSeed);
            path = blendPath(path, next, fastFollow);
        }

        private static List<Vec3> blendPath(List<Vec3> previous, List<Vec3> next, boolean fastFollow) {
            if (previous == null || previous.size() < 2 || previous.size() != next.size()) {
                return next;
            }
            double lerp = fastFollow ? 0.72D : 0.48D;
            List<Vec3> blended = new ArrayList<>(next.size());
            for (int i = 0; i < next.size(); i++) {
                Vec3 a = previous.get(i);
                Vec3 b = next.get(i);
                double t = fastFollow && i >= next.size() - 3
                    ? Mth.lerp(0.35D, lerp, 0.9D)
                    : lerp;
                blended.add(new Vec3(
                    Mth.lerp(t, a.x, b.x),
                    Mth.lerp(t, a.y, b.y),
                    Mth.lerp(t, a.z, b.z)
                ));
            }
            return blended;
        }
    }

    private static List<Vec3> buildWanderingPath(Minecraft mc, Vec3 targetFeet, int strength, long seed) {
        Vec3 playerFeet = mc.player.position().add(0.0D, 0.06D, 0.0D);

        Vec3 delta = targetFeet.subtract(playerFeet);
        double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        Vec3 forward = horizontalDist > 0.05D
            ? new Vec3(delta.x, 0.0D, delta.z).scale(1.0D / horizontalDist)
            : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);

        if (horizontalDist < 2.5D) {
            double spread = 1.6D + strength * 0.08D;
            Vec3 trailStart = playerFeet.add(right.scale((seed & 1L) == 0L ? spread : -spread));
            Vec3 trailEnd = targetFeet.add(right.scale((seed & 2L) == 0L ? -spread * 0.65D : spread * 0.65D));
            List<Vec3> shortPath = new ArrayList<>(12);
            for (int i = 0; i <= 10; i++) {
                double t = i / 10.0D;
                double wave = Math.sin(t * 4.6D + seed * 0.05D) * (0.35D + strength * 0.04D);
                Vec3 base = trailStart.add(trailEnd.subtract(trailStart).scale(t));
                shortPath.add(base.add(right.scale(wave)));
            }
            shortPath.add(trailEnd);
            return resolvePathAgainstWorld(mc, shortPath);
        }

        int steps = Mth.clamp((int) (horizontalDist / 0.38D) + 14, 22, 40);
        float sideAmp = 0.62F + strength * 0.1F;

        List<Vec3> path = new ArrayList<>(steps + 1);
        double startAlong = Math.min(2.8D, horizontalDist * 0.18D);
        Vec3 trailStart = playerFeet.add(
            forward.x * startAlong + right.x * (Math.sin(seed * 0.07D) * 0.35D),
            0.0D,
            forward.z * startAlong + right.z * (Math.sin(seed * 0.07D) * 0.35D)
        );
        path.add(trailStart);

        Vec3 span = targetFeet.subtract(trailStart);
        double spanHoriz = Math.sqrt(span.x * span.x + span.z * span.z);
        Vec3 spanForward = spanHoriz > 0.05D
            ? new Vec3(span.x, 0.0D, span.z).scale(1.0D / spanHoriz)
            : forward;
        Vec3 spanRight = new Vec3(-spanForward.z, 0.0D, spanForward.x);

        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            double along = t * 0.96D + 0.02D;
            Vec3 base = trailStart.add(
                spanForward.x * spanHoriz * along,
                0.0D,
                spanForward.z * spanHoriz * along
            );
            double w1 = Math.sin(t * 3.2D + seed * 0.05D) * sideAmp;
            double w2 = Math.cos(t * 2.1D + seed * 0.04D) * (sideAmp * 0.55D);
            double w3 = Math.sin(t * 4.8D + seed * 0.06D) * (sideAmp * 0.28D);
            path.add(base.add(
                spanRight.x * (w1 + w3) + spanForward.x * w2 * 0.1D,
                0.0D,
                spanRight.z * (w1 + w3) + spanForward.z * w2 * 0.1D
            ));
        }

        path.add(targetFeet);
        return resolvePathAgainstWorld(mc, path);
    }

    private static List<Vec3> resolvePathAgainstWorld(Minecraft mc, List<Vec3> raw) {
        if (raw.isEmpty() || mc.level == null) {
            return raw;
        }
        Level level = mc.level;
        Entity clipEntity = mc.player;
        List<Vec3> path = new ArrayList<>(raw.size());
        for (Vec3 point : raw) {
            path.add(snapAboveGround(level, point));
        }
        for (int i = 1; i < path.size(); i++) {
            path.set(i, liftSegmentOverBlocks(level, clipEntity, path.get(i - 1), path.get(i)));
            path.set(i, clampVerticalStep(path.get(i - 1), path.get(i)));
        }
        return smoothPathHeights(path);
    }

    private static Vec3 clampVerticalStep(Vec3 from, Vec3 to) {
        double dy = Mth.clamp(to.y - from.y, -MAX_Y_STEP, MAX_Y_STEP);
        return new Vec3(to.x, from.y + dy, to.z);
    }

    private static List<Vec3> smoothPathHeights(List<Vec3> path) {
        if (path.size() < 3) {
            return path;
        }
        List<Vec3> smoothed = new ArrayList<>(path);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 1; i < smoothed.size() - 1; i++) {
                Vec3 prev = smoothed.get(i - 1);
                Vec3 cur = smoothed.get(i);
                Vec3 next = smoothed.get(i + 1);
                double y = (prev.y + cur.y * 2.0D + next.y) * 0.25D;
                y = Mth.clamp(y, prev.y - MAX_Y_STEP, prev.y + MAX_Y_STEP);
                y = Mth.clamp(y, next.y - MAX_Y_STEP, next.y + MAX_Y_STEP);
                smoothed.set(i, new Vec3(cur.x, y, cur.z));
            }
        }
        return smoothed;
    }

    private static Vec3 snapAboveGround(Level level, Vec3 point) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(point.x), Mth.floor(point.z));
        double floorY = surfaceY + 0.1D;
        double y = floorY;
        return clearInsideBlocks(level, point.x, y, point.z, floorY);
    }

    private static Vec3 clearInsideBlocks(Level level, double x, double y, double z, double floorY) {
        BlockPos inside = BlockPos.containing(x, y, z);
        if (!level.getBlockState(inside).blocksMotion()) {
            return new Vec3(x, y, z);
        }
        for (int lift = 1; lift <= 3; lift++) {
            double candidate = inside.getY() + lift + 0.1D;
            if (!level.getBlockState(BlockPos.containing(x, candidate, z)).blocksMotion()) {
                return new Vec3(x, candidate, z);
            }
        }
        return new Vec3(x, floorY + 0.18D, z);
    }

    private static Vec3 liftSegmentOverBlocks(Level level, Entity clipEntity, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (horizontal < 0.04D) {
            return to;
        }
        int steps = Mth.clamp((int) (horizontal / 0.18D) + 1, 2, 16);
        double maxY = to.y;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 sample = new Vec3(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z)
            );
            maxY = Math.max(maxY, requiredClearanceY(level, clipEntity, sample));
        }
        maxY = Math.min(maxY, from.y + MAX_Y_STEP * 1.5D);
        return new Vec3(to.x, maxY, to.z);
    }

    private static double requiredClearanceY(Level level, Entity clipEntity, Vec3 sample) {
        BlockPos feet = BlockPos.containing(sample.x, sample.y, sample.z);
        double y = sample.y;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos check = feet.offset(0, dy, 0);
            BlockState state = level.getBlockState(check);
            if (state.blocksMotion()) {
                y = Math.max(y, check.getY() + 1.0D + 0.1D);
            }
        }
        HitResult hit = level.clip(new ClipContext(
            sample.add(0.0D, 0.4D, 0.0D),
            sample.add(0.0D, -0.8D, 0.0D),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            clipEntity
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            y = Math.max(y, hit.getLocation().y + 0.1D);
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ());
        return Mth.clamp(y, surfaceY + 0.1D, surfaceY + 0.22D);
    }
}
