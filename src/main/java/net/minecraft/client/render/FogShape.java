package net.minecraft.client.render;


public enum FogShape {
   SPHERE(0),
   CYLINDER(1);

   private final int id;

   FogShape(final int id) {
      this.id = id;
   }

   public int getId() {
      return this.id;
   }
}
