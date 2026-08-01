package dev.naturalis.client.perception;

import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Loader-neutral embodiment helpers (FOV, camera, first-person arm visibility).
 */
public final class MorphEmbodimentLogic {

    private MorphEmbodimentLogic() {
    }

    public static MorphEmbodimentProfile activeProfile(Minecraft mc) {
        if (mc.player == null || mc.getCameraEntity() != mc.player) {
            return MorphEmbodimentProfile.NONE;
        }
        return MorphEmbodimentProfiles.resolve(CurrentMorphUtil.getCurrentMorphId(mc.player));
    }

    public static MorphEmbodimentProfile profileFor(Player player) {
        if (player == null) {
            return MorphEmbodimentProfile.NONE;
        }
        return MorphEmbodimentProfiles.resolve(CurrentMorphUtil.getCurrentMorphId(player));
    }

    public static void applyCameraOffset(Camera camera, Player player, MorphEmbodimentProfile profile) {
        if (!dev.naturalis.experience.NaturalisExperienceProfile.useMorphHeadAndCameraClient()
            || !NaturalisConfig.clientEmbodimentCameraOffsets() || !profile.hasEmbodiment() || player == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        Vec3 pos = camera.getPosition();
        if (Math.abs(profile.eyeHeightOffset()) > 1.0E-6D) {
            pos = pos.add(0.0D, profile.eyeHeightOffset(), 0.0D);
        }
        if (Math.abs(profile.forwardOffset()) > 1.0E-6D) {
            pos = pos.add(player.getViewVector(1.0F).scale(profile.forwardOffset()));
        }
        setCameraPosition(camera, pos);
    }

    private static void setCameraPosition(Camera camera, Vec3 position) {
        try {
            Method method = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            method.setAccessible(true);
            method.invoke(camera, position);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method method = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
                method.setAccessible(true);
                method.invoke(camera, position.x, position.y, position.z);
            } catch (ReflectiveOperationException ignoredAgain) {
            }
        }
    }

    public static boolean shouldHideFirstPersonArm(Minecraft mc, MorphEmbodimentProfile profile, boolean isBreaking, boolean isPlacing) {
        if (!dev.naturalis.experience.NaturalisExperienceProfile.useHideVanillaArmsClient()
            || !NaturalisConfig.clientEmbodimentHideVanillaArms()) {
            return false;
        }
        if (profile.firstPersonArmHideStrength() < 0.05D) {
            return false;
        }
        if (profile.firstPersonArmHideStrength() >= 0.85D) {
            return true;
        }
        return !isBreaking && !isPlacing;
    }

    public static boolean usesPawDigging(MorphEmbodimentProfile profile) {
        if (!NaturalisConfig.clientEmbodimentPawDigVisuals()) {
            return false;
        }
        return switch (profile.armInteractionStyle()) {
            case CANINE, FELINE, SPIDER, EQUINE, GENERIC -> profile.firstPersonArmHideStrength() >= 0.5D;
            case AVIAN, AQUATIC -> profile.firstPersonArmHideStrength() >= 0.58D;
            default -> false;
        };
    }

    /** Full Walkers shape body in first person (hides vanilla player mesh). */
      public static boolean shouldRenderFirstPersonMorphBody(MorphEmbodimentProfile profile) {
        if (!dev.naturalis.experience.NaturalisExperienceProfile.useFirstPersonMorphBodyClient()
            || !NaturalisConfig.clientEmbodimentFirstPersonBody()) {
            return false;
        }
        return profile.hasEmbodiment()
            && profile.firstPersonArmHideStrength() >= NaturalisConfig.clientEmbodimentFpBodyMinArmHide();
    }

    /** Attack held on a block surface (includes scratch when mining is knowledge-gated). */
    public static boolean isAttackingBlock(Minecraft mc) {
        return isBreakingBlock(mc);
    }

    public static float blockDestroyProgress(Minecraft mc) {
        if (mc.gameMode instanceof MultiPlayerGameMode mode) {
            try {
                Field field = MultiPlayerGameMode.class.getDeclaredField("destroyProgress");
                field.setAccessible(true);
                return field.getFloat(mode);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return isBreakingBlock(mc) ? MorphDigClientState.digAnim() : 0.0F;
    }

    static void setCameraPositionPublic(Camera camera, Vec3 position) {
        setCameraPosition(camera, position);
    }

    public static void setCameraRotationPublic(Camera camera, float yaw, float pitch) {
        setCameraRotation(camera, yaw, pitch);
    }

    private static void setCameraRotation(Camera camera, float yaw, float pitch) {
        try {
            Method method = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
            method.setAccessible(true);
            method.invoke(camera, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void applyVibrationCameraNudge(Camera camera, ResourceLocation morphId) {
        if (!dev.naturalis.experience.NaturalisExperienceProfile.useVibrationCameraClient()) {
            return;
        }
        float intensity = MorphVibrationClientState.feltIntensity();
        if (intensity < 0.04F || morphId == null) {
            return;
        }

        MorphVibrationProfile profile = MorphVibrationProfiles.resolve(morphId);
        if (!profile.hasPawVibrationSense()) {
            return;
        }

        float phase = MorphVibrationClientState.tremorPhase();
        float scale = profile.cameraTremorScale() * intensity;
        float tremorX = (float) Math.sin(phase * 5.1D) * 0.12F * scale;
        float tremorY = (float) Math.sin(phase * 4.3D + 1.1D) * 0.18F * scale;
        setCameraRotation(camera, camera.getYRot() + tremorX, camera.getXRot() + tremorY);
    }

    public static boolean isBreakingBlock(Minecraft mc) {
        return mc.options.keyAttack.isDown()
            && mc.hitResult != null
            && mc.hitResult.getType() == HitResult.Type.BLOCK;
    }

    public static boolean isPlacingBlock(Minecraft mc, net.minecraft.world.InteractionHand hand) {
        return mc.options.keyUse.isDown()
            && mc.hitResult != null
            && mc.hitResult.getType() == HitResult.Type.BLOCK
            && mc.player != null
            && mc.player.getItemInHand(hand).getItem() instanceof BlockItem;
    }
}
