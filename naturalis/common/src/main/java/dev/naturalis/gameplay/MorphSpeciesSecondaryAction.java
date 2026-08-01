package dev.naturalis.gameplay;



import dev.naturalis.client.perception.MorphArmInteractionStyle;

import dev.naturalis.client.perception.MorphEmbodimentProfiles;

import dev.naturalis.client.perception.MorphEmbodimentProfile;

import dev.naturalis.client.perception.MorphHearingProfiles;

import dev.naturalis.instinct.InstinctManager;

import net.minecraft.resources.ResourceLocation;



import java.util.Locale;

import java.util.Set;



/**

 * Right-click species action while morphed (this update: sniff, listen, peck only).

 */

public enum MorphSpeciesSecondaryAction {

    SNIFF,

    LISTEN,

    PECK,

    NONE;



    private static final Set<String> PECK_PATHS = Set.of("chicken", "parrot");



    private static final Set<String> LISTEN_FIRST_PATHS = Set.of("bat", "rabbit");



    public static MorphSpeciesSecondaryAction resolve(ResourceLocation morphId) {

        if (morphId == null || MorphAnimalInteraction.isHumanoidMorph(morphId)) {

            return NONE;

        }



        String path = morphId.getPath().toLowerCase(Locale.ROOT);

        MorphEmbodimentProfile embodiment = MorphEmbodimentProfiles.resolve(morphId);



        if (prefersSniffSecondary(path, morphId)) {

            return SNIFF;

        }

        if (containsPath(path, PECK_PATHS)

            || (embodiment.armInteractionStyle() == MorphArmInteractionStyle.AVIAN && !path.contains("phantom"))) {

            return PECK;

        }

        if (path.contains("bat") || containsPath(path, LISTEN_FIRST_PATHS)) {

            return LISTEN;

        }

        if (MorphHearingProfiles.resolve(morphId).hasEnhancedHearing()) {

            return LISTEN;

        }

        return NONE;

    }



    public static MorphSpeciesSecondaryAction resolveForItemPickup(ResourceLocation morphId) {

        return resolve(morphId);

    }



    private static boolean prefersSniffSecondary(String path, ResourceLocation morphId) {

        if (!InstinctManager.hasSmellSense(morphId)) {

            return false;

        }

        if (InstinctManager.getSmellStrength(morphId) >= 2) {

            return true;

        }

        return path.contains("wolf")

            || path.contains("fox")

            || path.contains("dog")

            || path.contains("cat")

            || path.contains("ocelot")

            || path.contains("bear")

            || path.contains("pig")

            || path.contains("sniff");

    }



    private static boolean containsPath(String path, Set<String> tokens) {

        if (tokens.contains(path)) {

            return true;

        }

        for (String token : tokens) {

            if (path.contains(token)) {

                return true;

            }

        }

        return false;

    }

}


