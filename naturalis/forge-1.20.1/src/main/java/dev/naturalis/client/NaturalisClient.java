package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.client.render.EchoSovereignRenderer;
import dev.naturalis.client.render.MorphBeaconRenderer;
import dev.naturalis.content.NaturalisEntityTypes;
import dev.naturalis.client.screen.EchoForgeScreen;
import dev.naturalis.client.screen.MorphBeaconScreen;
import dev.naturalis.client.screen.MorphArmorForgeScreen;
import dev.naturalis.client.screen.MorphKnowledgeScreen;
import dev.naturalis.content.NaturalisBlockEntities;
import dev.naturalis.content.NaturalisMenus;
import dev.naturalis.network.CurlSleepTogglePayload;
import dev.naturalis.network.MorphMovementKeyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-side event registrations on the MOD bus (registration events). */
@EventBusSubscriber(modid = Naturalis.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NaturalisClient {

    public static final KeyMapping OPEN_KNOWLEDGE_KEY = new KeyMapping(
        "key.naturalis.open_knowledge",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.naturalis"
    );

    public static final KeyMapping RESONANCE_INSTINCT_KEY = new KeyMapping(
        "key.naturalis.resonance_instinct",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.naturalis"
    );

    public static final KeyMapping CURL_SLEEP_KEY = new KeyMapping(
        "key.naturalis.curl_sleep",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        "key.categories.naturalis"
    );

    public static final KeyMapping PRIMAL_MOVEMENT_KEY = new KeyMapping(
        "key.naturalis.primal_movement",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.naturalis"
    );

    private NaturalisClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(NaturalisMenus.ECHO_FORGE.get(), EchoForgeScreen::new);
            MenuScreens.register(NaturalisMenus.MORPH_KNOWLEDGE.get(), MorphKnowledgeScreen::new);
            MenuScreens.register(NaturalisMenus.MORPH_ARMOR_FORGE.get(), MorphArmorForgeScreen::new);
            MenuScreens.register(NaturalisMenus.MORPH_BEACON.get(), MorphBeaconScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NaturalisBlockEntities.MORPH_BEACON.get(), MorphBeaconRenderer::new);
        event.registerEntityRenderer(NaturalisEntityTypes.ECHO_SOVEREIGN.get(), EchoSovereignRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addRenderLayers(EntityRenderersEvent.AddLayers event) {
        // Attach to both player skin model renderers.
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new MorphArmorRenderLayer<>(renderer));
            }
        }

        // WoodWalkers renders morphed players through the shape's own renderer, not PlayerRenderer.
        // Attach this layer to all living renderers so morphs also display morph armor.
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER) {
                continue;
            }

            try {
                @SuppressWarnings("unchecked")
                EntityRenderer<?> baseRenderer = event.getRenderer(
                    (EntityType<? extends net.minecraft.world.entity.LivingEntity>) type);
                if (baseRenderer instanceof LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new MorphArmorRenderLayer<>(livingRenderer));
                }
            } catch (ClassCastException e) {
                // Entity type doesn't have a LivingEntityRenderer (e.g., has NoopRenderer).
                // Skip it safely.
            }
        }
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KNOWLEDGE_KEY);
        event.register(RESONANCE_INSTINCT_KEY);
        event.register(CURL_SLEEP_KEY);
        event.register(PRIMAL_MOVEMENT_KEY);
    }

    /** Client-side per-tick input events on the FORGE bus. */
    @EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
    public static final class InputEvents {

        private static boolean lastPrimalMovementDown;

        private InputEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player == null) {
                lastPrimalMovementDown = false;
                return;
            }

            MorphTextRefreshClientEvents.tick();
            dev.naturalis.client.perception.MorphSniffClientState.tick();
            ScentTrailClient.tick();

            while (OPEN_KNOWLEDGE_KEY.consumeClick()) {
                minecraft.player.connection.sendCommand("morph knowledge");
            }

            while (RESONANCE_INSTINCT_KEY.consumeClick()) {
                minecraft.player.connection.sendCommand("morph resonance instinct");
            }

            while (CURL_SLEEP_KEY.consumeClick()) {
                dev.naturalis.network.NaturalisNetwork.CHANNEL.sendToServer(new CurlSleepTogglePayload());
            }

            boolean primalMovementDown = !MorphQuickSlotClient.shouldBlockTransformKey() && PRIMAL_MOVEMENT_KEY.isDown();
            if (primalMovementDown != lastPrimalMovementDown) {
                dev.naturalis.network.NaturalisNetwork.CHANNEL.sendToServer(new MorphMovementKeyPayload(primalMovementDown));
                lastPrimalMovementDown = primalMovementDown;
            }
        }
    }
}
