package net.minecraft.client.render.entity.state;

import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity.Data;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity.TextLines;
import org.jetbrains.annotations.Nullable;

public class TextDisplayEntityRenderState extends DisplayEntityRenderState {
   @Nullable
   public Data data;
   @Nullable
   public TextLines textLines;

   @Override
   public boolean canRender() {
      return this.data != null;
   }
}
