package dev.naturalis.gameplay;

import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.resonance.ResonanceCurlBridge;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class FeralCurlSleepSystem {

    private static final String ACTIVE_TAG = "active";
    private static final String PREP_UNTIL_TICK_TAG = "prep_until_tick";
    private static final String PREP_READY_TAG = "prep_ready";
    private static final String ANCHOR_X_TAG = "anchor_x";
    private static final String ANCHOR_Y_TAG = "anchor_y";
    private static final String ANCHOR_Z_TAG = "anchor_z";
    private static final String ANCHOR_YAW_TAG = "anchor_yaw";

    private static final boolean REQUIRE_NIGHT_OR_RAIN = true;
    private static final boolean REQUIRE_SOLID_GROUND = true;
    private static final int PREP_DURATION_TICKS = 5 * 20;

    private FeralCurlSleepSystem() {
    }

    public static void handleToggleRequest(ServerPlayer player) {
        if (!NaturalisConfig.gameplayEnableFeralCurlSleep()) {
            return;
        }
        if (isActive(player)) {
            deactivate(player, true, false);
            return;
        }

        ServerLevel level = getServerLevel(player);
        if (level == null) {
            return;
        }

        CurlFailure failure = getCurlFailure(player, level);
        if (failure == CurlFailure.NONE) {
            activate(player);
            return;
        }

        // Rebirth only rides this keybind when curl cannot start.
        if (NaturalisConfig.resonanceCurlRebirthEnabled() && ResonanceCurlBridge.tryTriggerRebirthFromCurlKey(player)) {
            return;
        }

        sendCurlFailureMessage(player, failure);
    }

    private static CurlFailure getCurlFailure(ServerPlayer player, ServerLevel level) {
        if (CurrentMorphUtil.getCurrentMorphId(player) == null) {
            return CurlFailure.REQUIRE_FERAL;
        }

        if (REQUIRE_NIGHT_OR_RAIN && !isNightOrRain(level)) {
            return CurlFailure.REQUIRE_NIGHT_OR_RAIN;
        }

        if (REQUIRE_SOLID_GROUND && !isOnSolidGround(player)) {
            return CurlFailure.REQUIRE_SOLID_GROUND;
        }

        if (player.isPassenger() || player.isSwimming() || player.isInWater() || player.isSleeping()) {
            return CurlFailure.CANNOT_NOW;
        }

        return CurlFailure.NONE;
    }

    private static void sendCurlFailureMessage(ServerPlayer player, CurlFailure failure) {
        if (failure == CurlFailure.REQUIRE_FERAL) {
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.require_feral"), true);
            return;
        }
        if (failure == CurlFailure.REQUIRE_NIGHT_OR_RAIN) {
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.require_night_or_rain"), true);
            return;
        }
        if (failure == CurlFailure.REQUIRE_SOLID_GROUND) {
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.require_solid_ground"), true);
            return;
        }
        if (failure == CurlFailure.CANNOT_NOW) {
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.cannot_now"), true);
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }

        if (CurrentMorphUtil.getCurrentMorphId(player) == null) {
            deactivate(player, false, true);
            return;
        }

        if (player.isDeadOrDying() || player.isPassenger()) {
            deactivate(player, false, false);
            return;
        }

        BlockPos anchor = getAnchor(player);
        if (anchor == null) {
            deactivate(player, false, false);
            return;
        }

        if (player.blockPosition().distSqr(anchor) > 2.25D) {
            player.teleportTo(anchor.getX() + 0.5D, anchor.getY() + 0.02D, anchor.getZ() + 0.5D);
        }

        // Keep the body fully grounded and still while curled.
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        applyCurlPose(player);
        applyCurlAnimation(player);
        player.resetFallDistance();
    }

    public static void onServerTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null || !isNightOrRain(overworld)) {
            return;
        }

        int eligible = 0;
        int sleepingEquivalent = 0;
        long now = overworld.getGameTime();
        List<ServerPlayer> curledSleepers = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }

            eligible++;
            boolean curledReady = isPreparedAndActive(player, now);
            if (player.isSleeping() || curledReady) {
                sleepingEquivalent++;
            }
            if (curledReady) {
                curledSleepers.add(player);
            }
        }

        int sleepingPercentage = overworld.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        int required = Math.max(1, Mth.ceil(eligible * (sleepingPercentage / 100.0F)));
        if (sleepingEquivalent < required) {
            return;
        }

        if (isNightOrRain(overworld) && isNight(overworld)) {
            long dayTime = overworld.getDayTime();
            long timeToMorning = 24000L - (dayTime % 24000L);
            if (timeToMorning <= 0L) {
                timeToMorning = 24000L;
            }
            overworld.setDayTime(dayTime + timeToMorning);
        }

        if (overworld.isRaining() || overworld.isThundering()) {
            overworld.setWeatherParameters(6000, 0, false, false);
        }

        for (ServerPlayer player : curledSleepers) {
            if (ResonanceManager.isResonanceEnabled(player)) {
                ResonanceManager.applyHumanityActionLoss(player, 10);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSleeping()) {
                player.stopSleeping();
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isActive(player)) {
                deactivate(player, false, false);
            }
        }
    }

    private static void activate(ServerPlayer player) {
        var tag = CompatAccess.getPersistentData(player);
        BlockPos anchor = player.blockPosition();
        long now = player.level().getGameTime();
        tag.putBoolean(ACTIVE_TAG, true);
        tag.putLong(PREP_UNTIL_TICK_TAG, now + PREP_DURATION_TICKS);
        tag.putBoolean(PREP_READY_TAG, false);
        tag.putInt(ANCHOR_X_TAG, anchor.getX());
        tag.putInt(ANCHOR_Y_TAG, anchor.getY());
        tag.putInt(ANCHOR_Z_TAG, anchor.getZ());
        tag.putFloat(ANCHOR_YAW_TAG, player.getYRot());

        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        applyCurlPose(player);
        player.teleportTo(anchor.getX() + 0.5D, anchor.getY() + 0.02D, anchor.getZ() + 0.5D);
        player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.started", 5), true);
    }

    private static void deactivate(ServerPlayer player, boolean notifyStop, boolean lostFeral) {
        CompatAccess.getPersistentData(player).putBoolean(ACTIVE_TAG, false);
        CompatAccess.getPersistentData(player).putBoolean(PREP_READY_TAG, false);
        clearCurlPose(player);
        if (!player.isSleeping()) {
            player.setPose(Pose.STANDING);
        }

        if (lostFeral) {
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.cancelled_not_feral"), true);
            return;
        }

        if (notifyStop) {
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.stopped"), true);
        }
    }

    private static boolean isNightOrRain(ServerLevel level) {
        return isNight(level) || level.isRaining() || level.isThundering();
    }

    private static boolean isNight(ServerLevel level) {
        long timeOfDay = level.getDayTime() % 24000L;
        return timeOfDay >= 12542L && timeOfDay <= 23459L;
    }

    private static boolean isOnSolidGround(ServerPlayer player) {
        if (!player.onGround()) {
            return false;
        }

        ServerLevel level = getServerLevel(player);
        if (level == null) {
            return false;
        }

        BlockPos below = player.blockPosition().below();
        BlockState state = level.getBlockState(below);
        return state.isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
    }

    private static boolean isActive(ServerPlayer player) {
        return CompatAccess.getBoolean(CompatAccess.getPersistentData(player), ACTIVE_TAG);
    }

    private static boolean isPreparedAndActive(ServerPlayer player, long now) {
        if (!isActive(player)) {
            return false;
        }

        var tag = CompatAccess.getPersistentData(player);
        long prepUntil = CompatAccess.getLong(tag, PREP_UNTIL_TICK_TAG);
        if (now < prepUntil) {
            return false;
        }

        if (!CompatAccess.getBoolean(tag, PREP_READY_TAG)) {
            tag.putBoolean(PREP_READY_TAG, true);
            player.displayClientMessage(Component.translatable("message.naturalis.curl_sleep.asleep"), true);
        }

        return true;
    }

    private static BlockPos getAnchor(ServerPlayer player) {
        var tag = CompatAccess.getPersistentData(player);
        if (!CompatAccess.contains(tag, ANCHOR_X_TAG)
            || !CompatAccess.contains(tag, ANCHOR_Y_TAG)
            || !CompatAccess.contains(tag, ANCHOR_Z_TAG)) {
            return null;
        }
        return new BlockPos(
            CompatAccess.getInt(tag, ANCHOR_X_TAG),
            CompatAccess.getInt(tag, ANCHOR_Y_TAG),
            CompatAccess.getInt(tag, ANCHOR_Z_TAG)
        );
    }

    private static ServerLevel getServerLevel(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            return level;
        }
        return null;
    }

    private static void applyCurlPose(ServerPlayer player) {
        // SWIMMING pose gives a reliable flat, belly-to-ground posture across many morph rigs.
        try {
            player.getClass().getMethod("setForcedPose", Pose.class).invoke(player, Pose.SWIMMING);
        } catch (ReflectiveOperationException ignored) {
            player.setPose(Pose.SWIMMING);
        }
        // Keep vanilla swim state true so first/third person both render the curled body state.
        player.setSwimming(true);
    }

    private static void applyCurlAnimation(ServerPlayer player) {
        var tag = CompatAccess.getPersistentData(player);
        float baseYaw = CompatAccess.contains(tag, ANCHOR_YAW_TAG) ? CompatAccess.getFloat(tag, ANCHOR_YAW_TAG) : player.getYRot();
        float t = player.tickCount;

        // Slow breathing pulse and tiny head sweep to avoid a dead-static curl pose.
        float breathe = Mth.sin(t * 0.18F) * 1.6F;
        float bodySway = Mth.sin(t * 0.06F) * 2.4F;
        float headOffset = Mth.cos(t * 0.12F) * 3.0F;

        float bodyYaw = baseYaw + bodySway;
        player.setYRot(bodyYaw);
        player.setYBodyRot(bodyYaw);
        player.setYHeadRot(bodyYaw + headOffset);
        player.yRotO = bodyYaw;
        player.yBodyRotO = bodyYaw;
        player.yHeadRotO = bodyYaw + headOffset;

        float pitch = 16.0F + breathe;
        player.setXRot(pitch);
        player.xRotO = pitch;
    }

    private static void clearCurlPose(ServerPlayer player) {
        try {
            player.getClass().getMethod("setForcedPose", Pose.class).invoke(player, new Object[]{null});
        } catch (ReflectiveOperationException ignored) {
            // Fallback handled by explicit standing pose in deactivate when not sleeping.
        }
        player.setSwimming(false);
    }

    private enum CurlFailure {
        NONE,
        REQUIRE_FERAL,
        REQUIRE_NIGHT_OR_RAIN,
        REQUIRE_SOLID_GROUND,
        CANNOT_NOW
    }
}