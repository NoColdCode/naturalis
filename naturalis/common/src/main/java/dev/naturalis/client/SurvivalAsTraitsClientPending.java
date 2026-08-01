package dev.naturalis.client;

/**
 * Holds the Survival-as traits popup until the player dismisses it.
 * Survives Remorphed / experience screens stealing focus.
 */
public final class SurvivalAsTraitsClientPending {

    private static String morphId;
    private static double mass;
    private static String dietId;
    private static java.util.List<String> traitIds;
    private static java.util.List<String> traitExtras;
    private static int delayTicks = -1;
    private static boolean dismissed;

    private SurvivalAsTraitsClientPending() {
    }

    public static void queue(
        String morph,
        double massValue,
        String diet,
        java.util.List<String> traits,
        java.util.List<String> extras
    ) {
        morphId = morph;
        mass = massValue;
        dietId = diet;
        traitIds = traits == null ? java.util.List.of() : java.util.List.copyOf(traits);
        traitExtras = extras == null ? java.util.List.of() : java.util.List.copyOf(extras);
        delayTicks = 10; // wait for lock sync + swallow Remorphed auto-open
        dismissed = false;
    }

    public static boolean hasPending() {
        return morphId != null && !dismissed;
    }

    public static void clear() {
        morphId = null;
        dietId = null;
        traitIds = null;
        traitExtras = null;
        delayTicks = -1;
        dismissed = true;
    }

    /** Player closed the traits screen intentionally. */
    public static void dismiss() {
        clear();
    }

    public static void tickDelay() {
        if (delayTicks > 0) {
            delayTicks--;
        }
    }

    public static boolean isReady() {
        return hasPending() && delayTicks <= 0;
    }

    public static Pending peek() {
        if (!hasPending()) {
            return null;
        }
        return new Pending(
            morphId,
            mass,
            dietId,
            traitIds == null ? java.util.List.of() : traitIds,
            traitExtras == null ? java.util.List.of() : traitExtras
        );
    }

    public record Pending(
        String morphId,
        double mass,
        String dietId,
        java.util.List<String> traitIds,
        java.util.List<String> traitExtras
    ) {
    }
}
