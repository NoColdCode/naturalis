package dev.naturalis.client.perception;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tocraft.walkers.api.PlayerShape;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Third-person (back) and reverse third-person (front / "5th person") camera placement for large morphs.
 * Vanilla max zoom (~4 blocks) clips inside oversized boss models such as Cataclysm's Leviathan.
 */
public final class MorphThirdPersonCamera {

    private static final float VANILLA_MAX_ZOOM = 4.0F;

    /** Extra zoom (blocks beyond vanilla) for known oversized morph path tokens / ids. */
    private static final Map<String, Float> EXTRA_ZOOM_BY_PATH = Map.ofEntries(
        Map.entry("the_leviathan", 14.0F),
        Map.entry("the_baby_leviathan", 4.0F),
        Map.entry("leviathan", 12.0F),
        Map.entry("netherite_monstrosity", 12.0F),
        Map.entry("ender_guardian", 10.0F),
        Map.entry("ancient_remnant", 11.0F),
        Map.entry("the_harbinger", 9.0F),
        Map.entry("scylla", 9.0F),
        Map.entry("maledictus", 8.0F),
        Map.entry("ignis", 9.0F),
        Map.entry("ender_golem", 7.0F),
        Map.entry("coralssus", 7.0F),
        Map.entry("ender_dragon", 10.0F),
        Map.entry("wither", 5.0F),
        Map.entry("warden", 3.0F),
        Map.entry("elder_guardian", 5.0F),
        Map.entry("ravager", 3.5F),
        Map.entry("ghast", 6.0F),
        Map.entry("ur_ghast", 10.0F),
        Map.entry("aerwhale", 8.0F),
        Map.entry("cachalot_whale", 8.0F),
        Map.entry("void_worm", 10.0F),
        Map.entry("fire_dragon", 12.0F),
        Map.entry("ice_dragon", 12.0F),
        Map.entry("lightning_dragon", 12.0F),
        Map.entry("black_frost_dragon", 12.0F)
    );

    /** Vertical lift so the pivot sits above bulky body meshes instead of inside them. */
    private static final Map<String, Float> HEIGHT_BOOST_BY_PATH = Map.ofEntries(
        Map.entry("the_leviathan", 3.5F),
        Map.entry("the_baby_leviathan", 1.0F),
        Map.entry("leviathan", 3.0F),
        Map.entry("netherite_monstrosity", 3.5F),
        Map.entry("ender_guardian", 3.0F),
        Map.entry("ancient_remnant", 3.0F),
        Map.entry("the_harbinger", 2.5F),
        Map.entry("scylla", 2.5F),
        Map.entry("maledictus", 2.0F),
        Map.entry("ignis", 2.5F),
        Map.entry("ender_golem", 2.0F),
        Map.entry("ender_dragon", 2.5F),
        Map.entry("aerwhale", 2.0F),
        Map.entry("ghast", 1.5F),
        Map.entry("ur_ghast", 2.5F),
        Map.entry("void_worm", 2.0F),
        Map.entry("fire_dragon", 3.0F),
        Map.entry("ice_dragon", 3.0F),
        Map.entry("lightning_dragon", 3.0F),
        Map.entry("black_frost_dragon", 3.0F)
    );

    private MorphThirdPersonCamera() {
    }

    /**
     * Called after vanilla {@link Camera#setup} for third-person views.
     * Pushes the camera farther out and lifts it so large morph meshes no longer fill the lens.
     *
     * @param reverse {@code true} for front / reverse third-person (F5 twice)
     */
    public static void adjustAfterSetup(Camera camera, Entity entity, boolean reverse, float partialTick) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player || mc.getCameraEntity() != player) {
            return;
        }
        if (!dev.naturalis.config.NaturalisConfig.clientEmbodimentCameraOffsets()) {
            return;
        }

        ResourceLocation morphId = dev.naturalis.util.CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        float extraZoom = resolveExtraZoom(player, morphId);
        float heightBoost = resolveHeightBoost(player, morphId);
        if (extraZoom < 0.05F && Math.abs(heightBoost) < 0.05F) {
            return;
        }

        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 cam = camera.getPosition();
        Vec3 outward = cam.subtract(eye);
        if (outward.lengthSqr() < 1.0E-6D) {
            Vec3 look = player.getViewVector(partialTick);
            outward = reverse ? look : look.scale(-1.0D);
        }
        Vec3 dir = outward.normalize();
        setPosition(camera, cam.add(dir.scale(extraZoom)).add(0.0D, heightBoost, 0.0D));
    }

    /** Extra distance to add on top of vanilla third-person zoom. */
    public static float resolveExtraZoom(Player player, ResourceLocation morphId) {
        float explicit = explicitExtraZoom(morphId);
        float fromShape = shapeBasedExtraZoom(player);
        return Math.max(explicit, fromShape);
    }

    public static float resolveHeightBoost(Player player, ResourceLocation morphId) {
        float explicit = explicitHeightBoost(morphId);
        float fromShape = shapeBasedHeightBoost(player);
        return Math.max(explicit, fromShape);
    }

    private static float explicitExtraZoom(ResourceLocation morphId) {
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        Float exact = EXTRA_ZOOM_BY_PATH.get(path);
        if (exact != null) {
            return exact;
        }
        for (var entry : EXTRA_ZOOM_BY_PATH.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (path.contains("dragon") || path.contains("whale") || path.contains("leviathan") || path.contains("wyvern")) {
            return 10.0F;
        }
        if (path.contains("boss") || path.contains("golem") || path.contains("giant")) {
            return 5.0F;
        }
        return 0.0F;
    }

    private static float explicitHeightBoost(ResourceLocation morphId) {
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        Float exact = HEIGHT_BOOST_BY_PATH.get(path);
        if (exact != null) {
            return exact;
        }
        for (var entry : HEIGHT_BOOST_BY_PATH.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (path.contains("dragon") || path.contains("whale") || path.contains("leviathan")) {
            return 2.5F;
        }
        return 0.0F;
    }

    private static float shapeBasedExtraZoom(Player player) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape == null) {
            return 0.0F;
        }
        float extent = Math.max(shape.getBbWidth(), shape.getBbHeight());
        // Need roughly 1.6× the longer axis of clearance behind the eye.
        float desired = extent * 1.65F + 1.5F;
        return Mth.clamp(desired - VANILLA_MAX_ZOOM, 0.0F, 24.0F);
    }

    private static float shapeBasedHeightBoost(Player player) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape == null) {
            return 0.0F;
        }
        float height = shape.getBbHeight();
        if (height <= 2.2F) {
            return 0.0F;
        }
        // Lift toward ~40% of shape height above the eye pivot for bulky forms.
        return Mth.clamp(height * 0.35F - 0.6F, 0.0F, 6.0F);
    }

    private static void setPosition(Camera camera, Vec3 position) {
        try {
            Method vec = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            vec.setAccessible(true);
            vec.invoke(camera, position);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method xyz = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
            xyz.setAccessible(true);
            xyz.invoke(camera, position.x, position.y, position.z);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
