package dev.naturalis.fabric;

import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphAcquisition;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricMorphEffects {

    private static final int MIN_DURATION_TICKS = 20;
    private static final int DEFAULT_BINDING_DURATION_TICKS = 8 * 20 * 60;

    private static final Map<UUID, TimedMorphState> TIMED_STATES = new ConcurrentHashMap<>();

    private FabricMorphEffects() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTickCount();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                TimedMorphState state = TIMED_STATES.get(player.getUUID());
                if (state == null) {
                    continue;
                }

                if (now >= state.expiresAtTick()) {
                    TIMED_STATES.remove(player.getUUID());
                    continue;
                }

                MorphAcquisition.acquire(player, state.morphId());
            }
        });
    }

    public static boolean applyBrewedMorph(ServerPlayer player, ResourceLocation morphId, int durationTicks) {
        if (player == null || morphId == null) {
            return false;
        }

        if (!isValidLivingMorph(morphId)) {
            return false;
        }

        int clamped = Math.max(MIN_DURATION_TICKS, durationTicks);
        long expiry = player.level().getServer().getTickCount() + clamped;
        TIMED_STATES.put(player.getUUID(), new TimedMorphState(morphId, expiry));
        MorphAcquisition.acquire(player, morphId);
        return true;
    }

    public static boolean applyBindingCurrentMorph(ServerPlayer player) {
        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        if (current == null) {
            return false;
        }

        long expiry = player.level().getServer().getTickCount() + DEFAULT_BINDING_DURATION_TICKS;
        TIMED_STATES.put(player.getUUID(), new TimedMorphState(current, expiry));
        return true;
    }

    private static boolean isValidLivingMorph(ResourceLocation id) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return false;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null || type == EntityType.PLAYER || type.getCategory() == MobCategory.MISC) {
            return false;
        }
        return true;
    }

    private record TimedMorphState(ResourceLocation morphId, long expiresAtTick) {
    }
}
