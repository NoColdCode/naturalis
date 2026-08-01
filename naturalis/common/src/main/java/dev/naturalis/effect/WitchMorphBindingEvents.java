package dev.naturalis.effect;

import dev.naturalis.NaturalisMod;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.content.NaturalisMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class WitchMorphBindingEvents {

    private static final String EFFECT_ROOT = "naturalis_effects";
    private static final String WITCH_BINDING_COOLDOWN_UNTIL = "witch_binding_cooldown_until";

    private WitchMorphBindingEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isWitchAttack(event.getSource())) {
            return;
        }

        CompoundTag tag = getOrCreateEffectTag(player);
        long now = player.level().getGameTime();
        if (now < CompatAccess.getLong(tag, WITCH_BINDING_COOLDOWN_UNTIL)) {
            return;
        }

        int durationSeconds = 5 + player.getRandom().nextInt(26); // 5..30
        player.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING, durationSeconds * 20, 0, false, true, true));

        // Prevent rapid reapplication from multiple splash ticks.
        tag.putLong(WITCH_BINDING_COOLDOWN_UNTIL, now + 60L);
    }

    private static boolean isWitchAttack(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Witch) {
            return true;
        }

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Witch) {
            return true;
        }

        return false;
    }

    private static CompoundTag getOrCreateEffectTag(ServerPlayer player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(EFFECT_ROOT)) {
            root.put(EFFECT_ROOT, new CompoundTag());
        }
        return CompatAccess.getCompound(root, EFFECT_ROOT);
    }
}
