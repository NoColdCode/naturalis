package dev.naturalis.util;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/** Entity-type checks that survive NeoForge base-class mapping quirks. */
public final class MorphEntityUtil {

    private MorphEntityUtil() {
    }

    public static boolean isLivingEntityType(EntityType<?> entityType, ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return isLivingEntityType(entityType, serverLevel);
        }
        return false;
    }

    public static boolean isLivingEntityType(EntityType<?> entityType, Level level) {
        if (entityType == null) {
            return false;
        }
        if (LivingEntity.class.isAssignableFrom(entityType.getBaseClass())) {
            return true;
        }
        if (entityType.getCategory() != MobCategory.MISC) {
            return true;
        }
        if (level instanceof ServerLevel serverLevel) {
            Entity probe = CompatAccess.createEntity(entityType, serverLevel);
            if (probe instanceof LivingEntity living) {
                living.discard();
                return true;
            }
        }
        return false;
    }
}
