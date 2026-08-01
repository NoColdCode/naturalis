package dev.naturalis.metabolism;

import dev.naturalis.NaturalisMod;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class MetabolismEvents {

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "root");
    private static final ResourceLocation ADV_IMMOVABLE_OBJECT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "inertia/immovable_object");
    private static final ResourceLocation ADV_FEATHER_STEP = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "inertia/feather_step");

    private MetabolismEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        MorphInertiaLogic.tick(player, morphId);

        if (morphId != null && MetabolismManager.getMass(morphId) >= 8.0D && player.isSprinting()) {
            grantAdvancement(player, ADV_IMMOVABLE_OBJECT);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MorphInertiaLogic.onJump(player, CurrentMorphUtil.getCurrentMorphId(player));
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        double mass = MetabolismManager.getMass(morphId);
        float multiplier = (float) MassInertiaManager.getFallDamageMultiplier(mass);
        event.setDamageMultiplier(event.getDamageMultiplier() * multiplier);

        if (mass < 0.5D && event.getDistance() >= 8.0F) {
            grantAdvancement(player, ADV_FEATHER_STEP);
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        AdvancementHolder root = player.getServer().getAdvancements().get(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }
}
