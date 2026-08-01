package dev.naturalis.world.menu;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.diet.DietManager;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.inventory.InventoryRestrictionManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unchecked")
public class MorphKnowledgeMenu extends AbstractContainerMenu {

    public static final int TRAIT_QUADRUPED = 1;
    public static final int TRAIT_NYCTALOP = 1 << 1;
    public static final int TRAIT_HUNTER = 1 << 2;
    public static final int TRAIT_WANDERER = 1 << 3;
    public static final int TRAIT_FLIGHT_ONLY = 1 << 4;
    public static final int TRAIT_AQUATIC = 1 << 5;
    public static final int TRAIT_STATIC = 1 << 6;
    public static final int TRAIT_SCENTBOUND = 1 << 7;
    public static final int TRAIT_PHOTOPHOBIC = 1 << 8;
    public static final int TRAIT_FLOATING = 1 << 9;

    public static final int BRANCH_VITALITY_IDX = 12;
    public static final int BRANCH_HANDLING_IDX = 13;
    public static final int BRANCH_INSTINCT_IDX = 14;
    public static final int BRANCH_WANDER_IDX = 15;
    public static final int BRANCH_HUMAN_CONNECTION_IDX = 16;
    public static final int BRANCH_DAMAGE_IDX = 17;
    public static final int BRANCH_MORPH_RESISTANCE_IDX = 18;
    public static final int BRANCH_UTILITIES_IDX = 19;
    public static final int BRANCH_SOCIAL_IDX = 20;
    public static final int LEGACY_LEVEL_IDX = 21;
    public static final int GLOBAL_MODE_IDX = 22;

    private final ContainerData data;

    public MorphKnowledgeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        super((MenuType<MorphKnowledgeMenu>) CompatAccess.naturalisMenuType("morph_knowledge"), containerId);
        this.data = new SimpleContainerData(23);
        addDataSlots(this.data);
    }

    public MorphKnowledgeMenu(int containerId, Inventory playerInventory, ServerPlayer serverPlayer) {
        super((MenuType<MorphKnowledgeMenu>) CompatAccess.naturalisMenuType("morph_knowledge"), containerId);
        this.data = createServerData(serverPlayer);
        addDataSlots(this.data);
    }

    private static ContainerData createServerData(ServerPlayer player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                ResourceLocation currentMorph = CurrentMorphUtil.getCurrentMorphId(player);
                int globalXp = MorphKnowledgeManager.getEffectiveGlobalXp(player);
                int totalPoints = MorphKnowledgeManager.getTotalKnowledgePoints(player, currentMorph);
                int spentPoints = MorphKnowledgeManager.getSpentKnowledgePoints(player, currentMorph);
                int unspentPoints = MorphKnowledgeManager.getUnspentKnowledgePoints(player, currentMorph);
                int vitalityRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_VITALITY);
                int handlingRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_HANDLING);
                int instinctRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_INSTINCT);
                int wanderRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_WANDER);
                int humanConnectionRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION);
                int damageRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_DAMAGE);
                int morphResistanceRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE);
                int utilitiesRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_UTILITIES);
                int socialRank = MorphKnowledgeManager.getBranchRank(player, currentMorph, MorphKnowledgeManager.BRANCH_SOCIAL);
                ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
                boolean resonanceMorph = currentMorph != null && bonded != null && bonded.equals(currentMorph);
                boolean resonanceEnabled = ResonanceManager.isResonanceEnabled(player);
                int humanity = ResonanceManager.getHumanity(player);
                boolean humanityLocked = ResonanceManager.isHumanityLocked(player);
                int masteryTier = ResonanceManager.getMasteryTier(player);
                int masteryXp = ResonanceManager.getMasteryXp(player);

                if (currentMorph == null) {
                    return switch (index) {
                        case 0 -> totalPoints;
                        case 1 -> spentPoints;
                        case 2 -> unspentPoints;
                        case 3 -> globalXp;
                        case 4 -> DietManager.DietType.OMNIVORE.ordinal();
                        case 5 -> 0;
                        case 6 -> 0;
                        case 7 -> resonanceEnabled ? 1 : 0;
                        case 8 -> humanity;
                        case 9 -> humanityLocked ? 1 : 0;
                        case 10 -> masteryTier;
                        case 11 -> masteryXp;
                        case BRANCH_VITALITY_IDX -> vitalityRank;
                        case BRANCH_HANDLING_IDX -> handlingRank;
                        case BRANCH_INSTINCT_IDX -> instinctRank;
                        case BRANCH_WANDER_IDX -> wanderRank;
                        case BRANCH_HUMAN_CONNECTION_IDX -> humanConnectionRank;
                        case BRANCH_DAMAGE_IDX -> damageRank;
                        case BRANCH_MORPH_RESISTANCE_IDX -> morphResistanceRank;
                        case BRANCH_UTILITIES_IDX -> utilitiesRank;
                        case BRANCH_SOCIAL_IDX -> socialRank;
                        case LEGACY_LEVEL_IDX -> 0;
                        case GLOBAL_MODE_IDX -> 1;
                        default -> 0;
                    };
                }

                int localXp = MorphKnowledgeManager.getXp(player, currentMorph);

                int traitFlags = 0;
                if (InventoryRestrictionManager.isQuadruped(currentMorph)) {
                    traitFlags |= TRAIT_QUADRUPED;
                }
                if (EnvironmentalSusceptibilityManager.isNyctalopHostile(currentMorph)) {
                    traitFlags |= TRAIT_NYCTALOP;
                }
                if (InstinctManager.isHunterMorph(currentMorph)) {
                    traitFlags |= TRAIT_HUNTER;
                }
                if (InstinctManager.isWanderMorph(currentMorph)) {
                    traitFlags |= TRAIT_WANDERER;
                }
                if (InstinctManager.isFlightOnly(currentMorph)) {
                    traitFlags |= TRAIT_FLIGHT_ONLY;
                }
                if (EnvironmentalSusceptibilityManager.isDryVulnerable(currentMorph)) {
                    traitFlags |= TRAIT_AQUATIC;
                }
                if (InstinctManager.isStaticMorph(currentMorph)) {
                    traitFlags |= TRAIT_STATIC;
                }
                if (InstinctManager.isScentbound(currentMorph)) {
                    traitFlags |= TRAIT_SCENTBOUND;
                }
                if (InstinctManager.isPhotophobic(currentMorph)) {
                    traitFlags |= TRAIT_PHOTOPHOBIC;
                }
                if (InstinctManager.isFloatingMorph(currentMorph)) {
                    traitFlags |= TRAIT_FLOATING;
                }

                return switch (index) {
                    case 0 -> totalPoints;
                    case 1 -> spentPoints;
                    case 2 -> unspentPoints;
                    case 3 -> localXp;
                    case 4 -> DietManager.getDietType(currentMorph).ordinal();
                    case 5 -> traitFlags;
                    case 6 -> resonanceMorph ? 1 : 0;
                    case 7 -> resonanceEnabled ? 1 : 0;
                    case 8 -> humanity;
                    case 9 -> humanityLocked ? 1 : 0;
                    case 10 -> masteryTier;
                    case 11 -> masteryXp;
                    case BRANCH_VITALITY_IDX -> vitalityRank;
                    case BRANCH_HANDLING_IDX -> handlingRank;
                    case BRANCH_INSTINCT_IDX -> instinctRank;
                    case BRANCH_WANDER_IDX -> wanderRank;
                    case BRANCH_HUMAN_CONNECTION_IDX -> humanConnectionRank;
                    case BRANCH_DAMAGE_IDX -> damageRank;
                    case BRANCH_MORPH_RESISTANCE_IDX -> morphResistanceRank;
                    case BRANCH_UTILITIES_IDX -> utilitiesRank;
                    case BRANCH_SOCIAL_IDX -> socialRank;
                    case LEGACY_LEVEL_IDX -> MorphKnowledgeManager.getLevel(player, currentMorph);
                    case GLOBAL_MODE_IDX -> 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 23;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int level() {
        return data.get(LEGACY_LEVEL_IDX);
    }

    public int spentPoints() {
        return data.get(1);
    }

    public int unspentPoints() {
        return Math.max(0, data.get(2));
    }

    public int totalPoints() {
        return Math.max(0, data.get(0));
    }

    public int xpInLevel() {
        return MorphKnowledgeManager.getXpIntoCurrentLevel(totalXp());
    }

    public int xpNeeded() {
        return Math.max(1, MorphKnowledgeManager.getXpNeededForCurrentLevel(totalXp()));
    }

    public int totalXp() {
        return data.get(3);
    }

    public int progressPixels() {
        if (menuAtCap()) {
            return 120;
        }
        return Math.min(120, xpInLevel() * 120 / xpNeeded());
    }

    public boolean menuAtCap() {
        return totalXp() >= MorphKnowledgeManager.getMaxXp();
    }

    public int dietOrdinal() {
        return data.get(4);
    }

    public int traitFlags() {
        return data.get(5);
    }

    public boolean hasTrait(int traitBit) {
        return (traitFlags() & traitBit) != 0;
    }

    public boolean isResonanceMorph() {
        return data.get(6) == 1;
    }

    public boolean resonanceEnabled() {
        return data.get(7) == 1;
    }

    public int humanity() {
        return Math.max(0, Math.min(100, data.get(8)));
    }

    public boolean humanityLocked() {
        return data.get(9) == 1;
    }

    public int masteryTier() {
        return Math.max(0, data.get(10));
    }

    public int masteryXp() {
        return Math.max(0, data.get(11));
    }

    public int vitalityRank() {
        return Math.max(0, data.get(BRANCH_VITALITY_IDX));
    }

    public int handlingRank() {
        return Math.max(0, data.get(BRANCH_HANDLING_IDX));
    }

    public int instinctRank() {
        return Math.max(0, data.get(BRANCH_INSTINCT_IDX));
    }

    public int wanderRank() {
        return Math.max(0, data.get(BRANCH_WANDER_IDX));
    }

    public int resonanceRank() {
        return Math.max(0, data.get(BRANCH_HUMAN_CONNECTION_IDX));
    }

    public int humanConnectionRank() {
        return Math.max(0, data.get(BRANCH_HUMAN_CONNECTION_IDX));
    }

    public int damageRank() {
        return Math.max(0, data.get(BRANCH_DAMAGE_IDX));
    }

    public int morphResistanceRank() {
        return Math.max(0, data.get(BRANCH_MORPH_RESISTANCE_IDX));
    }

    public int utilitiesRank() {
        return Math.max(0, data.get(BRANCH_UTILITIES_IDX));
    }

    public int socialRank() {
        return Math.max(0, data.get(BRANCH_SOCIAL_IDX));
    }

    public boolean isGlobalMode() {
        return data.get(GLOBAL_MODE_IDX) == 1;
    }
}
