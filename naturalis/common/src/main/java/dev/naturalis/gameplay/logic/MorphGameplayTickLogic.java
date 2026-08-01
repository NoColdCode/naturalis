package dev.naturalis.gameplay.logic;

import dev.naturalis.compat.CompatAccess;
import dev.naturalis.gameplay.FeralCurlSleepSystem;
import dev.naturalis.gameplay.MorphListenFocusLogic;
import dev.naturalis.gameplay.PrimalMovementState;
import dev.naturalis.instinct.InstinctManager;
import dev.naturalis.knowledge.MorphKnowledgeManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotBridge;
import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import dev.naturalis.profile.MobProfileRegistry;
import dev.naturalis.util.CurrentMorphUtil;
import dev.naturalis.worldgen.NaturalDimensionRuntime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Loader-neutral morph gameplay invoked once per server tick per {@link ServerPlayer}.
 * Covers portal travel hooks, curl sleep, flight/movement fixes while morphed, primal movement,
 * and morph-knowledge XP from vanilla movement stats.
 */
public final class MorphGameplayTickLogic {

    private static final Set<String> AQUATIC_SPECIAL = Set.of(
        "turtle", "axolotl", "drowned", "frog",
        "orca", "giant_squid", "lobster", "seal", "hammerhead", "catfish", "blobfish",
        "mimic_octopus", "flying_fish", "cosmic_cod", "cachalot_whale",
        "alligator_snapping_turtle", "crocodilian", "platypus", "stradpole", "straddler");
    private static final Set<String> FLYING_SPECIAL = Set.of(
        "bat", "bee", "parrot", "phantom", "ender_dragon", "ghast", "blaze", "allay", "vex", "happy_ghast",
        "crow", "bald_eagle", "toucan", "sunbird", "hummingbird", "flutter",
        "spectre", "void_worm", "warped_mosco", "tarantula_hawk",
        "blue_jay", "seagull",
        "bluejay", "canary", "cardinal", "finch", "robin", "sparrow", "vulture", "duck",
        "butterfly", "dragonfly", "firefly",
        "moa", "cockatrice", "zephyr", "valkyrie", "valkyrie_queen", "aerwhale", "flying_cow", "phyg",
        "whirlwind", "evil_whirlwind", "sun_spirit");
    private static final Set<String> CLIMBING_SPECIAL = Set.of(
        "spider", "cave_spider", "goat",
        "gorilla");
    private static final Set<String> RUNNER_SPECIAL = Set.of(
        "wolf", "horse", "camel", "llama", "fox", "ocelot", "cat", "zombie_horse", "skeleton_horse", "strider", "sniffer",
        "maned_wolf", "tiger", "emu", "komodo_dragon",
        "zebra", "ostrich", "deer", "lion", "boar");
    private static final Set<String> SNEAKY_SPECIAL = Set.of(
        "creeper", "enderman", "silverfish", "endermite", "warden",
        "anaconda", "rattlesnake", "void_worm", "snake", "coral_snake", "lizard");
    private static final Set<String> JUMPY_SPECIAL = Set.of(
        "rabbit", "slime", "magma_cube", "frog", "goat",
        "warped_toad", "aerbunny");
    private static final Set<String> FLYING_ONLY_SPECIAL = Set.of(
        "ghast", "phantom", "vex", "allay", "blaze", "bat",
        "spectre", "void_worm", "warped_mosco", "hummingbird", "flutter",
        "bluejay", "canary", "cardinal", "finch", "robin", "sparrow", "vulture", "duck",
        "butterfly", "dragonfly", "firefly",
        "moa", "cockatrice", "zephyr", "valkyrie", "aerwhale", "whirlwind", "evil_whirlwind");
    private static final Set<String> DIVE_SPECIAL = Set.of(
        "bat", "bee", "parrot", "phantom", "allay", "vex", "blaze", "ghast", "ender_dragon",
        "bald_eagle", "crow", "sunbird",
        "bluejay", "canary", "cardinal", "finch", "robin", "sparrow", "vulture", "duck",
        "butterfly", "dragonfly", "firefly",
        "moa", "cockatrice", "zephyr", "valkyrie", "aerwhale");
    private static final Set<String> QUADRUPED_BURST_SPECIAL = Set.of(
        "wolf", "fox", "cat", "ocelot", "horse", "zombie_horse", "skeleton_horse", "donkey", "mule", "camel", "llama", "goat", "pig", "cow", "sheep", "sniffer",
        "grizzly_bear", "gorilla", "rhinoceros", "elephant", "emu",
        "bear", "lion", "boar", "deer", "zebra", "rhino", "hippo", "phyg", "flying_cow", "sheepuff");
    private static final Set<String> LEAP_SPECIAL = Set.of(
        "wolf", "fox", "cat", "ocelot", "rabbit", "frog", "goat", "spider", "cave_spider",
        "warped_toad", "lion", "deer", "boar", "aerbunny");
    private static final Set<String> SCRAMBLE_SPECIAL = Set.of("spider", "cave_spider", "goat", "frog", "silverfish", "endermite", "lizard");
    private static final Set<String> AQUA_DASH_SPECIAL = Set.of(
        "dolphin", "guardian", "elder_guardian", "axolotl", "turtle", "frog", "drowned", "squid", "glow_squid", "cod", "salmon", "tropical_fish",
        "orca", "giant_squid", "seal", "hammerhead", "catfish", "mimic_octopus", "cachalot_whale",
        "alligator", "hippo", "bass");
    private static final Set<String> STATIC_SPECIAL = Set.of("shulker", "aechor_plant");

    private static final String PREV_ON_GROUND = "prev_on_ground";
    private static final String BURST_READY_TICK = "burst_ready_tick";
    private static final String LEAP_READY_TICK = "leap_ready_tick";
    private static final String SCRAMBLE_READY_TICK = "scramble_ready_tick";
    private static final String AQUA_READY_TICK = "aqua_ready_tick";
    private static final String STATIC_ANCHOR_MORPH = "static_anchor_morph";
    private static final String STATIC_ANCHOR_X = "static_anchor_x";
    private static final String STATIC_ANCHOR_Y = "static_anchor_y";
    private static final String STATIC_ANCHOR_Z = "static_anchor_z";

    private MorphGameplayTickLogic() {
    }

    public static void tick(ServerPlayer player) {
        NaturalDimensionRuntime.handlePortalTravel(player);
        FeralCurlSleepSystem.onPlayerTick(player);
        MorphListenFocusLogic.tick(player);

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            if (!player.isCreative() && !player.isSpectator()) {
                boolean needsAbilityUpdate = false;
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    needsAbilityUpdate = true;
                }
                if (Math.abs(CompatAccess.getAbilitiesFlyingSpeed(player.getAbilities()) - 0.05f) > 0.001f) {
                    CompatAccess.setAbilitiesFlyingSpeed(player.getAbilities(), 0.05f);
                    needsAbilityUpdate = true;
                }
                if (needsAbilityUpdate) player.onUpdateAbilities();
            }
            return;
        }

        int gained = 0;

        MorphQuickSlotBridge.recordUsageTick(player, morphId);

        if (player.tickCount % 600 == 0) {
            gained += 1;
        }

        EntityType<?> morphType = CompatAccess.getEntityType(morphId);
        MobCategory category = morphType != null ? morphType.getCategory() : MobCategory.MISC;
        String mobPath = morphId.getPath();
        boolean flightOnly = isFlyingOnly(morphId, mobPath);
        boolean canFly = flightOnly || canFly(morphId, mobPath);

        if (morphType != null) {
            if (flightOnly
                && player.onGround()
                && !player.getAbilities().flying
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004D
                && player.tickCount % 40 == 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.naturalis.flight_only_grounded"), true);
            }

            if (flightOnly) {
                if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
            } else if (canFly) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            } else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }

            if (canFly) {
                float targetFlySpeed = getFlySpeedForMorph(mobPath);
                if (Math.abs(CompatAccess.getAbilitiesFlyingSpeed(player.getAbilities()) - targetFlySpeed) > 0.001f) {
                    CompatAccess.setAbilitiesFlyingSpeed(player.getAbilities(), targetFlySpeed);
                    player.onUpdateAbilities();
                }
            }
            applyMorphMovementAbilities(player, morphId, mobPath);
        }

        gained += xpFromDelta(player, "walk", Stats.WALK_ONE_CM, 3500);
        gained += xpFromDelta(player, "jump", Stats.JUMP, 12);
        gained += xpFromDelta(player, "fall", Stats.FALL_ONE_CM, 1800);

        if (category == MobCategory.MONSTER || category == MobCategory.CREATURE) {
            gained += xpFromDelta(player, "sprint", Stats.SPRINT_ONE_CM, 5000);
        }

        if (category == MobCategory.MONSTER) {
            gained += xpFromDelta(player, "crouch", Stats.CROUCH_ONE_CM, 2600);
        }

        if (isAquatic(category, mobPath)) {
            gained += xpFromDelta(player, "swim", Stats.SWIM_ONE_CM, 2200);
            gained += xpFromDelta(player, "water_walk", Stats.WALK_ON_WATER_ONE_CM, 2400);
        }

        if (isFlying(category, morphId, mobPath)) {
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

        if (mobPath.contains("horse")) {
            gained += xpFromDelta(player, "horse_gait", Stats.HORSE_ONE_CM, 2600);
        }

        if (mobPath.contains("strider")) {
            gained += xpFromDelta(player, "strider_stride", Stats.STRIDER_ONE_CM, 2400);
        }

        if (!"minecraft".equals(morphId.getNamespace()) && player.tickCount % 1200 == 0) {
            gained += 1;
        }

        if (gained > 0) {
            MorphKnowledgeManager.addXp(player, morphId, gained);
        }
    }

    private static void applyMorphMovementAbilities(ServerPlayer player, ResourceLocation morphId, String mobPath) {
        CompoundTag movement = PrimalMovementState.movementTag(player);
        long now = player.level().getGameTime();

        if (applyStaticMorphLock(player, movement, morphId, mobPath)) {
            return;
        }
        clearStaticAnchor(movement);

        boolean primalDown = CompatAccess.getBoolean(movement, PrimalMovementState.PRIMAL_KEY_DOWN);
        boolean previousOnGround = CompatAccess.getBoolean(movement, PREV_ON_GROUND);
        boolean jumpedThisTick = previousOnGround && !player.onGround() && player.getDeltaMovement().y > 0.20D;
        movement.putBoolean(PREV_ON_GROUND, player.onGround());

        applyDiveMomentum(player, mobPath, primalDown);
        applyFloatingBuoyancy(player, morphId);
        applyQuadrupedBurst(player, movement, mobPath, primalDown, now);
        applyLeapBound(player, movement, mobPath, primalDown, jumpedThisTick, now);
        applyWallScramble(player, movement, mobPath, primalDown, now);
        applyAquaticJet(player, movement, mobPath, primalDown, now);
    }

    /**
     * Surface floaters bob on water and cannot stay submerged (pairs with Walkers stand_on_fluid).
     */
    private static void applyFloatingBuoyancy(ServerPlayer player, ResourceLocation morphId) {
        if (!InstinctManager.isFloatingMorph(morphId) || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (!CompatAccess.isInWaterOrBubble(player)) {
            return;
        }

        player.setSwimming(false);
        Vec3 velocity = player.getDeltaMovement();
        if (player.isUnderWater()) {
            player.setDeltaMovement(velocity.x, Math.max(0.22D, velocity.y + 0.08D), velocity.z);
            player.fallDistance = 0.0F;
            return;
        }
        if (velocity.y < -0.02D) {
            player.setDeltaMovement(velocity.x, Math.max(velocity.y * 0.35D, -0.02D), velocity.z);
        }
        player.fallDistance = 0.0F;
    }

    private static boolean applyStaticMorphLock(ServerPlayer player, CompoundTag movement, ResourceLocation morphId, String mobPath) {
        if (!isStaticMorph(morphId, mobPath) || player.isCreative() || player.isSpectator()) {
            return false;
        }

        if (!movement.contains(STATIC_ANCHOR_MORPH) || !mobPath.equals(movement.getString(STATIC_ANCHOR_MORPH))) {
            movement.putString(STATIC_ANCHOR_MORPH, mobPath);
            movement.putDouble(STATIC_ANCHOR_X, player.getX());
            movement.putDouble(STATIC_ANCHOR_Y, player.getY());
            movement.putDouble(STATIC_ANCHOR_Z, player.getZ());
        }

        double anchorX = CompatAccess.getDouble(movement, STATIC_ANCHOR_X);
        double anchorY = CompatAccess.getDouble(movement, STATIC_ANCHOR_Y);
        double anchorZ = CompatAccess.getDouble(movement, STATIC_ANCHOR_Z);

        if (player.distanceToSqr(anchorX, anchorY, anchorZ) > 1.0E-4D) {
            player.teleportTo(anchorX, anchorY, anchorZ);
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.fallDistance = 0.0F;
        return true;
    }

    private static void clearStaticAnchor(CompoundTag movement) {
        movement.remove(STATIC_ANCHOR_MORPH);
        movement.remove(STATIC_ANCHOR_X);
        movement.remove(STATIC_ANCHOR_Y);
        movement.remove(STATIC_ANCHOR_Z);
    }

    private static void applyDiveMomentum(ServerPlayer player, String mobPath, boolean primalDown) {
        if (!primalDown || !DIVE_SPECIAL.contains(mobPath) || player.onGround()) {
            return;
        }

        Vec3 look = player.getLookAngle();
        if (look.y > -0.22D) {
            return;
        }

        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 direction = horizontalLook.normalize();
        double diveFactor = Math.min(1.0D, Math.abs(look.y));
        double accel = 0.04D + 0.09D * diveFactor;
        Vec3 velocity = player.getDeltaMovement();
        double horizontalLimit = 1.95D;
        if (velocity.horizontalDistance() >= horizontalLimit) {
            return;
        }

        player.setDeltaMovement(
            velocity.x + direction.x * accel,
            velocity.y - 0.015D,
            velocity.z + direction.z * accel
        );
    }

    private static void applyQuadrupedBurst(ServerPlayer player, CompoundTag movement, String mobPath, boolean primalDown, long now) {
        if (!primalDown || !player.onGround() || !player.isSprinting() || !QUADRUPED_BURST_SPECIAL.contains(mobPath)) {
            return;
        }
        if (player.getDeltaMovement().horizontalDistanceSqr() < 0.01D) {
            return;
        }
        if (now < CompatAccess.getLong(movement, BURST_READY_TICK)) {
            return;
        }

        Vec3 direction = horizontalFacing(player);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
            velocity.x * 1.03D + direction.x * 0.20D,
            velocity.y,
            velocity.z * 1.03D + direction.z * 0.20D
        );
        movement.putLong(BURST_READY_TICK, now + 8L);
    }

    private static void applyLeapBound(ServerPlayer player, CompoundTag movement, String mobPath, boolean primalDown, boolean jumpedThisTick, long now) {
        if (!primalDown || !jumpedThisTick || !LEAP_SPECIAL.contains(mobPath)) {
            return;
        }
        if (now < CompatAccess.getLong(movement, LEAP_READY_TICK)) {
            return;
        }

        Vec3 direction = horizontalFacing(player);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
            velocity.x + direction.x * 0.50D,
            Math.max(velocity.y, 0.40D) + 0.12D,
            velocity.z + direction.z * 0.50D
        );
        player.fallDistance = 0.0F;
        movement.putLong(LEAP_READY_TICK, now + 20L);
    }

    private static void applyWallScramble(ServerPlayer player, CompoundTag movement, String mobPath, boolean primalDown, long now) {
        if (!primalDown || player.onGround() || !player.horizontalCollision || !SCRAMBLE_SPECIAL.contains(mobPath)) {
            return;
        }
        if (now < CompatAccess.getLong(movement, SCRAMBLE_READY_TICK)) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
            velocity.x * 0.92D,
            Math.max(velocity.y, -0.08D) + 0.24D,
            velocity.z * 0.92D
        );
        player.fallDistance = 0.0F;
        movement.putLong(SCRAMBLE_READY_TICK, now + 6L);
    }

    private static void applyAquaticJet(ServerPlayer player, CompoundTag movement, String mobPath, boolean primalDown, long now) {
        if (!primalDown || !AQUA_DASH_SPECIAL.contains(mobPath) || !CompatAccess.isInWaterOrBubble(player) || !player.isSprinting()) {
            return;
        }
        if (now < CompatAccess.getLong(movement, AQUA_READY_TICK)) {
            return;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
            velocity.x + look.x * 0.24D,
            velocity.y + look.y * 0.08D,
            velocity.z + look.z * 0.24D
        );
        movement.putLong(AQUA_READY_TICK, now + 8L);
    }

    private static Vec3 horizontalFacing(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return Vec3.ZERO;
        }
        return horizontal.normalize();
    }

    private static boolean isAquatic(MobCategory category, String path) {
        return category.getName().contains("water") || AQUATIC_SPECIAL.contains(path);
    }

    private static boolean isFlying(MobCategory category, ResourceLocation morphId, String path) {
        return category == MobCategory.AMBIENT || canFly(morphId, path);
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

    private static boolean isFlyingOnly(ResourceLocation morphId, String path) {
        return InstinctManager.isFlightOnly(morphId) || FLYING_ONLY_SPECIAL.contains(path);
    }

    private static boolean canFly(ResourceLocation morphId, String path) {
        if (isFlyingOnly(morphId, path) || FLYING_SPECIAL.contains(path)) {
            return true;
        }
        // Aerial quick-slot profiles (Aether / birds / insects) should keep Walkers flight.
        var categories = MobProfileRegistry.getQuickSlotCategories(morphId);
        if (categories.isPresent() && categories.get().contains(MorphQuickSlotCategory.AERIAL)) {
            return true;
        }
        return MobProfileRegistry.getFlightOnly(morphId).orElse(false);
    }

    private static boolean isStaticMorph(ResourceLocation morphId, String path) {
        if (STATIC_SPECIAL.contains(path)) {
            return true;
        }
        return InstinctManager.isStaticMorph(morphId);
    }

    private static float getFlySpeedForMorph(String path) {
        if ("phantom".equals(path) || "blaze".equals(path) || morphPathContains(path, "sunscorcher")) {
            return 0.14f;
        }
        if (morphPathContains(path, "bee", "vex", "hummingbird", "flutter", "firefly", "dragonfly", "mosquito")) {
            return 0.125f;
        }
        if (morphPathContains(path, "eagle", "hawk", "falcon", "swift", "tern", "martin", "osprey")) {
            return 0.11f;
        }
        if (morphPathContains(path, "crow", "raven", "parrot", "sunbird", "jay", "gull", "toucan", "seagull",
            "booby", "shoebill", "pelican", "roadrunner")) {
            return 0.098f;
        }
        if ("allay".equals(path)) {
            return 0.078f;
        }
        if ("bat".equals(path)) {
            return 0.088f;
        }
        if (morphPathContains(path, "ender_dragon", "void_worm", "wyvern", "drake", "ghast", "happy_ghast")) {
            return 0.052f;
        }
        return 0.085f;
    }

    private static boolean morphPathContains(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) return true;
        }
        return false;
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

        int previous = CompatAccess.getInt(cache, cacheKey);
        cache.putInt(cacheKey, current);

        int delta = Math.max(0, current - previous);
        if (delta == 0) {
            return 0;
        }

        String carryKey = cacheKey + "_carry";
        int carry = CompatAccess.getInt(cache, carryKey);
        int total = carry + delta;
        int gained = total / perXp;
        cache.putInt(carryKey, total % perXp);
        return gained;
    }
}
