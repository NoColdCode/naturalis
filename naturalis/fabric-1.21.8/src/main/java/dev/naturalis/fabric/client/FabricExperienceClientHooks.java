package dev.naturalis.fabric.client;

import dev.naturalis.client.ExperienceModeClientActions;
import dev.naturalis.client.ExperienceModeClientCache;
import dev.naturalis.client.SurvivalAsClientCache;
import dev.naturalis.client.SurvivalAsCreateWorldGameModePatch;
import dev.naturalis.client.SurvivalAsTraitsClientPending;
import dev.naturalis.client.screen.NaturalisExperienceChoiceScreen;
import dev.naturalis.client.screen.SurvivalAsTraitsScreen;
import dev.naturalis.network.SetExperienceModePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

/** Fabric mirror of NeoForge {@code NaturalisExperienceClientEvents} + Create World game-mode patch. */
@Environment(EnvType.CLIENT)
public final class FabricExperienceClientHooks {

    private static boolean networkingReady;

    private FabricExperienceClientHooks() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ExperienceModeClientCache.clearPromptPending();
            SurvivalAsClientCache.clear();
            SurvivalAsTraitsClientPending.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick(client));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof CreateWorldScreen createWorld)) {
                return;
            }
            CycleButton<?> gameModeCycle = findGameModeCycle(screen);
            if (gameModeCycle != null) {
                SurvivalAsCreateWorldGameModePatch.patch(gameModeCycle, createWorld);
            }
        });
    }

    private static void registerNetworking() {
        ExperienceModeClientActions.registerChoiceSender(modeId -> {
            if (Minecraft.getInstance().getConnection() != null) {
                ClientPlayNetworking.send(new SetExperienceModePayload((byte) modeId));
            }
        });
    }

    private static void onClientTick(Minecraft mc) {
        if (!networkingReady) {
            registerNetworking();
            networkingReady = true;
        }

        if (mc.player == null) {
            return;
        }

        if (SurvivalAsClientCache.isLocked()) {
            ExperienceModeClientCache.clearPromptPending();
            if (isRemorphedMorphMenu(mc.screen)) {
                mc.setScreen(null);
            }
        }

        if (SurvivalAsTraitsClientPending.hasPending()) {
            SurvivalAsTraitsClientPending.tickDelay();
            if (isRemorphedMorphMenu(mc.screen)) {
                mc.setScreen(null);
            }
            if (SurvivalAsTraitsClientPending.isReady()
                && !(mc.screen instanceof SurvivalAsTraitsScreen)) {
                SurvivalAsTraitsClientPending.Pending pending = SurvivalAsTraitsClientPending.peek();
                if (pending != null && (mc.screen == null || isRemorphedMorphMenu(mc.screen))) {
                    mc.setScreen(new SurvivalAsTraitsScreen(
                        pending.morphId(),
                        pending.mass(),
                        pending.dietId(),
                        pending.traitIds(),
                        pending.traitExtras()
                    ));
                    return;
                }
            }
        }

        if (SurvivalAsClientCache.isLocked()) {
            return;
        }
        if (!ExperienceModeClientCache.isPromptPending()) {
            return;
        }
        if (mc.screen != null) {
            return;
        }
        ExperienceModeClientCache.clearPromptPending();
        mc.setScreen(new NaturalisExperienceChoiceScreen(null));
    }

    private static boolean isRemorphedMorphMenu(Screen screen) {
        if (screen == null) {
            return false;
        }
        String name = screen.getClass().getName();
        return name.contains("remorphed")
            && (name.endsWith("RemorphedScreen") || name.endsWith("RemorphedMenu"));
    }

    private static CycleButton<?> findGameModeCycle(Screen screen) {
        for (GuiEventListener listener : screen.children()) {
            CycleButton<?> found = findGameModeCycle(listener);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static CycleButton<?> findGameModeCycle(GuiEventListener listener) {
        if (listener instanceof CycleButton<?> cycle) {
            if (SurvivalAsCreateWorldGameModePatch.isPatched(cycle)
                || SurvivalAsCreateWorldGameModePatch.isVanillaGameModeCycle(cycle)) {
                return cycle;
            }
            return null;
        }
        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                CycleButton<?> found = findGameModeCycle(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
