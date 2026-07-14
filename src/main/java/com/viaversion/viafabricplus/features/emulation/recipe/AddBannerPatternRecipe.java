package com.viaversion.viafabricplus.features.emulation.recipe;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.component.type.BannerPatternsComponent.Builder;
import net.minecraft.component.type.BannerPatternsComponent.Layer;
import net.minecraft.item.BannerItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.DyeColor;
import net.minecraft.world.World;

public final class AddBannerPatternRecipe extends SpecialCraftingRecipe {
   public static final RecipeSerializer<AddBannerPatternRecipe> SERIALIZER = new SpecialRecipeSerializer(AddBannerPatternRecipe::new);

   public AddBannerPatternRecipe(CraftingRecipeCategory craftingRecipeCategory) {
      super(craftingRecipeCategory);
   }

   public boolean matches(CraftingRecipeInput input, World world) {
      boolean foundBanner = false;

      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getStackInSlot(i);
         if (stack.getItem() instanceof BannerItem) {
            if (foundBanner) {
               return false;
            }

            if (((BannerPatternsComponent)stack.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT)).layers().size() >= 6) {
               return false;
            }

            foundBanner = true;
         }
      }

      return foundBanner && getBannerPattern(input) != null;
   }

   public ItemStack craft(CraftingRecipeInput input, WrapperLookup lookup) {
      ItemStack result = ItemStack.EMPTY;

      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getStackInSlot(i);
         if (!stack.isEmpty() && stack.getItem() instanceof BannerItem) {
            result = stack.copy();
            result.setCount(1);
            break;
         }
      }

      BannerPattern_1_13_2 pattern = getBannerPattern(input);
      if (pattern != null) {
         Reference<BannerPattern> patternKey = lookup.getOrThrow(RegistryKeys.BANNER_PATTERN).getOrThrow(pattern.getKey());
         DyeColor color = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) ? DyeColor.BLACK : DyeColor.WHITE;

         for (int i = 0; i < input.size(); i++) {
            if (input.getStackInSlot(i).getItem() instanceof DyeItem dyeItem) {
               color = dyeItem.getColor();
            }
         }

         Builder patternsBuilder = new Builder();
         if (result.contains(DataComponentTypes.BANNER_PATTERNS)) {
            patternsBuilder.addAll((BannerPatternsComponent)result.get(DataComponentTypes.BANNER_PATTERNS));
         }

         patternsBuilder.add(new Layer(patternKey, color));
         result.set(DataComponentTypes.BANNER_PATTERNS, patternsBuilder.build());
      }

      return result;
   }

   public RecipeSerializer<AddBannerPatternRecipe> getSerializer() {
      return SERIALIZER;
   }

   private static BannerPattern_1_13_2 getBannerPattern(CraftingRecipeInput input) {
      for (BannerPattern_1_13_2 pattern : BannerPattern_1_13_2.values()) {
         if (pattern.isCraftable()) {
            boolean matches = true;
            if (pattern.hasBaseStack()) {
               boolean foundBaseItem = false;
               boolean foundDye = false;

               for (int i = 0; i < input.size(); i++) {
                  ItemStack stack = input.getStackInSlot(i);
                  if (!stack.isEmpty() && !(stack.getItem() instanceof BannerItem)) {
                     if (stack.getItem() instanceof DyeItem) {
                        if (foundDye) {
                           matches = false;
                           break;
                        }

                        foundDye = true;
                     } else {
                        if (foundBaseItem || !ItemStack.areItemsEqual(stack, pattern.getBaseStack())) {
                           matches = false;
                           break;
                        }

                        foundBaseItem = true;
                     }
                  }
               }

               if (!foundBaseItem || !foundDye && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_10)) {
                  matches = false;
               }
            } else if (input.size() == pattern.getRecipePattern().length * pattern.getRecipePattern()[0].length()) {
               DyeColor patternColor = null;

               for (int i = 0; i < input.size(); i++) {
                  int row = i / 3;
                  int col = i % 3;
                  ItemStack stack = input.getStackInSlot(i);
                  Item item = stack.getItem();
                  if (!stack.isEmpty() && !(item instanceof BannerItem)) {
                     if (!(item instanceof DyeItem)) {
                        matches = false;
                        break;
                     }

                     DyeColor color = ((DyeItem)item).getColor();
                     if (patternColor != null && color != patternColor) {
                        matches = false;
                        break;
                     }

                     if (pattern.getRecipePattern()[row].charAt(col) == ' ') {
                        matches = false;
                        break;
                     }

                     patternColor = color;
                  } else if (pattern.getRecipePattern()[row].charAt(col) != ' ') {
                     matches = false;
                     break;
                  }
               }
            } else {
               matches = false;
            }

            if (matches) {
               return pattern;
            }
         }
      }

      return null;
   }
}
