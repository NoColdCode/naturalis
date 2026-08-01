package dev.naturalis.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Loader-specific brewed morph application ({@link MorphEffectEvents} on NeoForge, {@code FabricMorphEffects} on Fabric). */
public final class BrewedMorphBridge {

    @FunctionalInterface
    public interface Applicator {
        boolean apply(ServerPlayer player, ResourceLocation morphId, int durationTicks);
    }

    private static Applicator applicator = (p, id, d) -> false;

    private BrewedMorphBridge() {
    }

    public static void register(Applicator impl) {
        applicator = impl != null ? impl : (p, id, d) -> false;
    }

    public static boolean apply(ServerPlayer player, ResourceLocation morphId, int durationTicks) {
        return applicator.apply(player, morphId, durationTicks);
    }
}
