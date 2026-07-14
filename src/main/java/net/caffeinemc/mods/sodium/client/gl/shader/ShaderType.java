package net.caffeinemc.mods.sodium.client.gl.shader;

public enum ShaderType {
   VERTEX(35633),
   GEOMETRY(36313),
   TESS_CONTROL(36488),
   TESS_EVALUATION(36487),
   FRAGMENT(35632);

   public final int id;

   private ShaderType(int id) {
      this.id = id;
   }

   public static ShaderType fromGlShaderType(int id) {
      for (ShaderType type : values()) {
         if (type.id == id) {
            return type;
         }
      }

      return null;
   }
}
