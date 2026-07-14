package net.minecraft.client.render.entity.state;


public class PigEntityRenderState extends LivingEntityRenderState implements SaddleableRenderState {
   public boolean saddled;

   @Override
   public boolean isSaddled() {
      return this.saddled;
   }
}
