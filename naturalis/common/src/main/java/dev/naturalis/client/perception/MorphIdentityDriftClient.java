package dev.naturalis.client.perception;

import dev.naturalis.client.HumanityClientCache;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Tracks how deeply the player has become the morph (humanity loss + time in form).
 * Drives perception scaling and non-verbal embodiment — no action-bar whispers.
 */
public final class MorphIdentityDriftClient {

    private static final int TENURE_FULL_TICKS = 7200;

    private static ResourceLocation trackedMorph;
    private static int morphTenureTicks;

    private MorphIdentityDriftClient() {
    }

    public static void clientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            resetSession();
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            resetSession();
            return;
        }

        if (!morphId.equals(trackedMorph)) {
            trackedMorph = morphId;
            morphTenureTicks = 0;
        } else if (morphTenureTicks < Integer.MAX_VALUE - 1) {
            morphTenureTicks++;
        }
    }

    /**
     * @return 0 = still mostly yourself, 1 = deeply the animal
     */
    public static float embodimentBlend() {
        if (!HumanityClientCache.isActive()) {
            return tenureBlend() * 0.35F;
        }
        float humanityLoss = 1.0F - HumanityClientCache.getHumanity() / 100.0F;
        return Mth.clamp(humanityLoss * 0.72F + tenureBlend() * 0.38F, 0.0F, 1.0F);
    }

    public static float tenureBlend() {
        return Mth.clamp(morphTenureTicks / (float) TENURE_FULL_TICKS, 0.0F, 1.0F);
    }

    public static float humanityBlend() {
        if (!HumanityClientCache.isActive()) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - HumanityClientCache.getHumanity() / 100.0F, 0.0F, 1.0F);
    }

    public static void reset() {
        trackedMorph = null;
        morphTenureTicks = 0;
        MorphAnimalView.reset();
    }

    private static void resetSession() {
        reset();
    }
}
