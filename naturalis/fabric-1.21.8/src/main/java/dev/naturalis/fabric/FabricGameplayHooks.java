package dev.naturalis.fabric;

import dev.naturalis.util.MorphDataUtil;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FabricGameplayHooks {

    private FabricGameplayHooks() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            return tryCollectEcho(player, player.getItemInHand(hand), entity)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
        });
    }

    private static boolean tryCollectEcho(Player player, ItemStack inHand, net.minecraft.world.entity.Entity targetEntity) {
        if (player.level().isClientSide()) {
            return false;
        }

        if (!inHand.is(FabricNaturalisItems.ECHO_COLLECTOR)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(inHand)) {
            return false;
        }

        if (!(targetEntity instanceof LivingEntity target) || target.isDeadOrDying()) {
            return false;
        }

        if (target.getHealth() >= target.getMaxHealth() - 0.01F) {
            return false;
        }

        if (target.getHealth() > target.getMaxHealth() * 0.30F) {
            return false;
        }

        int emptyVialSlot = findFirstEmptyVial(player);
        if (emptyVialSlot < 0) {
            return false;
        }

        ItemStack emptyVial = player.getInventory().getItem(emptyVialSlot);
        emptyVial.shrink(1);

        ItemStack filledVial = new ItemStack(FabricNaturalisItems.FILLED_ECHO_VIAL);
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        MorphDataUtil.setMobId(filledVial, mobId.toString());

        boolean placed = false;
        if (emptyVial.isEmpty()) {
            player.getInventory().setItem(emptyVialSlot, filledVial);
            placed = true;
        }

        if (!placed && !player.getInventory().add(filledVial)) {
            player.drop(filledVial, false);
        }

        player.getCooldowns().addCooldown(inHand, 2);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.echo_collected", mobId.toString()), true);
        }

        return true;
    }

    private static int findFirstEmptyVial(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(FabricNaturalisItems.EMPTY_ECHO_VIAL)) {
                return i;
            }
        }
        return -1;
    }
}
