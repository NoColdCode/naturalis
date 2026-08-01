package dev.naturalis.client.perception;

import dev.naturalis.client.NaturalisClientPrefs;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.TranslationDeviceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Feral attack feedback (angry wolf on prey, snarl on threats).
 */
public final class MorphCombatFeedback {

    private static final SoundEvent WOLF_GROWL = registrySound("entity.wolf.growl");
    private static int attackSoundCooldown;

    private MorphCombatFeedback() {
    }

    public static void onAttackEntity(LocalPlayer player, Entity target) {
        if (player == null || target == null || !(target instanceof LivingEntity living)) {
            return;
        }
        if (TranslationDeviceUtil.isTranslationCoreHeld(player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        MorphHearingProfile hearing = MorphHearingProfiles.resolve(morphId);
        if (!hearing.hasEnhancedHearing()) {
            return;
        }

        if (attackSoundCooldown > 0 || NaturalisClientPrefs.isMuteMorphPerceptionSounds()) {
            return;
        }

        MorphHearingCueKind kind = classifyPreyOrThreat(player, living, hearing);
        if (kind == MorphHearingCueKind.PREY) {
            playAt(player, living, WOLF_GROWL, 0.88F, 0.58F + player.getRandom().nextFloat() * 0.10F, 14);
        } else if (kind == MorphHearingCueKind.THREAT) {
            playAt(player, living, WOLF_GROWL, 0.72F, 0.52F + player.getRandom().nextFloat() * 0.08F, 10);
        } else if (kind == MorphHearingCueKind.PLAYER) {
            playAt(player, living, WOLF_GROWL, 0.65F, 0.62F + player.getRandom().nextFloat() * 0.06F, 12);
        }
    }

    public static void tickCooldown() {
        if (attackSoundCooldown > 0) {
            attackSoundCooldown--;
        }
    }

    private static MorphHearingCueKind classifyPreyOrThreat(LocalPlayer player, LivingEntity living, MorphHearingProfile profile) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        String path = id.getPath().toLowerCase();

        if (living instanceof net.minecraft.world.entity.player.Player) {
            return MorphHearingCueKind.PLAYER;
        }
        for (String token : profile.preyEntityPaths()) {
            if (path.contains(token)) {
                return MorphHearingCueKind.PREY;
            }
        }
        if (living instanceof net.minecraft.world.entity.monster.Monster) {
            return MorphHearingCueKind.THREAT;
        }
        for (String token : profile.threatEntityPaths()) {
            if (path.contains(token)) {
                return MorphHearingCueKind.THREAT;
            }
        }
        return MorphHearingCueKind.NEUTRAL;
    }

    private static void playAt(LocalPlayer player, LivingEntity target, SoundEvent sound, float volume, float pitch, int cooldown) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        mc.level.playLocalSound(
            target.getX(), target.getY(), target.getZ(),
            sound,
            SoundSource.HOSTILE,
            volume,
            pitch,
            false
        );
        attackSoundCooldown = cooldown;
    }

    private static SoundEvent registrySound(String id) {
        return BuiltInRegistries.SOUND_EVENT
            .getOptional(ResourceLocation.withDefaultNamespace(id))
            .orElse(SoundEvents.GENERIC_HURT);
    }
}
