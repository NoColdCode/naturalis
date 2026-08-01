package dev.naturalis.client.screen;

import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.world.menu.MorphKnowledgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public class MorphKnowledgeScreen extends AbstractContainerScreen<MorphKnowledgeMenu> {

    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 272;
    private static final int BAR_WIDTH = 278;

    private static final int TREE_LEFT = 12;
    private static final int TREE_TOP = 54;
    private static final int TREE_WIDTH = 286;
    private static final int TREE_HEIGHT = 152;
    private static final int RIGHT_COL_X = 308;
    private static final int RIGHT_COL_WIDTH = 112;

    private static final int FEEDBACK_TICKS = 90;
    private static final float ROOT_X = -34.0F;
    private static final float ROOT_Y = 2.0F;
    private static final float NODE_SPACING = 26.0F;

    private float treeZoom = 1.0F;
    private float treePanX = 0.0F;
    private float treePanY = 0.0F;
    private boolean draggingTree = false;
    private double lastDragX;
    private double lastDragY;
    private int previousSpentPoints = -1;
    private int[] previousBranchRanks = null;
    private @Nullable Component recentBoughtMessage;
    private int recentBoughtTicks;

    private static final List<TreeBranch> BRANCHES = List.of(
        new TreeBranch(MorphKnowledgeManager.BRANCH_VITALITY, "gui.naturalis.knowledge.branch.vitality", 5, -0.82F, -0.52F, 0xFF8EE0C4),
        new TreeBranch(MorphKnowledgeManager.BRANCH_HANDLING, "gui.naturalis.knowledge.branch.handling", 8, 0.98F, -0.18F, 0xFFE3C587),
        new TreeBranch(MorphKnowledgeManager.BRANCH_INSTINCT, "gui.naturalis.knowledge.branch.instinct", 5, 0.78F, -0.62F, 0xFF9FB8FF),
        new TreeBranch(MorphKnowledgeManager.BRANCH_WANDER, "gui.naturalis.knowledge.branch.wander", 5, -0.84F, 0.56F, 0xFF9BEA9B),
        new TreeBranch(MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION, "gui.naturalis.knowledge.branch.human_connection", 5, 0.74F, 0.64F, 0xFFE2A8FF),
        new TreeBranch(MorphKnowledgeManager.BRANCH_DAMAGE, "gui.naturalis.knowledge.branch.damage", 5, 0.96F, 0.34F, 0xFFFF8D73),
        new TreeBranch(MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE, "gui.naturalis.knowledge.branch.morph_resistance", 5, -0.28F, -0.98F, 0xFFA8D6FF),
        new TreeBranch(MorphKnowledgeManager.BRANCH_UTILITIES, "gui.naturalis.knowledge.branch.utilities", 3, -0.22F, 0.94F, 0xFFFFD88E),
        new TreeBranch(MorphKnowledgeManager.BRANCH_SOCIAL, "gui.naturalis.knowledge.branch.social", 5, 0.16F, 1.02F, 0xFFFFA5C9)
    );

    public MorphKnowledgeScreen(MorphKnowledgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF0E1320, 0xFF151F33);
        guiGraphics.fill(x, y, x + imageWidth, y + 2, 0xFF6CB6FF);
        guiGraphics.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, 0xFF101722);
        guiGraphics.fill(x, y, x + 2, y + imageHeight, 0xFF2E4F73);
        guiGraphics.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, 0xFF101722);

        guiGraphics.fill(x + 10, y + 20, x + imageWidth - 10, y + 22, 0xAA2A3C57);
        guiGraphics.fill(x + RIGHT_COL_X - 8, y + 8, x + RIGHT_COL_X - 7, y + imageHeight - 8, 0x99486A93);

        int barX = leftPos + 12;
        int barY = topPos + 26;
        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + 12, 0xFF1A2435);
        int xpInPointLevel = menu.isGlobalMode()
            ? MorphKnowledgeManager.getGlobalXpIntoCurrentPointLevel(menu.totalXp())
            : MorphKnowledgeManager.getXpIntoCurrentPointLevel(menu.totalXp());
        int xpNeededPointLevel = menu.isGlobalMode()
            ? MorphKnowledgeManager.getGlobalXpNeededForCurrentPointLevel(menu.totalXp())
            : MorphKnowledgeManager.getXpNeededForCurrentPointLevel(menu.totalXp());
        int progressWidth = Math.min(BAR_WIDTH - 2, xpInPointLevel * (BAR_WIDTH - 2) / Math.max(1, xpNeededPointLevel));
        guiGraphics.fill(barX + 1, barY + 1, barX + 1 + progressWidth, barY + 11, 0xFF28D7AC);

        if (shouldShowHumanityInfo()) {
            int humanityBarY = barY + 16;
            int humanityBarWidth = 120;
            guiGraphics.fill(barX, humanityBarY, barX + humanityBarWidth, humanityBarY + 8, 0xFF1A2435);
            int humanityFill = Mth.clamp(menu.humanity(), 0, 100) * (humanityBarWidth - 2) / 100;
            int humanityColor = menu.humanityLocked() ? 0xFFC44F4F : 0xFF69C3FF;
            guiGraphics.fill(barX + 1, humanityBarY + 1, barX + 1 + humanityFill, humanityBarY + 7, humanityColor);
        }

        int panelX = leftPos + RIGHT_COL_X;
        int panelY = topPos + 54;
        guiGraphics.fill(panelX, panelY, panelX + RIGHT_COL_WIDTH, panelY + 160, 0xAA121C2D);
        guiGraphics.fill(panelX, panelY, panelX + RIGHT_COL_WIDTH, panelY + 1, 0xFF5F89B8);
        guiGraphics.fill(panelX, panelY + 159, panelX + RIGHT_COL_WIDTH, panelY + 160, 0xFF0D1522);

        renderTreeCanvas(guiGraphics, mouseX, mouseY);

        if (recentBoughtMessage != null && recentBoughtTicks > 0) {
            int fx0 = leftPos + 14;
            int fy0 = topPos + imageHeight - 30;
            int fx1 = leftPos + imageWidth - 14;
            int fy1 = topPos + imageHeight - 12;
            guiGraphics.fill(fx0, fy0, fx1, fy1, 0xCC123420);
            guiGraphics.fill(fx0, fy0, fx1, fy0 + 1, 0xFF3DD786);
            guiGraphics.drawString(this.font, recentBoughtMessage, fx0 + 6, fy0 + 5, 0xFFB9FFD8, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int pointLevel = menu.isGlobalMode()
            ? MorphKnowledgeManager.getGlobalPointLevelForXp(menu.totalXp())
            : MorphKnowledgeManager.getPointLevelForXp(menu.totalXp());
        int pointLevelCap = MorphKnowledgeManager.getMaxPointLevel();
        int xpInPointLevel = menu.isGlobalMode()
            ? MorphKnowledgeManager.getGlobalXpIntoCurrentPointLevel(menu.totalXp())
            : MorphKnowledgeManager.getXpIntoCurrentPointLevel(menu.totalXp());
        int xpNeededPointLevel = menu.isGlobalMode()
            ? MorphKnowledgeManager.getGlobalXpNeededForCurrentPointLevel(menu.totalXp())
            : MorphKnowledgeManager.getXpNeededForCurrentPointLevel(menu.totalXp());
        int maxTrackXp = menu.isGlobalMode() ? MorphKnowledgeManager.getGlobalMaxXp() : MorphKnowledgeManager.getMaxXp();

        guiGraphics.drawString(this.font, this.title, 12, 8, 0xFF96D3FF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.total", menu.totalXp(), maxTrackXp), 12, 24, 0xFF89F4CE, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.xp", xpInPointLevel, xpNeededPointLevel), 12, 38, 0xFFB7C5E3, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.points", menu.spentPoints(), menu.totalPoints(), menu.unspentPoints()), RIGHT_COL_X, 22, 0xFFE6D49A, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.level", pointLevel, pointLevelCap), RIGHT_COL_X, 36, 0xFF9CC9F8, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.tip_tree"), 12, 214, 0xFF8FAECC, false);

        if (shouldShowHumanityInfo()) {
            int humanityColor = menu.humanityLocked() ? 0xFFC44F4F : 0xFF88D5FF;
            guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.humanity", menu.humanity()), 136, 40, humanityColor, false);
        }

        TreeNode hovered = findHoveredNode(mouseX, mouseY);
        int infoY = 60;
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.details"), RIGHT_COL_X + 4, infoY, 0xFF9CC9F8, false);
        infoY += 12;
        if (hovered != null) {
            int rank = branchRank(hovered.branch.id);
            int max = hovered.branch.maxRank;
            int cost = hovered.rank <= max ? MorphKnowledgeManager.getBranchUpgradeCost(hovered.branch.id, hovered.rank) : 0;
            guiGraphics.drawString(this.font, Component.translatable(hovered.branch.titleKey), RIGHT_COL_X + 4, infoY, hovered.branch.color, false);
            infoY += 12;
            guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.rank", rank, max), RIGHT_COL_X + 4, infoY, 0xFFB7C5E3, false);
            infoY += 12;
            guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.cost", cost), RIGHT_COL_X + 4, infoY, 0xFF9DB7D8, false);
            infoY += 12;
            if (hovered.rank <= rank) {
                guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.node_state.bought"), RIGHT_COL_X + 4, infoY, 0xFF61E28E, false);
            } else if (hovered.rank == rank + 1 && menu.unspentPoints() >= cost) {
                guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.node_state.next"), RIGHT_COL_X + 4, infoY, 0xFFF6D587, false);
            } else {
                guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.node_state.locked"), RIGHT_COL_X + 4, infoY, 0xFF8CA0BC, false);
            }

            infoY += 14;
            guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.preview"), RIGHT_COL_X + 4, infoY, 0xFF9CC9F8, false);
            infoY += 12;
            List<Component> previewLines = getUpgradePreviewLines(hovered);
            int wrapW = RIGHT_COL_WIDTH - 8;
            for (Component line : previewLines) {
                drawWrapped(guiGraphics, line, RIGHT_COL_X + 4, infoY, wrapW, 0xFFB7C5E3);
                infoY += wrappedPreviewHeight(line, wrapW);
            }
        } else {
            drawWrapped(guiGraphics, Component.translatable("gui.naturalis.knowledge.hover_hint"), RIGHT_COL_X + 4, infoY, RIGHT_COL_WIDTH - 8, 0xFF90A8C7);
        }

        int lowerInfoY = 232;
        int lowerClipX0 = leftPos + RIGHT_COL_X;
        int lowerClipY0 = topPos + lowerInfoY - 2;
        int lowerClipX1 = leftPos + RIGHT_COL_X + RIGHT_COL_WIDTH;
        int lowerClipY1 = topPos + PANEL_HEIGHT - 12;
        guiGraphics.enableScissor(lowerClipX0, lowerClipY0, lowerClipX1, lowerClipY1);
        guiGraphics.drawString(this.font, Component.translatable("gui.naturalis.knowledge.diet", getDietLabel()), RIGHT_COL_X, lowerInfoY, 0xFFE6D49A, false);
        drawWrappedLimited(guiGraphics, Component.translatable("gui.naturalis.knowledge.traits", getTraitsLabel()), RIGHT_COL_X, lowerInfoY + 12, RIGHT_COL_WIDTH, 0xFF9DB7D8, 3);
        guiGraphics.disableScissor();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTreeCanvas(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = leftPos + TREE_LEFT;
        int y0 = topPos + TREE_TOP;
        int x1 = x0 + TREE_WIDTH;
        int y1 = y0 + TREE_HEIGHT;

        g.fill(x0, y0, x1, y1, 0xAA0C1628);
        g.fill(x0, y0, x1, y0 + 1, 0xFF5D8EC2);
        g.fill(x0, y1 - 1, x1, y1, 0xFF0A1220);

        for (int gx = 0; gx < TREE_WIDTH; gx += 16) {
            g.fill(x0 + gx, y0, x0 + gx + 1, y1, 0x11354C6E);
        }
        for (int gy = 0; gy < TREE_HEIGHT; gy += 16) {
            g.fill(x0, y0 + gy, x1, y0 + gy + 1, 0x11354C6E);
        }

        g.enableScissor(x0 + 1, y0 + 1, x1 - 1, y1 - 1);

        int rootScreenX = worldToScreenX(ROOT_X);
        int rootScreenY = worldToScreenY(ROOT_Y);
        TreeNode hoveredNode = findHoveredNode(mouseX, mouseY);

        for (TreeBranch branch : BRANCHES) {
            int rank = branchRank(branch.id);

            int firstX = worldToScreenX(branchNodeWorldX(branch, 1));
            int firstY = worldToScreenY(branchNodeWorldY(branch, 1));
            drawThinConnection(g, rootScreenX, rootScreenY, firstX, firstY, 0x44739AC8);

            for (int i = 1; i <= branch.maxRank; i++) {
                float wx = branchNodeWorldX(branch, i);
                float wy = branchNodeWorldY(branch, i);
                int sx = worldToScreenX(wx);
                int sy = worldToScreenY(wy);
                int r = Math.max(3, (int) (4 * treeZoom));

                boolean unlocked = i <= rank;
                int nodeCost = MorphKnowledgeManager.getBranchUpgradeCost(branch.id, i);
                boolean next = i == rank + 1 && menu.unspentPoints() >= nodeCost;
                int nodeColor = unlocked ? branch.color : 0xFF2A3442;
                g.fill(sx - r, sy - r, sx + r, sy + r, nodeColor);
                g.fill(sx - 1, sy - 1, sx + 1, sy + 1, unlocked ? 0xEEFFFFFF : 0xAA556178);

                if (unlocked) {
                    g.drawString(this.font, "+", sx - 2, sy - 4, 0xFF10263E, false);
                } else if (next) {
                    g.fill(sx - 2, sy - 2, sx + 2, sy + 2, 0xFFEACF7A);
                }

                if (i > 1) {
                    int px = worldToScreenX(branchNodeWorldX(branch, i - 1));
                    int py = worldToScreenY(branchNodeWorldY(branch, i - 1));
                    drawThinConnection(g, px, py, sx, sy, unlocked ? 0xAA99D8FF : 0x44445A78);
                }
            }

            int titleX = worldToScreenX(branchNodeWorldX(branch, 1) + (branch.dirX < 0 ? -58.0F : 8.0F));
            int titleY = worldToScreenY(branchNodeWorldY(branch, 1) - 11.0F);
            int endX = worldToScreenX(branchNodeWorldX(branch, branch.maxRank));
            int endY = worldToScreenY(branchNodeWorldY(branch, branch.maxRank));
            int iconX = endX + (branch.dirX >= 0 ? 6 : -14);
            int iconY = endY - 5;
            g.drawString(this.font, branchIcon(branch.id), iconX, iconY, branch.color, false);

            if (hoveredNode != null && hoveredNode.branch.id.equals(branch.id)) {
                titleX = endX + (branch.dirX >= 0 ? 16 : -92);
                titleY = endY - 9;
                titleX = Math.max(x0 + 6, Math.min(x1 - 120, titleX));
                titleY = Math.max(y0 + 6, Math.min(y1 - 18, titleY));
                g.drawString(this.font, Component.translatable(branch.titleKey), titleX, titleY, branch.color, false);
                g.drawString(this.font, rank + "/" + branch.maxRank, titleX, titleY + 10, 0xFF9DB7D8, false);
            }
        }

        int rootR = Math.max(7, (int) (8 * treeZoom));
        g.fill(rootScreenX - rootR, rootScreenY - rootR, rootScreenX + rootR, rootScreenY + rootR, 0xFF4D6B8E);
        g.fill(rootScreenX - rootR + 1, rootScreenY - rootR + 1, rootScreenX + rootR - 1, rootScreenY + rootR - 1, 0xFFB8D6FF);
        g.drawString(this.font, "R", rootScreenX - 2, rootScreenY - 4, 0xFF0E223A, false);

        g.disableScissor();
    }

    private int branchRank(String branch) {
        return switch (branch) {
            case MorphKnowledgeManager.BRANCH_VITALITY -> menu.vitalityRank();
            case MorphKnowledgeManager.BRANCH_HANDLING -> menu.handlingRank();
            case MorphKnowledgeManager.BRANCH_INSTINCT -> menu.instinctRank();
            case MorphKnowledgeManager.BRANCH_WANDER -> menu.wanderRank();
            case MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION -> menu.humanConnectionRank();
            case MorphKnowledgeManager.BRANCH_DAMAGE -> menu.damageRank();
            case MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE -> menu.morphResistanceRank();
            case MorphKnowledgeManager.BRANCH_UTILITIES -> menu.utilitiesRank();
            case MorphKnowledgeManager.BRANCH_SOCIAL -> menu.socialRank();
            default -> 0;
        };
    }

    private int worldToScreenX(float worldX) {
        int centerX = leftPos + TREE_LEFT + TREE_WIDTH / 2;
        return centerX + (int) ((worldX + treePanX) * treeZoom);
    }

    private int worldToScreenY(float worldY) {
        int centerY = topPos + TREE_TOP + TREE_HEIGHT / 2;
        return centerY + (int) ((worldY + treePanY) * treeZoom);
    }

    private boolean insideTree(double x, double y) {
        return x >= leftPos + TREE_LEFT
            && y >= topPos + TREE_TOP
            && x <= leftPos + TREE_LEFT + TREE_WIDTH
            && y <= topPos + TREE_TOP + TREE_HEIGHT;
    }

    @Nullable
    private TreeNode findHoveredNode(double mouseX, double mouseY) {
        if (!insideTree(mouseX, mouseY)) {
            return null;
        }
        for (TreeBranch branch : BRANCHES) {
            for (int i = 1; i <= branch.maxRank; i++) {
                float wx = branchNodeWorldX(branch, i);
                float wy = branchNodeWorldY(branch, i);
                int sx = worldToScreenX(wx);
                int sy = worldToScreenY(wy);
                int r = Math.max(7, (int) (8 * treeZoom));
                double dx = mouseX - sx;
                double dy = mouseY - sy;
                if ((dx * dx + dy * dy) <= (r * r)) {
                    return new TreeNode(branch, i);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (insideTree(mouseX, mouseY) && button == 0) {
            TreeNode node = findHoveredNode(mouseX, mouseY);
            if (node != null) {
                int rank = branchRank(node.branch.id);
                int clickCost = MorphKnowledgeManager.getBranchUpgradeCost(node.branch.id, node.rank);
                if (menu.unspentPoints() >= clickCost && node.rank == rank + 1 && rank < node.branch.maxRank) {
                    if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
                        minecraft.player.connection.sendCommand("morph knowledge spend " + node.branch.id);
                    }
                    return true;
                }
            }

            draggingTree = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingTree = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingTree && insideTree(mouseX, mouseY)) {
            treePanX += (float) ((mouseX - lastDragX) / Math.max(0.2F, treeZoom));
            treePanY += (float) ((mouseY - lastDragY) / Math.max(0.2F, treeZoom));
            clampTreePan();
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (insideTree(mouseX, mouseY)) {
            treeZoom = Math.max(0.70F, Math.min(2.4F, treeZoom + (float) scrollY * 0.08F));
            clampTreePan();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return mouseScrolled(mouseX, mouseY, 0.0D, scrollY);
    }

    private Component getDietLabel() {
        return switch (menu.dietOrdinal()) {
            case 0 -> Component.translatable("gui.naturalis.knowledge.diet.carnivore");
            case 1 -> Component.translatable("gui.naturalis.knowledge.diet.herbivore");
            default -> Component.translatable("gui.naturalis.knowledge.diet.omnivore");
        };
    }

    private Component getTraitsLabel() {
        java.util.List<Component> traits = new java.util.ArrayList<>();
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_QUADRUPED)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.quadruped"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_NYCTALOP)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.nyctalop"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_HUNTER)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.hunter"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_WANDERER)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.wanderer"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_FLIGHT_ONLY)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.flight_only"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_AQUATIC)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.aquatic"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_STATIC)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.static"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_SCENTBOUND)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.scentbound"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_PHOTOPHOBIC)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.photophobic"));
        }
        if (menu.hasTrait(MorphKnowledgeMenu.TRAIT_FLOATING)) {
            traits.add(Component.translatable("gui.naturalis.knowledge.trait.floating"));
        }
        if (traits.isEmpty()) {
            return Component.translatable("gui.naturalis.knowledge.trait.none");
        }

        Component joined = traits.get(0);
        for (int i = 1; i < traits.size(); i++) {
            joined = Component.empty().append(joined).append(Component.literal(", ")).append(traits.get(i));
        }
        return joined;
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (previousSpentPoints < 0) {
            previousSpentPoints = menu.spentPoints();
            previousBranchRanks = currentBranchRanks();
        } else {
            int spentNow = menu.spentPoints();
            if (spentNow > previousSpentPoints && previousBranchRanks != null) {
                int[] nowRanks = currentBranchRanks();
                for (int i = 0; i < nowRanks.length; i++) {
                    if (nowRanks[i] > previousBranchRanks[i]) {
                        TreeBranch branch = BRANCHES.get(i);
                        recentBoughtMessage = Component.translatable(
                            "gui.naturalis.knowledge.bought",
                            Component.translatable(branch.titleKey),
                            nowRanks[i],
                            branch.maxRank
                        );
                        recentBoughtTicks = FEEDBACK_TICKS;
                        break;
                    }
                }
                previousBranchRanks = nowRanks;
            }
            previousSpentPoints = spentNow;
        }

        if (recentBoughtTicks > 0) {
            recentBoughtTicks--;
            if (recentBoughtTicks == 0) {
                recentBoughtMessage = null;
            }
        }
    }

    private int[] currentBranchRanks() {
        return new int[] {
            menu.vitalityRank(),
            menu.handlingRank(),
            menu.instinctRank(),
            menu.wanderRank(),
            menu.humanConnectionRank(),
            menu.damageRank(),
            menu.morphResistanceRank(),
            menu.utilitiesRank(),
            menu.socialRank()
        };
    }

    private int wrappedPreviewHeight(Component text, int width) {
        return Math.max(10, this.font.split(text, width).size() * 10 + 2);
    }

    private void drawWrapped(GuiGraphics g, Component text, int x, int y, int width, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text, width);
        int lineY = y;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            g.drawString(this.font, line, x, lineY, color, false);
            lineY += 10;
        }
    }

    private void drawWrappedLimited(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text, width);
        int lineY = y;
        int count = 0;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (count >= maxLines) {
                g.drawString(this.font, "...", x, lineY - 1, color, false);
                return;
            }
            g.drawString(this.font, line, x, lineY, color, false);
            lineY += 10;
            count++;
        }
    }

    private boolean shouldShowHumanityInfo() {
        return menu.resonanceEnabled() || menu.humanity() < 100 || menu.humanityLocked() || menu.isResonanceMorph();
    }

    private String branchIcon(String branchId) {
        return switch (branchId) {
            case MorphKnowledgeManager.BRANCH_VITALITY -> "\u2665";
            case MorphKnowledgeManager.BRANCH_HANDLING -> "\u270B";
            case MorphKnowledgeManager.BRANCH_INSTINCT -> "\u25C9";
            case MorphKnowledgeManager.BRANCH_WANDER -> "\u27A4";
            case MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION -> "\u263B";
            case MorphKnowledgeManager.BRANCH_DAMAGE -> "\u2694";
            case MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE -> "\u26E8";
            case MorphKnowledgeManager.BRANCH_UTILITIES -> "\u2699";
            case MorphKnowledgeManager.BRANCH_SOCIAL -> "\u266B";
            default -> "+";
        };
    }

    private List<Component> getUpgradePreviewLines(TreeNode hovered) {
        int currentRank = branchRank(hovered.branch.id);
        int targetRank = Math.max(1, Math.min(hovered.rank, hovered.branch.maxRank));
        if (targetRank <= currentRank && currentRank < hovered.branch.maxRank) {
            targetRank = currentRank + 1;
        }
        int fromRank = targetRank - 1;
        int toRank = targetRank;

        List<Component> lines = new ArrayList<>();
        switch (hovered.branch.id) {
            case MorphKnowledgeManager.BRANCH_VITALITY -> lines.add(Component.translatable(
                "gui.naturalis.knowledge.preview.vitality",
                MorphKnowledgeManager.getHealthBonusPercentDisplay(fromRank),
                MorphKnowledgeManager.getHealthBonusPercentDisplay(toRank)
            ));
            case MorphKnowledgeManager.BRANCH_HANDLING -> {
                ResourceLocation morphId = minecraft != null && minecraft.player != null
                    ? CurrentMorphUtil.getCurrentMorphId(minecraft.player)
                    : null;
                int fromSlots = MorphKnowledgeManager.getAllowedHotbarSlots(fromRank, morphId);
                int toSlots = MorphKnowledgeManager.getAllowedHotbarSlots(toRank, morphId);
                if (toSlots > fromSlots) {
                    lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.handling",
                        fromSlots,
                        toSlots
                    ));
                } else {
                    lines.add(Component.translatable("gui.naturalis.knowledge.preview.handling.full", toSlots));
                }
                if (!MorphKnowledgeManager.canOpenInventory(fromRank) && MorphKnowledgeManager.canOpenInventory(toRank)) {
                    lines.add(Component.translatable("gui.naturalis.knowledge.preview.inventory"));
                }
            }
            case MorphKnowledgeManager.BRANCH_INSTINCT -> {
                if (toRank >= MorphKnowledgeManager.getMaxRankForBranch(MorphKnowledgeManager.BRANCH_INSTINCT)) {
                    lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.instinct_disabled",
                        MorphKnowledgeManager.getInstinctCheckIntervalTicks(fromRank)
                    ));
                } else {
                    lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.instinct",
                        MorphKnowledgeManager.getInstinctCheckIntervalTicks(fromRank),
                        MorphKnowledgeManager.getInstinctCheckIntervalTicks(toRank)
                    ));
                }
            }
            case MorphKnowledgeManager.BRANCH_WANDER -> {
                if (toRank >= MorphKnowledgeManager.getMaxRankForBranch(MorphKnowledgeManager.BRANCH_WANDER)) {
                    lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.wander_disabled",
                        MorphKnowledgeManager.getAfkThresholdTicks(fromRank)
                    ));
                } else {
                    lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.wander",
                        MorphKnowledgeManager.getAfkThresholdTicks(fromRank),
                        MorphKnowledgeManager.getAfkThresholdTicks(toRank)
                    ));
                }
            }
            case MorphKnowledgeManager.BRANCH_HUMAN_CONNECTION -> lines.add(Component.translatable(
                "gui.naturalis.knowledge.preview.human_connection",
                MorphKnowledgeManager.getHumanityLossAvoidPercent(fromRank),
                MorphKnowledgeManager.getHumanityLossAvoidPercent(toRank)
            ));
            case MorphKnowledgeManager.BRANCH_DAMAGE -> lines.add(Component.translatable(
                "gui.naturalis.knowledge.preview.damage",
                (int) Math.round((MorphKnowledgeManager.getNaturalAttackDamageMultiplier(fromRank) - 1.0D) * 100.0D),
                (int) Math.round((MorphKnowledgeManager.getNaturalAttackDamageMultiplier(toRank) - 1.0D) * 100.0D)
            ));
            case MorphKnowledgeManager.BRANCH_MORPH_RESISTANCE -> lines.add(Component.translatable(
                "gui.naturalis.knowledge.preview.morph_resistance",
                MorphKnowledgeManager.getMorphResistancePercent(fromRank),
                MorphKnowledgeManager.getMorphResistancePercent(toRank)
            ));
            case MorphKnowledgeManager.BRANCH_UTILITIES -> {
                if (!MorphKnowledgeManager.canUseToolsAsMorph(fromRank) && MorphKnowledgeManager.canUseToolsAsMorph(toRank)) {
                    lines.add(Component.translatable("gui.naturalis.knowledge.preview.utilities.tools"));
                }
                if (!MorphKnowledgeManager.canPlaceBlocksAsMorph(fromRank) && MorphKnowledgeManager.canPlaceBlocksAsMorph(toRank)) {
                    lines.add(Component.translatable("gui.naturalis.knowledge.preview.utilities.place"));
                }
                if (!MorphKnowledgeManager.canUseWorldInteractionsAsMorph(fromRank) && MorphKnowledgeManager.canUseWorldInteractionsAsMorph(toRank)) {
                    lines.add(Component.translatable("gui.naturalis.knowledge.preview.utilities.interactions"));
                }
            }
            case MorphKnowledgeManager.BRANCH_SOCIAL -> {
                switch (toRank) {
                    case 1 -> lines.add(Component.translatable("gui.naturalis.knowledge.preview.social.follow"));
                    case 2 -> lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.social.cooldown",
                        MorphKnowledgeManager.getGroupCallCooldownTicks(fromRank),
                        MorphKnowledgeManager.getGroupCallCooldownTicks(toRank)
                    ));
                    case 3 -> lines.add(Component.translatable("gui.naturalis.knowledge.preview.social.regen"));
                    case 4 -> lines.add(Component.translatable(
                        "gui.naturalis.knowledge.preview.social.pack_range",
                        (int) MorphKnowledgeManager.getPackAssistRadius(fromRank),
                        (int) MorphKnowledgeManager.getPackAssistRadius(toRank)
                    ));
                    case 5 -> lines.add(Component.translatable("gui.naturalis.knowledge.preview.social.alpha"));
                    default -> {
                    }
                }
            }
            default -> {
            }
        }

        if (targetRank > currentRank + 1) {
            lines.add(Component.translatable("gui.naturalis.knowledge.preview.locked_far"));
        }

        return lines;
    }

    private float branchNodeWorldX(TreeBranch branch, int nodeRank) {
        return ROOT_X + branch.dirX * NODE_SPACING * nodeRank;
    }

    private float branchNodeWorldY(TreeBranch branch, int nodeRank) {
        return ROOT_Y + branch.dirY * NODE_SPACING * nodeRank;
    }

    private void clampTreePan() {
        float halfW = TREE_WIDTH / (2.0F * treeZoom);
        float halfH = TREE_HEIGHT / (2.0F * treeZoom);

        float contentMinX = ROOT_X - 120.0F;
        float contentMaxX = ROOT_X + 120.0F;
        float contentMinY = ROOT_Y - 100.0F;
        float contentMaxY = ROOT_Y + 100.0F;

        treePanX = clampPanAxis(treePanX, contentMinX, contentMaxX, halfW);
        treePanY = clampPanAxis(treePanY, contentMinY, contentMaxY, halfH);
    }

    private float clampPanAxis(float currentPan, float contentMin, float contentMax, float visibleHalfSpan) {
        float minPan = -visibleHalfSpan - contentMin;
        float maxPan = visibleHalfSpan - contentMax;
        if (minPan > maxPan) {
            float freePan = 80.0F / Math.max(0.8F, treeZoom);
            return Math.max(-freePan, Math.min(freePan, currentPan));
        }
        return Math.max(minPan, Math.min(maxPan, currentPan));
    }

    private void drawThinConnection(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + dx * i / steps;
            int y = y0 + dy * i / steps;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private record TreeBranch(String id, String titleKey, int maxRank, float dirX, float dirY, int color) {
    }

    private record TreeNode(TreeBranch branch, int rank) {
    }
}
