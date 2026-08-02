package dev.naturalis.metabolism;

import dev.naturalis.NaturalisMod;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MetabolismLogic {

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "root");
    private static final ResourceLocation ADV_IMMOVABLE_OBJECT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "inertia/immovable_object");
    private static final ResourceLocation ADV_FEATHER_STEP = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "inertia/feather_step");

    private MetabolismLogic() {
    }

    public static void tick(ServerPlayer player) {
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        MorphInertiaLogic.tick(player, morphId);

        if (morphId != null && MetabolismManager.getMass(morphId) >= 8.0D && player.isSprinting()) {
            grantAdvancement(player, ADV_IMMOVABLE_OBJECT);
        }
    }

    public static void onJump(ServerPlayer player) {
        MorphInertiaLogic.onJump(player, CurrentMorphUtil.getCurrentMorphId(player));
    }

    /** @return fall damage multiplier to multiply into the vanilla/event multiplier (1.0 = unchanged). */
    public static float fallDamageMultiplier(ServerPlayer player, float fallDistance) {
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return 1.0F;
        }

        double mass = MetabolismManager.getMass(morphId);
        if (mass < 0.5D && fallDistance >= 8.0F) {
            grantAdvancement(player, ADV_FEATHER_STEP);
        }
        return (float) MassInertiaManager.getFallDamageMultiplier(mass);
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
