package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.screen.NaturalisExperienceChoiceScreen;
import dev.naturalis.client.screen.SurvivalAsTraitsScreen;
import dev.naturalis.network.SetExperienceModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class NaturalisExperienceClientEvents {

    private static boolean networkingReady;

    private NaturalisExperienceClientEvents() {
    }

    private static void registerNetworking() {
        ExperienceModeClientActions.registerChoiceSender(modeId -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new SetExperienceModePayload((byte) modeId));
            }
        });
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ExperienceModeClientCache.clearPromptPending();
        SurvivalAsClientCache.clear();
        SurvivalAsTraitsClientPending.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!networkingReady) {
            registerNetworking();
            networkingReady = true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Survival as…: never open Realistic/Softened, and close Remorphed if it auto-opened.
        if (SurvivalAsClientCache.isLocked()) {
            ExperienceModeClientCache.clearPromptPending();
            if (isRemorphedMorphMenu(mc.screen)) {
                mc.setScreen(null);
            }
        }

        // Keep traits on top until the player dismisses it (Remorphed must not steal it).
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
}
