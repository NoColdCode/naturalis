package dev.naturalis.gameplay;

import dev.naturalis.Naturalis;
import dev.naturalis.chat.FeralChatSystem;
import dev.naturalis.combat.NaturalAttackManager;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.command.MorphCommand;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.gameplay.MorphAnimalInteraction;
import dev.naturalis.gameplay.logic.MorphGameplayTickLogic;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.gameplay.PrimalMovementState;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphAcquisition;
import dev.naturalis.util.MorphDataUtil;
import dev.naturalis.util.MorphShapeUtil;
import dev.naturalis.worldgen.EchoSovereignRuntime;
import dev.naturalis.worldgen.NaturalDimensionKeys;
import dev.naturalis.worldgen.NaturalDimensionRuntime;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;
import dev.tocraft.walkers.api.PlayerShape;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class NaturalisGameplayEvents {

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(Naturalis.MOD_ID, "root");

    private static final ResourceLocation KNOWLEDGE_ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Naturalis.MOD_ID, "knowledge_morph_resistance_armor");
    private static final ResourceLocation KNOWLEDGE_KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Naturalis.MOD_ID, "knowledge_morph_resistance_knockback");
    private static final int BINDING_POTION_DURATION = 8 * 20 * 60;

    /**
     * Humanoid-form mobs that retain the ability to use vanilla tools while morphed.
     * All other morphs are restricted to echo tools only.
     */
    private static final Set<String> HUMANOID_TOOL_USERS = Set.of(
        "villager", "zombie_villager", "wandering_trader",
        "pillager", "evoker", "vindicator", "illusioner", "witch"
    );
    private static final double SPLASH_POTION_RADIUS = 4.0D;
    private static final double LINGERING_POTION_RADIUS = 3.0D;
    private static final ResourceKey<Biome> ECHO_BIOME_KEY = NaturalDimensionKeys.NATURAL_ECHO;

    private NaturalisGameplayEvents() {
    }

    public static void register(IEventBus modEventBus) {
        // Uses global event bus listeners via @EventBusSubscriber.
    }

    public static void setPrimalMovementKey(ServerPlayer player, boolean down) {
        PrimalMovementState.setPrimalKeyDown(player, down);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        MorphCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            grantAdvancement(player, ADV_ROOT);
            MorphQuickSlotBridge.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        FeralChatSystem.handleServerChat(event);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        NaturalDimensionRuntime.modifyMobLoot(event.getEntity(), event.getSource(), event.getDrops());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
            NaturalDimensionRuntime.onOverworldChunkLoaded(serverLevel, chunk);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) {
            return;
        }
        if (EchoSovereignRuntime.isEchoSovereign(mob)) {
            return;
        }
        if (!event.getLevel().getBiome(mob.blockPosition()).is(ECHO_BIOME_KEY)) {
            return;
        }
        if (!(mob instanceof net.minecraft.world.entity.monster.Monster)) {
            return;
        }
        event.setCanceled(true);
        mob.discard();
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        float damage = EchoSovereignRuntime.modifyIncomingBossDamage(event.getEntity(), event.getSource(), event.getNewDamage());
        event.setNewDamage(damage);
        EchoSovereignRuntime.prepareBossPhaseMeterAfterClamp(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        EchoSovereignRuntime.onBossDamagedAfterApplied(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (trySovereignEchoCapture(player, event.getItemStack(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (tryCollectEcho(player, event.getItemStack(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId != null
            && MorphSpeciesSecondaryAction.resolve(morphId) == MorphSpeciesSecondaryAction.PECK
            && trySpeciesSecondaryUse(player, event.getHand(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (trySovereignEchoCapture(player, event.getItemStack(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (tryCollectEcho(player, event.getItemStack(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (tryUseMorphPotions(player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (tryUseKnowledgeBoostItems(player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (tryForgetKnowledge(player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (trySpeciesSecondaryUse(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean tryUseMorphPotions(Player player, ItemStack inHand) {
        if (player.level().isClientSide()) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (inHand.is(NaturalisItems.BREWED_MORPH_POTION.get())) {
            if (MorphEffectEvents.applyBrewedMorphFromStack(serverPlayer, inHand, true)) {
                consumeDrinkablePotion(serverPlayer, inHand);
            }
            return true;
        }

        if (inHand.is(NaturalisItems.MORPH_BINDING_POTION.get())) {
            serverPlayer.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING, BINDING_POTION_DURATION, 0, true, false, true));
            consumeDrinkablePotion(serverPlayer, inHand);
            return true;
        }

        return false;
    }

    private static void consumeDrinkablePotion(ServerPlayer player, ItemStack stack) {
        if (player.isCreative()) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
            return;
        }

        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        ItemStack held = player.getMainHandItem();
        boolean echoTool = isEchoTool(held);

        if (morphId == null || echoTool || (isToolLike(held) && isHumanoidToolUser(morphId))) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
            && MorphAnimalInteraction.usesSpeciesPrimaryAction(morphId)
            && !MorphAnimalInteraction.canMineBlocksAsMorph(serverPlayer, morphId, echoTool)) {
            MorphInteractionSystem.handleBreakFeedback(
                player, morphId, event.getPos(), player.level().getBlockState(event.getPos())
            );
            if (serverPlayer.tickCount % 24 == 0) {
                serverPlayer.displayClientMessage(
                    Component.translatable("message.naturalis.interaction.species_primary"),
                    true
                );
            }
            event.setCanceled(true);
            return;
        }

        MorphInteractionSystem.handleBreakFeedback(player, morphId, event.getPos(), player.level().getBlockState(event.getPos()));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        ItemStack held = player.getMainHandItem();
        boolean echoTool = isEchoTool(held);
        if (morphId == null || echoTool || (isToolLike(held) && isHumanoidToolUser(morphId))) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
            && MorphAnimalInteraction.usesSpeciesPrimaryAction(morphId)
            && !MorphAnimalInteraction.canMineBlocksAsMorph(serverPlayer, morphId, echoTool)) {
            event.setNewSpeed(0.0F);
            return;
        }

        event.setNewSpeed(MorphInteractionSystem.adjustBreakSpeed(player, morphId, event.getNewSpeed()));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        ItemStack held = player.getMainHandItem();
        boolean echoTool = isEchoTool(held);
        if (morphId == null || echoTool || (isToolLike(held) && isHumanoidToolUser(morphId))) {
            return;
        }

        if (MorphAnimalInteraction.usesSpeciesPrimaryAction(morphId)
            && !MorphAnimalInteraction.canMineBlocksAsMorph(player, morphId, echoTool)) {
            event.setCanceled(true);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockState state = event.getState();
        if (state.isAir() || !state.requiresCorrectToolForDrops() || state.hasBlockEntity()) {
            return;
        }

        ItemStack fallbackDrop = new ItemStack(state.getBlock().asItem());
        if (fallbackDrop.isEmpty()) {
            return;
        }

        Block.popResource(level, event.getPos(), fallbackDrop);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (NaturalDimensionRuntime.tryActivatePortal(event.getEntity(), event.getLevel(), event.getPos(), event.getItemStack(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        Player player = event.getEntity();
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId != null && !canUseWorldInteractionsAsMorph(player, morphId)) {
            BlockState clickedState = player.level().getBlockState(event.getPos());
            if (isUtilitiesControlledInteraction(clickedState.getBlock())) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable("message.naturalis.interaction.controls_locked"), true);
                }
                return;
            }
        }

        ItemStack stack = event.getItemStack();
        if (morphId != null && (stack.isEmpty() || !(stack.getItem() instanceof BlockItem))) {
            if (trySpeciesSecondaryUse(player, event.getHand(), event.getHitVec())) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }

        if (morphId == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return;
        }

        if (canPlaceBlocksAsMorph(player, morphId)) {
            return;
        }

        if (MorphInteractionSystem.handleAlternatePlaceAction(player, morphId, stack, event.getPos(), player.level().getBlockState(event.getPos()))) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onThrownPotionImpact(ProjectileImpactEvent event) {
        net.minecraft.world.entity.Entity projectile = event.getProjectile();
        if (projectile == null) {
            return;
        }
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = tryGetProjectileItem(projectile);
        if (stack.isEmpty()) {
            return;
        }

        boolean isBrewedSplash = stack.is(NaturalisItems.BREWED_MORPH_SPLASH_POTION.get());
        boolean isBrewedLingering = stack.is(NaturalisItems.BREWED_MORPH_LINGERING_POTION.get());
        boolean isBindingSplash = stack.is(NaturalisItems.MORPH_BINDING_SPLASH_POTION.get());
        boolean isBindingLingering = stack.is(NaturalisItems.MORPH_BINDING_LINGERING_POTION.get());

        if (!isBrewedSplash && !isBrewedLingering && !isBindingSplash && !isBindingLingering) {
            return;
        }

        double radius = (isBrewedLingering || isBindingLingering) ? LINGERING_POTION_RADIUS : SPLASH_POTION_RADIUS;

        if (isBindingSplash || isBindingLingering) {
            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                projectile.getBoundingBox().inflate(radius),
                entity -> entity.isAlive() && entity.distanceToSqr(projectile) <= radius * radius)) {
                target.addEffect(new MobEffectInstance(NaturalisMobEffects.MORPH_BINDING, BINDING_POTION_DURATION, 0, true, false, true));
            }
        }

        if (isBrewedSplash || isBrewedLingering) {
            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                projectile.getBoundingBox().inflate(radius),
                entity -> entity.isAlive() && entity.distanceToSqr(projectile) <= radius * radius)) {
                MorphEffectEvents.applyBrewedMorphPotionToLiving(target, stack);
            }
        }
    }

    private static ItemStack tryGetProjectileItem(net.minecraft.world.entity.Entity projectile) {
        try {
            Object raw = projectile.getClass().getMethod("getItem").invoke(projectile);
            if (raw instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException ignored) {
            // Not an item projectile type.
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryCollectEcho(Player player, ItemStack inHand, net.minecraft.world.entity.Entity targetEntity) {
        if (player.level().isClientSide()) {
            return false;
        }

        if (!inHand.is(NaturalisItems.ECHO_COLLECTOR.get())) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(inHand)) {
            return false;
        }

        if (!(targetEntity instanceof LivingEntity target) || target.isDeadOrDying()) {
            return false;
        }

        // Must be genuinely weakened: never allow full-health capture.
        if (target.getHealth() >= target.getMaxHealth() - 0.01F) {
            return false;
        }

        if (target.getHealth() > target.getMaxHealth() * 0.30F) {
            return false;
        }

        int emptyVialSlot = findFirstEmptyVial(player);
        if (emptyVialSlot < 0) {
            return false;
        }

        ItemStack emptyVial = player.getInventory().getItem(emptyVialSlot);
        emptyVial.shrink(1);

        ItemStack filledVial = new ItemStack(NaturalisItems.FILLED_ECHO_VIAL.get());
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        MorphDataUtil.setMobId(filledVial, mobId.toString());
        MorphDataUtil.setShapeData(filledVial, MorphShapeUtil.captureFromEntity(target));

        // Prefer replacing the consumed empty-vial slot directly to avoid merge edge cases.
        boolean placed = false;
        if (emptyVial.isEmpty()) {
            player.getInventory().setItem(emptyVialSlot, filledVial);
            placed = true;
        }

        if (!placed && !player.getInventory().add(filledVial)) {
            player.drop(filledVial, false);
        }

        player.getCooldowns().addCooldown(inHand, 2);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.echo_collected", mobId.toString()), true);
        }

        return true;
    }

    private static boolean trySovereignEchoCapture(Player player, ItemStack inHand, net.minecraft.world.entity.Entity targetEntity) {
        if (player.level().isClientSide()) {
            return false;
        }

        if (!inHand.is(NaturalisItems.SOVEREIGN_AMULET.get())) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(inHand)) {
            return false;
        }

        if (!(targetEntity instanceof LivingEntity target) || target.isDeadOrDying()) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        CompoundTag shapeData = MorphShapeUtil.captureFromEntity(target);

        if (player.isShiftKeyDown()) {
            ItemStack orb = new ItemStack(NaturalisItems.MORPH_ORB.get());
            MorphDataUtil.setMobId(orb, mobId.toString());
            MorphDataUtil.setShapeData(orb, shapeData);

            if (!player.getInventory().add(orb)) {
                player.drop(orb, false);
            }

            boolean acquired = MorphAcquisition.acquire(serverPlayer, orb);
            if (acquired) {
                serverPlayer.displayClientMessage(
                    Component.translatable("message.naturalis.sovereign_orb_captured", mobId.toString()), true);
            }
        } else {
            ItemStack filledVial = new ItemStack(NaturalisItems.FILLED_ECHO_VIAL.get());
            MorphDataUtil.setMobId(filledVial, mobId.toString());
            MorphDataUtil.setShapeData(filledVial, shapeData);

            if (!player.getInventory().add(filledVial)) {
                player.drop(filledVial, false);
            }

            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.sovereign_vial_captured", mobId.toString()), true);
        }

        player.getCooldowns().addCooldown(inHand, 20);
        return true;
    }

    private static boolean tryForgetKnowledge(Player player, ItemStack inHand) {
        if (player.level().isClientSide()) {
            return false;
        }

        if (!inHand.is(NaturalisItems.KNOWLEDGE_RESET_TOTEM.get())) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(serverPlayer);
        if (morphId == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.naturalis.knowledge_forget.no_morph"), true);
            return true;
        }

        MorphKnowledgeManager.forgetMorphKnowledge(serverPlayer, morphId);
        if (!serverPlayer.isCreative()) {
            inHand.shrink(1);
        }

        serverPlayer.displayClientMessage(Component.translatable("message.naturalis.knowledge_forget.done", morphId.toString()), true);
        return true;
    }

    private static boolean tryUseKnowledgeBoostItems(Player player, ItemStack inHand) {
        if (player.level().isClientSide()) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (inHand.is(NaturalisItems.GROWTH_SEED.get())) {
            ResourceLocation currentMorph = CurrentMorphUtil.getCurrentMorphId(serverPlayer);
            if (currentMorph == null) {
                serverPlayer.displayClientMessage(Component.translatable("message.naturalis.knowledge_seed.no_morph"), true);
                return true;
            }

            MorphKnowledgeManager.grantBonusTreeKnowledgePoint(serverPlayer, currentMorph);
            if (!serverPlayer.isCreative()) {
                inHand.shrink(1);
            }
            int unspent = MorphKnowledgeManager.getUnspentKnowledgePoints(serverPlayer, currentMorph);
            serverPlayer.displayClientMessage(
                Component.translatable("message.naturalis.knowledge_seed.point_applied", currentMorph.toString(), unspent),
                true
            );
            return true;
        }

        if (inHand.is(NaturalisItems.APEX_ELIXIR.get())) {
            ResourceLocation targetMorph = MorphDataUtil.resolveMobId(inHand);
            if (targetMorph == null) {
                targetMorph = CurrentMorphUtil.getCurrentMorphId(serverPlayer);
            }
            if (targetMorph == null) {
                serverPlayer.displayClientMessage(Component.translatable("message.naturalis.knowledge_elixir.no_morph"), true);
                return true;
            }

            int durationTicks = 20 * 180;
            if (!MorphKnowledgeManager.applyTemporaryFullUnlock(serverPlayer, targetMorph, durationTicks)) {
                return true;
            }

            if (!serverPlayer.isCreative()) {
                inHand.shrink(1);
            }
            serverPlayer.displayClientMessage(Component.translatable("message.naturalis.knowledge_elixir.applied", targetMorph.toString(), durationTicks / 20), true);
            return true;
        }

        return false;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        ItemStack held = player.getMainHandItem();
        // Echo tools always work while morphed; humanoid morphs can use any tool normally.
        // Not morphed (morphId == null) defers to vanilla below via the morphId null guard.
        if (isEchoTool(held) || (isToolLike(held) && (morphId == null || isHumanoidToolUser(morphId)))) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        if (morphId == null) {
            return;
        }

        rallySameSpeciesHunters(player, morphId, target);

        float morphAttackDamage = getMorphAttackDamage(player, morphId);
        boolean shouldCancelVanilla = NaturalAttackManager.tryUseNaturalAttack(player, target, morphId, morphAttackDamage);
        if (shouldCancelVanilla) {
            event.setCanceled(true);
        }
    }

    private static void rallySameSpeciesHunters(ServerPlayer player, ResourceLocation morphId, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!InstinctManager.isHunterMorph(morphId)) {
            return;
        }

        EntityType<?> morphType = CompatAccess.getEntityType(morphId);
        if (morphType == null) {
            return;
        }

        double radius = MorphKnowledgeManager.getPackAssistRadius(MorphKnowledgeManager.getSocialRank(player, morphId));
        for (Mob ally : level.getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(radius),
            mob -> mob.isAlive() && mob.getType() == morphType && mob != target
        )) {
            ally.setTarget(target);
            ally.getNavigation().moveTo(target, 1.18D);
        }
    }

    private static boolean trySpeciesSecondaryUse(Player player, InteractionHand hand) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        return MorphSpeciesSecondaryLogic.tryUse(player, hand, player.pick(5.0D, 1.0F, false));
    }

    private static boolean trySpeciesSecondaryUse(Player player, InteractionHand hand, net.minecraft.world.phys.HitResult hit) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        return MorphSpeciesSecondaryLogic.tryUse(player, hand, hit);
    }

    private static boolean trySpeciesSecondaryUse(Player player, InteractionHand hand, net.minecraft.world.entity.Entity target) {
        return trySpeciesSecondaryUse(player, hand, new net.minecraft.world.phys.EntityHitResult(target));
    }

    private static boolean isToolLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        var item = stack.getItem();
        String className = item.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return className.contains("tiered")
            || className.contains("digger")
            || item instanceof ShearsItem
            || item instanceof BowItem
            || item instanceof CrossbowItem
            || item instanceof TridentItem;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            removeKnowledgeResistanceModifiers(player);
        } else {
            applyKnowledgeResistanceBonuses(player, morphId);
        }

        MorphGameplayTickLogic.tick(player);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        FeralCurlSleepSystem.onServerTick(event.getServer());
        NaturalDimensionRuntime.onServerTick(event.getServer());
    }

    private static int findFirstEmptyVial(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(NaturalisItems.EMPTY_ECHO_VIAL.get())) {
                return i;
            }
        }
        return -1;
    }

    private static float getMorphAttackDamage(ServerPlayer player, ResourceLocation morphId) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape == null) {
            return 1.0F;
        }

        AttributeInstance attack = shape.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return 1.0F;
        }

        return Math.max(1.0F, (float) attack.getValue());

    }

    /** Returns true when the item is one of the naturalis echo morph tools. */
    private static boolean isEchoTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        return item == NaturalisItems.ECHO_MORPH_BLADE.get()
            || item == NaturalisItems.ECHO_MORPH_PICK.get()
            || item == NaturalisItems.ECHO_MORPH_AXE.get()
            || item == NaturalisItems.ECHO_MORPH_SHOVEL.get();
    }

    /** Returns true for humanoid-type morphs that may use vanilla tools normally. */
    private static boolean isHumanoidToolUser(ResourceLocation morphId) {
        return morphId != null && HUMANOID_TOOL_USERS.contains(morphId.getPath());
    }

    private static boolean canPlaceBlocksAsMorph(Player player, ResourceLocation morphId) {
        if (!(player instanceof ServerPlayer serverPlayer) || morphId == null) {
            return false;
        }
        return MorphKnowledgeManager.canPlaceBlocksAsMorph(serverPlayer, morphId);
    }

    private static boolean canUseWorldInteractionsAsMorph(Player player, ResourceLocation morphId) {
        if (!(player instanceof ServerPlayer serverPlayer) || morphId == null) {
            return false;
        }
        return MorphKnowledgeManager.canUseWorldInteractionsAsMorph(serverPlayer, morphId);
    }

    private static boolean isUtilitiesControlledInteraction(Block block) {
        return block instanceof DoorBlock
            || block instanceof TrapDoorBlock
            || block instanceof FenceGateBlock
            || block instanceof LeverBlock
            || block instanceof ButtonBlock;
    }

    private static void applyKnowledgeResistanceBonuses(ServerPlayer player, ResourceLocation morphId) {
        int rank = MorphKnowledgeManager.getMorphResistanceRank(player, morphId);
        double armorBonus = MorphKnowledgeManager.getNaturalArmorBonusPoints(rank);
        double knockbackBonus = MorphKnowledgeManager.getKnockbackResistanceBonus(rank);

        upsertModifier(player.getAttribute(Attributes.ARMOR), KNOWLEDGE_ARMOR_MODIFIER_ID, armorBonus, AttributeModifier.Operation.ADD_VALUE);
        upsertModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOWLEDGE_KNOCKBACK_MODIFIER_ID, knockbackBonus, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeKnowledgeResistanceModifiers(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.ARMOR), KNOWLEDGE_ARMOR_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOWLEDGE_KNOCKBACK_MODIFIER_ID);
    }

    private static void upsertModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        if (amount != 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Preserve morph knowledge XP and stat caches through death.
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        if (oldData.contains("naturalis_knowledge")) {
            CompoundTag copied = CompatAccess.getCompound(oldData, "naturalis_knowledge");
            newData.put("naturalis_knowledge", copied.copy());
        }
        if (oldData.contains("naturalis_effects")) {
            CompoundTag copied = CompatAccess.getCompound(oldData, "naturalis_effects");
            newData.put("naturalis_effects", copied.copy());
        }
        if (oldData.contains("naturalis_resonance")) {
            CompoundTag copied = CompatAccess.getCompound(oldData, "naturalis_resonance");
            newData.put("naturalis_resonance", copied.copy());
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : advancement.value().criteria().keySet()) {
            if (!progress.isDone()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }
}
