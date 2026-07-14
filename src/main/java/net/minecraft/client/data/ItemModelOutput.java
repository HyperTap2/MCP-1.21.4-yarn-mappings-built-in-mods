package net.minecraft.client.data;

import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.item.Item;

public interface ItemModelOutput {
   void accept(Item item, ItemModel.Unbaked model);

   void acceptAlias(Item base, Item alias);
}
