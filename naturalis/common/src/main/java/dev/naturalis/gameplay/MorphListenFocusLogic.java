package dev.naturalis.gameplay;

import dev.naturalis.client.perception.MorphHearingProfiles;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.network.ListenPulsePayload;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.network.ScentHintPayload;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Sustained active listen: periodic scans and client focus while the player holds still-ish attention. */
public final class MorphListenFocusLogic {

    private static final String ROOT_TAG = "naturalis_listen_focus";
    private static final String UNTIL_TICK = "until_tick";
    private static final String LAST_PULSE_TICK = "last_pulse_tick";

    private static final int FOCUS_TICKS = 72;
    private static final int PULSE_INTERVAL = 7;

    private MorphListenFocusLogic() {
    }

    public static void beginFocus(ServerPlayer player, ResourceLocation morphId) {
        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        var tag = CompatAccess.getCompound(root, ROOT_TAG);
        long now = player.level().getGameTime();
        tag.putLong(UNTIL_TICK, now + FOCUS_TICKS);
        tag.putLong(LAST_PULSE_TICK, now - PULSE_INTERVAL);
        pulseListen(player, morphId, true);
    }

    public static void tick(ServerPlayer player) {
        if (!isInFocus(player)) {
            return;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || !MorphHearingProfiles.resolve(morphId).hasEnhancedHearing()) {
            clearFocus(player);
            return;
        }
        long now = player.level().getGameTime();
        var tag = CompatAccess.getCompound(CompatAccess.getPersistentData(player), ROOT_TAG);
        long lastPulse = CompatAccess.getLong(tag, LAST_PULSE_TICK);
        if (now - lastPulse >= PULSE_INTERVAL) {
            tag.putLong(LAST_PULSE_TICK, now);
            pulseListen(player, morphId, false);
        }
        if (now >= CompatAccess.getLong(tag, UNTIL_TICK)) {
            clearFocus(player);
        }
    }

    public static boolean isInFocus(ServerPlayer player) {
        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            return false;
        }
        long until = CompatAccess.getLong(CompatAccess.getCompound(root, ROOT_TAG), UNTIL_TICK);
        return player.level().getGameTime() < until;
    }

    private static void clearFocus(ServerPlayer player) {
        CompatAccess.getPersistentData(player).remove(ROOT_TAG);
    }

    private static void pulseListen(ServerPlayer player, ResourceLocation morphId, boolean initial) {
        var profile = MorphHearingProfiles.resolve(morphId);
        double range = profile.scanRange() * (initial ? 1.0D : 1.28D);
        LivingEntity target = findNearestLiving(player, range);
        if (target == null) {
            PlayToClientSender.send(player, new ListenPulsePayload(0, false, ScentHintPayload.CATEGORY_UNKNOWN, 0, -1));
            if (initial) {
                player.level().playSound(
                    null,
                    player.getX(),
                    player.getEyeY(),
                    player.getZ(),
                    BuiltInRegistries.SOUND_EVENT
                        .getOptional(ResourceLocation.withDefaultNamespace("block.sculk_sensor.clicking"))
                        .orElse(SoundEvents.SCULK_SENSOR_STEP),
                    SoundSource.PLAYERS,
                    0.18F,
                    1.55F
                );
            }
            return;
        }

        double bearing = bearingDegrees(player, target.position());
        byte category = classifyListenTarget(morphId, target);
        int dist = (int) Math.round(player.distanceTo(target));
        PlayToClientSender.send(
            player,
            new ListenPulsePayload(
                (int) Math.round(bearing * 10.0D),
                true,
                category,
                dist,
                target.getId()
            )
        );

        float volume = initial ? 0.42F : 0.26F;
        SoundSource source = category == ScentHintPayload.CATEGORY_HOSTILE ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
        player.level().playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            listenPingSound(morphId, target, category),
            source,
            volume,
            0.8F + player.getRandom().nextFloat() * 0.25F
        );
    }

    static byte classifyListenTarget(ResourceLocation morphId, LivingEntity target) {
        if (target instanceof Player) {
            return ScentHintPayload.CATEGORY_HOSTILE;
        }
        if (target instanceof Enemy) {
            return ScentHintPayload.CATEGORY_HOSTILE;
        }
        ResourceLocation targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (targetId != null && InstinctManager.hunts(morphId, targetId)) {
            return ScentHintPayload.CATEGORY_PREY;
        }
        return ScentHintPayload.CATEGORY_UNKNOWN;
    }

    private static net.minecraft.sounds.SoundEvent listenPingSound(
        ResourceLocation morphId,
        LivingEntity target,
        byte category
    ) {
        if (category == ScentHintPayload.CATEGORY_HOSTILE) {
            return SoundEvents.WARDEN_HEARTBEAT;
        }
        if (category == ScentHintPayload.CATEGORY_PREY) {
            return SoundEvents.EXPERIENCE_ORB_PICKUP;
        }
        String path = morphId.getPath();
        if (path.contains("bat")) {
            return SoundEvents.BAT_HURT;
        }
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
    }

    private static LivingEntity findNearestLiving(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = ((ServerLevel) player.level()).getEntitiesOfClass(
            LivingEntity.class,
            box,
            e -> e.isAlive() && e != player
        );
        return nearby.stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    }

    private static double bearingDegrees(Player player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        Vec3 delta = target.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-4D) {
            return 0.0D;
        }
        double worldAngle = Math.toDegrees(Math.atan2(-delta.x, delta.z));
        return Mth.degreesDifference(player.getYRot(), (float) worldAngle);
    }
}
