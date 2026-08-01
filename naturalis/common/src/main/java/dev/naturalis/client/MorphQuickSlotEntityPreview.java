package dev.naturalis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cached mob previews for the morph quick-slot wheel.
 */
public final class MorphQuickSlotEntityPreview {

    private static final Map<ResourceLocation, LivingEntity> CACHE = new HashMap<>();
    private static final AtomicInteger RENDER_ID = new AtomicInteger(9000);

    private MorphQuickSlotEntityPreview() {
    }

    public static void clearCache() {
        for (LivingEntity entity : CACHE.values()) {
            entity.discard();
        }
        CACHE.clear();
    }

    public static void render(GuiGraphics graphics, float centerX, float centerY, int boxSize, float partialTick, @Nullable ResourceLocation morphId) {
        if (morphId == null) {
            return;
        }

        LivingEntity entity = getOrCreate(morphId);
        if (entity == null) {
            return;
        }

        float bbMax = Math.max(entity.getBbHeight(), entity.getBbWidth());
        float renderScale = 25.0F / Math.max(0.35F, bbMax);

        int anchorX = (int) centerX;
        int anchorY = (int) (centerY + boxSize * 0.10F);
        int halfW = Math.max(14, (int) (boxSize * 0.26F));
        int halfH = Math.max(18, (int) (boxSize * 0.32F));
        int x1 = anchorX - halfW;
        int y1 = anchorY - halfH;
        int x2 = anchorX + halfW;
        int y2 = anchorY + (int) (halfH * 1.15F);
        float spin = (Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.getGameTime() + partialTick
            : partialTick) * 0.08F;

        Quaternionf bodyRotation = new Quaternionf().rotationXYZ(0.43633232F, (float) Math.PI + spin, (float) Math.PI);
        Vector3f translation = new Vector3f();

        if (invokeRemorphedPreview(graphics, x1, y1, x2, y2, renderScale, translation, bodyRotation, entity)) {
            return;
        }
        if (invokeLoaderPreview(graphics, x1, y1, x2, y2, renderScale, spin, entity)) {
            return;
        }
        invokeInventoryPreview(graphics, x1, y1, x2, y2, renderScale, bodyRotation, entity);
    }

    @Nullable
    private static LivingEntity getOrCreate(ResourceLocation morphId) {
        LivingEntity cached = CACHE.get(morphId);
        if (cached != null && !cached.isRemoved()) {
            return cached;
        }

        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(morphId);
        if (type.isEmpty()) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }

        LivingEntity living = createPreviewEntity(type.get(), mc);
        if (living == null) {
            return null;
        }

        if (living instanceof Mob mob) {
            mob.setNoAi(true);
        }
        living.setInvulnerable(true);
        CACHE.put(morphId, living);
        return living;
    }

    @Nullable
    private static LivingEntity createPreviewEntity(EntityType<?> type, Minecraft mc) {
        LivingEntity fromShapeType = createViaShapeType(type, mc);
        if (fromShapeType != null) {
            return fromShapeType;
        }

        Entity created = createViaEntityType(type, mc);
        if (created instanceof LivingEntity living) {
            return living;
        }
        if (created != null) {
            created.discard();
        }
        return null;
    }

    @Nullable
    private static LivingEntity createViaShapeType(EntityType<?> type, Minecraft mc) {
        try {
            @SuppressWarnings("unchecked")
            Class<?> shapeTypeClass = Class.forName("dev.tocraft.walkers.api.variant.ShapeType");
            Object shapeType = shapeTypeClass.getMethod("from", EntityType.class).invoke(null, type);
            if (shapeType == null) {
                return null;
            }
            Object entity = shapeTypeClass.getMethod("create", Level.class, Player.class)
                .invoke(shapeType, mc.level, mc.player);
            return entity instanceof LivingEntity living ? living : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private static Entity createViaEntityType(EntityType<?> type, Minecraft mc) {
        try {
            Class<?> spawnReasonClass = Class.forName("net.minecraft.world.entity.EntitySpawnReason");
            Object loadReason = Enum.valueOf((Class<Enum>) spawnReasonClass, "LOAD");
            return (Entity) EntityType.class.getMethod("create", Level.class, spawnReasonClass)
                .invoke(type, mc.level, loadReason);
        } catch (ReflectiveOperationException ignored) {
            // 1.20.x and older loaders.
        }

        try {
            return (Entity) EntityType.class.getMethod("create", Level.class).invoke(type, mc.level);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean invokeRemorphedPreview(
        GuiGraphics graphics,
        int x1,
        int y1,
        int x2,
        int y2,
        float renderScale,
        Vector3f translation,
        Quaternionf bodyRotation,
        LivingEntity entity
    ) {
        try {
            Class<?> remorphedClient = Class.forName("dev.tocraft.remorphed.RemorphedClient");
            remorphedClient.getMethod(
                "renderEntityInInventory",
                int.class,
                GuiGraphics.class,
                int.class,
                int.class,
                int.class,
                int.class,
                float.class,
                Vector3f.class,
                Quaternionf.class,
                Quaternionf.class,
                LivingEntity.class
            ).invoke(
                null,
                RENDER_ID.incrementAndGet(),
                graphics,
                x1,
                y1,
                x2,
                y2,
                renderScale,
                translation,
                bodyRotation,
                null,
                entity
            );
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeLoaderPreview(
        GuiGraphics graphics,
        int x1,
        int y1,
        int x2,
        int y2,
        float renderScale,
        float spin,
        LivingEntity entity
    ) {
        try {
            Class<?> support = Class.forName("dev.naturalis.client.MorphQuickSlotEntityPreviewSupport");
            support.getMethod(
                "render",
                GuiGraphics.class,
                int.class,
                int.class,
                int.class,
                int.class,
                float.class,
                float.class,
                LivingEntity.class
            ).invoke(null, graphics, x1, y1, x2, y2, renderScale, spin, entity);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void invokeInventoryPreview(
        GuiGraphics graphics,
        int x1,
        int y1,
        int x2,
        int y2,
        float renderScale,
        Quaternionf bodyRotation,
        LivingEntity entity
    ) {
        int intScale = (int) renderScale;
        float centerX = (x1 + x2) * 0.5F;
        float centerY = (y1 + y2) * 0.5F;

        if (tryInventoryRender(
            new Class<?>[] {
                GuiGraphics.class, int.class, int.class, int.class, int.class, int.class,
                Vector3f.class, Quaternionf.class, Quaternionf.class, LivingEntity.class
            },
            graphics, x1, y1, x2, y2, intScale, new Vector3f(), bodyRotation, null, entity
        )) {
            return;
        }

        if (tryInventoryRender(
            new Class<?>[] {
                GuiGraphics.class, int.class, int.class, int.class, int.class, float.class,
                Vector3f.class, Quaternionf.class, Quaternionf.class, LivingEntity.class
            },
            graphics, x1, y1, x2, y2, renderScale, new Vector3f(), bodyRotation, null, entity
        )) {
            return;
        }

        tryInventoryRender(
            new Class<?>[] {
                GuiGraphics.class, float.class, float.class, float.class,
                Vector3f.class, Quaternionf.class, Quaternionf.class, LivingEntity.class
            },
            graphics, centerX, centerY, renderScale, new Vector3f(), bodyRotation, new Quaternionf(), entity
        );

        tryInventoryRender(
            new Class<?>[] {
                GuiGraphics.class, int.class, int.class, int.class,
                Quaternionf.class, Quaternionf.class, LivingEntity.class
            },
            graphics, (int) centerX, (int) centerY, intScale, bodyRotation, new Quaternionf(), entity
        );
    }

    private static boolean tryInventoryRender(Class<?>[] signature, Object... args) {
        try {
            Class<?> inventoryScreen = Class.forName("net.minecraft.client.gui.screens.inventory.InventoryScreen");
            inventoryScreen.getMethod("renderEntityInInventory", signature).invoke(null, args);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
