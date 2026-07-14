package net.minecraft.client.recipebook;

import java.util.Map;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipePropertySet;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay.Grouping;
import net.minecraft.registry.RegistryKey;

public class ClientRecipeManager implements RecipeManager {
   private final Map<RegistryKey<RecipePropertySet>, RecipePropertySet> propertySets;
   private final Grouping<StonecuttingRecipe> recipes;

   public ClientRecipeManager(Map<RegistryKey<RecipePropertySet>, RecipePropertySet> propertySets, Grouping<StonecuttingRecipe> recipes) {
      this.propertySets = propertySets;
      this.recipes = recipes;
   }

   public RecipePropertySet getPropertySet(RegistryKey<RecipePropertySet> key) {
      return this.propertySets.getOrDefault(key, RecipePropertySet.EMPTY);
   }

   public Grouping<StonecuttingRecipe> getStonecutterRecipes() {
      return this.recipes;
   }
}
