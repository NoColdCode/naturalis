package dev.naturalis.gameplay;

import dev.naturalis.Naturalis;
import dev.naturalis.combat.NaturalAttackManager;
import dev.naturalis.command.MorphCommand;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.Set;
import tocraft.walkers.api.PlayerShape;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class NaturalisGameplayEvents {

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(Naturalis.MOD_ID, "root");

    private static final Set<String> AQUATIC_SPECIAL = Set.of("turtle", "axolotl", "drowned", "frog");
    private static final Set<String> FLYING_SPECIAL = Set.of("bat", "bee", "parrot", "phantom", "ender_dragon", "ghast", "blaze", "allay", "vex", "happy_ghast");
    private static final Set<String> CLIMBING_SPECIAL = Set.of("spider", "cave_spider", "goat");
    private static final Set<String> RUNNER_SPECIAL = Set.of("wolf", "horse", "camel", "llama", "fox", "ocelot", "cat", "zombie_horse", "skeleton_horse", "strider", "sniffer");
    private static final Set<String> SNEAKY_SPECIAL = Set.of("creeper", "enderman", "silverfish", "endermite", "warden");
    private static final Set<String> JUMPY_SPECIAL = Set.of("rabbit", "slime", "magma_cube", "frog", "goat");
    private static final Set<String> FLYING_ONLY_SPECIAL = Set.of("ghast", "phantom", "vex", "allay", "blaze", "bat");

    private NaturalisGameplayEvents() {
    }

    public static void register(IEventBus modEventBus) {
        // Uses global event bus listeners via @EventBusSubscriber.
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        MorphCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            grantAdvancement(player, ADV_ROOT);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (tryCollectEcho(player, event.getItemStack(), event.getTarget())) {
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
        if (tryCollectEcho(player, event.getItemStack(), event.getTarget())) {
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

        if (!player.getInventory().add(filledVial)) {
            player.drop(filledVial, false);
        }

        player.getCooldowns().addCooldown(inHand, 2);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.echo_collected", mobId.toString()), true);
        }

        return true;
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

    private static boolean isToolLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        var item = stack.getItem();
        return item instanceof TieredItem
            || item instanceof DiggerItem
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
            // Remove forced flight when player is no longer morphed.
            if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            return;
        }

        int gained = 0;

        // Universal learning: 1 XP every 30 seconds while morphed.
        if (player.tickCount % 600 == 0) {
            gained += 1;
        }

        EntityType<?> morphType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
        MobCategory category = morphType.getCategory();
        String mobPath = morphId.getPath();

        if (isFlyingOnly(mobPath)
            && player.onGround()
            && !player.getAbilities().flying
            && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004D
            && player.tickCount % 40 == 0) {
            player.displayClientMessage(Component.translatable("message.naturalis.flight_only_grounded"), true);
        }
        // Flight-only morphs must always remain airborne; prevent MC from disabling fly on landing.
        if (isFlyingOnly(mobPath)) {
            if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
        } else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
            // Morph changed from flight-only to a grounded form — revoke forced flight.
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        // Generic behavior tracks shared by every morph including modded mobs.
        gained += xpFromDelta(player, "walk", Stats.WALK_ONE_CM, 3500);
        gained += xpFromDelta(player, "jump", Stats.JUMP, 12);
        gained += xpFromDelta(player, "fall", Stats.FALL_ONE_CM, 1800);

        if (category == MobCategory.MONSTER || category == MobCategory.CREATURE) {
            gained += xpFromDelta(player, "sprint", Stats.SPRINT_ONE_CM, 5000);
        }

        if (category == MobCategory.MONSTER) {
            gained += xpFromDelta(player, "crouch", Stats.CROUCH_ONE_CM, 2600);
        }

        // Aquatic archetypes: water categories + known mixed-water vanilla mobs.
        if (isAquatic(category, mobPath)) {
            gained += xpFromDelta(player, "swim", Stats.SWIM_ONE_CM, 2200);
            gained += xpFromDelta(player, "water_walk", Stats.WALK_ON_WATER_ONE_CM, 2400);
        }

        // Flying archetypes: ambient flyers + explicit list.
        if (isFlying(category, mobPath)) {
            gained += xpFromDelta(player, "fly", Stats.AVIATE_ONE_CM, 2800);
            gained += xpFromDelta(player, "soft_fall", Stats.FALL_ONE_CM, 4200);
        }

        if (isClimbing(mobPath)) {
            gained += xpFromDelta(player, "climb", Stats.CLIMB_ONE_CM, 1800);
        }

        if (isRunner(mobPath)) {
            gained += xpFromDelta(player, "runner_sprint", Stats.SPRINT_ONE_CM, 3800);
            gained += xpFromDelta(player, "runner_walk", Stats.WALK_ONE_CM, 3000);
        }

        if (isSneaky(mobPath)) {
            gained += xpFromDelta(player, "sneak_focus", Stats.CROUCH_ONE_CM, 1800);
        }

        if (isJumpy(mobPath)) {
            gained += xpFromDelta(player, "jump_mastery", Stats.JUMP, 6);
        }

        // Extra specialization for ride-like forms.
        if (mobPath.contains("horse")) {
            gained += xpFromDelta(player, "horse_gait", Stats.HORSE_ONE_CM, 2600);
        }

        if (mobPath.contains("strider")) {
            gained += xpFromDelta(player, "strider_stride", Stats.STRIDER_ONE_CM, 2400);
        }

        // Modded morph fallback: very slight adaptation bonus for unknown mobs.
        if (!"minecraft".equals(morphId.getNamespace()) && player.tickCount % 1200 == 0) {
            gained += 1;
        }

        if (gained > 0) {
            MorphKnowledgeManager.addXp(player, morphId, gained);
        }
    }

    private static boolean isAquatic(MobCategory category, String path) {
        return category.getName().contains("water") || AQUATIC_SPECIAL.contains(path);
    }

    private static boolean isFlying(MobCategory category, String path) {
        return category == MobCategory.AMBIENT || FLYING_SPECIAL.contains(path);
    }

    private static boolean isClimbing(String path) {
        return CLIMBING_SPECIAL.contains(path);
    }

    private static boolean isRunner(String path) {
        return RUNNER_SPECIAL.contains(path);
    }

    private static boolean isSneaky(String path) {
        return SNEAKY_SPECIAL.contains(path);
    }

    private static boolean isJumpy(String path) {
        return JUMPY_SPECIAL.contains(path);
    }

    private static boolean isFlyingOnly(String path) {
        return FLYING_ONLY_SPECIAL.contains(path);
    }

    private static int xpFromDelta(ServerPlayer player, String cacheKey, ResourceLocation statId, int perXp) {
        if (perXp <= 0) {
            return 0;
        }

        Stat<ResourceLocation> stat = Stats.CUSTOM.get(statId);
        int current = player.getStats().getValue(stat);

        CompoundTag cache = MorphKnowledgeManager.getStatCache(player);
        if (!cache.contains(cacheKey)) {
            cache.putInt(cacheKey, current);
            return 0;
        }

        int previous = cache.getInt(cacheKey);
        cache.putInt(cacheKey, current);

        int delta = Math.max(0, current - previous);
        if (delta == 0) {
            return 0;
        }

        // Carry partial progress forward so small deltas are not lost and do not over-reward.
        String carryKey = cacheKey + "_carry";
        int carry = cache.getInt(carryKey);
        int total = carry + delta;
        int gained = total / perXp;
        cache.putInt(carryKey, total % perXp);
        return gained;
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

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Preserve morph knowledge XP and stat caches through death.
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        if (oldData.contains("naturalis_knowledge")) {
            newData.put("naturalis_knowledge", oldData.getCompound("naturalis_knowledge").copy());
        }
        if (oldData.contains("naturalis_effects")) {
            newData.put("naturalis_effects", oldData.getCompound("naturalis_effects").copy());
        }
        if (oldData.contains("naturalis_resonance")) {
            newData.put("naturalis_resonance", oldData.getCompound("naturalis_resonance").copy());
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
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

        ItemStack fallbackDrop = new ItemStack(state.getBlock().asItem());
        if (fallbackDrop.isEmpty()) {
            return;
        }

        Block.popResource(level, event.getPos(), fallbackDrop);
    }
}
