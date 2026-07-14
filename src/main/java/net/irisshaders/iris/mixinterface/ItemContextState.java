package net.irisshaders.iris.mixinterface;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

public interface ItemContextState {
   void setDisplayItem(Item var1, Identifier var2);

   Item getDisplayItem();

   Identifier getDisplayItemModel();
}
