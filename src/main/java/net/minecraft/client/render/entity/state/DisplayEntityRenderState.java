package net.minecraft.client.render.entity.state;

import net.minecraft.entity.decoration.DisplayEntity.RenderState;
import org.jetbrains.annotations.Nullable;

public abstract class DisplayEntityRenderState extends EntityRenderState {
   @Nullable
   public RenderState displayRenderState;
   public float lerpProgress;
   public float yaw;
   public float pitch;

   public abstract boolean canRender();
}
