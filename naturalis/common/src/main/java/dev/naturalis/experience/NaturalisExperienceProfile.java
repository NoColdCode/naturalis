package dev.naturalis.experience;

import dev.naturalis.client.ExperienceModeClientCache;
import dev.naturalis.config.NaturalisConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Gates morph features by per-world experience mode (realistic vs softened).
 * <p>
 * Softened keeps: vision filter, scent trails, cinematic dig view blend, inventory, diet, knowledge.
 * Softened disables: neck limits, gait sway, vibration tremor, FP morph body, dig shake, scratch feedback.
 */
public final class NaturalisExperienceProfile {

    private NaturalisExperienceProfile() {
    }

    public static NaturalisExperienceMode modeOnServer(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return modeOnServer(serverLevel.getServer());
        }
        return NaturalisExperienceMode.UNSET;
    }

    public static NaturalisExperienceMode modeOnServer(MinecraftServer server) {
        if (server == null) {
            return NaturalisExperienceMode.UNSET;
        }
        NaturalisExperienceMode stored = NaturalisWorldExperienceStorage.getMode();
        return stored == NaturalisExperienceMode.UNSET ? NaturalisExperienceMode.REALISTIC : stored;
    }

    public static NaturalisExperienceMode modeOnClient() {
        return ExperienceModeClientCache.getEffectiveMode();
    }

    private static boolean realisticServer(Level level) {
        return modeOnServer(level).isRealistic();
    }

    private static boolean realisticClient() {
        return modeOnClient().isRealistic();
    }

    /** Neck limits, gait sway, eye offsets, dig shake — not the dig blend alone. */
    public static boolean useMorphHeadAndCameraClient() {
        return realisticClient();
    }

    public static boolean useMorphHeadAndCamera(Level level) {
        return realisticServer(level);
    }

    /** Cinematic pull toward block while digging — both profiles (sharp free look otherwise). */
    public static boolean useDigViewBlendClient() {
        return true;
    }

    public static boolean useGaitCameraSwayClient() {
        return realisticClient();
    }

    public static boolean useVibrationCameraClient() {
        return realisticClient();
    }

    public static boolean useFirstPersonMorphBodyClient() {
        return realisticClient();
    }

    public static boolean useHideVanillaArmsClient() {
        return realisticClient();
    }

    public static boolean useScentTrailParticlesClient() {
        return NaturalisConfig.clientHudScentTrails();
    }

    public static boolean useQuadrupedDigFeedbackClient() {
        return realisticClient();
    }

    public static boolean useInstinctScentHints(Level level) {
        return NaturalisConfig.instinctsScentHints();
    }
}
