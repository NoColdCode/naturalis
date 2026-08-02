package dev.naturalis.instinct;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.network.WanderLookPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives AFK wander by ticking an invisible vanilla mob with full AI at the player's position.
 * Movement deltas and eased look rotation are applied on the server; look is synced to the client.
 */
public final class VanillaMobWanderDriver {

    private static final float MAX_YAW_STEP = 3.0F;
    private static final float MAX_PITCH_STEP = 1.25F;

    private static final Map<UUID, WanderProxy> PROXIES = new ConcurrentHashMap<>();

    private VanillaMobWanderDriver() {
    }

    public static boolean tick(ServerPlayer player, ServerLevel level, ResourceLocation morphId, long now) {
        Mob proxy = resolveProxy(player, level, morphId);
        if (proxy == null) {
            return false;
        }

        Vec3 anchor = player.position();
        CompatAccess.moveEntity(proxy, anchor.x, anchor.y, anchor.z, proxy.getYRot(), proxy.getXRot());
        proxy.setOnGround(player.onGround());
        proxy.fallDistance = player.fallDistance;

        proxy.tick();

        applyMotionAndSmoothLook(player, proxy, anchor);

        var tag = InstinctLogic.getOrCreateInstinctTag(player);
        tag.putLong(InstinctLogic.WANDER_UNTIL_TICK, now + 40L);

        if (InstinctDebug.enabled()) {
            Vec3 delta = proxy.getDeltaMovement();
            InstinctDebug.event(
                player,
                "wander",
                "sim=" + morphId.getPath()
                    + " step=" + String.format("%.3f,%.3f", proxy.getX() - anchor.x, proxy.getZ() - anchor.z)
                    + " delta=" + String.format("%.3f,%.3f,%.3f", delta.x, delta.y, delta.z)
                    + " proxyYaw=" + String.format("%.1f", proxy.getYRot())
                    + " yaw=" + String.format("%.1f", player.getYRot())
            );
        }

        return true;
    }

    private static void applyMotionAndSmoothLook(ServerPlayer player, Mob proxy, Vec3 anchor) {
        Vec3 motion = proxy.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-8) {
            player.move(MoverType.PLAYER, motion);
        }

        player.setDeltaMovement(motion);

        LookTarget look = resolveLookTarget(player, proxy, anchor, motion);
        if (InstinctDebug.enabled()) {
            InstinctDebug.event(
                player,
                "wander-look",
                "targetYaw=" + String.format("%.1f", look.yaw())
                    + " playerYaw=" + String.format("%.1f", player.getYRot())
                    + " proxyYaw=" + String.format("%.1f", proxy.getYRot())
                    + " source=" + look.source()
            );
        }
        float bodyYaw = stepTowardAngle(player.yBodyRot, look.yaw(), MAX_YAW_STEP);
        float yaw = stepTowardAngle(player.getYRot(), look.yaw(), MAX_YAW_STEP);
        float headYaw = stepTowardAngle(player.getYHeadRot(), look.headYaw(), MAX_YAW_STEP);
        float pitch = stepTowardPitch(player.getXRot(), look.pitch(), MAX_PITCH_STEP);

        player.setYBodyRot(bodyYaw);
        player.setYRot(yaw);
        player.setYHeadRot(headYaw);
        player.setXRot(pitch);
        player.hurtMarked = true;

        var tag = InstinctLogic.getOrCreateInstinctTag(player);
        tag.putFloat(InstinctLogic.WANDER_SYNC_YAW, yaw);
        tag.putFloat(InstinctLogic.WANDER_SYNC_PITCH, pitch);

        WanderLookSync.send(player, new WanderLookPayload(true, yaw, pitch, bodyYaw));
    }

    private static LookTarget resolveLookTarget(ServerPlayer player, Mob proxy, Vec3 anchor, Vec3 motion) {
        Vec3 after = player.position();
        Vec3 displacement = new Vec3(after.x - anchor.x, 0.0D, after.z - anchor.z);
        double stepX = proxy.getX() - anchor.x;
        double stepZ = proxy.getZ() - anchor.z;
        Vec3 proxyStep = new Vec3(stepX, 0.0D, stepZ);
        Vec3 horizontalMotion = new Vec3(motion.x, 0.0D, motion.z);

        Vec3 facing;
        String source;
        if (displacement.lengthSqr() > 1.0E-10) {
            facing = displacement;
            source = "player-step";
        } else if (proxyStep.lengthSqr() > 1.0E-10) {
            facing = proxyStep;
            source = "proxy-step";
        } else if (horizontalMotion.lengthSqr() > 1.0E-10) {
            facing = horizontalMotion;
            source = "motion";
        } else {
            return new LookTarget(proxy.getYRot(), proxy.getYHeadRot(), proxy.getXRot(), "proxy-yaw");
        }

        float moveYaw = (float) (Mth.atan2(facing.z, facing.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = proxy.getXRot();
        if (isFlyingMob(proxy) && motion.lengthSqr() > 1.0E-8) {
            double horizLen = Math.max(horizontalMotion.length(), 1.0E-4D);
            pitch = (float) (-Mth.atan2(motion.y, horizLen) * Mth.RAD_TO_DEG);
            pitch = Mth.clamp(pitch, -30.0F, 30.0F);
        }
        return new LookTarget(moveYaw, moveYaw, pitch, source);
    }

    private static float stepTowardAngle(float current, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxStep) {
            delta = maxStep;
        } else if (delta < -maxStep) {
            delta = -maxStep;
        }
        return current + delta;
    }

    private static float stepTowardPitch(float current, float target, float maxStep) {
        float delta = target - current;
        if (delta > maxStep) {
            delta = maxStep;
        } else if (delta < -maxStep) {
            delta = -maxStep;
        }
        return Mth.clamp(current + delta, -90.0F, 90.0F);
    }

    public static void discardIfActive(ServerPlayer player) {
        WanderProxy session = PROXIES.remove(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.mob.isAlive()) {
            session.mob.discard();
        }
        WanderLookSync.sendClear(player);
    }

    public static void discard(ServerPlayer player) {
        discardIfActive(player);
    }

    public static boolean hasActiveProxy(ServerPlayer player) {
        WanderProxy session = PROXIES.get(player.getUUID());
        return session != null && session.mob.isAlive();
    }

    private static void removeProxyEntity(ServerPlayer player) {
        WanderProxy session = PROXIES.remove(player.getUUID());
        if (session != null && session.mob.isAlive()) {
            session.mob.discard();
        }
    }

    @Nullable
    private static Mob resolveProxy(ServerPlayer player, ServerLevel level, ResourceLocation morphId) {
        WanderProxy session = PROXIES.get(player.getUUID());
        if (session != null && morphId.equals(session.morphId) && session.mob.isAlive()) {
            return session.mob;
        }

        removeProxyEntity(player);

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return null;
        }

        Entity created = CompatAccess.createEntity(type, level);
        if (!(created instanceof Mob mob)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }

        configureProxy(mob);
        CompatAccess.moveEntity(mob, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        // Keep off-world: world ticking + manual tick() double-ran AI and tanked TPS.
        // The entity still has a level reference for navigation during our manual tick().
        PROXIES.put(player.getUUID(), new WanderProxy(mob, morphId));
        return mob;
    }

    private static volatile Class<?> flyingMobClass;
    private static volatile boolean flyingMobResolved;

    private static boolean isFlyingMob(Mob proxy) {
        if (!flyingMobResolved) {
            synchronized (VanillaMobWanderDriver.class) {
                if (!flyingMobResolved) {
                    try {
                        flyingMobClass = Class.forName("net.minecraft.world.entity.FlyingMob");
                    } catch (ClassNotFoundException ignored) {
                        flyingMobClass = null;
                    }
                    flyingMobResolved = true;
                }
            }
        }
        return flyingMobClass != null && flyingMobClass.isInstance(proxy);
    }

    private static void configureProxy(Mob mob) {
        mob.setInvisible(true);
        mob.setSilent(true);
        mob.setInvulnerable(true);
        mob.setNoAi(false);
        mob.setPersistenceRequired();
        mob.setCustomNameVisible(false);
        mob.setAggressive(false);
        mob.addTag("naturalis_wander_proxy");
    }

    private record LookTarget(float yaw, float headYaw, float pitch, String source) {
    }

    private record WanderProxy(Mob mob, ResourceLocation morphId) {
    }
}
