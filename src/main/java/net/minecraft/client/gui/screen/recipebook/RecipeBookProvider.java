package net.minecraft.client.gui.screen.recipebook;

import net.minecraft.recipe.display.RecipeDisplay;

public interface RecipeBookProvider {
   void refreshRecipeBook();

   void onCraftFailed(RecipeDisplay display);
}
