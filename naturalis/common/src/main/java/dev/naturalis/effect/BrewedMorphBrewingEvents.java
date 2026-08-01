package dev.naturalis.effect;

import dev.naturalis.NaturalisMod;
import dev.naturalis.content.NaturalisItems;
import dev.naturalis.util.MorphDataUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

public final class BrewedMorphBrewingEvents {

    private BrewedMorphBrewingEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BrewedMorphBrewingEvents::onRegisterBrewingRecipes);
    }

    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(new BrewedMorphRecipe());
        event.getBuilder().addRecipe(new MorphBindingRecipe());
        event.getBuilder().addRecipe(new BrewedMorphSplashRecipe());
        event.getBuilder().addRecipe(new BrewedMorphLingeringRecipe());
        event.getBuilder().addRecipe(new MorphBindingSplashRecipe());
        event.getBuilder().addRecipe(new MorphBindingLingeringRecipe());
    }

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
            output.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD));
            MorphDataUtil.setMobId(output, mobId);
            CustomData.update(DataComponents.CUSTOM_DATA, output, tag -> tag.putString("BrewedMorphId", mobId));
            return output;
        }

        private static boolean isAwkwardPotion(ItemStack stack) {
            if (!stack.is(Items.POTION)) {
                return false;
            }

            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            return contents.is(Potions.AWKWARD);
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

            ItemStack output = new ItemStack(NaturalisItems.MORPH_BINDING_POTION.get());
            output.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.THICK));
            return output;
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

    private static ItemStack convertPotionForm(ItemStack input, ItemStack output) {
        PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        output.set(DataComponents.POTION_CONTENTS, contents);

        CustomData customData = input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag copied = customData.copyTag();
        if (!copied.isEmpty()) {
            output.set(DataComponents.CUSTOM_DATA, CustomData.of(copied));
        }

        String mobId = MorphDataUtil.getMobId(input);
        if (mobId != null && !mobId.isEmpty()) {
            MorphDataUtil.setMobId(output, mobId);
            CustomData.update(DataComponents.CUSTOM_DATA, output, tag -> tag.putString("BrewedMorphId", mobId));
        }

        return output;
    }

    private static boolean isAwkwardPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.AWKWARD);
    }
}