package dev.naturalis.morph.quickslot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MorphQuickSlotBridge {

    private static volatile Consumer<ServerPlayer> syncHandler = player -> {};
    private static volatile BiConsumer<ServerPlayer, ResourceLocation> usageHandler = (player, morphId) -> {};
    private static volatile MorphQuickSlotActions actions = MorphQuickSlotActions.NOOP;

    private MorphQuickSlotBridge() {
    }

    public static void register(
        Consumer<ServerPlayer> sync,
        BiConsumer<ServerPlayer, ResourceLocation> usage,
        MorphQuickSlotActions actionHandler
    ) {
        syncHandler = sync != null ? sync : player -> {};
        usageHandler = usage != null ? usage : (player, morphId) -> {};
        actions = actionHandler != null ? actionHandler : MorphQuickSlotActions.NOOP;
    }

    public static void sync(ServerPlayer player) {
        syncHandler.accept(player);
    }

    public static void setSessionActive(ServerPlayer player, boolean active) {
        MorphQuickSlotServerSession.setActive(player, active);
    }

    public static void recordUsageTick(ServerPlayer player, ResourceLocation morphId) {
        usageHandler.accept(player, morphId);
    }

    public static void handleSelect(ServerPlayer player, int slotIndex, @Nullable ResourceLocation morphId) {
        actions.handleSelect(player, slotIndex, morphId);
    }

    public static void handleAssign(ServerPlayer player, int slotIndex, ResourceLocation morphId) {
        actions.handleAssign(player, slotIndex, morphId);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        syncHandler.accept(player);
    }

    public interface MorphQuickSlotActions {
        MorphQuickSlotActions NOOP = new MorphQuickSlotActions() {
            @Override
            public void handleSelect(ServerPlayer player, int slotIndex, @Nullable ResourceLocation morphId) {
            }

            @Override
            public void handleAssign(ServerPlayer player, int slotIndex, ResourceLocation morphId) {
            }
        };

        void handleSelect(ServerPlayer player, int slotIndex, @Nullable ResourceLocation morphId);

        void handleAssign(ServerPlayer player, int slotIndex, ResourceLocation morphId);
    }
}
