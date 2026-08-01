package dev.naturalis.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import dev.tocraft.walkers.api.PlayerShape;

public final class CurrentMorphUtil {

    private CurrentMorphUtil() {
    }

    @Nullable
    public static ResourceLocation getCurrentMorphId(ServerPlayer player) {
        return getCurrentMorphId((Player) player);
    }

    @Nullable
    public static ResourceLocation getCurrentMorphId(Player player) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(shape.getType());
    }
}
