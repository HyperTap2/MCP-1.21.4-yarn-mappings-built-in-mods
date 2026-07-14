package com.viaversion.viafabricplus.features.emulation.recipe;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.world.World;

public final class ShulkerBoxColoringRecipe extends SpecialCraftingRecipe {
   public static final RecipeSerializer<ShulkerBoxColoringRecipe> SERIALIZER = new SpecialRecipeSerializer(ShulkerBoxColoringRecipe::new);

   public ShulkerBoxColoringRecipe(CraftingRecipeCategory craftingRecipeCategory) {
      super(craftingRecipeCategory);
   }

   public boolean matches(CraftingRecipeInput input, World world) {
      int i = 0;
      int j = 0;

      for (int k = 0; k < input.size(); k++) {
         ItemStack stack = input.getStackInSlot(k);
         if (!stack.isEmpty()) {
            if (Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock) {
               i++;
            } else {
               if (!(stack.getItem() instanceof DyeItem)) {
                  return false;
               }

               j++;
            }

            if (j > 1 || i > 1) {
               return false;
            }
         }
      }

      return i == 1 && j == 1;
   }

   public ItemStack craft(CraftingRecipeInput input, WrapperLookup wrapperLookup) {
      ItemStack result = ItemStack.EMPTY;
      DyeItem dyeItem = (DyeItem)Items.WHITE_DYE;

      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getStackInSlot(i);
         if (!stack.isEmpty()) {
            Item item = stack.getItem();
            if (Block.getBlockFromItem(item) instanceof ShulkerBoxBlock) {
               result = stack;
            } else if (item instanceof DyeItem) {
               dyeItem = (DyeItem)item;
            }
         }
      }

      return result.copyComponentsToNewStack(ShulkerBoxBlock.get(dyeItem.getColor()), 1);
   }

   public RecipeSerializer<ShulkerBoxColoringRecipe> getSerializer() {
      return SERIALIZER;
   }
}
