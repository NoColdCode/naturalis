package dev.naturalis.knowledge;

import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.survivalas.SurvivalAsWorldStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MorphKnowledgeManager {

    private static final String ROOT_TAG = "naturalis_knowledge";
    private static final String XP_MAP_TAG = "xp_map";
    private static final String TREE_MAP_TAG = "tree_map";
    private static final String GLOBAL_XP_TAG = "global_xp";
    private static final String GLOBAL_TREE_TAG = "global_tree";
    private static final String STAT_CACHE_TAG = "stat_cache";
    private static final String TEMP_UNLOCK_MORPH_ID_TAG = "temp_unlock_morph_id";
    private static final String TEMP_UNLOCK_EXPIRES_AT_TAG = "temp_unlock_expires_at";
    /** Extra spendable tree points from items (e.g. Mnemonic Seed), not tied to adaptation XP. */
    private static final String BONUS_TREE_POINTS_TAG = "bonus_tree_points";
    /** Fractional XP bank so Survival-as ×20 slowdown does not discard sub-threshold awards. */
    private static final String SURVIVAL_AS_XP_REMAINDER_TAG = "survival_as_xp_remainder";
    private static final int SURVIVAL_AS_XP_DIVISOR = 20;

    public static final String BRANCH_VITALITY = "vitality";
    public static final String BRANCH_HANDLING = "handling";
    public static final String BRANCH_INSTINCT = "instinct";
    public static final String BRANCH_WANDER = "wander";
    public static final String BRANCH_HUMAN_CONNECTION = "resonance";
    public static final String BRANCH_RESONANCE = BRANCH_HUMAN_CONNECTION;
    public static final String BRANCH_DAMAGE = "damage";
    public static final String BRANCH_MORPH_RESISTANCE = "morph_resistance";
    public static final String BRANCH_UTILITIES = "utilities";
    public static final String BRANCH_SOCIAL = "social";

    private static final int MAX_LEVEL = 5;
    private static final int MAX_XP = 3200;
    private static final int GLOBAL_XP_TARGET_MORPHS = 25;
    private static final int GLOBAL_MAX_XP = MAX_XP * GLOBAL_XP_TARGET_MORPHS;
    // Full-branch completion budget with current costs/ranks:
    // vitality 15 + handling 22 + instinct 9 + wander 9 + human connection 15
    // + damage 10 + morph resistance 15 + utilities 12 + social 16 = 123
    // Echo Sovereign reward grants +10 global levels beyond that baseline (= 133 cap).
    private static final int MAX_POINTS = 133;
    private static final int[] LEVEL_REQUIREMENTS = new int[]{0, 180, 360, 720, 1280, 2000};
    private static final int[] LEGACY_LEVEL_POINT_REQUIREMENTS = new int[]{0, 2, 4, 7, 10, 14};

    private MorphKnowledgeManager() {
    }

    public static int getXp(ServerPlayer player, ResourceLocation mobId) {
        CompoundTag xpMap = getXpMap(player);
        return clampXp(CompatAccess.getInt(xpMap, mobId.toString()));
    }

    public static void addXp(ServerPlayer player, ResourceLocation mobId, int amount) {
        amount = applySurvivalAsXpScale(player, amount);
        if (amount <= 0) {
            return;
        }

        int oldXp = getXp(player, mobId);
        int newXp = clampXp(oldXp + amount);

        CompoundTag knowledgeRoot = getKnowledgeRoot(player);
        CompoundTag xpMap = getXpMap(player);
        xpMap.putInt(mobId.toString(), newXp);
        knowledgeRoot.put(XP_MAP_TAG, xpMap);
        addGlobalXpRaw(player, amount);
        KnowledgeClientSync.flush(player);
    }

    public static void setXp(ServerPlayer player, ResourceLocation mobId, int value) {
        CompoundTag xpMap = getXpMap(player);
        xpMap.putInt(mobId.toString(), clampXp(value));
    }

    public static void forgetMorphKnowledge(ServerPlayer player, ResourceLocation mobId) {
        CompoundTag xpMap = getXpMap(player);
        xpMap.putInt(mobId.toString(), 0);

        CompoundTag treeMap = getTreeMap(player);
        // Clears branch ranks and bonus Mnemonic Seed points for this morph.
        treeMap.put(mobId.toString(), new CompoundTag());
    }

    public static void setLevel(ServerPlayer player, ResourceLocation mobId, int level) {
        int clampedLevel = Math.max(0, Math.min(MAX_LEVEL, level));
        setXp(player, mobId, getRequiredXpForLevel(clampedLevel));
    }

    public static int getLevelForXp(int xp) {
        int clampedXp = clampXp(xp);
        int level = 0;
        for (int i = 1; i <= MAX_LEVEL; i++) {
            if (clampedXp >= LEVEL_REQUIREMENTS[i]) {
                level = i;
            }
        }
        return level;
    }

    public static int getLevelForPoints(int spentPoints) {
        int clamped = Math.max(0, spentPoints);
        int level = 0;
        for (int i = 1; i <= MAX_LEVEL; i++) {
            if (clamped >= LEGACY_LEVEL_POINT_REQUIREMENTS[i]) {
                level = i;
            }
        }
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    public static int getLevel(ServerPlayer player, ResourceLocation mobId) {
        return getLevelForXp(getXp(player, mobId));
    }

    public static int getXpIntoCurrentLevel(int xp) {
        int clampedXp = clampXp(xp);
        int level = getLevelForXp(xp);
        int levelStart = LEVEL_REQUIREMENTS[Math.max(0, level)];
        return Math.max(0, clampedXp - levelStart);
    }

    public static int getXpNeededForCurrentLevel(int xp) {
        int clampedXp = clampXp(xp);
        int level = getLevelForXp(xp);
        if (level >= MAX_LEVEL) {
            int span = MAX_XP - LEVEL_REQUIREMENTS[MAX_LEVEL - 1];
            return Math.max(1, span);
        }
        int start = LEVEL_REQUIREMENTS[Math.max(0, level)];
        int end = LEVEL_REQUIREMENTS[level + 1];
        return Math.max(1, end - start);
    }

    public static int getRequiredXpForLevel(int level) {
        int clampedLevel = Math.max(0, Math.min(MAX_LEVEL, level));
        return LEVEL_REQUIREMENTS[clampedLevel];
    }

    public static int getMaxLevel() {
        return MAX_LEVEL;
    }

    public static int getMaxXp() {
        return MAX_XP;
    }

    public static boolean isCapped(int xp) {
        return clampXp(xp) >= MAX_XP;
    }

    public static int getTotalKnowledgePointsForXp(int xp) {
        int clampedXp = clampXp(xp);
        return Math.max(0, Math.min(MAX_POINTS, (clampedXp * MAX_POINTS) / MAX_XP));
    }

    public static int getGlobalKnowledgePointsForXp(int xp) {
        int clampedXp = clampGlobalXp(xp);
        return Math.max(0, Math.min(MAX_POINTS, (clampedXp * MAX_POINTS) / GLOBAL_MAX_XP));
    }

    public static int getMaxPointLevel() {
        return MAX_POINTS;
    }

    public static int getPointLevelForXp(int xp) {
        return getTotalKnowledgePointsForXp(xp);
    }

    public static int getGlobalPointLevelForXp(int xp) {
        return getGlobalKnowledgePointsForXp(xp);
    }

    public static int getXpIntoCurrentPointLevel(int xp) {
        int clampedXp = clampXp(xp);
        int pointLevel = getPointLevelForXp(clampedXp);
        int levelStart = getRequiredXpForPointLevel(pointLevel);
        return Math.max(0, clampedXp - levelStart);
    }

    public static int getGlobalXpIntoCurrentPointLevel(int xp) {
        int clampedXp = clampGlobalXp(xp);
        int pointLevel = getGlobalPointLevelForXp(clampedXp);
        int levelStart = getRequiredGlobalXpForPointLevel(pointLevel);
        return Math.max(0, clampedXp - levelStart);
    }

    public static int getXpNeededForCurrentPointLevel(int xp) {
        int pointLevel = getPointLevelForXp(clampXp(xp));
        int maxPointLevel = getMaxPointLevel();
        if (pointLevel >= maxPointLevel) {
            return Math.max(1, MAX_XP - getRequiredXpForPointLevel(pointLevel));
        }

        int start = getRequiredXpForPointLevel(pointLevel);
        int end = getRequiredXpForPointLevel(pointLevel + 1);
        return Math.max(1, end - start);
    }

    public static int getGlobalXpNeededForCurrentPointLevel(int xp) {
        int pointLevel = getGlobalPointLevelForXp(clampGlobalXp(xp));
        int maxPointLevel = getMaxPointLevel();
        if (pointLevel >= maxPointLevel) {
            return Math.max(1, GLOBAL_MAX_XP - getRequiredGlobalXpForPointLevel(pointLevel));
        }

        int start = getRequiredGlobalXpForPointLevel(pointLevel);
        int end = getRequiredGlobalXpForPointLevel(pointLevel + 1);
        return Math.max(1, end - start);
    }

    public static int getRequiredXpForPointLevel(int pointLevel) {
        int clamped = Math.max(0, Math.min(MAX_POINTS, pointLevel));
        return Math.min(MAX_XP, (clamped * MAX_XP) / MAX_POINTS);
    }

    public static int getRequiredGlobalXpForPointLevel(int pointLevel) {
        int clamped = Math.max(0, Math.min(MAX_POINTS, pointLevel));
        return Math.min(GLOBAL_MAX_XP, (clamped * GLOBAL_MAX_XP) / MAX_POINTS);
    }

    public static int getTotalKnowledgePoints(ServerPlayer player, ResourceLocation mobId) {
        if (mobId == null) {
            return getGlobalKnowledgePointsForXp(getGlobalXp(player));
        }
        int fromXp = getTotalKnowledgePointsForXp(getXp(player, mobId));
        int bonus = CompatAccess.getInt(getNodeTag(player, mobId), BONUS_TREE_POINTS_TAG);
        return fromXp + Math.max(0, bonus);
    }

    /**
     * Grants one extra knowledge-tree point for the given morph (Mnemonic Seed, etc.).
     * Does not change adaptation XP or morph level tiers.
     */
    public static void grantBonusTreeKnowledgePoint(ServerPlayer player, ResourceLocation mobId) {
        if (mobId == null) {
            return;
        }
        CompoundTag node = getNodeTag(player, mobId);
        int cur = CompatAccess.getInt(node, BONUS_TREE_POINTS_TAG);
        node.putInt(BONUS_TREE_POINTS_TAG, cur + 1);
        KnowledgeClientSync.flush(player);
    }

    public static int getSpentKnowledgePoints(ServerPlayer player, ResourceLocation mobId) {
        CompoundTag nodeTag = mobId == null ? getGlobalNodeTag(player) : getNodeTag(player, mobId);
        int total = 0;
        total += getBranchSpentCost(nodeTag, BRANCH_VITALITY);
        total += getBranchSpentCost(nodeTag, BRANCH_HANDLING);
        total += getBranchSpentCost(nodeTag, BRANCH_INSTINCT);
        total += getBranchSpentCost(nodeTag, BRANCH_WANDER);
        total += getBranchSpentCost(nodeTag, BRANCH_RESONANCE);
        total += getBranchSpentCost(nodeTag, BRANCH_DAMAGE);
        total += getBranchSpentCost(nodeTag, BRANCH_MORPH_RESISTANCE);
        total += getBranchSpentCost(nodeTag, BRANCH_UTILITIES);
        total += getBranchSpentCost(nodeTag, BRANCH_SOCIAL);
        return total;
    }

    public static int getUnspentKnowledgePoints(ServerPlayer player, ResourceLocation mobId) {
        int total = getTotalKnowledgePoints(player, mobId);
        int spent = getSpentKnowledgePoints(player, mobId);
        return Math.max(0, total - spent);
    }

    public static int getBranchRank(ServerPlayer player, ResourceLocation mobId, String branch) {
        int globalRank = getBranchRank(getGlobalNodeTag(player), branch);
        if (mobId == null) {
            return globalRank;
        }
        int morphRank = getBranchRank(getNodeTag(player, mobId), branch);
        int rank = Math.max(globalRank, morphRank);
        if (isKnownBranch(branch) && isTemporaryUnlockActiveForMorph(player, mobId)) {
            return getMaxRankForBranch(branch);
        }
        return rank;
    }

    public static boolean applyTemporaryFullUnlock(ServerPlayer player, ResourceLocation morphId, int durationTicks) {
        if (player == null || morphId == null || durationTicks <= 0) {
            return false;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(morphId)) {
            return false;
        }

        CompoundTag root = getKnowledgeRoot(player);
        long now = player.level().getGameTime();
        root.putString(TEMP_UNLOCK_MORPH_ID_TAG, morphId.toString());
        root.putLong(TEMP_UNLOCK_EXPIRES_AT_TAG, now + Math.max(20L, durationTicks));
        return true;
    }

    public static int getGlobalBranchRank(ServerPlayer player, String branch) {
        return getBranchRank(getGlobalNodeTag(player), branch);
    }

    public static int getMorphOnlyBranchRank(ServerPlayer player, ResourceLocation mobId, String branch) {
        if (mobId == null) {
            return 0;
        }
        return getBranchRank(getNodeTag(player, mobId), branch);
    }

    public static boolean spendKnowledgePoint(ServerPlayer player, ResourceLocation mobId, String branch) {
        if (!isKnownBranch(branch)) {
            return false;
        }

        CompoundTag nodeTag = mobId == null ? getGlobalNodeTag(player) : getNodeTag(player, mobId);
        int rank = getBranchRank(nodeTag, branch);
        int maxRank = getMaxRankForBranch(branch);
        if (rank >= maxRank) {
            return false;
        }

        int nextRank = rank + 1;
        int cost = getBranchUpgradeCost(branch, nextRank);

        if (getUnspentKnowledgePoints(player, mobId) < cost) {
            return false;
        }

        nodeTag.putInt(branch, nextRank);
        KnowledgeClientSync.flush(player);
        return true;
    }

    public static int getGlobalXp(ServerPlayer player) {
        CompoundTag root = getKnowledgeRoot(player);
        return clampGlobalXp(CompatAccess.getInt(root, GLOBAL_XP_TAG));
    }

    /** Uses stored global XP, backfilling from morph XP totals on legacy saves. */
    public static int getEffectiveGlobalXp(ServerPlayer player) {
        int stored = getGlobalXp(player);
        int morphTotal = sumMorphXp(player);
        int effective = Math.max(stored, morphTotal);
        if (effective > stored) {
            setGlobalXp(player, effective);
        }
        return effective;
    }

    private static int sumMorphXp(ServerPlayer player) {
        CompoundTag xpMap = getXpMap(player);
        int sum = 0;
        for (String key : getTagKeys(xpMap)) {
            sum += CompatAccess.getInt(xpMap, key);
        }
        return clampGlobalXp(sum);
    }

    public static void setGlobalXp(ServerPlayer player, int value) {
        CompoundTag root = getKnowledgeRoot(player);
        root.putInt(GLOBAL_XP_TAG, clampGlobalXp(value));
    }

    public static int getGlobalMaxXp() {
        return GLOBAL_MAX_XP;
    }

    public static int getBranchUpgradeCost(String branch, int toRank) {
        int clampedRank = Math.max(1, Math.min(getMaxRankForBranch(branch), toRank));
        return switch (branch) {
            case BRANCH_HUMAN_CONNECTION -> clampedRank;
            case BRANCH_WANDER, BRANCH_INSTINCT -> clampedRank >= getMaxRankForBranch(branch) ? 5 : 1;
            case BRANCH_HANDLING -> switch (clampedRank) {
                case 1 -> 1;
                case 2, 3 -> 2;
                case 4, 5 -> 2;
                case 6, 7 -> 3;
                case 8 -> 5;
                default -> 0;
            };
            case BRANCH_VITALITY -> clampedRank;
            case BRANCH_DAMAGE -> 2;
            case BRANCH_MORPH_RESISTANCE -> clampedRank;
            case BRANCH_UTILITIES -> switch (clampedRank) {
                case 1 -> 3;
                case 2 -> 4;
                case 3 -> 5;
                default -> 0;
            };
            case BRANCH_SOCIAL -> switch (clampedRank) {
                case 1 -> 1;
                case 2 -> 2;
                case 3 -> 3;
                case 4 -> 4;
                case 5 -> 6;
                default -> 0;
            };
            default -> 0;
        };
    }

    public static int getBranchSpentCost(ServerPlayer player, ResourceLocation mobId, String branch) {
        CompoundTag nodeTag = mobId == null ? getGlobalNodeTag(player) : getNodeTag(player, mobId);
        return getBranchSpentCost(nodeTag, branch);
    }

    public static boolean hasResonanceEligibility(ServerPlayer player, ResourceLocation mobId) {
        return getXp(player, mobId) >= 1400;
    }

    public static ResourceLocation findBestResonanceCandidate(ServerPlayer player) {
        CompoundTag xpMap = getXpMap(player);
        ResourceLocation bestId = null;
        int bestXp = 0;
        int secondBest = 0;

        for (String key : getTagKeys(xpMap)) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                continue;
            }

            int xp = clampXp(CompatAccess.getInt(xpMap, key));
            if (xp > bestXp) {
                secondBest = bestXp;
                bestXp = xp;
                bestId = id;
            } else if (xp > secondBest) {
                secondBest = xp;
            }
        }

        if (bestId == null) {
            return null;
        }

        if (bestXp < 1400 || (bestXp - secondBest) < 260) {
            return null;
        }

        return bestId;
    }

    public static CompoundTag getStatCache(ServerPlayer player) {
        CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(ROOT_TAG)) {
            root.put(ROOT_TAG, new CompoundTag());
        }

        CompoundTag knowledgeRoot = CompatAccess.getCompound(root, ROOT_TAG);
        if (!knowledgeRoot.contains(STAT_CACHE_TAG)) {
            knowledgeRoot.put(STAT_CACHE_TAG, new CompoundTag());
        }

        return CompatAccess.getCompound(knowledgeRoot, STAT_CACHE_TAG);
    }

    private static CompoundTag getXpMap(ServerPlayer player) {
        CompoundTag knowledgeRoot = getKnowledgeRoot(player);
        if (!knowledgeRoot.contains(XP_MAP_TAG)) {
            knowledgeRoot.put(XP_MAP_TAG, new CompoundTag());
        }
        CompoundTag xpMap = CompatAccess.getCompound(knowledgeRoot, XP_MAP_TAG);
        knowledgeRoot.put(XP_MAP_TAG, xpMap);
        return xpMap;
    }

    private static CompoundTag getTreeMap(ServerPlayer player) {
        CompoundTag knowledgeRoot = getKnowledgeRoot(player);
        if (!knowledgeRoot.contains(TREE_MAP_TAG)) {
            knowledgeRoot.put(TREE_MAP_TAG, new CompoundTag());
        }
        CompoundTag treeMap = CompatAccess.getCompound(knowledgeRoot, TREE_MAP_TAG);
        knowledgeRoot.put(TREE_MAP_TAG, treeMap);
        return treeMap;
    }

    private static CompoundTag getNodeTag(ServerPlayer player, ResourceLocation mobId) {
        CompoundTag treeMap = getTreeMap(player);
        String key = mobId.toString();
        if (!treeMap.contains(key)) {
            treeMap.put(key, new CompoundTag());
        }
        CompoundTag node = CompatAccess.getCompound(treeMap, key);
        treeMap.put(key, node);
        return node;
    }

    private static CompoundTag getGlobalNodeTag(ServerPlayer player) {
        CompoundTag knowledgeRoot = getKnowledgeRoot(player);
        if (!knowledgeRoot.contains(GLOBAL_TREE_TAG)) {
            knowledgeRoot.put(GLOBAL_TREE_TAG, new CompoundTag());
        }
        CompoundTag node = CompatAccess.getCompound(knowledgeRoot, GLOBAL_TREE_TAG);
        knowledgeRoot.put(GLOBAL_TREE_TAG, node);
        return node;
    }

    private static CompoundTag getKnowledgeRoot(ServerPlayer player) {
        CompoundTag persistent = CompatAccess.getPersistentData(player);
        if (!persistent.contains(ROOT_TAG)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag knowledgeRoot = CompatAccess.getCompound(persistent, ROOT_TAG);
        // Re-attach in case Optional-based getCompound returned a detached empty tag.
        persistent.put(ROOT_TAG, knowledgeRoot);
        return knowledgeRoot;
    }

    private static int clampXp(int xp) {
        return Math.max(0, Math.min(MAX_XP, xp));
    }

    private static int clampGlobalXp(int xp) {
        return Math.max(0, Math.min(GLOBAL_MAX_XP, xp));
    }

    public static void addGlobalXp(ServerPlayer player, int amount) {
        addGlobalXpRaw(player, applySurvivalAsXpScale(player, amount));
    }

    private static void addGlobalXpRaw(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        CompoundTag root = getKnowledgeRoot(player);
        int current = clampGlobalXp(CompatAccess.getInt(root, GLOBAL_XP_TAG));
        root.putInt(GLOBAL_XP_TAG, clampGlobalXp(current + amount));
    }

    /**
     * Survival-as worlds evolve ~20× slower. Remainder is banked so small awards (1 XP) still accumulate.
     * Writes remainder through the persistent root so Optional-based NBT getters cannot drop the bank.
     */
    private static int applySurvivalAsXpScale(ServerPlayer player, int amount) {
        if (amount <= 0 || !SurvivalAsWorldStorage.isEnabled()) {
            return amount;
        }
        CompoundTag persistent = CompatAccess.getPersistentData(player);
        if (!persistent.contains(ROOT_TAG)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag root = CompatAccess.getCompound(persistent, ROOT_TAG);
        // If getCompound returned an unattached empty tag, re-attach before mutating.
        persistent.put(ROOT_TAG, root);
        int rem = CompatAccess.getInt(root, SURVIVAL_AS_XP_REMAINDER_TAG) + amount;
        int granted = rem / SURVIVAL_AS_XP_DIVISOR;
        root.putInt(SURVIVAL_AS_XP_REMAINDER_TAG, rem % SURVIVAL_AS_XP_DIVISOR);
        persistent.put(ROOT_TAG, root);
        return granted;
    }

    public static double getHealthBonusPercent(int vitalityRank) {
        return switch (Math.max(0, Math.min(getMaxRankForBranch(BRANCH_VITALITY), vitalityRank))) {
            case 1 -> 0.10;
            case 2 -> 0.25;
            case 3 -> 0.45;
            case 4 -> 0.70;
            case 5 -> 1.00;
            default -> 0.0;
        };
    }

    public static double getHealthBonusPercent(ServerPlayer player, ResourceLocation mobId) {
        return getHealthBonusPercent(getBranchRank(player, mobId, BRANCH_VITALITY));
    }

    public static int getHealthBonusPercentDisplay(int vitalityRank) {
        return (int) Math.floor(getHealthBonusPercent(vitalityRank) * 100.0D);
    }

    /** Hotbar slot count by handling branch rank (index = rank). Monotonic through rank 6, then full bar. */
    private static final int[] HANDLING_HOTBAR_SLOTS = {3, 4, 5, 6, 7, 8, 9, 9, 9};

    public static int getAllowedHotbarSlots(int handlingRank) {
        int rank = Math.max(0, Math.min(getMaxRankForBranch(BRANCH_HANDLING), handlingRank));
        if (rank < HANDLING_HOTBAR_SLOTS.length) {
            return HANDLING_HOTBAR_SLOTS[rank];
        }
        return 9;
    }

    public static int getAllowedHotbarSlots(int handlingRank, ResourceLocation mobId) {
        if (mobId != null && isCanineCarrierMorph(mobId) && handlingRank <= 0) {
            return NaturalisConfig.inventoryCanineHotbarSlotsAtRank0();
        }
        return getAllowedHotbarSlots(handlingRank);
    }

    public static int getAllowedHotbarSlots(ServerPlayer player, ResourceLocation mobId) {
        return getAllowedHotbarSlots(getBranchRank(player, mobId, BRANCH_HANDLING), mobId);
    }

    private static boolean isCanineCarrierMorph(ResourceLocation mobId) {
        String path = mobId.getPath().toLowerCase(java.util.Locale.ROOT);
        return "wolf".equals(path) || "fox".equals(path) || path.contains("wolf") || path.contains("fox");
    }

    public static boolean canOpenInventory(int handlingRank) {
        return handlingRank >= getMaxRankForBranch(BRANCH_HANDLING);
    }

    public static boolean canOpenInventory(ServerPlayer player, ResourceLocation mobId) {
        return canOpenInventory(getBranchRank(player, mobId, BRANCH_HANDLING));
    }

    public static boolean areInstinctsDisabled(int instinctRank) {
        return instinctRank >= getMaxRankForBranch(BRANCH_INSTINCT);
    }

    public static boolean areInstinctsDisabled(ServerPlayer player, ResourceLocation mobId) {
        return areInstinctsDisabled(getBranchRank(player, mobId, BRANCH_INSTINCT));
    }

    public static int getInstinctCheckIntervalTicks(int instinctRank) {
        // Never run every tick — fear/hunt use expensive entity scans.
        return switch (Math.max(0, Math.min(getMaxRankForBranch(BRANCH_INSTINCT), instinctRank))) {
            case 1 -> 12;
            case 2 -> 14;
            case 3 -> 16;
            case 4 -> 20;
            case 5 -> Integer.MAX_VALUE;
            default -> 10;
        };
    }

    public static int getInstinctCheckIntervalTicks(ServerPlayer player, ResourceLocation mobId) {
        return getInstinctCheckIntervalTicks(getBranchRank(player, mobId, BRANCH_INSTINCT));
    }

    public static int getAfkThresholdTicks(int wanderRank) {
        return switch (Math.max(0, Math.min(getMaxRankForBranch(BRANCH_WANDER), wanderRank))) {
            case 1 -> 450;
            case 2 -> 700;
            case 3 -> 1000;
            case 4 -> 1400;
            case 5 -> Integer.MAX_VALUE;
            default -> 300;
        };
    }

    public static int getAfkThresholdTicks(ServerPlayer player, ResourceLocation mobId) {
        return getAfkThresholdTicks(getBranchRank(player, mobId, BRANCH_WANDER));
    }

    public static int getResonanceAffinityRank(ServerPlayer player, ResourceLocation mobId) {
        return getBranchRank(player, mobId, BRANCH_HUMAN_CONNECTION);
    }

    public static int getHumanConnectionRank(ServerPlayer player, ResourceLocation mobId) {
        return getBranchRank(player, mobId, BRANCH_HUMAN_CONNECTION);
    }

    public static int getHumanityLossAvoidPercent(int humanConnectionRank) {
        int clamped = Math.max(0, Math.min(getMaxRankForBranch(BRANCH_HUMAN_CONNECTION), humanConnectionRank));
        return clamped * 10;
    }

    public static float getHumanityLossAvoidChance(int humanConnectionRank) {
        return getHumanityLossAvoidPercent(humanConnectionRank) / 100.0F;
    }

    public static int getHumanityLossAvoidPercent(ServerPlayer player, ResourceLocation mobId) {
        return getHumanityLossAvoidPercent(getHumanConnectionRank(player, mobId));
    }

    public static int getDamageRank(ServerPlayer player, ResourceLocation mobId) {
        return getBranchRank(player, mobId, BRANCH_DAMAGE);
    }

    public static double getNaturalAttackDamageMultiplier(int damageRank) {
        int clamped = Math.max(0, Math.min(getMaxRankForBranch(BRANCH_DAMAGE), damageRank));
        return 1.0D + (clamped * 0.10D);
    }

    public static double getNaturalAttackDamageMultiplier(ServerPlayer player, ResourceLocation mobId) {
        return getNaturalAttackDamageMultiplier(getDamageRank(player, mobId));
    }

    public static int getMorphResistanceRank(ServerPlayer player, ResourceLocation mobId) {
        return getBranchRank(player, mobId, BRANCH_MORPH_RESISTANCE);
    }

    public static int getMorphResistancePercent(int morphResistanceRank) {
        int clamped = Math.max(0, Math.min(getMaxRankForBranch(BRANCH_MORPH_RESISTANCE), morphResistanceRank));
        return clamped * 5;
    }

    public static double getNaturalArmorBonusPoints(int morphResistanceRank) {
        // Vanilla armor provides roughly 4% mitigation per armor point.
        return getMorphResistancePercent(morphResistanceRank) / 4.0D;
    }

    public static double getKnockbackResistanceBonus(int morphResistanceRank) {
        return getMorphResistancePercent(morphResistanceRank) / 100.0D;
    }

    public static int getUtilitiesRank(ServerPlayer player, ResourceLocation mobId) {
        return getBranchRank(player, mobId, BRANCH_UTILITIES);
    }

    public static boolean canUseToolsAsMorph(int utilitiesRank) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return true;
        }
        return utilitiesRank >= NaturalisConfig.knowledgeUtilitiesRankToMine();
    }

    public static boolean canUseToolsAsMorph(ServerPlayer player, ResourceLocation mobId) {
        return canUseToolsAsMorph(getUtilitiesRank(player, mobId));
    }

    public static boolean canPlaceBlocksAsMorph(int utilitiesRank) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return true;
        }
        return utilitiesRank >= NaturalisConfig.knowledgeUtilitiesRankToPlace();
    }

    public static boolean canPlaceBlocksAsMorph(ServerPlayer player, ResourceLocation mobId) {
        return canPlaceBlocksAsMorph(getUtilitiesRank(player, mobId));
    }

    public static boolean canUseWorldInteractionsAsMorph(int utilitiesRank) {
        if (!NaturalisConfig.gameplayEnableKnowledgeGates()) {
            return true;
        }
        return utilitiesRank >= NaturalisConfig.knowledgeUtilitiesRankForWorldUse();
    }

    public static boolean canUseWorldInteractionsAsMorph(ServerPlayer player, ResourceLocation mobId) {
        return canUseWorldInteractionsAsMorph(getUtilitiesRank(player, mobId));
    }

    public static int getSocialRank(ServerPlayer player, ResourceLocation mobId) {
        return getBranchRank(player, mobId, BRANCH_SOCIAL);
    }

    public static int getGroupCallCooldownTicks(int socialRank) {
        return socialRank >= 2 ? 30 : 60;
    }

    public static double getPackAssistRadius(int socialRank) {
        return socialRank >= 4 ? 30.0D : 20.0D;
    }

    public static int getMaxRankForBranch(String branch) {
        return switch (branch) {
            case BRANCH_VITALITY -> 5;
            case BRANCH_HANDLING -> 8;
            case BRANCH_INSTINCT, BRANCH_WANDER, BRANCH_DAMAGE -> 5;
            case BRANCH_HUMAN_CONNECTION -> 5;
            case BRANCH_MORPH_RESISTANCE -> 5;
            case BRANCH_UTILITIES -> 3;
            case BRANCH_SOCIAL -> 5;
            default -> 0;
        };
    }

    public static boolean isKnownBranch(String branch) {
        return BRANCH_VITALITY.equals(branch)
            || BRANCH_HANDLING.equals(branch)
            || BRANCH_INSTINCT.equals(branch)
            || BRANCH_WANDER.equals(branch)
                || BRANCH_HUMAN_CONNECTION.equals(branch)
            || BRANCH_DAMAGE.equals(branch)
            || BRANCH_MORPH_RESISTANCE.equals(branch)
                    || BRANCH_UTILITIES.equals(branch)
                    || BRANCH_SOCIAL.equals(branch);
    }

    private static int getBranchRank(CompoundTag nodeTag, String branch) {
        int rank = CompatAccess.getInt(nodeTag, branch);
        return Math.max(0, Math.min(getMaxRankForBranch(branch), rank));
    }

    private static int getBranchSpentCost(CompoundTag nodeTag, String branch) {
        int rank = getBranchRank(nodeTag, branch);
        int total = 0;
        for (int i = 1; i <= rank; i++) {
            total += getBranchUpgradeCost(branch, i);
        }
        return total;
    }

    private static boolean isTemporaryUnlockActiveForMorph(ServerPlayer player, ResourceLocation morphId) {
        ResourceLocation activeMorph = getTemporaryUnlockMorphIfActive(player);
        return activeMorph != null && activeMorph.equals(morphId);
    }

    private static ResourceLocation getTemporaryUnlockMorphIfActive(ServerPlayer player) {
        CompoundTag root = getKnowledgeRoot(player);
        String rawMorphId = CompatAccess.getString(root, TEMP_UNLOCK_MORPH_ID_TAG);
        if (rawMorphId == null || rawMorphId.isBlank()) {
            return null;
        }

        long expiresAt = CompatAccess.getLong(root, TEMP_UNLOCK_EXPIRES_AT_TAG);
        if (expiresAt <= player.level().getGameTime()) {
            root.remove(TEMP_UNLOCK_MORPH_ID_TAG);
            root.remove(TEMP_UNLOCK_EXPIRES_AT_TAG);
            return null;
        }

        ResourceLocation parsed = ResourceLocation.tryParse(rawMorphId);
        if (parsed == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(parsed)) {
            root.remove(TEMP_UNLOCK_MORPH_ID_TAG);
            root.remove(TEMP_UNLOCK_EXPIRES_AT_TAG);
            return null;
        }

        return parsed;
    }

    @SuppressWarnings("unchecked")
    private static Iterable<String> getTagKeys(CompoundTag tag) {
        try {
            Object raw = tag.getClass().getMethod("getAllKeys").invoke(tag);
            if (raw instanceof Iterable<?>) {
                return (Iterable<String>) raw;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = tag.getClass().getMethod("keySet").invoke(tag);
            if (raw instanceof Iterable<?>) {
                return (Iterable<String>) raw;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return java.util.Collections.emptyList();
    }

}
