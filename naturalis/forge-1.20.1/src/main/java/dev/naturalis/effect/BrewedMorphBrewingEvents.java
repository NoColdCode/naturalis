package dev.naturalis.effect;

import dev.naturalis.content.NaturalisItems;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Forge 1.20.1 version — registers brewing recipes via
 * {@link BrewingRecipeRegistry#addRecipe} inside {@link FMLCommonSetupEvent}.
 * The NeoForge {@code RegisterBrewingRecipesEvent} / {@code PotionContents}
 * APIs do not exist in 1.20.1.
 */
public final class BrewedMorphBrewingEvents {

    private BrewedMorphBrewingEvents() {
    }

    /** Called from {@code Naturalis} constructor; pass the mod event bus. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BrewedMorphBrewingEvents::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(new BrewedMorphRecipe());
            BrewingRecipeRegistry.addRecipe(new MorphBindingRecipe());
            BrewingRecipeRegistry.addRecipe(new BrewedMorphSplashRecipe());
            BrewingRecipeRegistry.addRecipe(new BrewedMorphLingeringRecipe());
            BrewingRecipeRegistry.addRecipe(new MorphBindingSplashRecipe());
            BrewingRecipeRegistry.addRecipe(new MorphBindingLingeringRecipe());
        });
    }

    // ─── Recipes ──────────────────────────────────────────────────────────────

    private static final class BrewedMorphRecipe implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {
            return isAwkwardPotion(input);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            if (!ingredient.is(NaturalisItems.FILLED_ECHO_VIAL.get())) {
                return false;
            }
            String mobId = MorphDataUtil.getMobId(ingredient);
            return mobId != null && !mobId.isEmpty();
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }

            String mobId = MorphDataUtil.getMobId(ingredient);
            ItemStack output = new ItemStack(NaturalisItems.BREWED_MORPH_POTION.get());
            MorphDataUtil.setMobId(output, mobId);
            output.getOrCreateTag().putString("BrewedMorphId", mobId);
            return output;
        }
    }

    private static final class MorphBindingRecipe implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {
            return isAwkwardPotion(input);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(NaturalisItems.MORPH_ORB.get());
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(NaturalisItems.MORPH_BINDING_POTION.get());
        }
    }

    private static final class BrewedMorphSplashRecipe implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {
            return input.is(NaturalisItems.BREWED_MORPH_POTION.get());
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.GUNPOWDER);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return convertPotionForm(input, NaturalisItems.BREWED_MORPH_SPLASH_POTION.get().getDefaultInstance());
        }
    }

    private static final class BrewedMorphLingeringRecipe implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {
            return input.is(NaturalisItems.BREWED_MORPH_SPLASH_POTION.get());
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.DRAGON_BREATH);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return convertPotionForm(input, NaturalisItems.BREWED_MORPH_LINGERING_POTION.get().getDefaultInstance());
        }
    }

    private static final class MorphBindingSplashRecipe implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {
            return input.is(NaturalisItems.MORPH_BINDING_POTION.get());
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.GUNPOWDER);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return convertPotionForm(input, NaturalisItems.MORPH_BINDING_SPLASH_POTION.get().getDefaultInstance());
        }
    }

    private static final class MorphBindingLingeringRecipe implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {
            return input.is(NaturalisItems.MORPH_BINDING_SPLASH_POTION.get());
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.DRAGON_BREATH);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return convertPotionForm(input, NaturalisItems.MORPH_BINDING_LINGERING_POTION.get().getDefaultInstance());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Copies the morph-related NBT data from {@code input} into {@code output}
     * and returns {@code output}. In 1.20.1 the potion type is determined by
     * the item class, so no POTION_CONTENTS component needs to be copied.
     */
    private static ItemStack convertPotionForm(ItemStack input, ItemStack output) {
        CompoundTag inputTag = input.getTag();
        if (inputTag != null && !inputTag.isEmpty()) {
            output.setTag(inputTag.copy());
        }

        String mobId = MorphDataUtil.getMobId(input);
        if (mobId != null && !mobId.isEmpty()) {
            MorphDataUtil.setMobId(output, mobId);
            output.getOrCreateTag().putString("BrewedMorphId", mobId);
        }

        return output;
    }

    /** Returns true when {@code stack} is an Awkward Potion. */
    private static boolean isAwkwardPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        return PotionUtils.getPotion(stack) == Potions.AWKWARD;
    }
}
