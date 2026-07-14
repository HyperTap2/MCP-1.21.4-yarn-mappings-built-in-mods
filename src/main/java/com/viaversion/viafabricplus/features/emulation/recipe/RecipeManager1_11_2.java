package com.viaversion.viafabricplus.features.emulation.recipe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public final class RecipeManager1_11_2 {
   private final Multimap<RecipeType<?>, RecipeEntry<?>> recipesByType;
   private final Map<RegistryKey<Recipe<?>>, RecipeEntry<?>> recipesById;

   public RecipeManager1_11_2(Iterable<RecipeEntry<?>> recipes) {
      Builder<RecipeType<?>, RecipeEntry<?>> recipesByTypeBuilder = ImmutableMultimap.builder();
      com.google.common.collect.ImmutableMap.Builder<RegistryKey<Recipe<?>>, RecipeEntry<?>> recipesByIdBuilder = ImmutableMap.builder();

      for (RecipeEntry<?> recipeEntry : recipes) {
         RecipeType<?> recipeType = recipeEntry.value().getType();
         recipesByTypeBuilder.put(recipeType, recipeEntry);
         recipesByIdBuilder.put(recipeEntry.id(), recipeEntry);
      }

      this.recipesByType = recipesByTypeBuilder.build();
      this.recipesById = recipesByIdBuilder.build();
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(RecipeType<T> type, I input, World world) {
      return input.isEmpty()
         ? Optional.empty()
         : this.recipesByType.get(type).stream().map(e -> (RecipeEntry<T>)e).filter(recipe -> recipe.value().matches(input, world)).findFirst();
   }

   public Optional<RecipeEntry<?>> get(RegistryKey<Recipe<?>> id) {
      return Optional.ofNullable(this.recipesById.get(id));
   }

   public Collection<RecipeEntry<?>> values() {
      return this.recipesById.values();
   }

   public Stream<RegistryKey<Recipe<?>>> keys() {
      return this.recipesById.keySet().stream();
   }
}
