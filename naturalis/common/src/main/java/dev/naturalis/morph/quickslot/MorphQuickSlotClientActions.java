package dev.naturalis.morph.quickslot;

import dev.naturalis.morph.quickslot.MorphQuickSlotDebug;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public final class MorphQuickSlotClientActions {

    @FunctionalInterface
    public interface AssignSender {
        void send(int slotIndex, ResourceLocation morphId);
    }

    @FunctionalInterface
    public interface SelectSender {
        void send(int slotIndex, @Nullable ResourceLocation morphId);
    }

    @FunctionalInterface
    public interface RemorphedMorphSender {
        void send(ResourceLocation morphId);
    }

    private static volatile AssignSender assignSender = (slot, morph) -> {};
    private static volatile Runnable resyncSender = () -> {};
    private static volatile SelectSender selectSender = (slot, morph) -> {};
    private static volatile RemorphedMorphSender remorphedMorphSender = morph -> {};
    private static volatile Runnable sessionActiveSender = () -> {};
    private static volatile Runnable sessionInactiveSender = () -> {};

    private MorphQuickSlotClientActions() {
    }

    public static void registerAssignSender(AssignSender sender) {
        assignSender = sender != null ? sender : (slot, morph) -> {};
    }

    public static void registerResyncSender(Runnable sender) {
        resyncSender = sender != null ? sender : () -> {};
    }

    public static void requestResync() {
        resyncSender.run();
    }

    public static void registerSelectSender(SelectSender sender) {
        selectSender = sender != null ? sender : (slot, morph) -> {};
    }

    public static void registerRemorphedMorphSender(RemorphedMorphSender sender) {
        remorphedMorphSender = sender != null ? sender : morph -> {};
    }

    public static void registerSessionSender(Runnable onActive, Runnable onInactive) {
        sessionActiveSender = onActive != null ? onActive : () -> {};
        sessionInactiveSender = onInactive != null ? onInactive : () -> {};
    }

    public static void notifySessionActive() {
        sessionActiveSender.run();
    }

    public static void notifySessionInactive() {
        sessionInactiveSender.run();
    }

    public static void sendAssign(int slotIndex, ResourceLocation morphId) {
        if (morphId == null || slotIndex < 0) {
            return;
        }
        dev.naturalis.client.MorphQuickSlotClientState.assignLocal(slotIndex, morphId);
        assignSender.send(slotIndex, morphId);
        resyncSender.run();
    }

    public static void sendSelect(int slotIndex, @Nullable ResourceLocation morphId) {
        MorphQuickSlotDebug.event("network", "sendSelect slot=" + slotIndex + " morph=" + morphId);
        selectSender.send(slotIndex, morphId);
    }
}
