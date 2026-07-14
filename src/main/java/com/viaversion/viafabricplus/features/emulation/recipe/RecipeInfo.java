package com.viaversion.viafabricplus.features.emulation.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.collection.DefaultedList;

public final class RecipeInfo {
   private final Supplier<Recipe<?>> creator;

   private RecipeInfo(Supplier<Recipe<?>> creator) {
      this.creator = creator;
   }

   public static RecipeInfo of(Supplier<Recipe<?>> creator) {
      return new RecipeInfo(creator);
   }

   public static RecipeInfo shaped(ItemConvertible output, Object... args) {
      return shaped("", output, args);
   }

   public static RecipeInfo shaped(int count, ItemConvertible output, Object... args) {
      return shaped("", count, output, args);
   }

   public static RecipeInfo shaped(String group, ItemStack output, Object... args) {
      List<String> shape = new ArrayList<>();
      int width = 0;

      int i;
      for (i = 0; i < args.length; i++) {
         if (!(args[i] instanceof String str)) {
            break;
         }

         if (i == 0) {
            width = str.length();
         } else if (str.length() != width) {
            throw new IllegalArgumentException("Rows do not have consistent width");
         }

         shape.add(str);
      }

      Map<Character, Ingredient> legend = new HashMap<>();

      while (i < args.length) {
         if (!(args[i] instanceof Character key)) {
            break;
         }

         i++;
         List<ItemConvertible> items = new ArrayList<>();

         while (i < args.length && args[i] instanceof ItemConvertible) {
            items.add((ItemConvertible)args[i]);
            i++;
         }

         legend.put(key, Ingredient.ofItems(items.toArray(new ItemConvertible[0])));
      }

      if (i != args.length) {
         throw new IllegalArgumentException("Unexpected argument at index " + i + ": " + args[i]);
      }

      int height = shape.size();
      DefaultedList<Optional<Ingredient>> ingredients = DefaultedList.of();

      for (String row : shape) {
         for (int x = 0; x < width; x++) {
            char key = row.charAt(x);
            Ingredient ingredient = legend.get(key);
            if (ingredient == null) {
               if (key != ' ') {
                  throw new IllegalArgumentException("Unknown character in shape: " + key);
               }

               ingredients.add(Optional.empty());
            } else {
               ingredients.add(Optional.of(ingredient));
            }
         }
      }

      int width_f = width;
      return new RecipeInfo(
         () -> new ShapedRecipe(group, CraftingRecipeCategory.MISC, new RawShapedRecipe(width_f, height, ingredients, Optional.empty()), output, false)
      );
   }

   public static RecipeInfo shaped(String group, ItemConvertible output, Object... args) {
      return shaped(group, new ItemStack(output), args);
   }

   public static RecipeInfo shaped(String group, int count, ItemConvertible output, Object... args) {
      return shaped(group, new ItemStack(output, count), args);
   }

   public static RecipeInfo shapeless(String group, ItemStack output, ItemConvertible... inputs) {
      ItemConvertible[][] newInputs = new ItemConvertible[inputs.length][1];

      for (int i = 0; i < inputs.length; i++) {
         newInputs[i] = new ItemConvertible[]{inputs[i]};
      }

      return shapeless(group, output, newInputs);
   }

   public static RecipeInfo shapeless(String group, ItemConvertible output, ItemConvertible... inputs) {
      return shapeless(group, new ItemStack(output), inputs);
   }

   public static RecipeInfo shapeless(String group, int count, ItemConvertible output, ItemConvertible... inputs) {
      return shapeless(group, new ItemStack(output, count), inputs);
   }

   public static RecipeInfo shapeless(String group, ItemStack output, ItemConvertible[]... inputs) {
      DefaultedList<Ingredient> ingredients = DefaultedList.of();

      for (ItemConvertible[] input : inputs) {
         ingredients.add(Ingredient.ofItems(input));
      }

      return new RecipeInfo(() -> new ShapelessRecipe(group, CraftingRecipeCategory.MISC, output, ingredients));
   }

   public static RecipeInfo shapeless(String group, int count, ItemConvertible output, ItemConvertible[]... inputs) {
      return shapeless(group, new ItemStack(output, count), inputs);
   }

   public static RecipeInfo shapeless(ItemConvertible output, ItemConvertible... inputs) {
      return shapeless("", output, inputs);
   }

   public static RecipeInfo shapeless(int count, ItemConvertible output, ItemConvertible... inputs) {
      return shapeless("", count, output, inputs);
   }

   public static RecipeInfo shapeless(int count, ItemConvertible output, ItemConvertible[]... inputs) {
      return shapeless("", count, output, inputs);
   }

   public static RecipeInfo smelting(ItemConvertible output, ItemConvertible input, float experience) {
      return smelting(output, input, experience, 200);
   }

   public static RecipeInfo smelting(ItemConvertible output, Ingredient input, float experience) {
      return smelting(output, input, experience, 200);
   }

   public static RecipeInfo smelting(ItemConvertible output, ItemConvertible input, float experience, int cookTime) {
      return smelting(output, Ingredient.ofItems(new ItemConvertible[]{input}), experience, cookTime);
   }

   public static RecipeInfo smelting(ItemConvertible output, Ingredient input, float experience, int cookTime) {
      return new RecipeInfo(() -> new SmeltingRecipe("", CookingRecipeCategory.MISC, input, new ItemStack(output), experience, cookTime));
   }

   public RecipeEntry<?> create(RegistryKey<Recipe<?>> id) {
      return new RecipeEntry(id, this.creator.get());
   }
}
