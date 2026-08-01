package dev.naturalis.resonance;

import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.item.MorphArmorItem;
import dev.naturalis.item.MorphArmorTier;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ResonanceManager {

    private static final String ROOT_TAG = "naturalis_resonance";
    private static final String BONDED_MORPH_ID = "bonded_morph_id";
    private static final String RESONANCE_ENABLED = "resonance_enabled";
    private static final String ACTIVE_INSTINCT_COOLDOWN_UNTIL_TICK = "active_instinct_cooldown_until_tick";
    private static final String LAST_LOCK_WARNING_TICK = "last_lock_warning_tick";
    private static final String RECOVERY_WINDOW_UNTIL_TICK = "recovery_window_until_tick";
    private static final String HUMAN_FORM_LOCK_UNTIL_TICK = "human_form_lock_until_tick";
    private static final String HUMANITY = "humanity";
    private static final String HUMANITY_RECOVERY_PROGRESS = "humanity_recovery_progress";
    private static final String AUTO_CANDIDATE_ID = "auto_candidate_id";
    private static final String AUTO_CANDIDATE_TICKS = "auto_candidate_ticks";

    public static final int MAX_HUMANITY = 100;
    public static final int MIN_HUMANITY = 0;

    private static final int[] MILESTONE_XP = new int[]{0, 700, 1400, 2100};
    // Candidate must remain the undisputed #1 morph for this many consecutive ticks
    // before the auto-bond is allowed.  10 minutes @ 20 TPS = 12 000 ticks.
    private static final int AUTO_BOND_REQUIRED_STABILITY_TICKS = 10 * 60 * 20;

    public enum ResonanceArchetype {
        PREDATOR,
        SURVIVOR,
        AQUATIC,
        OTHER
    }

    public enum HumanityStage {
        GROUNDED,
        DRIFTING,
        SPLIT,
        FERAL,
        PRIMAL,
        LOST
    }

    private ResonanceManager() {
    }

    public static CompoundTag getOrCreateTag(ServerPlayer player) {
        CompoundTag data = CompatAccess.getPersistentData(player);
        if (!data.contains(ROOT_TAG)) {
            data.put(ROOT_TAG, new CompoundTag());
        }
        return CompatAccess.getCompound(data, ROOT_TAG);
    }

    public static ResourceLocation getBondedMorph(ServerPlayer player) {
        CompoundTag tag = getOrCreateTag(player);
        String raw = CompatAccess.getString(tag, BONDED_MORPH_ID);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (!isValidLivingMorph(id)) {
            return null;
        }

        return id;
    }

    public static boolean setBondedMorph(ServerPlayer player, ResourceLocation morphId) {
        if (morphId == null || !isValidLivingMorph(morphId)) {
            return false;
        }

        if (!MorphKnowledgeManager.hasResonanceEligibility(player, morphId)) {
            return false;
        }

        CompoundTag tag = getOrCreateTag(player);
        tag.putString(BONDED_MORPH_ID, morphId.toString());
        return true;
    }

    public static void clearBond(ServerPlayer player) {
        CompoundTag tag = getOrCreateTag(player);
        tag.remove(BONDED_MORPH_ID);
        tag.putBoolean(RESONANCE_ENABLED, false);
        tag.remove(ACTIVE_INSTINCT_COOLDOWN_UNTIL_TICK);
        tag.remove(HUMANITY_RECOVERY_PROGRESS);
        tag.remove(RECOVERY_WINDOW_UNTIL_TICK);
        tag.remove(HUMAN_FORM_LOCK_UNTIL_TICK);
        tag.remove(HUMANITY);
    }

    public static boolean isResonanceEnabled(ServerPlayer player) {
        if (dev.naturalis.survivalas.SurvivalAsWorldStorage.isEnabled()) {
            return false;
        }
        if (!NaturalisConfig.resonanceEnabled()) {
            return false;
        }
        return CompatAccess.getBoolean(getOrCreateTag(player), RESONANCE_ENABLED);
    }

    public static boolean setResonanceEnabled(ServerPlayer player, boolean enabled) {
        CompoundTag tag = getOrCreateTag(player);
        if (enabled && getBondedMorph(player) == null) {
            return false;
        }

        if (!enabled && isHumanityLocked(player)) {
            return false;
        }

        tag.putBoolean(RESONANCE_ENABLED, enabled);
        if (enabled && !tag.contains(HUMANITY)) {
            tag.putInt(HUMANITY, MAX_HUMANITY);
        }
        if (!enabled && !isHumanityLocked(player)) {
            tag.remove(ACTIVE_INSTINCT_COOLDOWN_UNTIL_TICK);
        }
        return true;
    }

    public static int getHumanity(ServerPlayer player) {
        CompoundTag tag = getOrCreateTag(player);
        if (!tag.contains(HUMANITY)) {
            tag.putInt(HUMANITY, MAX_HUMANITY);
        }
        return Math.max(MIN_HUMANITY, Math.min(MAX_HUMANITY, CompatAccess.getInt(tag, HUMANITY)));
    }

    public static void setHumanity(ServerPlayer player, int humanity) {
        getOrCreateTag(player).putInt(HUMANITY, Math.max(MIN_HUMANITY, Math.min(MAX_HUMANITY, humanity)));
    }

    public static void addHumanity(ServerPlayer player, int amount) {
        if (amount == 0) {
            return;
        }
        if (dev.naturalis.survivalas.SurvivalAsWorldStorage.isEnabled()) {
            return;
        }

        if (amount < 0 && hasEchoMorphArmorEquipped(player)) {
            amount = -Math.max(1, Math.round(Math.abs(amount) * 0.5F));
        }

        if (amount > 0 && CurrentMorphUtil.getCurrentMorphId(player) == null && hasHumanAmulet(player)) {
            amount = Math.max(1, Math.round(amount * 1.5F));
        }

        int next = getHumanity(player) + amount;
        if (amount < 0 && hasSovereignAmulet(player)) {
            next = Math.max(1, next);
        }
        setHumanity(player, next);
    }

    public static void applyHumanityActionLoss(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (dev.naturalis.survivalas.SurvivalAsWorldStorage.isEnabled()) {
            return;
        }

        ResourceLocation currentMorph = CurrentMorphUtil.getCurrentMorphId(player);
        float avoidChance = MorphKnowledgeManager.getHumanityLossAvoidChance(
            MorphKnowledgeManager.getHumanConnectionRank(player, currentMorph)
        );

        if (player.getRandom().nextFloat() < avoidChance) {
            return;
        }

        addHumanity(player, -amount);
    }

    private static boolean hasEchoMorphArmorEquipped(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof MorphArmorItem)) {
            return false;
        }
        return MorphArmorItem.getTier(chest) == MorphArmorTier.ECHO;
    }

    private static boolean hasHumanAmulet(ServerPlayer player) {
        Item item = CompatAccess.naturalisItem("human_amulet");
        return item != null && player.getInventory().contains(new ItemStack(item));
    }

    private static boolean hasSovereignAmulet(ServerPlayer player) {
        Item item = CompatAccess.naturalisItem("sovereign_amulet");
        return item != null && player.getInventory().contains(new ItemStack(item));
    }

    public static void addHumanityRecoveryProgress(ServerPlayer player, int amountTenths) {
        if (amountTenths <= 0) {
            return;
        }

        CompoundTag tag = getOrCreateTag(player);
        int progress = Math.max(0, CompatAccess.getInt(tag, HUMANITY_RECOVERY_PROGRESS)) + amountTenths;
        int gain = progress / 10;
        tag.putInt(HUMANITY_RECOVERY_PROGRESS, progress % 10);
        if (gain > 0) {
            addHumanity(player, gain);
        }
    }

    public static boolean isHumanityLocked(ServerPlayer player) {
        return getHumanity(player) <= 0;
    }

    public static boolean isRecoveryWindowActive(ServerPlayer player, long now) {
        return now < CompatAccess.getLong(getOrCreateTag(player), RECOVERY_WINDOW_UNTIL_TICK);
    }

    public static boolean isHumanFormLockActive(ServerPlayer player, long now) {
        return now < CompatAccess.getLong(getOrCreateTag(player), HUMAN_FORM_LOCK_UNTIL_TICK);
    }

    public static void openRecoveryWindow(ServerPlayer player, long now, int windowTicks, int humanLockTicks) {
        CompoundTag tag = getOrCreateTag(player);
        long recoveryUntil = now + Math.max(0, windowTicks);
        long humanLockUntil = now + Math.max(0, humanLockTicks);
        tag.putLong(RECOVERY_WINDOW_UNTIL_TICK, Math.max(CompatAccess.getLong(tag, RECOVERY_WINDOW_UNTIL_TICK), recoveryUntil));
        tag.putLong(HUMAN_FORM_LOCK_UNTIL_TICK, Math.max(CompatAccess.getLong(tag, HUMAN_FORM_LOCK_UNTIL_TICK), humanLockUntil));
    }

    public static void extendHumanFormLock(ServerPlayer player, long now, int extraTicks) {
        if (extraTicks <= 0) {
            return;
        }
        CompoundTag tag = getOrCreateTag(player);
        long base = Math.max(now, CompatAccess.getLong(tag, HUMAN_FORM_LOCK_UNTIL_TICK));
        tag.putLong(HUMAN_FORM_LOCK_UNTIL_TICK, base + extraTicks);
    }

    public static HumanityStage getHumanityStage(ServerPlayer player) {
        return getHumanityStage(getHumanity(player));
    }

    public static HumanityStage getHumanityStage(int humanity) {
        if (humanity <= 0) {
            return HumanityStage.LOST;
        }
        if (humanity <= 19) {
            return HumanityStage.PRIMAL;
        }
        if (humanity <= 39) {
            return HumanityStage.FERAL;
        }
        if (humanity <= 59) {
            return HumanityStage.SPLIT;
        }
        if (humanity <= 79) {
            return HumanityStage.DRIFTING;
        }
        return HumanityStage.GROUNDED;
    }

    public static boolean shouldSendLockWarning(ServerPlayer player, long now) {
        CompoundTag tag = getOrCreateTag(player);
        long last = CompatAccess.getLong(tag, LAST_LOCK_WARNING_TICK);
        if (now - last < 20L) {
            return false;
        }
        tag.putLong(LAST_LOCK_WARNING_TICK, now);
        return true;
    }

    public static boolean canUseActiveInstinct(ServerPlayer player, long now) {
        if (!isResonanceEnabled(player) || !isAligned(player)) {
            return false;
        }
        return now >= CompatAccess.getLong(getOrCreateTag(player), ACTIVE_INSTINCT_COOLDOWN_UNTIL_TICK);
    }

    public static void markActiveInstinctUsed(ServerPlayer player, long now) {
        getOrCreateTag(player).putLong(ACTIVE_INSTINCT_COOLDOWN_UNTIL_TICK, now + getActiveInstinctCooldownTicks(player));
    }

    public static long getActiveInstinctCooldownRemaining(ServerPlayer player, long now) {
        long until = CompatAccess.getLong(getOrCreateTag(player), ACTIVE_INSTINCT_COOLDOWN_UNTIL_TICK);
        return Math.max(0L, until - now);
    }

    public static int getActiveInstinctCooldownTicks(ServerPlayer player) {
        int humanity = getHumanity(player);
        int tier = getMasteryTier(player);

        int base;
        if (humanity >= 80) {
            base = 45 * 20;
        } else if (humanity >= 60) {
            base = 38 * 20;
        } else if (humanity >= 40) {
            base = 32 * 20;
        } else if (humanity >= 20) {
            base = 26 * 20;
        } else {
            base = 20 * 20;
        }

        return Math.max(12 * 20, base - (tier * 2 * 20));
    }

    public static int getMasteryXp(ServerPlayer player) {
        ResourceLocation bonded = getBondedMorph(player);
        if (bonded == null) {
            return 0;
        }
        return MorphKnowledgeManager.getXp(player, bonded);
    }

    public static void addMasteryXp(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        ResourceLocation bonded = getBondedMorph(player);
        if (bonded == null) {
            return;
        }
        MorphKnowledgeManager.addXp(player, bonded, amount);
    }

    public static int getMasteryTier(ServerPlayer player) {
        return getMasteryTierForXp(getMasteryXp(player));
    }

    public static int getMasteryTierForXp(int xp) {
        int tier = 0;
        for (int i = 1; i < MILESTONE_XP.length; i++) {
            if (xp >= MILESTONE_XP[i]) {
                tier = i;
            }
        }
        return tier;
    }

    public static int getRequiredXpForTier(int tier) {
        int idx = Math.max(0, Math.min(MILESTONE_XP.length - 1, tier));
        return MILESTONE_XP[idx];
    }

    public static boolean maybeAutoBondMostUsedMorph(ServerPlayer player) {
        if (getBondedMorph(player) != null) {
            clearAutoCandidate(player);
            return false;
        }

        ResourceLocation candidate = MorphKnowledgeManager.findBestResonanceCandidate(player);
        if (candidate == null) {
            clearAutoCandidate(player);
            return false;
        }

        CompoundTag tag = getOrCreateTag(player);
        String old = CompatAccess.getString(tag, AUTO_CANDIDATE_ID);
        int stableTicks = Math.max(0, CompatAccess.getInt(tag, AUTO_CANDIDATE_TICKS));

        if (!candidate.toString().equals(old)) {
            tag.putString(AUTO_CANDIDATE_ID, candidate.toString());
            tag.putInt(AUTO_CANDIDATE_TICKS, 20);
            return false;
        }

        stableTicks += 20;
        tag.putInt(AUTO_CANDIDATE_TICKS, stableTicks);

        if (stableTicks < AUTO_BOND_REQUIRED_STABILITY_TICKS) {
            return false;
        }

        boolean bonded = setBondedMorph(player, candidate);
        if (bonded) {
            setResonanceEnabled(player, true);
            clearAutoCandidate(player);
            return true;
        }

        return false;
    }

    private static void clearAutoCandidate(ServerPlayer player) {
        CompoundTag tag = getOrCreateTag(player);
        tag.remove(AUTO_CANDIDATE_ID);
        tag.remove(AUTO_CANDIDATE_TICKS);
    }

    public static boolean isAligned(ServerPlayer player) {
        ResourceLocation bonded = getBondedMorph(player);
        ResourceLocation current = CurrentMorphUtil.getCurrentMorphId(player);
        return bonded != null && current != null && bonded.equals(current);
    }

    public static ResonanceArchetype getBondedArchetype(ServerPlayer player) {
        return getArchetype(getBondedMorph(player));
    }

    public static ResonanceArchetype getArchetype(ResourceLocation morphId) {
        if (morphId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(morphId)) {
            return ResonanceArchetype.OTHER;
        }

        // Some CREATURE entities still fit the predator fantasy loop better than survivor.
        String path = morphId.getPath();
        if ("wolf".equals(path)) {
            return ResonanceArchetype.PREDATOR;
        }

        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return ResonanceArchetype.OTHER;
        }
        return switch (type.getCategory()) {
            case MONSTER -> ResonanceArchetype.PREDATOR;
            case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> ResonanceArchetype.AQUATIC;
            case CREATURE, AMBIENT -> ResonanceArchetype.SURVIVOR;
            default -> ResonanceArchetype.OTHER;
        };
    }

    public static float getDamageMultiplier(ServerPlayer player) {
        if (!isResonanceEnabled(player) || !isAligned(player)) {
            return 1.0F;
        }

        float archetypeBase = switch (getBondedArchetype(player)) {
            case PREDATOR -> 1.22F;
            case SURVIVOR -> 1.14F;
            case AQUATIC -> CompatAccess.isInWaterOrBubble(player) ? 1.18F : 1.06F;
            case OTHER -> 1.12F;
        };

        int humanity = getHumanity(player);
        float humanityFactor;
        if (humanity >= 80) {
            humanityFactor = 1.00F;
        } else if (humanity >= 60) {
            humanityFactor = 1.05F;
        } else if (humanity >= 40) {
            humanityFactor = 1.10F;
        } else if (humanity >= 20) {
            humanityFactor = 1.16F;
        } else {
            humanityFactor = 1.22F;
        }

        int tier = getMasteryTier(player);
        float masteryFactor = 1.00F + (tier * 0.04F);

        float morphFactor = 1.00F;
        ResourceLocation bonded = getBondedMorph(player);
        if (bonded != null && BuiltInRegistries.ENTITY_TYPE.containsKey(bonded)) {
            EntityType<?> type = CompatAccess.getEntityType(bonded);
            if (type == null) {
                return archetypeBase * humanityFactor * masteryFactor;
            }
            double size = Math.max(0.25D, type.getWidth() * type.getHeight());
            // Small agile bodies and huge heavy bodies diverge slightly from neutral.
            morphFactor = (float) Math.max(0.95D, Math.min(1.12D, 1.0D + ((size - 1.0D) * 0.05D)));
        }

        return archetypeBase * humanityFactor * masteryFactor * morphFactor;
    }

    public static boolean isValidLivingMorph(ResourceLocation morphId) {
        if (morphId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(morphId)) {
            return false;
        }
        EntityType<?> type = CompatAccess.getEntityType(morphId);
        if (type == null) {
            return false;
        }
        if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
            return true;
        }
        return type.getCategory() != MobCategory.MISC;
    }
}
