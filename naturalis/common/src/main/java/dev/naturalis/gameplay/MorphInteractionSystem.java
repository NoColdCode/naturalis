package dev.naturalis.gameplay;

import dev.naturalis.compat.CompatAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class MorphInteractionSystem {

    private static final String ROOT_TAG = "naturalis_interaction";
    private static final String LAST_BREAK_FEEDBACK_TICK = "last_break_feedback_tick";
    private static final String PLACE_HINT_SHOWN = "place_hint_shown";

    private static final Set<String> HUMANOID_PATHS = Set.of(
        "player", "villager", "wandering_trader", "zombie", "husk", "drowned", "skeleton", "stray",
        "wither_skeleton", "bogged", "creeper", "witch", "evoker", "vindicator", "pillager", "illusioner",
        "piglin", "piglin_brute", "zombified_piglin", "enderman"
    );
    private static final Set<String> DIGGING_PATHS = Set.of(
        "wolf", "fox", "cat", "ocelot", "dog", "polar_bear", "panda", "badger", "armadillo", "sniffer",
        "rabbit", "spider", "cave_spider", "silverfish", "endermite"
    );
    private static final Set<String> HOOF_PATHS = Set.of(
        "horse", "donkey", "mule", "llama", "camel", "cow", "goat", "sheep", "pig", "deer"
    );
    private static final Set<String> PECK_PATHS = Set.of(
        "chicken", "parrot", "duck", "penguin"
    );
    private static final Set<String> BURROW_PATHS = Set.of(
        "rabbit", "armadillo", "sniffer", "silverfish", "endermite"
    );
    private static final Set<String> GEL_PATHS = Set.of(
        "slime", "magma_cube"
    );

    private MorphInteractionSystem() {
    }

    public static float adjustBreakSpeed(Player player, ResourceLocation morphId, float originalSpeed) {
        if (morphId == null) {
            return originalSpeed;
        }

        Profile profile = resolveProfile(morphId);
        return originalSpeed * profile.breakSpeedMultiplier;
    }

    public static void handleBreakFeedback(Player player, ResourceLocation morphId, BlockPos pos, BlockState state) {
        if (morphId == null || player.level().isClientSide()) {
            return;
        }

        Profile profile = resolveProfile(morphId);
        if (profile.humanoidLike) {
            return;
        }

        long now = player.level().getGameTime();
        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        var tag = CompatAccess.getCompound(root, ROOT_TAG);
        long last = CompatAccess.getLong(tag, LAST_BREAK_FEEDBACK_TICK);
        if (now - last < profile.breakFeedbackIntervalTicks) {
            return;
        }
        tag.putLong(LAST_BREAK_FEEDBACK_TICK, now);

        applyBreakAnimation(player, profile);
        player.level().playSound(
            null,
            pos,
            profile.useStepSoundForBreak ? state.getSoundType().getStepSound() : state.getSoundType().getHitSound(),
            SoundSource.PLAYERS,
            profile.breakSoundVolume,
            profile.breakPitchMin + player.getRandom().nextFloat() * (profile.breakPitchMax - profile.breakPitchMin)
        );

        if (player.level() instanceof ServerLevel serverLevel && profile.spawnBlockCrackParticles) {
            serverLevel.levelEvent(2001, pos, Block.getId(state));
        }
    }

    public static boolean handleAlternatePlaceAction(Player player, ResourceLocation morphId, ItemStack stack, BlockPos pos, BlockState state) {
        if (morphId == null) {
            return false;
        }

        Profile profile = resolveProfile(morphId);
        if (profile.humanoidLike) {
            return false;
        }

        if (!(stack.getItem() instanceof BlockItem)) {
            return false;
        }

        if (!player.level().isClientSide()) {
            applyPlaceAnimation(player, profile);
            player.level().playSound(
                null,
                pos,
                profile.useStepSoundForPlace ? state.getSoundType().getStepSound() : state.getSoundType().getHitSound(),
                SoundSource.PLAYERS,
                profile.placeSoundVolume,
                profile.placePitchMin + player.getRandom().nextFloat() * (profile.placePitchMax - profile.placePitchMin)
            );

            if (player.level() instanceof ServerLevel serverLevel && profile.spawnBlockCrackParticles) {
                serverLevel.levelEvent(2001, pos, Block.getId(state));
            }

            CompatAccess.addItemCooldown(player, stack, profile.placeCooldownTicks);
            maybeSendPlaceHint(player);
        }

        return true;
    }

    private static void maybeSendPlaceHint(Player player) {
        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        var tag = CompatAccess.getCompound(root, ROOT_TAG);

        if (CompatAccess.getBoolean(tag, PLACE_HINT_SHOWN)) {
            return;
        }

        tag.putBoolean(PLACE_HINT_SHOWN, true);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.interaction.place_replaced"), true);
    }

    private static Profile resolveProfile(ResourceLocation morphId) {
        String path = morphId.getPath();

        if (isHumanoidPath(path)) {
            return Profile.HUMANOID;
        }
        if (isPathLike(path, PECK_PATHS)) {
            return Profile.PECKER;
        }
        if (isPathLike(path, GEL_PATHS)) {
            return Profile.GEL;
        }
        if (isPathLike(path, HOOF_PATHS)) {
            return Profile.HOOFED;
        }
        if (isPathLike(path, BURROW_PATHS)) {
            return Profile.BURROWER;
        }
        if (isPathLike(path, DIGGING_PATHS)) {
            return Profile.DIGGER;
        }

        var type = CompatAccess.getEntityType(morphId);
        if (type != null) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MONSTER) {
                return Profile.CLAWED;
            }
            if (category == MobCategory.AMBIENT
                || category.getName().contains("water")
                || category == MobCategory.MISC) {
                return Profile.PECKER;
            }
        }

        return Profile.DIGGER;
    }

    private static void applyBreakAnimation(Player player, Profile profile) {
        switch (profile) {
            case HOOFED -> {
                player.swing(InteractionHand.OFF_HAND);
                player.swing(InteractionHand.MAIN_HAND);
            }
            case PECKER -> player.swing(InteractionHand.OFF_HAND);
            case GEL -> {
                player.swing(InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.OFF_HAND);
            }
            default -> player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void applyPlaceAnimation(Player player, Profile profile) {
        switch (profile) {
            case HOOFED -> player.swing(InteractionHand.OFF_HAND);
            case PECKER, BURROWER -> {
                player.swing(InteractionHand.OFF_HAND);
                player.swing(InteractionHand.MAIN_HAND);
            }
            case GEL -> player.swing(InteractionHand.OFF_HAND);
            default -> player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static boolean isHumanoidPath(String path) {
        if (isPathLike(path, HUMANOID_PATHS)) {
            return true;
        }
        return path.contains("villager")
            || path.contains("zombie")
            || path.contains("skeleton")
            || path.contains("piglin")
            || path.contains("illager");
    }

    private static boolean isPathLike(String path, Set<String> ids) {
        if (ids.contains(path)) {
            return true;
        }

        for (String id : ids) {
            if (path.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private enum Profile {
        HUMANOID(true, 1.00F, 4, 0.75F, 0.90F, 1.08F, 6, 0.85F, 0.85F, 1.05F, false, false, false),
        DIGGER(false, 0.68F, 4, 0.78F, 0.70F, 0.92F, 8, 0.90F, 0.68F, 0.90F, true, false, true),
        HOOFED(false, 0.55F, 7, 1.00F, 0.48F, 0.66F, 10, 0.95F, 0.45F, 0.64F, false, true, true),
        PECKER(false, 0.50F, 3, 0.62F, 1.20F, 1.40F, 5, 0.75F, 1.18F, 1.36F, false, false, false),
        CLAWED(false, 0.74F, 4, 0.84F, 0.90F, 1.20F, 7, 0.90F, 0.90F, 1.16F, true, false, true),
        BURROWER(false, 0.60F, 6, 0.88F, 0.65F, 0.84F, 9, 0.88F, 0.62F, 0.82F, true, false, true),
        GEL(false, 0.52F, 5, 0.70F, 0.55F, 0.72F, 6, 0.72F, 0.60F, 0.78F, false, true, false);

        private final boolean humanoidLike;
        private final float breakSpeedMultiplier;
        private final int breakFeedbackIntervalTicks;
        private final float breakSoundVolume;
        private final float breakPitchMin;
        private final float breakPitchMax;
        private final int placeCooldownTicks;
        private final float placeSoundVolume;
        private final float placePitchMin;
        private final float placePitchMax;
        private final boolean spawnBlockCrackParticles;
        private final boolean useStepSoundForBreak;
        private final boolean useStepSoundForPlace;

        Profile(
            boolean humanoidLike,
            float breakSpeedMultiplier,
            int breakFeedbackIntervalTicks,
            float breakSoundVolume,
            float breakPitchMin,
            float breakPitchMax,
            int placeCooldownTicks,
            float placeSoundVolume,
            float placePitchMin,
            float placePitchMax,
            boolean spawnBlockCrackParticles,
            boolean useStepSoundForBreak,
            boolean useStepSoundForPlace
        ) {
            this.humanoidLike = humanoidLike;
            this.breakSpeedMultiplier = breakSpeedMultiplier;
            this.breakFeedbackIntervalTicks = breakFeedbackIntervalTicks;
            this.breakSoundVolume = breakSoundVolume;
            this.breakPitchMin = breakPitchMin;
            this.breakPitchMax = breakPitchMax;
            this.placeCooldownTicks = placeCooldownTicks;
            this.placeSoundVolume = placeSoundVolume;
            this.placePitchMin = placePitchMin;
            this.placePitchMax = placePitchMax;
            this.spawnBlockCrackParticles = spawnBlockCrackParticles;
            this.useStepSoundForBreak = useStepSoundForBreak;
            this.useStepSoundForPlace = useStepSoundForPlace;
        }
    }
}