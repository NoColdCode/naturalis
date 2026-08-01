package dev.naturalis.diet;

import dev.naturalis.Naturalis;
import dev.naturalis.compat.CompatAccess;
import dev.naturalis.resonance.ResonanceManager;
import dev.naturalis.util.CurrentMorphUtil;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Locale;

@EventBusSubscriber(modid = Naturalis.MOD_ID)
public final class DietEvents {

    private static final ResourceLocation ADV_ROOT                = new ResourceLocation(Naturalis.MOD_ID, "root");
    private static final ResourceLocation ADV_CARNIVORE_PRIME     = new ResourceLocation(Naturalis.MOD_ID, "diet/carnivore_prime");
    private static final ResourceLocation ADV_HERBIVORE_PRIME     = new ResourceLocation(Naturalis.MOD_ID, "diet/herbivore_prime");
    private static final ResourceLocation ADV_IRON_STOMACH        = new ResourceLocation(Naturalis.MOD_ID, "diet/iron_stomach");
    private static final ResourceLocation ADV_HUMAN_DIET_PENALTY  = new ResourceLocation(Naturalis.MOD_ID, "resonance/human_diet_penalty");
    private static final ResourceLocation ADV_HUMAN_DIET_LOCKED   = new ResourceLocation(Naturalis.MOD_ID, "resonance/human_diet_locked");

    private DietEvents() {
    }

    @SubscribeEvent
    public static void onFoodFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack consumed = event.getItem();
        Item item = consumed.getItem();
        if (!item.isEdible()) {
            return;
        }
        FoodProperties food = item.getFoodProperties();
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

    private static void blockInvalidHumanFoodWhileFractured(ServerPlayer player, ItemStack stack,
                                                             PlayerInteractEvent.RightClickItem event) {
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

        if (!stack.getItem().isEdible()) {
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

    private static void handleHumanBoundDietPenalty(ServerPlayer player, ItemStack consumed, FoodProperties food) {
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
        // 1.20.1: food.getNutrition() / food.getSaturationModifier()
        int penaltyFood = Math.max(1, Math.round(food.getNutrition() * (humanity <= 60 ? 0.65F : 0.35F)));
        player.getFoodData().setFoodLevel(Math.max(0, currentFood - penaltyFood));

        float satPenalty = food.getSaturationModifier() * (humanity <= 60 ? 1.35F : 0.70F);
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

    private static void handleCarnivore(ServerPlayer player, ItemStack stack,
                                        ResourceLocation itemId, DietManager.FoodType type) {
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

    private static void handleHerbivore(ServerPlayer player, ItemStack stack,
                                        ResourceLocation itemId, DietManager.FoodType type) {
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
        if (type == DietManager.FoodType.NEUTRAL && player.getRandom().nextFloat() < 0.35F) {
            player.getFoodData().eat(1, 0.15F);
        }
    }

    private static void handlePiscivore(ServerPlayer player, ItemStack stack, ResourceLocation itemId, DietManager.FoodType type) {
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
        String food  = itemId.getPath();

        if ("parrot".equals(morph) && "cookie".equals(food)) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 220, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, true, true));
            return;
        }

        if (("cat".equals(morph) || "ocelot".equals(morph))
            && ("cod".equals(food) || "salmon".equals(food) || "tropical_fish".equals(food))) {
            player.getFoodData().eat(1, 0.25F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("MOVEMENT_SPEED", "SPEED"), 80, 0, false, true, true));
            return;
        }

        if ("rabbit".equals(morph) && ("carrot".equals(food) || "golden_carrot".equals(food))) {
            player.getFoodData().eat(1, 0.30F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("JUMP", "JUMP_BOOST"), 120, 0, false, true, true));
            return;
        }

        if ("bee".equals(morph) && "honey_bottle".equals(food)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, true, true));
            return;
        }

        if (("zombie".equals(morph) || "husk".equals(morph) || "drowned".equals(morph)
            || "zombie_villager".equals(morph) || "wither_skeleton".equals(morph))
            && ("rotten_flesh".equals(food) || "spider_eye".equals(food))) {
            player.getFoodData().eat(1, 0.20F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("DAMAGE_RESISTANCE", "RESISTANCE"), 80, 0, false, true, true));
            return;
        }

        if ("goat".equals(morph) && ("wheat".equals(food) || "sweet_berries".equals(food))) {
            player.getFoodData().eat(1, 0.20F);
            player.addEffect(new MobEffectInstance(CompatAccess.resolveMobEffect("DAMAGE_BOOST", "STRENGTH"), 60, 0, false, true, true));
        }
    }

    private static void applyDislikedFoodPenalty(ServerPlayer player) {
        float roll = player.getRandom().nextFloat();

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
                180, 0, false, true, true));
        }
    }

    private static void applyRawParityBonus(ServerPlayer player, ResourceLocation itemId, boolean meatParity) {
        String path = itemId.getPath();
        switch (path) {
            case "beef"      -> player.getFoodData().eat(3, 0.45F);
            case "porkchop"  -> player.getFoodData().eat(5, 0.65F);
            case "chicken"   -> player.getFoodData().eat(3, 0.45F);
            case "mutton"    -> player.getFoodData().eat(4, 0.60F);
            case "rabbit"    -> player.getFoodData().eat(2, 0.45F);
            case "cod"       -> player.getFoodData().eat(2, 0.30F);
            case "salmon"    -> player.getFoodData().eat(2, 0.30F);
            default          -> { /* no parity mapping */ }
        }
    }

    private static void applyVeggieParityBonus(ServerPlayer player, ResourceLocation itemId) {
        String path = itemId.getPath();
        switch (path) {
            case "potato" -> player.getFoodData().eat(4, 0.60F);
            case "kelp"   -> player.getFoodData().eat(5, 0.60F);
            default       -> { }
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        if (player.getServer() == null) {
            return;
        }

        Advancement root = player.getServer().getAdvancements().getAdvancement(ADV_ROOT);
        if (root != null) {
            player.getAdvancements().award(root, "tick");
        }

        Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, "trigger");
    }

    public static String debugDiet(ResourceLocation morphId, ResourceLocation itemId) {
        DietManager.DietType diet  = DietManager.getDietType(morphId);
        DietManager.FoodType food  = DietManager.getFoodType(itemId);
        return String.format(Locale.ROOT, "%s eating %s -> %s", morphId, itemId, diet + "/" + food);
    }
}
