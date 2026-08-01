package dev.naturalis.client.perception;

import dev.naturalis.config.NaturalisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Particles and sounds while morphed players dig or scratch blocks with paws.
 */
public final class MorphDigFeedback {

    private static final ResourceLocation WOLF_PANT = ResourceLocation.withDefaultNamespace("entity.wolf.pant");
    private static final ResourceLocation WOLF_AMBIENT = ResourceLocation.withDefaultNamespace("entity.wolf.ambient");
    private static final ResourceLocation WOLF_GROWL = ResourceLocation.withDefaultNamespace("entity.wolf.growl");
    private static int feedbackCooldown;

    private MorphDigFeedback() {
    }

    public static void clientTick(Minecraft mc) {
        if (!dev.naturalis.experience.NaturalisExperienceProfile.useQuadrupedDigFeedbackClient()
            || !NaturalisConfig.knowledgeQuadrupedDigFeedback()) {
            MorphDigClientState.reset();
            return;
        }
        if (mc.player == null || mc.level == null) {
            MorphDigClientState.reset();
            return;
        }

        MorphEmbodimentProfile profile = MorphEmbodimentLogic.profileFor(mc.player);
        boolean attackingBlock = MorphEmbodimentLogic.isAttackingBlock(mc);
        boolean miningBlocked = MorphAnimalInteractionClient.shouldSuppressBlockMining(mc);

        if (attackingBlock && miningBlocked && MorphEmbodimentLogic.usesPawDigging(profile)) {
            MorphDigClientState.pulseScratch();
        }

        boolean digging = attackingBlock && !miningBlocked;
        boolean scratching = attackingBlock && miningBlocked;
        float progress = digging
            ? MorphEmbodimentLogic.blockDestroyProgress(mc)
            : (scratching ? 0.42F : 0.0F);
        MorphDigClientState.tick(digging || scratching, progress);

        boolean pawDig = MorphEmbodimentLogic.usesPawDigging(profile);
        boolean activeDig = digging || scratching;
        if (!activeDig || !pawDig || MorphDigClientState.digAnim() < 0.12F) {
            if (feedbackCooldown > 0) {
                feedbackCooldown--;
            }
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (feedbackCooldown > 0) {
            feedbackCooldown--;
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        RandomSource random = mc.player.getRandom();
        Vec3 hit = blockHit.getLocation();
        int count = scratching ? 3 + random.nextInt(2) : 2 + (int) (progress * 4);
        for (int i = 0; i < count; i++) {
            double px = hit.x + (random.nextDouble() - 0.5D) * 0.35D;
            double py = hit.y + (random.nextDouble() - 0.5D) * 0.35D;
            double pz = hit.z + (random.nextDouble() - 0.5D) * 0.35D;
            mc.level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                px, py, pz,
                (random.nextDouble() - 0.5D) * 0.14D,
                random.nextDouble() * 0.10D,
                (random.nextDouble() - 0.5D) * 0.14D
            );
        }

        float pitch = Mth.lerp(progress, 0.82F, 1.18F);
        float blockVol = scratching ? 0.42F : 0.35F + progress * 0.25F;
        mc.level.playLocalSound(
            hit.x, hit.y, hit.z,
            state.getSoundType().getHitSound(),
            SoundSource.BLOCKS,
            blockVol,
            pitch,
            false
        );

        if (profile.armInteractionStyle() == MorphArmInteractionStyle.CANINE) {
            float canineChance = scratching ? 0.38F : 0.18F + progress * 0.12F;
            if (random.nextFloat() < canineChance) {
                playMorphSound(mc, hit, random.nextBoolean() ? WOLF_PANT : WOLF_AMBIENT, scratching ? 0.28F : 0.22F, 0.85F + random.nextFloat() * 0.25F);
            }
            if (scratching && random.nextFloat() < 0.22F) {
                playMorphSound(mc, hit, WOLF_GROWL, 0.12F, 1.35F + random.nextFloat() * 0.15F);
            }
        } else if (random.nextFloat() < 0.10F) {
            playMorphSound(mc, hit, SoundEvents.GRASS_STEP, 0.20F, 1.05F + random.nextFloat() * 0.15F);
        }

        feedbackCooldown = scratching ? 3 : Math.max(2, 6 - (int) (progress * 4));
    }

    private static void playMorphSound(Minecraft mc, Vec3 at, ResourceLocation id, float volume, float pitch) {
        BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound ->
            playMorphSound(mc, at, sound, volume, pitch)
        );
    }

    private static void playMorphSound(Minecraft mc, Vec3 at, SoundEvent sound, float volume, float pitch) {
        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }
}
