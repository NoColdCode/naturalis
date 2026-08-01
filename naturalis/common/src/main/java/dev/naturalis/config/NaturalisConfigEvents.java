package dev.naturalis.config;

import dev.naturalis.NaturalisMod;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * Server-safe config hooks. Client JSON pref sync lives in {@link NaturalisClientConfigEvents}.
 */
public final class NaturalisConfigEvents {

    private NaturalisConfigEvents() {
    }

    public static void onConfigLoad(ModConfigEvent event) {
        if (!NaturalisMod.ID.equals(event.getConfig().getModId())) {
            return;
        }
        // Client-only pref sync is registered from Naturalis on Dist.CLIENT only.
    }
}
