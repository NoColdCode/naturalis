package dev.naturalis.util;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import dev.tocraft.remorphed.Remorphed;
import dev.tocraft.remorphed.impl.PlayerMorph;
import dev.tocraft.walkers.api.PlayerAbilities;
import dev.tocraft.walkers.api.PlayerShape;
import dev.tocraft.walkers.api.PlayerShapeChanger;
import dev.tocraft.walkers.api.variant.ShapeType;
import dev.tocraft.walkers.impl.PlayerDataProvider;

import java.util.Map;

public final class MorphAcquisition {

    private MorphAcquisition() {
    }

    @SuppressWarnings("unchecked")
    public static boolean acquire(ServerPlayer player, ResourceLocation mobId) {
        try {
            EntityType<?> entityType = resolveEntityType(mobId);
            if (entityType == null) {
                player.displayClientMessage(
                    Component.literal("[Naturalis] Entity type not in registry: " + mobId), true);
                return false;
            }
            if (!isLivingEntityType(entityType, player)) {
                player.displayClientMessage(
                    Component.literal("[Naturalis] Not a living entity: " + mobId), true);
                return false;
            }

            ShapeType<? extends LivingEntity> shapeType = ShapeType.from((EntityType<? extends LivingEntity>) entityType);

            // Unlock so the morph appears in the Remorphed menu.
            Map<ShapeType<? extends LivingEntity>, Integer> unlockedShapes = PlayerMorph.getUnlockedShapes(player);
            if (!unlockedShapes.containsKey(shapeType)) {
                unlockedShapes.put(shapeType, Remorphed.getKillToUnlock(shapeType.getEntityType()));
            }

            // Directly set the 2nd shape â€” bypasses UNLOCK_SHAPE event so Remorphed kill gates can't block this.
            PlayerDataProvider provider = (PlayerDataProvider) player;
            provider.walkers$set2ndShape(shapeType);
            PlayerShapeChanger.sync(player);
            PlayerAbilities.sync(player);

            // Apply visual transform immediately.
            LivingEntity shape = shapeType.create(player.level(), player);
            if (shape != null) {
                provider.walkers$updateShapes(shape);
                PlayerShape.sync(player);
            }

            return true;
        } catch (Exception e) {
            player.displayClientMessage(
                Component.literal("[Naturalis] Acquire error: " + e.getClass().getSimpleName() + ": " + e.getMessage()), true);
            return false;
        }
    }

    private static EntityType<?> resolveEntityType(ResourceLocation mobId) {
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
            EntityType<?> resolved = CompatAccess.getEntityType(mobId);
            if (resolved != null) {
                return resolved;
            }
        }

        // Runtime fallback: covers cases where the live entity registry has the id
        // but BuiltInRegistries lookup path misses it.
        EntityType<?> byString = EntityType.byString(mobId.toString()).orElse(null);
        if (byString != null) {
            return byString;
        }

        ResourceLocation vanillaFallback = ResourceLocation.fromNamespaceAndPath("minecraft", mobId.getPath());
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(vanillaFallback)) {
            EntityType<?> resolved = CompatAccess.getEntityType(vanillaFallback);
            if (resolved != null) {
                return resolved;
            }
        }

        byString = EntityType.byString(vanillaFallback.toString()).orElse(null);
        if (byString != null) {
            return byString;
        }

        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (id.getPath().equals(mobId.getPath())) {
                EntityType<?> resolved = CompatAccess.getEntityType(id);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return null;
    }

    private static boolean isLivingEntityType(EntityType<?> entityType, ServerPlayer player) {
        if (LivingEntity.class.isAssignableFrom(entityType.getBaseClass())) {
            return true;
        }

        // NeoForge mappings can report broad base classes for entity types.
        // Runtime probe keeps validation accurate for real living mobs.
        if (player.level() instanceof ServerLevel serverLevel) {
            return CompatAccess.createEntity(entityType, serverLevel) instanceof LivingEntity;
        }
        return false;
    }

    public static Component formatAcquireSuccess(ResourceLocation id) {
        return Component.translatable("command.naturalis.morph.acquire.success", id.toString());
    }

    public static Component formatAcquireFailed(ResourceLocation id) {
        return Component.translatable("command.naturalis.morph.acquire.failed", id.toString());
    }
}
