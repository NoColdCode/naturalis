package dev.naturalis.diet;

import dev.naturalis.NaturalisMod;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.survivalas.SurvivalAsWorldStorage;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Locale;

@EventBusSubscriber(modid = NaturalisMod.ID)
public final class DietEvents {

    private static final ResourceLocation ADV_ROOT = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "root");
    private static final ResourceLocation ADV_CARNIVORE_PRIME = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "diet/carnivore_prime");
    private static final ResourceLocation ADV_HERBIVORE_PRIME = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "diet/herbivore_prime");
    private static final ResourceLocation ADV_IRON_STOMACH = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "diet/iron_stomach");
    private static final ResourceLocation ADV_HUMAN_DIET_PENALTY = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "resonance/human_diet_penalty");
    private static final ResourceLocation ADV_HUMAN_DIET_LOCKED = ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, "resonance/human_diet_locked");

    private DietEvents() {
    }

    @SubscribeEvent
    public static void onFoodFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!NaturalisConfig.dietEnabled()) {
            return;
        }

        ItemStack consumed = event.getItem();
        FoodProperties food = consumed.getItem().components().get(DataComponents.FOOD);
        if (food == null) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            handleHumanBoundDietPenalty(player, consumed, food);
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(consumed.getItem());
        DietManager.DietType diet = DietManager.getDietType(morphId);
        DietManager.FoodType foodType = DietManager.getFoodType(itemId);

        applySpeciesFoodDetails(player, morphId, itemId);

        if (SurvivalAsWorldStorage.isEnabled()) {
            if (!isStrictFoodCompatible(diet, foodType, itemId)) {
                applyHardDietViolation(player, food);
                return;
            }
            // Preferred foods still get species bonuses below.
        }

        switch (diet) {
            case CARNIVORE, HEMATOPHAGE -> handleCarnivore(player, consumed, itemId, foodType);
            case HERBIVORE, FLORIVORE -> handleHerbivore(player, consumed, itemId, foodType);
            case OMNIVORE, SCAVENGER -> handleOmnivore(player, consumed, foodType);
            case PISCIVORE -> handlePiscivore(player, consumed, itemId, foodType);
            case INSECTIVORE -> handleInsectivore(player, consumed, itemId, foodType);
            case NECROVORE -> handleNecrovore(player, consumed, itemId, foodType);
            case FRUGIVORE, NECTARIVORE -> handleFrugivore(player, consumed, itemId, foodType);
            case FUNGIVORE -> handleFungivore(player, consumed, itemId, foodType);
            case LITHOVORE -> handleLithovore(player, consumed, itemId, foodType);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation morphId = CurrentMorphUtil.getCurrentMorphId(player);
        if (morphId == null) {
            blockInvalidHumanFoodWhileFractured(player, stack, event);
            return;
        }

        if (blockHardSurvivalAsDiet(player, morphId, stack, event)) {
            return;
        }

        String morphPath = morphId.getPath();
        if (("wolf".equals(morphPath) || "fox".equals(morphPath)) && stack.is(Items.BONE)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }

            player.getFoodData().eat(2, 0.35F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true, true));
            SoundEvent eatSound = CompatAccess.resolveSoundEvent("GENERIC_EAT", "PLAYER_BURP");
            if (eatSound != null) {
                player.level().playSound(null, player, eatSound, SoundSource.PLAYERS, 0.8F, 0.92F + player.getRandom().nextFloat() * 0.16F);
            }
            player.displayClientMessage(Component.translatable("message.naturalis.diet.wolf_bone"), true);

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void blockInvalidHumanFoodWhileFractured(ServerPlayer player, ItemStack stack, PlayerInteractEvent.RightClickItem event) {
        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        if (bonded == null) {
            return;
        }

        if (ResonanceManager.getHumanity(player) > 40) {
            return;
        }

        if (stack.get(DataComponents.FOOD) == null) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        DietManager.DietType diet = DietManager.getDietType(bonded);
        DietManager.FoodType foodType = DietManager.getFoodType(itemId);

        if (isFoodCompatible(diet, foodType, itemId)) {
            return;
        }

        grantAdvancement(player, ADV_HUMAN_DIET_LOCKED);

        player.displayClientMessage(Component.translatable("message.naturalis.resonance.human_diet_locked"), true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    /** Survival-as: refuse food that is not true species fare (no bread-for-wolves free pass). */
    private static boolean blockHardSurvivalAsDiet(
        ServerPlayer player,
        ResourceLocation morphId,
        ItemStack stack,
        PlayerInteractEvent.RightClickItem event
    ) {
        if (!SurvivalAsWorldStorage.isEnabled()) {
            return false;
        }
        if (stack.get(DataComponents.FOOD) == null) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        DietManager.DietType diet = DietManager.getDietType(morphId);
        DietManager.FoodType foodType = DietManager.getFoodType(itemId);
        if (isStrictFoodCompatible(diet, foodType, itemId)) {
            return false;
        }

        player.displayClientMessage(Component.translatable("message.naturalis.survival_as.hard_diet"), true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        return true;
    }

    private static void applyHardDietViolation(ServerPlayer player, FoodProperties food) {
        int currentFood = player.getFoodData().getFoodLevel();
        int revoke = Math.max(1, food.nutrition());
        player.getFoodData().setFoodLevel(Math.max(0, currentFood - revoke));
        player.causeFoodExhaustion(Math.max(2.0F, food.saturation() * 2.0F));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 260, 1, false, true, true));
        player.addEffect(new MobEffectInstance(
            CompatAccess.resolveMobEffect("NAUSEA", "CONFUSION"),
            200,
            0,
            false,
            true,
            true
        ));
        if (player.getRandom().nextFloat() < 0.45F) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true, true));
        }
        player.displayClientMessage(Component.translatable("message.naturalis.survival_as.hard_diet_sick"), true);
    }

    private static void handleHumanBoundDietPenalty(ServerPlayer player, ItemStack consumed, FoodProperties food) {
        if (!NaturalisConfig.dietHumanFoodPenaltyWhileMorphed()) {
            return;
        }
        if (!ResonanceManager.isResonanceEnabled(player)) {
            return;
        }

        ResourceLocation bonded = ResonanceManager.getBondedMorph(player);
        if (bonded == null) {
            return;
        }

        int humanity = ResonanceManager.getHumanity(player);
        if (humanity > 80) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(consumed.getItem());
        DietManager.DietType diet = DietManager.getDietType(bonded);
        DietManager.FoodType foodType = DietManager.getFoodType(itemId);
        if (isFoodCompatible(diet, foodType, itemId)) {
            return;
        }

        grantAdvancement(player, ADV_HUMAN_DIET_PENALTY);

        int currentFood = player.getFoodData().getFoodLevel();
        int penaltyFood = Math.max(1, Math.round(food.nutrition() * (humanity <= 60 ? 0.65F : 0.35F)));
        player.getFoodData().setFoodLevel(Math.max(0, currentFood - penaltyFood));

        float satPenalty = food.saturation() * (humanity <= 60 ? 1.35F : 0.70F);
        player.causeFoodExhaustion(Math.max(1.0F, satPenalty));

        if (humanity <= 60) {
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.human_diet_penalty_strong"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.naturalis.resonance.human_diet_penalty"), true);
        }
    }

    private static boolean isFoodCompatible(DietManager.DietType diet, DietManager.FoodType foodType, ResourceLocation itemId) {
        String path = itemId.getPath();
        return switch (diet) {
            case CARNIVORE -> foodType == DietManager.FoodType.MEAT || foodType == DietManager.FoodType.NEUTRAL;
            case HERBIVORE -> foodType == DietManager.FoodType.VEGGIE || foodType == DietManager.FoodType.NEUTRAL;
            case OMNIVORE -> true;
            case PISCIVORE -> isFishPath(path) || foodType == DietManager.FoodType.NEUTRAL;
            case INSECTIVORE -> isInsectPath(path) || foodType == DietManager.FoodType.NEUTRAL;
            case NECROVORE -> "rotten_flesh".equals(path) || foodType == DietManager.FoodType.MEAT;
            case FRUGIVORE -> isFruitPath(path) || foodType == DietManager.FoodType.VEGGIE;
            case FUNGIVORE -> isFungusPath(path);
            case HEMATOPHAGE -> foodType == DietManager.FoodType.MEAT;
            case SCAVENGER -> foodType == DietManager.FoodType.MEAT
                || foodType == DietManager.FoodType.NEUTRAL
                || "rotten_flesh".equals(path);
            case LITHOVORE -> isMineralPath(path);
            case FLORIVORE -> isFlowerPath(path) || foodType == DietManager.FoodType.VEGGIE;
            case NECTARIVORE -> "honey_bottle".equals(path) || isFlowerPath(path) || isFruitPath(path);
        };
    }

    /**
     * Survival-as hard diet: no bread/cookie free pass for predators or grazers.
     * Only foods that truly match the species niche are accepted.
     */
    private static boolean isStrictFoodCompatible(
        DietManager.DietType diet,
        DietManager.FoodType foodType,
        ResourceLocation itemId
    ) {
        String path = itemId.getPath();
        return switch (diet) {
            case CARNIVORE, HEMATOPHAGE -> foodType == DietManager.FoodType.MEAT;
            case HERBIVORE -> foodType == DietManager.FoodType.VEGGIE;
            case OMNIVORE -> foodType == DietManager.FoodType.MEAT
                || foodType == DietManager.FoodType.VEGGIE
                || foodType == DietManager.FoodType.NEUTRAL;
            case PISCIVORE -> isFishPath(path);
            case INSECTIVORE -> isInsectPath(path);
            case NECROVORE -> "rotten_flesh".equals(path)
                || "spider_eye".equals(path)
                || foodType == DietManager.FoodType.MEAT;
            case FRUGIVORE -> isFruitPath(path);
            case FUNGIVORE -> isFungusPath(path);
            case SCAVENGER -> foodType == DietManager.FoodType.MEAT || "rotten_flesh".equals(path);
            case LITHOVORE -> isMineralPath(path);
            case FLORIVORE -> isFlowerPath(path) || foodType == DietManager.FoodType.VEGGIE;
            case NECTARIVORE -> "honey_bottle".equals(path) || isFlowerPath(path) || isFruitPath(path);
        };
    }

    private static boolean isFishPath(String path) {
        return path.contains("cod") || path.contains("salmon") || path.contains("fish")
            || path.contains("kelp") || "tropical_fish".equals(path) || "pufferfish".equals(path);
    }

    private static boolean isInsectPath(String path) {
        return path.contains("spider_eye") || path.contains("honeycomb") || path.contains("larva");
    }

    private static boolean isFruitPath(String path) {
        return path.contains("berry") || path.contains("berries") || path.contains("apple")
            || path.contains("melon") || path.contains("chorus_fruit") || path.contains("sweet");
    }

    private static boolean isFungusPath(String path) {
        return path.contains("mushroom") || path.contains("fungus") || "mushroom_stew".equals(path);
    }

    private static boolean isFlowerPath(String path) {
        return path.contains("flower") || path.contains("bloom") || path.contains("petal");
    }

    private static boolean isMineralPath(String path) {
        return path.contains("iron_ingot") || path.contains("copper_ingot") || path.contains("gold_ingot")
            || path.contains("stone") || path.contains("deepslate") || path.contains("nugget");
    }

    private static void handleCarnivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (!NaturalisConfig.dietCarnivorePenalties()) {
            return;
        }
        if (type == DietManager.FoodType.MEAT) {
            applyRawParityBonus(player, itemId, true);
            grantAdvancement(player, ADV_CARNIVORE_PRIME);
            return;
        }

        if (type == DietManager.FoodType.VEGGIE) {
            applyDislikedFoodPenalty(player);
            grantAdvancement(player, ADV_IRON_STOMACH);
        }
    }

    private static void handleHerbivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (!NaturalisConfig.dietHerbivorePenalties()) {
            return;
        }
        if (type == DietManager.FoodType.VEGGIE || type == DietManager.FoodType.NEUTRAL) {
            applyVeggieParityBonus(player, itemId);
            grantAdvancement(player, ADV_HERBIVORE_PRIME);
            return;
        }

        if (type == DietManager.FoodType.MEAT) {
            applyDislikedFoodPenalty(player);
            grantAdvancement(player, ADV_IRON_STOMACH);
        }
    }

    private static void handleOmnivore(ServerPlayer player, ItemStack stack, DietManager.FoodType type) {
        // Omnivores can eat everything. Prefer neutral foods with a tiny comfort bonus.
        if (type == DietManager.FoodType.NEUTRAL && player.getRandom().nextFloat() < 0.35F) {
            player.getFoodData().eat(1, 0.15F);
        }
    }

    private static void handlePiscivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (!NaturalisConfig.dietCarnivorePenalties()) {
            return;
        }
        if (isFishPath(itemId.getPath()) || type == DietManager.FoodType.MEAT) {
            applyRawParityBonus(player, itemId, true);
            return;
        }
        if (type == DietManager.FoodType.VEGGIE) {
            applyDislikedFoodPenalty(player);
        }
    }

    private static void handleInsectivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (isInsectPath(itemId.getPath()) || type == DietManager.FoodType.MEAT) {
            return;
        }
        if (type == DietManager.FoodType.VEGGIE) {
            applyDislikedFoodPenalty(player);
        }
    }

    private static void handleNecrovore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if ("rotten_flesh".equals(itemId.getPath())) {
            return;
        }
        if (type == DietManager.FoodType.VEGGIE) {
            applyDislikedFoodPenalty(player);
        }
    }

    private static void handleFrugivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (!NaturalisConfig.dietHerbivorePenalties()) {
            return;
        }
        if (isFruitPath(itemId.getPath()) || isFlowerPath(itemId.getPath()) || type == DietManager.FoodType.VEGGIE) {
            applyVeggieParityBonus(player, itemId);
            return;
        }
        if (type == DietManager.FoodType.MEAT) {
            applyDislikedFoodPenalty(player);
        }
    }

    private static void handleFungivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (isFungusPath(itemId.getPath())) {
            applyVeggieParityBonus(player, itemId);
            return;
        }
        if (type == DietManager.FoodType.MEAT) {
            applyDislikedFoodPenalty(player);
        }
    }

    private static void handleLithovore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
        if (!isMineralPath(itemId.getPath())) {
            applyDislikedFoodPenalty(player);
        }
    }

    private static void applySpeciesFoodDetails(ServerPlayer player, ResourceLocation morphId, ResourceLocation itemId) {
        String morph = morphId.getPath();
        String food = itemId.getPath();

        // Vanilla detail: parrots are poisoned by cookies.
        if ("parrot".equals(morph) && "cookie".equals(food)) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 220, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, true, true));
            return;
        }

        // Feline fish affinity.
        if (("cat".equals(morph) || "ocelot".equals(morph))
            && ("cod".equals(food) || "salmon".equals(food) || "tropical_fish".equals(food))) {
            player.getFoodData().eat(1, 0.25F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("MOVEMENT_SPEED", "SPEED"), 80, 0, false, true, true));
            return;
        }

        // Rabbit carrot affinity.
        if ("rabbit".equals(morph) && ("carrot".equals(food) || "golden_carrot".equals(food))) {
            player.getFoodData().eat(1, 0.30F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("JUMP", "JUMP_BOOST"), 120, 0, false, true, true));
            return;
        }

        // Bees thrive with honey.
        if ("bee".equals(morph) && "honey_bottle".equals(food)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, true, true));
            return;
        }

        // Undead-like forms tolerate rotten flesh and spider eyes better.
        if (("zombie".equals(morph) || "husk".equals(morph) || "drowned".equals(morph)
            || "zombie_villager".equals(morph) || "wither_skeleton".equals(morph))
            && ("rotten_flesh".equals(food) || "spider_eye".equals(food))) {
            player.getFoodData().eat(1, 0.20F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("DAMAGE_RESISTANCE", "RESISTANCE"), 80, 0, false, true, true));
            return;
        }

        // Goat likes rough forage.
        if ("goat".equals(morph) && ("wheat".equals(food) || "sweet_berries".equals(food))) {
            player.getFoodData().eat(1, 0.20F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("DAMAGE_BOOST", "STRENGTH"), 60, 0, false, true, true));
        }
    }

    private static void applyDislikedFoodPenalty(ServerPlayer player) {
        float roll = player.getRandom().nextFloat();

        // Rare poison, otherwise sometimes nausea or hunger.
        if (roll < 0.12F) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, true, true));
            return;
        }
        if (roll < 0.40F) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 220, 0, false, true, true));
            return;
        }
        if (roll < 0.68F) {
            player.addEffect(new MobEffectInstance(
                CompatAccess.resolveMobEffect("NAUSEA", "CONFUSION"),
                180,
                0,
                false,
                true,
                true
            ));
        }
    }

    private static void applyRawParityBonus(ServerPlayer player, ResourceLocation itemId, boolean meatParity) {
        String path = itemId.getPath();
        switch (path) {
            case "beef" -> player.getFoodData().eat(3, 0.45F);        // beef -> cooked_beef
            case "porkchop" -> player.getFoodData().eat(5, 0.65F);    // porkchop -> cooked_porkchop
            case "chicken" -> player.getFoodData().eat(3, 0.45F);     // chicken -> cooked_chicken
            case "mutton" -> player.getFoodData().eat(4, 0.60F);      // mutton -> cooked_mutton
            case "rabbit" -> player.getFoodData().eat(2, 0.45F);      // rabbit -> cooked_rabbit
            case "cod" -> player.getFoodData().eat(2, 0.30F);         // cod -> cooked_cod
            case "salmon" -> player.getFoodData().eat(2, 0.30F);      // salmon -> cooked_salmon
            default -> {
                if (meatParity) {
                    // No parity mapping needed for this meat item.
                }
            }
        }
    }

    private static void applyVeggieParityBonus(ServerPlayer player, ResourceLocation itemId) {
        String path = itemId.getPath();
        switch (path) {
            case "potato" -> player.getFoodData().eat(4, 0.60F); // potato -> baked_potato
            case "kelp" -> player.getFoodData().eat(5, 0.60F);   // kelp -> dried kelp style bulk bonus
            default -> {
            }
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        AdvancementHolder root = player.getServer().getAdvancements().get(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }

    public static String debugDiet(ResourceLocation morphId, ResourceLocation itemId) {
        DietManager.DietType diet = DietManager.getDietType(morphId);
        DietManager.FoodType food = DietManager.getFoodType(itemId);
        return String.format(Locale.ROOT, "%s eating %s -> %s", morphId, itemId, diet + "/" + food);
    }
}
