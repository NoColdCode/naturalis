package dev.naturalis.client.perception;

/**
 * Client-side dig animation state shared between hand cancel, overlay, and feedback.
 */
public final class MorphDigClientState {

    private static float digAnim;
    private static float destroyProgress;
    private static int digScratchPhase;

    private MorphDigClientState() {
    }

    public static float digAnim() {
        return digAnim;
    }

    public static float destroyProgress() {
        return destroyProgress;
    }

    public static int digScratchPhase() {
        return digScratchPhase;
    }

    public static void tick(boolean digging, float blockDestroyProgress) {
        digAnim = MorphArmInteractionLogic.lerpAnim(digAnim, digging);
        destroyProgress = MorphArmInteractionLogic.lerpToward(destroyProgress, blockDestroyProgress);
        if (digging && digAnim > 0.2F) {
            digScratchPhase++;
        }
    }

    public static void reset() {
        digAnim = 0.0F;
        destroyProgress = 0.0F;
        MorphDigViewBlend.reset();
    }

    /** Claw/scratch on a block when mining is locked to animal primary action. */
    public static void pulseScratch() {
        digAnim = Math.max(digAnim, 0.92F);
        digScratchPhase++;
    }
}
