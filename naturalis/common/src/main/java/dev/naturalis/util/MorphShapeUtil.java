package dev.naturalis.util;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Captures and restores per-entity appearance (species, variant, Cobblemon data, etc.). */
public final class MorphShapeUtil {

    private static final String PLAYER_SHAPES_ROOT = "morph_shapes";

    private MorphShapeUtil() {
    }

    public static CompoundTag captureFromEntity(LivingEntity entity) {
        if (entity == null || entity.level() == null) {
            return new CompoundTag();
        }

        CompoundTag tag = new CompoundTag();
        if (!saveEntityWithoutId(entity, tag)) {
            return new CompoundTag();
        }

        stripEphemeralKeys(tag);
        return tag;
    }

    public static void applyToEntity(LivingEntity entity, CompoundTag shapeData) {
        if (entity == null || shapeData == null || shapeData.isEmpty() || entity.level() == null) {
            return;
        }

        CompoundTag copy = shapeData.copy();
        stripEphemeralKeys(copy);
        if (!loadEntity(entity, copy)) {
            ResonanceShapeCompat.copyKnownVariantData(shapeData, entity);
        }
    }

    public static boolean matches(LivingEntity entity, CompoundTag stored) {
        if (entity == null || stored == null || stored.isEmpty()) {
            return stored == null || stored.isEmpty();
        }
        CompoundTag current = captureFromEntity(entity);
        return current.equals(stored);
    }

    public static void storeForPlayer(ServerPlayer player, ResourceLocation morphId, CompoundTag shapeData) {
        if (player == null || morphId == null || shapeData == null || shapeData.isEmpty()) {
            return;
        }
        CompoundTag root = CompatAccess.getPersistentData(player);
        CompoundTag shapes = CompatAccess.getCompound(root, PLAYER_SHAPES_ROOT);
        shapes.put(morphId.toString(), shapeData.copy());
        root.put(PLAYER_SHAPES_ROOT, shapes);
    }

    public static CompoundTag getForPlayer(ServerPlayer player, ResourceLocation morphId) {
        if (player == null || morphId == null) {
            return new CompoundTag();
        }
        CompoundTag root = CompatAccess.getPersistentData(player);
        CompoundTag shapes = CompatAccess.getCompound(root, PLAYER_SHAPES_ROOT);
        return CompatAccess.getCompound(shapes, morphId.toString());
    }

    public static void applyStoredShape(ServerPlayer player, LivingEntity shape, ResourceLocation morphId) {
        CompoundTag stored = getForPlayer(player, morphId);
        if (!stored.isEmpty()) {
            applyToEntity(shape, stored);
        }
    }

    public static void enforceCurrentShape(ServerPlayer player) {
        if (player == null || player.tickCount % 20 != 0) {
            return;
        }

        LivingEntity shape = getCurrentShape(player);
        if (shape == null) {
            return;
        }

        ResourceLocation morphId = BuiltInRegistries.ENTITY_TYPE.getKey(shape.getType());
        CompoundTag stored = getForPlayer(player, morphId);
        if (stored.isEmpty() || matches(shape, stored)) {
            return;
        }

        applyToEntity(shape, stored);
        syncCurrentShape(player);
    }

    public static void applyShapeData(ServerPlayer player, LivingEntity shape, ResourceLocation morphId, CompoundTag shapeData) {
        if (shapeData != null && !shapeData.isEmpty()) {
            applyToEntity(shape, shapeData);
            storeForPlayer(player, morphId, shapeData);
        } else {
            applyStoredShape(player, shape, morphId);
        }
    }

    private static LivingEntity getCurrentShape(Player player) {
        for (String className : new String[]{"dev.tocraft.walkers.api.PlayerShape", "tocraft.walkers.api.PlayerShape"}) {
            try {
                Class<?> clazz = Class.forName(className);
                Object result = clazz.getMethod("getCurrentShape", Player.class).invoke(null, player);
                if (result instanceof LivingEntity living) {
                    return living;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void syncCurrentShape(Player player) {
        for (String className : new String[]{"dev.tocraft.walkers.api.PlayerShape", "tocraft.walkers.api.PlayerShape"}) {
            try {
                Class<?> clazz = Class.forName(className);
                clazz.getMethod("sync", Player.class).invoke(null, player);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static boolean saveEntityWithoutId(LivingEntity entity, CompoundTag tag) {
        HolderLookup.Provider registries = resolveRegistries(entity);
        if (registries != null) {
            try {
                entity.getClass()
                    .getMethod("saveWithoutId", HolderLookup.Provider.class, CompoundTag.class)
                    .invoke(entity, registries, tag);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            entity.getClass().getMethod("saveWithoutId", CompoundTag.class).invoke(entity, tag);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean loadEntity(LivingEntity entity, CompoundTag tag) {
        HolderLookup.Provider registries = resolveRegistries(entity);
        if (registries != null) {
            try {
                entity.getClass()
                    .getMethod("load", HolderLookup.Provider.class, CompoundTag.class)
                    .invoke(entity, registries, tag);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            entity.getClass().getMethod("load", CompoundTag.class).invoke(entity, tag);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static HolderLookup.Provider resolveRegistries(LivingEntity entity) {
        Level level = entity.level();
        if (level == null) {
            return null;
        }

        try {
            Object registries = level.getClass().getMethod("registryAccess").invoke(level);
            if (registries instanceof HolderLookup.Provider provider) {
                return provider;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Object registries = entity.getClass().getMethod("registryAccess").invoke(entity);
            if (registries instanceof HolderLookup.Provider provider) {
                return provider;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static void stripEphemeralKeys(CompoundTag tag) {
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Rotation");
        tag.remove("UUID");
        tag.remove("Dimension");
        tag.remove("PortalCooldown");
        tag.remove("Air");
        tag.remove("Fire");
        tag.remove("FallDistance");
        tag.remove("OnGround");
    }

    /** Reflection helpers shared with resonance rebirth variant copy. */
    public static final class ResonanceShapeCompat {
        private ResonanceShapeCompat() {
        }

        public static void copyKnownVariantData(CompoundTag fromSnapshot, LivingEntity to) {
            if (fromSnapshot == null || to == null || fromSnapshot.isEmpty()) {
                return;
            }
            Level level = to.level();
            if (level == null) {
                return;
            }
            LivingEntity probe = createProbe(to, level);
            if (probe == null) {
                return;
            }
            applyToEntity(probe, fromSnapshot);
            copyVariantFields(probe, to);
            probe.discard();
        }

        private static LivingEntity createProbe(LivingEntity template, Level level) {
            try {
                var created = CompatAccess.createEntity(
                    template.getType(),
                    level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel : null
                );
                if (created instanceof LivingEntity living) {
                    return living;
                }
                if (created != null) {
                    created.discard();
                }
            } catch (Throwable ignored) {
            }
            return null;
        }

        private static void copyVariantFields(LivingEntity from, LivingEntity to) {
            if (from == null || to == null || from.getType() != to.getType()) {
                return;
            }
            try {
                Object variant = from.getClass().getMethod("getVariant").invoke(from);
                if (variant == null) {
                    return;
                }
                for (java.lang.reflect.Method method : to.getClass().getMethods()) {
                    if ("setVariant".equals(method.getName()) && method.getParameterCount() == 1) {
                        method.invoke(to, variant);
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
