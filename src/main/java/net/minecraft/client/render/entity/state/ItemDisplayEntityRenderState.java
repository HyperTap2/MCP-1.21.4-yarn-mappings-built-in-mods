package net.minecraft.client.render.entity.state;

import net.minecraft.client.render.item.ItemRenderState;

public class ItemDisplayEntityRenderState extends DisplayEntityRenderState {
   public final ItemRenderState itemRenderState = new ItemRenderState();

   @Override
   public boolean canRender() {
      return !this.itemRenderState.isEmpty();
   }
}
