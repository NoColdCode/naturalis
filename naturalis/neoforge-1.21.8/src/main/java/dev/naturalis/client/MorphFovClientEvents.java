package dev.naturalis.client;

import dev.naturalis.client.perception.MorphEmbodimentLogic;
import dev.naturalis.client.perception.MorphEmbodimentProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Legacy FOV helpers for code that still queries morph FOV multiplier.
 * Viewport FOV is applied by {@link MorphEmbodimentClientEvents}.
 */
public final class MorphFovClientEvents {

    private MorphFovClientEvents() {
    }

    static double getActiveMorphFovMultiplier(Minecraft mc) {
        MorphEmbodimentProfile profile = MorphEmbodimentLogic.activeProfile(mc);
        return profile.fovMultiplier();
    }

    static double getMorphFovMultiplier(ResourceLocation morphId) {
        return dev.naturalis.client.perception.MorphEmbodimentProfiles.resolve(morphId).fovMultiplier();
    }
}
