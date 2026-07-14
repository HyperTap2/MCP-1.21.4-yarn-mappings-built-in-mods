package net.minecraft.client.render.entity.state;

import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity.Data;
import org.jetbrains.annotations.Nullable;

public class BlockDisplayEntityRenderState extends DisplayEntityRenderState {
   @Nullable
   public Data data;

   @Override
   public boolean canRender() {
      return this.data != null;
   }
}
