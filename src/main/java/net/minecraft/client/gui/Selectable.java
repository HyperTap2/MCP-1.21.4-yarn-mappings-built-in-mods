package net.minecraft.client.gui;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.navigation.Navigable;

public interface Selectable extends Navigable, Narratable {
   Selectable.SelectionType getType();

   default boolean isNarratable() {
      return true;
   }

   default Collection<? extends Selectable> getNarratedParts() {
      return List.of(this);
   }

   enum SelectionType {
      NONE,
      HOVERED,
      FOCUSED;

      public boolean isFocused() {
         return this == FOCUSED;
      }
   }
}
