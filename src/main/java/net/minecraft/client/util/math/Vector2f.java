package net.minecraft.client.util.math;


public class Vector2f {
   public float x;
   public float y;

   public Vector2f(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   @Override
   public String toString() {
      return "(" + this.x + "," + this.y + ")";
   }
}
