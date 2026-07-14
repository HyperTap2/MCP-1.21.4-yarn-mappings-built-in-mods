package net.minecraft.client.render.entity.state;


public class StriderEntityRenderState extends LivingEntityRenderState implements SaddleableRenderState {
   public boolean saddled;
   public boolean cold;
   public boolean hasPassengers;

   @Override
   public boolean isSaddled() {
      return this.saddled;
   }
}
