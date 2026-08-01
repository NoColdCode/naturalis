package dev.naturalis.command;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Loader-specific hooks used by {@link MorphCommand} without importing Neo-only event classes,
 * so the Brigadier tree can compile on Fabric from shared sources.
 */
public final class MorphCommandBridge {

    @FunctionalInterface
    public interface BrewedMorphApplier {
        boolean apply(ServerPlayer player, ResourceLocation morphId, int durationTicks);
    }

    /** Mirrors resonance rebirth outcomes used by {@link MorphCommand}. */
    public enum RebirthOutcome {
        OK,
        NOT_LOST,
        NOT_MORPHED,
        SPAWN_FAILED,
        FORBIDDEN_IN_NATURAL
    }

    /** Mirrors active-instinct outcomes used by {@link MorphCommand}. */
    public enum InstinctOutcome {
        OK,
        NO_BOND,
        NOT_ACTIVE,
        NOT_ALIGNED,
        COOLDOWN
    }

    private static BrewedMorphApplier brewedMorph = (p, id, t) -> false;
    private static Consumer<ServerPlayer> bondSetSideEffect = p -> {};
    private static Function<ServerPlayer, RebirthOutcome> rebirthResolver = p -> RebirthOutcome.NOT_LOST;
    private static Function<ServerPlayer, InstinctOutcome> instinctResolver = p -> InstinctOutcome.NO_BOND;
    private static BiFunction<ResourceLocation, ResourceLocation, String> dietDebugger = (morph, item) -> "—";

    private MorphCommandBridge() {
    }

    public static boolean applyBrewedMorph(ServerPlayer player, ResourceLocation morphId, int durationTicks) {
        return brewedMorph.apply(player, morphId, durationTicks);
    }

    public static void resonanceOnBondSet(ServerPlayer player) {
        bondSetSideEffect.accept(player);
    }

    public static RebirthOutcome resonanceRebirth(ServerPlayer player) {
        return rebirthResolver.apply(player);
    }

    public static InstinctOutcome resonanceInstinct(ServerPlayer player) {
        return instinctResolver.apply(player);
    }

    public static String debugDiet(ResourceLocation morphId, ResourceLocation itemId) {
        return dietDebugger.apply(morphId, itemId);
    }

    public static void installNeoForge(
        BrewedMorphApplier brewed,
        Consumer<ServerPlayer> onBondSet,
        Function<ServerPlayer, RebirthOutcome> rebirth,
        Function<ServerPlayer, InstinctOutcome> instinct,
        BiFunction<ResourceLocation, ResourceLocation, String> dietDebug) {
        brewedMorph = brewed != null ? brewed : brewedMorph;
        bondSetSideEffect = onBondSet != null ? onBondSet : bondSetSideEffect;
        rebirthResolver = rebirth != null ? rebirth : rebirthResolver;
        instinctResolver = instinct != null ? instinct : instinctResolver;
        dietDebugger = dietDebug != null ? dietDebug : dietDebugger;
    }

    /** Fabric (and other loaders): brewed morph only; resonance/diet use stubs until parity wiring exists. */
    public static void installFabric(BrewedMorphApplier brewed) {
        brewedMorph = brewed != null ? brewed : brewedMorph;
        bondSetSideEffect = p -> {};
        rebirthResolver = p -> RebirthOutcome.NOT_LOST;
        instinctResolver = p -> InstinctOutcome.NO_BOND;
        dietDebugger = (morph, item) -> "n/a (loader)";
    }
}
