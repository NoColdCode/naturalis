package dev.naturalis.client.instinct;

import dev.naturalis.client.perception.MorphAnimalView;
import dev.naturalis.client.perception.MorphBodyYawTracker;
import dev.naturalis.instinct.InstinctClientDebug;
import dev.naturalis.network.WanderLookPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Client-side wander look override. The local player camera is client-authoritative;
 * server steering must be mirrored here each frame (not only on tick — packets can be late).
 */
public final class WanderLookClientState {

    private static boolean active;
    private static float targetYaw;
    private static float targetPitch;
    private static float targetBodyYaw;

    private static int packetsReceived;
    private static int applyCount;
    private static float lastCameraYaw = Float.NaN;
    private static float lastCameraDrift;

    private WanderLookClientState() {
    }

    public static void applyPayload(WanderLookPayload payload) {
        if (!isOnClient()) {
            return;
        }
        if (payload.active()) {
            update(true, payload.yaw(), payload.pitch(), payload.bodyYaw());
            packetsReceived++;
            if (InstinctClientDebug.enabled()) {
                InstinctClientDebug.log(
                    "pkt yaw=" + String.format("%.1f", payload.yaw())
                        + " pitch=" + String.format("%.1f", payload.pitch())
                        + " total=" + packetsReceived
                );
            }
        } else {
            clear();
            if (InstinctClientDebug.enabled()) {
                InstinctClientDebug.log("pkt clear");
            }
        }
    }

    public static void update(boolean wanderActive, float yaw, float pitch, float bodyYaw) {
        active = wanderActive;
        if (wanderActive) {
            targetYaw = yaw;
            targetPitch = pitch;
            targetBodyYaw = bodyYaw;
        }
    }

    public static void clear() {
        active = false;
        packetsReceived = 0;
        applyCount = 0;
        lastCameraYaw = Float.NaN;
        lastCameraDrift = 0.0F;
    }

    public static boolean isActive() {
        return active;
    }

    public static float targetYaw() {
        return targetYaw;
    }

    public static float targetPitch() {
        return targetPitch;
    }

    public static float lastCameraDrift() {
        return lastCameraDrift;
    }

    public static void tick(Player player) {
        apply(player);
    }

    /** Apply after {@link net.minecraft.client.Camera} setup — overrides vanilla look for wander. */
    public static void applyAfterCameraSetup(Minecraft mc) {
        apply(mc != null ? mc.player : null);
    }

    private static void apply(Player player) {
        if (!isOnClient() || !(player instanceof LocalPlayer local) || !active) {
            return;
        }

        float yaw = targetYaw;
        float pitch = targetPitch;
        float bodyYaw = targetBodyYaw;
        float beforeYaw = local.getYRot();

        local.setYBodyRot(bodyYaw);
        local.setYRot(yaw);
        local.setYHeadRot(yaw);
        local.setXRot(pitch);

        MorphAnimalView.setEmbodiedYaw(yaw);
        MorphAnimalView.setEmbodiedPitch(pitch);
        MorphBodyYawTracker.snapBodyYaw(bodyYaw);

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameRenderer != null) {
            Camera camera = mc.gameRenderer.getMainCamera();
            if (camera.isInitialized()) {
                setCameraRotation(camera, yaw, pitch);
                lastCameraYaw = camera.getYRot();
                lastCameraDrift = Mth.wrapDegrees(lastCameraYaw - yaw);
            }
        }

        applyCount++;
        if (InstinctClientDebug.enabled() && local.tickCount % 20 == 0) {
            InstinctClientDebug.log(
                "apply active=" + active
                    + " targetYaw=" + String.format("%.1f", yaw)
                    + " playerYaw=" + String.format("%.1f", local.getYRot())
                    + " before=" + String.format("%.1f", beforeYaw)
                    + " camYaw=" + String.format("%.1f", lastCameraYaw)
                    + " camDrift=" + String.format("%.1f", lastCameraDrift)
                    + " pkts=" + packetsReceived
                    + " applies=" + applyCount
            );
        }
    }

    public static float embodiedYawForCamera(Player player) {
        return active ? targetYaw : Float.NaN;
    }

    public static float embodiedPitchForCamera(Player player) {
        return active ? targetPitch : Float.NaN;
    }

    private static void setCameraRotation(Camera camera, float yaw, float pitch) {
        try {
            Method method = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
            method.setAccessible(true);
            method.invoke(camera, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isOnClient() {
        Thread thread = Thread.currentThread();
        String name = thread.getName();
        if (!name.equals("Render thread") && !name.startsWith("Client")) {
            return false;
        }
        try {
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
            return mc.getMethod("getInstance").invoke(null) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
