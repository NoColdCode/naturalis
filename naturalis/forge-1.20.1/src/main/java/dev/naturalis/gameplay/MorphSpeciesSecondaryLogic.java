package dev.naturalis.gameplay;

import dev.naturalis.client.perception.MorphHearingProfiles;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.instinct.InstinctEvents;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.network.PeckPulsePayload;
import dev.naturalis.network.PlayToClientSender;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Server-side right-click species interactions for morphed players.
 */
public final class MorphSpeciesSecondaryLogic {

    private static final String ROOT_TAG = "naturalis_secondary";
    private static final String LAST_USE_TICK = "last_use_tick";

    private static final int COOLDOWN_TICKS = 16;

    private MorphSpeciesSecondaryLogic() {
    }

    public static boolean canTriggerSecondary(Player player, ItemStack held) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || MorphAnimalInteraction.isHumanoidMorph(morphId)) {
            return false;
        }
        if (MorphSpeciesSecondaryAction.resolve(morphId) == MorphSpeciesSecondaryAction.NONE) {
            return false;
        }
        if (player.isShiftKeyDown() && InstinctManager.getSmellStrength(morphId) <= 0) {
            return false;
        }
        if (!held.isEmpty() && (held.getItem() instanceof BlockItem || isEchoTool(held))) {
            return false;
        }
        return true;
    }

    public static boolean tryUse(Player player, InteractionHand hand, HitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!canTriggerSecondary(player, player.getMainHandItem())) {
            return false;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return false;
        }

        if (onCooldown(serverPlayer)) {
            return false;
        }

        MorphSpeciesSecondaryAction action = MorphSpeciesSecondaryAction.resolve(morphId);
        if ((action == MorphSpeciesSecondaryAction.SNIFF || action == MorphSpeciesSecondaryAction.LISTEN)
            && isAimingAtLivingEntity(serverPlayer, hit)) {
            return false;
        }

        boolean used = switch (action) {
            case SNIFF -> serverPlayer.isShiftKeyDown() && trySniff(serverPlayer, morphId);
            case LISTEN -> !serverPlayer.isShiftKeyDown() && tryListen(serverPlayer, morphId);
            case PECK -> tryPeck(serverPlayer, morphId, hit);
            case NONE -> false;
        };

        if (used) {
            markUsed(serverPlayer);
        }
        return used;
    }

    /** Sniff/listen are air/block actions — do not steal clicks aimed at other mobs. */
    private static boolean isAimingAtLivingEntity(ServerPlayer player, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living && living != player) {
            return true;
        }

        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0F).scale(5.0D));
        AABB search = player.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            player,
            from,
            to,
            search,
            candidate -> candidate.isPickable()
                && candidate instanceof LivingEntity living
                && living != player,
            25.0D
        );
        return entityHit != null;
    }

    private static boolean trySniff(ServerPlayer player, ResourceLocation morphId) {
        if (!InstinctManager.hasSmellSense(morphId)) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        playSpeciesSniffSound(player, morphId);
        InstinctEvents.performDeepSniff(player);
        return true;
    }

    private static boolean tryListen(ServerPlayer player, ResourceLocation morphId) {
        if (!MorphHearingProfiles.resolve(morphId).hasEnhancedHearing()) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        playListenSound(player, morphId);
        MorphListenFocusLogic.beginFocus(player, morphId);
        return true;
    }

    private static boolean tryPeck(ServerPlayer player, ResourceLocation morphId, HitResult hit) {
        ServerLevel level = (ServerLevel) player.level();
        player.swing(InteractionHand.MAIN_HAND, true);
        playPeckSound(player, morphId);

        boolean struckEntity = false;
        boolean struckBlock = false;

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living
            && living != player) {
            struckEntity = true;
            Vec3 hitPos = living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D);
            level.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 2, 0.06D, 0.05D, 0.06D, 0.01D);
            applyPeckEntityBenefits(player, living);
        } else {
            BlockPos pos = resolveBlockPos(player, hit);
            if (pos != null) {
                struckBlock = true;
                BlockState state = level.getBlockState(pos);
                level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.85D,
                    pos.getZ() + 0.5D,
                    4,
                    0.1D,
                    0.06D,
                    0.1D,
                    0.02D
                );
                highlightForageOnBlock(level, pos, state);
            }
            nudgeNearbyForageItems(player, level);
            startleNearbyAvians(player, level);
        }

        PlayToClientSender.send(player, new PeckPulsePayload(struckEntity, struckBlock));
        return true;
    }

    private static void applyPeckEntityBenefits(ServerPlayer player, LivingEntity living) {
        if (living instanceof Mob mob) {
            mob.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ(), 30.0F, 30.0F);
        }
        if (living instanceof Enemy) {
            living.addEffect(new MobEffectInstance(
                CompatAccess.resolveMobEffect("SLOWNESS", "MOVEMENT_SLOWDOWN"), 14, 0, false, false, true));
        }
    }

    private static void highlightForageOnBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.WHEAT)
            || state.is(Blocks.BEETROOTS)
            || state.is(Blocks.CARROTS)
            || state.is(Blocks.POTATOES)
            || state.is(Blocks.SWEET_BERRY_BUSH)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 2, 0.08D, 0.04D, 0.08D, 0.0D);
        }
    }

    private static void nudgeNearbyForageItems(ServerPlayer player, ServerLevel level) {
        AABB box = player.getBoundingBox().inflate(2.4D);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive);
        for (ItemEntity item : items) {
            if (player.distanceToSqr(item) > 6.0D) {
                continue;
            }
            Vec3 toward = player.getEyePosition().subtract(item.position());
            if (toward.lengthSqr() < 0.01D) {
                continue;
            }
            Vec3 nudge = toward.normalize().scale(0.07D);
            item.setDeltaMovement(item.getDeltaMovement().add(nudge.x, 0.02D, nudge.z));
        }
    }

    private static void startleNearbyAvians(ServerPlayer player, ServerLevel level) {
        AABB box = player.getBoundingBox().inflate(7.0D);
        List<LivingEntity> birds = level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            e -> e.isAlive() && e != player && e.getType().toString().toLowerCase(Locale.ROOT).contains("chicken")
        );
        for (LivingEntity bird : birds) {
            if (player.distanceToSqr(bird) > 49.0D) {
                continue;
            }
            Vec3 flee = bird.position().subtract(player.position()).normalize().scale(0.35D);
            bird.setDeltaMovement(bird.getDeltaMovement().add(flee.x, 0.12D, flee.z));
            bird.hurtMarked = true;
        }
    }

    private static BlockPos resolveBlockPos(Player player, HitResult hit) {
        if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(2.5D));
        var blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
            eye,
            reach,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return ((net.minecraft.world.phys.BlockHitResult) blockHit).getBlockPos();
        }
        return player.blockPosition();
    }

    private static void playSpeciesSniffSound(ServerPlayer player, ResourceLocation morphId) {
        String path = morphId.getPath();
        if (path.contains("wolf") || path.contains("fox")) {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT
                .getOptional(new ResourceLocation("entity.wolf.sniff"))
                .orElse(SoundEvents.GRASS_STEP);
            player.playNotifySound(sound, SoundSource.PLAYERS, 0.65F, 0.88F + player.getRandom().nextFloat() * 0.15F);
            return;
        }
        if (path.contains("cat") || path.contains("ocelot")) {
            player.playNotifySound(SoundEvents.CAT_PURR, SoundSource.PLAYERS, 0.35F, 1.35F);
            return;
        }
        if (path.contains("bear")) {
            player.playNotifySound(SoundEvents.POLAR_BEAR_WARNING, SoundSource.PLAYERS, 0.4F, 0.75F);
            return;
        }
        player.playNotifySound(SoundEvents.GRASS_STEP, SoundSource.PLAYERS, 0.45F, 0.65F);
    }

    private static void playListenSound(ServerPlayer player, ResourceLocation morphId) {
        String path = morphId.getPath();
        if (path.contains("bat")) {
            player.playNotifySound(SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 0.55F, 0.65F);
            return;
        }
        if (path.contains("rabbit")) {
            player.playNotifySound(SoundEvents.RABBIT_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.8F);
            return;
        }
        if (path.contains("cat") || path.contains("ocelot")) {
            player.playNotifySound(SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.2F);
            return;
        }
        player.playNotifySound(SoundEvents.WARDEN_LISTENING, SoundSource.PLAYERS, 0.35F, 1.1F);
    }

    private static void playPeckSound(ServerPlayer player, ResourceLocation morphId) {
        String path = morphId.getPath();
        if (path.contains("parrot")) {
            player.playNotifySound(SoundEvents.PARROT_IMITATE_ZOMBIE, SoundSource.PLAYERS, 0.45F, 1.4F + player.getRandom().nextFloat() * 0.3F);
            return;
        }
        player.playNotifySound(SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.6F, 0.95F + player.getRandom().nextFloat() * 0.25F);
    }

    private static boolean onCooldown(ServerPlayer player) {
        long now = player.level().getGameTime();
        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            return false;
        }
        long last = CompatAccess.getLong(CompatAccess.getCompound(root, ROOT_TAG), LAST_USE_TICK);
        return now - last < COOLDOWN_TICKS;
    }

    private static void markUsed(ServerPlayer player) {
        var root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        CompatAccess.getCompound(root, ROOT_TAG).putLong(LAST_USE_TICK, player.level().getGameTime());
    }

    private static boolean isEchoTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem().toString().toLowerCase(Locale.ROOT).contains("echo_morph");
    }
}
