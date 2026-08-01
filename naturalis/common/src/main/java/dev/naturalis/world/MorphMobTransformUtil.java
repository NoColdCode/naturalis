package dev.naturalis.world;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import org.jetbrains.annotations.Nullable;

/**
 * Replaces a mob with another entity type (beacon / brewed morph splash on mobs).
 */
public final class MorphMobTransformUtil {

    private static final String BEACON_ROOT = "naturalis_beacon";
    private static final String BEACON_POS = "beacon_pos";
    private static final String ORIGINAL_TYPE = "original_type";

    private MorphMobTransformUtil() {
    }

    public static boolean isValidLivingMorph(ResourceLocation morphId) {
        if (morphId == null) {
            return false;
        }
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        return type != null && LivingEntity.class.isAssignableFrom(type.getBaseClass());
    }

    public static boolean canTransform(Mob mob) {
        if (mob == null || !mob.isAlive()) {
            return false;
        }
        if (mob instanceof Witch || mob instanceof Evoker || mob instanceof Ravager || mob instanceof Warden
            || mob instanceof EnderDragon || mob instanceof WitherBoss) {
            return false;
        }
        if (mob.getType() == EntityType.ELDER_GUARDIAN) {
            return false;
        }
        if (mob.getMaxHealth() >= 80.0F && mob.getType().getCategory() == MobCategory.MONSTER) {
            return false;
        }
        return true;
    }

    /**
     * Beacon morph is more permissive than splash potions — witches and evokers morph immediately
     * (splash applies an effect and waits for damage). True bosses stay immune.
     */
    public static boolean canBeaconTransform(Mob mob) {
        if (mob == null || !mob.isAlive()) {
            return false;
        }
        if (mob instanceof EnderDragon || mob instanceof WitherBoss || mob instanceof Warden) {
            return false;
        }
        if (mob.getType() == EntityType.ELDER_GUARDIAN) {
            return false;
        }
        if (mob.getMaxHealth() >= 200.0F && mob.getType().getCategory() == MobCategory.MONSTER) {
            return false;
        }
        return true;
    }

    public static boolean isBeaconTagged(LivingEntity entity, BlockPos beaconPos) {
        if (entity == null || beaconPos == null) {
            return false;
        }
        CompoundTag root = CompatAccess.getCompound(CompatAccess.getPersistentData(entity), BEACON_ROOT);
        return root.contains(BEACON_POS) && CompatAccess.getLong(root, BEACON_POS) == beaconPos.asLong();
    }

    @Nullable
    public static LivingEntity transformForBeacon(Mob source, ResourceLocation morphId, BlockPos beaconPos) {
        if (!canBeaconTransform(source)) {
            return null;
        }
        ResourceLocation current = BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());
        if (current != null && current.equals(morphId)) {
            return source;
        }

        LivingEntity transformed = replaceEntity(source, morphId);
        if (transformed == null) {
            return null;
        }

        tagBeaconTransform(transformed, beaconPos, current != null ? current : morphId);
        return transformed;
    }

    @Nullable
    public static LivingEntity revertBeaconTransform(LivingEntity entity, BlockPos beaconPos) {
        if (entity == null || !(entity instanceof Mob mob) || !mob.isAlive()) {
            return null;
        }
        CompoundTag root = CompatAccess.getCompound(CompatAccess.getPersistentData(entity), BEACON_ROOT);
        if (!root.contains(BEACON_POS) || CompatAccess.getLong(root, BEACON_POS) != beaconPos.asLong()) {
            return null;
        }
        String original = CompatAccess.getString(root, ORIGINAL_TYPE);
        ResourceLocation originalId = ResourceLocation.tryParse(original);
        if (originalId == null) {
            return null;
        }
        ResourceLocation current = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (originalId.equals(current)) {
            clearBeaconTag(entity);
            return mob;
        }
        LivingEntity restored = replaceEntity(mob, originalId);
        if (restored != null) {
            clearBeaconTag(restored);
        }
        return restored;
    }

    public static void clearBeaconTag(LivingEntity entity) {
        CompatAccess.getPersistentData(entity).remove(BEACON_ROOT);
    }

    @Nullable
    private static LivingEntity replaceEntity(Mob source, ResourceLocation morphId) {
        if (!(source.level() instanceof ServerLevel level)) {
            return null;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return null;
        }

        Entity created = CompatAccess.createEntity(type, level);
        if (!(created instanceof LivingEntity transformed)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }

        CompatAccess.moveEntity(transformed, source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        transformed.setCustomName(source.getCustomName());
        transformed.setCustomNameVisible(source.isCustomNameVisible());
        if (transformed instanceof Mob transformedMob) {
            transformedMob.setNoAi(source.isNoAi());
        }

        float ratio = source.getMaxHealth() <= 0.0F ? 1.0F : source.getHealth() / source.getMaxHealth();
        float targetHealth = Math.max(1.0F, transformed.getMaxHealth() * Math.max(0.1F, Math.min(1.0F, ratio)));
        transformed.setHealth(Math.min(transformed.getMaxHealth(), targetHealth));

        source.discard();
        level.addFreshEntity(transformed);
        return transformed;
    }

    private static void tagBeaconTransform(LivingEntity entity, BlockPos beaconPos, ResourceLocation originalType) {
        CompoundTag root = CompatAccess.getCompound(CompatAccess.getPersistentData(entity), BEACON_ROOT);
        root.putLong(BEACON_POS, beaconPos.asLong());
        root.putString(ORIGINAL_TYPE, originalType.toString());
    }
}
