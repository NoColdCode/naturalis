package dev.naturalis.config;

import net.minecraft.world.level.Level;

/**
 * Fabric stub matching the public API of NeoForge {@code NaturalisConfig}.
 * Returns sensible defaults until a Fabric config backend is wired.
 */
public final class NaturalisConfig {

    private NaturalisConfig() {
    }

    // ── Effective feature gates (config ∧ optional gamerule) ───────────────────

    public static boolean isColorFilterEnabled(Level level) {
        return dev.naturalis.rule.NaturalisGameRules.isColorFilterEnabled(level);
    }

    public static boolean isInventoryRestrictionEnabled(Level level) {
        return dev.naturalis.rule.NaturalisGameRules.isInventoryRestrictionEnabled(level);
    }

    public static boolean isInstinctsEnabled(Level level) {
        return dev.naturalis.rule.NaturalisGameRules.isInstinctsEnabled(level);
    }

    public static boolean isClientVisionEnabled() {
        return true;
    }

    // ── Common accessors ───────────────────────────────────────────────────────

    public static boolean gameplayEnableQuadrupedRestrictions() {
        return true;
    }

    public static boolean gameplayEnableKnowledgeGates() {
        return true;
    }

    public static boolean gameplayEnablePrimalMovement() {
        return true;
    }

    public static boolean gameplayEnableFeralCurlSleep() {
        return true;
    }

    public static boolean dietEnabled() {
        return true;
    }

    public static boolean dietCarnivorePenalties() {
        return true;
    }

    public static boolean dietHerbivorePenalties() {
        return true;
    }

    public static boolean dietHumanFoodPenaltyWhileMorphed() {
        return true;
    }

    public static boolean humanityEnabled() {
        return true;
    }

    public static boolean resonanceEnabled() {
        return true;
    }

    public static boolean resonanceCurlRebirthEnabled() {
        return true;
    }

    public static boolean morphBindingEnabled() {
        return true;
    }

    public static boolean morphBindingBlockTransformKey() {
        return true;
    }

    public static boolean morphBindingBlockRemorphedMenu() {
        return true;
    }

    public static boolean brewedMorphEnabled() {
        return true;
    }

    public static boolean brewedMorphOverridesBinding() {
        return true;
    }

    public static boolean instinctsScentHints() {
        return true;
    }

    public static int inventoryCanineHotbarSlotsAtRank0() {
        return 1;
    }

    public static int inventoryHandlingRank1Slots() {
        return 4;
    }

    public static int inventoryHandlingRank2Slots() {
        return 5;
    }

    public static int inventoryHandlingRank3Slots() {
        return 6;
    }

    public static int inventoryHandlingRank4Slots() {
        return 7;
    }

    public static int inventoryHandlingRank5Slots() {
        return 8;
    }

    public static int knowledgeUtilitiesRankToMine() {
        return 1;
    }

    public static int knowledgeUtilitiesRankToPlace() {
        return 2;
    }

    public static int knowledgeUtilitiesRankForWorldUse() {
        return 3;
    }

    public static boolean knowledgeQuadrupedDigFeedback() {
        return true;
    }

    // ── Client accessors ───────────────────────────────────────────────────────

    public static boolean clientVisionUseWolfLegacyShader() {
        return true;
    }

    public static boolean clientVisionUploadPaletteUbo() {
        return false;
    }

    public static double clientVisionIntensityMultiplier() {
        return 1.0D;
    }

    public static double clientVisionPhotoStressCap() {
        return 0.35D;
    }

    public static boolean clientEmbodimentFirstPersonBody() {
        return true;
    }

    public static boolean clientEmbodimentPawDigVisuals() {
        return true;
    }

    public static boolean clientEmbodimentCameraOffsets() {
        return true;
    }

    public static boolean clientEmbodimentHideVanillaArms() {
        return true;
    }

    public static double clientEmbodimentFpBodyMinArmHide() {
        return 0.75D;
    }

    public static boolean clientMuteMorphPerceptionSounds() {
        return false;
    }

    public static boolean clientHudKnowledgeHints() {
        return true;
    }

    public static boolean clientHudScentTrails() {
        return true;
    }
}
