package dev.naturalis.gameplay;

import dev.naturalis.chat.FeralChatSystem;
import dev.naturalis.gameplay.logic.MorphGameplayTickLogic;
import dev.naturalis.combat.NaturalAttackManager;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.command.MorphCommand;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.content.NaturalisMobEffects;
import dev.naturalis.effect.MorphEffectEvents;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
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
import dev.naturalis.NaturalisMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tocraft.walkers.api.PlayerShape;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class NaturalisGameplayEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaturalisGameplayEvents.class);

    private static final double SPLASH_POTION_RADIUS = 4.0D;
    private static final double LINGERING_POTION_RADIUS = 3.0D;
    private static final int BINDING_POTION_DURATION = 8 * 20 * 60;

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "root");

    private static final String SELF_INTERACTION_ROOT = "naturalis_self_interactions";
    private static final String SHEEP_WOOL_READY_TICK = "sheep_wool_ready_tick";
    private static final String MOOSHROOM_SHEAR_READY_TICK = "mooshroom_shear_ready_tick";
    private static final String SNOW_GOLEM_PUMPKIN_READY_TICK = "snow_golem_pumpkin_ready_tick";
    private static final String BOGGED_MUSHROOM_READY_TICK = "bogged_mushroom_ready_tick";
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

    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        LOGGER.info("[naturalis] ServerStartedEvent fired — server is up");
        // forceLoadSpawnChunks removed: adding 2028 FORCED tickets generates all chunks
        // synchronously on the server thread and causes a multi-second startup freeze.
        // The "Loading terrain…" on first portal use is the normal vanilla client overlay
        // and does not require pre-generation.
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
        if (tryUseKnowledgeBoostItems(player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (tryMorphSelfInteraction(player, event.getItemStack())) {
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

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || isToolLike(player.getMainHandItem())) {
            return;
        }

        MorphInteractionSystem.handleBreakFeedback(player, morphId, event.getPos(), player.level().getBlockState(event.getPos()));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null || isToolLike(player.getMainHandItem())) {
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
        if (morphId == null || isToolLike(player.getMainHandItem())) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockState state = event.getState();
        if (state.isAir() || !state.requiresCorrectToolForDrops() || state.hasBlockEntity()) {
            return;
        }

        // Vanilla already applies proper loot when the held item is correct for drops.
        // Without this, morph + pickaxe would spawn the block item in addition to normal drops.
        if (player.hasCorrectToolForDrops(state)) {
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

        if (MorphInteractionSystem.handleAlternatePlaceAction(player, morphId, stack, event.getPos(), player.level().getBlockState(event.getPos()))) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean tryCollectEcho(Player player, ItemStack inHand, net.minecraft.world.entity.Entity targetEntity) {
        if (player.level().isClientSide()) {
            return false;
        }

        if (!inHand.is(NaturalisItems.ECHO_COLLECTOR.get())) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(inHand.getItem())) {
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

        player.getCooldowns().addCooldown(inHand.getItem(), 2);

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

        if (player.getCooldowns().isOnCooldown(inHand.getItem())) {
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

        player.getCooldowns().addCooldown(inHand.getItem(), 20);
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

    private static boolean tryMorphSelfInteraction(Player player, ItemStack held) {
        if (player.level().isClientSide() || held.isEmpty()) {
            return false;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return false;
        }

        String path = morphId.getPath();
        long now = player.level().getGameTime();

        if (("cow".equals(path) || "mooshroom".equals(path) || "goat".equals(path)) && held.is(Items.BUCKET)) {
            return consumeHeldForContainer(player, held, new ItemStack(Items.MILK_BUCKET), 4);
        }

        if ("mooshroom".equals(path) && held.is(Items.BOWL)) {
            return consumeHeldForContainer(player, held, new ItemStack(Items.MUSHROOM_STEW), 4);
        }

        if ("sheep".equals(path) && held.getItem() instanceof ShearsItem) {
            if (!isReady(player, SHEEP_WOOL_READY_TICK, now)) {
                player.displayClientMessage(Component.translatable("message.naturalis.interaction.not_ready"), true);
                return true;
            }

            setReadyAfter(player, SHEEP_WOOL_READY_TICK, now, 5 * 60 * 20);
            int count = 1 + player.getRandom().nextInt(3);
            Item woolItem = getWoolForCurrentSheepColor(player);
            for (int i = 0; i < count; i++) {
                giveOrDrop(player, new ItemStack(woolItem));
            }
            damageHeldItem(player, held, 1);
            player.level().playSound(null, player, net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }

        if ("mooshroom".equals(path) && held.getItem() instanceof ShearsItem) {
            if (!isReady(player, MOOSHROOM_SHEAR_READY_TICK, now)) {
                player.displayClientMessage(Component.translatable("message.naturalis.interaction.not_ready"), true);
                return true;
            }

            setReadyAfter(player, MOOSHROOM_SHEAR_READY_TICK, now, 5 * 60 * 20);
            Item mushroom = getMooshroomDrop(player);
            for (int i = 0; i < 5; i++) {
                giveOrDrop(player, new ItemStack(mushroom));
            }
            damageHeldItem(player, held, 1);
            player.level().playSound(null, player, net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }

        if ("snow_golem".equals(path) && held.getItem() instanceof ShearsItem) {
            if (!isReady(player, SNOW_GOLEM_PUMPKIN_READY_TICK, now)) {
                player.displayClientMessage(Component.translatable("message.naturalis.interaction.not_ready"), true);
                return true;
            }

            setReadyAfter(player, SNOW_GOLEM_PUMPKIN_READY_TICK, now, 5 * 60 * 20);
            giveOrDrop(player, new ItemStack(Items.CARVED_PUMPKIN));
            damageHeldItem(player, held, 1);
            player.level().playSound(null, player, net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }

        if ("bogged".equals(path) && held.getItem() instanceof ShearsItem) {
            if (!isReady(player, BOGGED_MUSHROOM_READY_TICK, now)) {
                player.displayClientMessage(Component.translatable("message.naturalis.interaction.not_ready"), true);
                return true;
            }

            setReadyAfter(player, BOGGED_MUSHROOM_READY_TICK, now, 5 * 60 * 20);
            giveOrDrop(player, new ItemStack(Items.BROWN_MUSHROOM));
            giveOrDrop(player, new ItemStack(Items.RED_MUSHROOM));
            damageHeldItem(player, held, 1);
            player.level().playSound(null, player, net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }

        return false;
    }

    private static boolean consumeHeldForContainer(Player player, ItemStack held, ItemStack output, int cooldownTicks) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        giveOrDrop(player, output);
        if (cooldownTicks > 0) {
            player.getCooldowns().addCooldown(output.getItem(), cooldownTicks);
        }
        return true;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void damageHeldItem(Player player, ItemStack held, int amount) {
        if (player.getAbilities().instabuild || amount <= 0) {
            return;
        }
        held.hurtAndBreak(amount, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    private static net.minecraft.nbt.CompoundTag getSelfInteractionTag(Player player) {
        net.minecraft.nbt.CompoundTag root = CompatAccess.getPersistentData(player);
        if (!root.contains(SELF_INTERACTION_ROOT)) {
            root.put(SELF_INTERACTION_ROOT, new net.minecraft.nbt.CompoundTag());
        }
        return CompatAccess.getCompound(root, SELF_INTERACTION_ROOT);
    }

    private static boolean isReady(Player player, String key, long now) {
        return now >= CompatAccess.getLong(getSelfInteractionTag(player), key);
    }

    private static void setReadyAfter(Player player, String key, long now, int ticks) {
        getSelfInteractionTag(player).putLong(key, now + Math.max(1, ticks));
    }

    private static Item getWoolForCurrentSheepColor(Player player) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        DyeColor color = DyeColor.WHITE;
        if (shape instanceof Sheep sheep) {
            color = sheep.getColor();
        }

        return switch (color) {
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
            default -> Items.WHITE_WOOL;
        };
    }

    private static Item getMooshroomDrop(Player player) {
        LivingEntity shape = PlayerShape.getCurrentShape(player);
        if (shape instanceof MushroomCow mooshroom && mooshroom.getVariant() == MushroomCow.MushroomType.BROWN) {
            return Items.BROWN_MUSHROOM;
        }
        return Items.RED_MUSHROOM;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Treat non-tool held items (e.g. dirt, blocks, food) as bare-hand attacks.
        // Real tools/weapons keep vanilla behavior.
        if (isToolLike(player.getMainHandItem())) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            return;
        }

        float morphAttackDamage = getMorphAttackDamage(player, morphId);
        boolean shouldCancelVanilla = NaturalAttackManager.tryUseNaturalAttack(player, target, morphId, morphAttackDamage);
        if (shouldCancelVanilla) {
            event.setCanceled(true);
        }
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
        double branchMultiplier = MorphKnowledgeManager.getNaturalAttackDamageMultiplier(player, morphId);
        if (shape == null) {
            return (float) Math.max(1.0D, branchMultiplier);
        }

        AttributeInstance attack = shape.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return (float) Math.max(1.0D, branchMultiplier);
        }

        return Math.max(1.0F, (float) (attack.getValue() * branchMultiplier));

    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Preserve morph knowledge XP and stat caches through death AND dimension change.
        // In NeoForge 1.21.1, cross-dimension teleport also fires Clone with isWasDeath()=false.
        CompoundTag oldData = CompatAccess.getPersistentData(event.getOriginal());
        CompoundTag newData = CompatAccess.getPersistentData(event.getEntity());
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
