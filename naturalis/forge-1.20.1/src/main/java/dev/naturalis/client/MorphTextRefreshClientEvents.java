package dev.naturalis.client;

import dev.naturalis.Naturalis;
import dev.naturalis.chat.MorphComprehensionProfile;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = Naturalis.MOD_ID, value = Dist.CLIENT)
public final class MorphTextRefreshClientEvents {

    private static long lastSignHintTick = -200L;

    private MorphTextRefreshClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(mc.player);
        if (morphId == null) {
            return;
        }

        MorphComprehensionProfile.Literacy literacy = MorphComprehensionProfile.getLiteracy(morphId);
        if (literacy == MorphComprehensionProfile.Literacy.CLEAR) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (!(mc.level.getBlockEntity(blockHit.getBlockPos()) instanceof SignBlockEntity)) {
            return;
        }

        long now = mc.level.getGameTime();
        if (now - lastSignHintTick < 20L) {
            return;
        }

        mc.player.displayClientMessage(Component.translatable("message.naturalis.literacy.sign_unreadable"), true);
        lastSignHintTick = now;
    }
}
