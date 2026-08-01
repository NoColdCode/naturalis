package dev.naturalis.client.perception;

import dev.naturalis.experience.NaturalisExperienceProfile;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Non-verbal identity drift: body sway, breathing FOV, and vision stress deepen as humanity fades.
 */
public final class MorphEmbodimentDriftEffects {

    private static float breathPhase;

    private MorphEmbodimentDriftEffects() {
    }

    public static void applyToCamera(
        Camera camera,
        Player player,
        MorphEmbodimentProfile profile,
        float partialTick
    ) {
        if (!NaturalisExperienceProfile.useMorphHeadAndCameraClient()) {
            return;
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        if (drift < 0.04F || !profile.hasEmbodiment()) {
            return;
        }

        breathPhase += (0.04F + drift * 0.03F) * (1.0F + partialTick * 0.05F);
        float breath = (float) Math.sin(breathPhase) * drift * 0.9F;

        float roll = MorphAnimalView.cameraRollDegrees(player, profile, partialTick);
        roll += (float) Math.sin(breathPhase * 0.7D) * drift * 2.2F;
        setRoll(camera, roll);

        nudgePosition(camera, 0.0D, breath * 0.006D, 0.0D);
    }

    public static float fovBreathOffset() {
        if (!MorphMotionVisionClient.shouldApplyMotionFov()) {
            return 0.0F;
        }
        float drift = MorphIdentityDriftClient.embodimentBlend();
        if (drift < 0.08F) {
            return 0.0F;
        }
        float scale = MorphMotionVisionClient.motionFovMultiplier();
        return (float) Math.sin(breathPhase * 1.1D) * drift * 2.5F * scale;
    }

    private static void nudgePosition(Camera camera, double dx, double dy, double dz) {
        try {
            Method getPos = Camera.class.getDeclaredMethod("getPosition");
            getPos.setAccessible(true);
            var pos = (net.minecraft.world.phys.Vec3) getPos.invoke(camera);
            Method set = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
            set.setAccessible(true);
            set.invoke(camera, pos.x + dx, pos.y + dy, pos.z + dz);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setRoll(Camera camera, float roll) {
        for (String name : new String[]{"roll", "zRot"}) {
            try {
                Field field = Camera.class.getDeclaredField(name);
                field.setAccessible(true);
                field.setFloat(camera, roll);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
