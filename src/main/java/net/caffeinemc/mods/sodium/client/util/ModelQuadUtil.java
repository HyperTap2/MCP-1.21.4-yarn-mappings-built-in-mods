package net.caffeinemc.mods.sodium.client.util;

public class ModelQuadUtil {
   public static final int POSITION_INDEX = 0;
   public static final int COLOR_INDEX = 3;
   public static final int TEXTURE_INDEX = 4;
   public static final int LIGHT_INDEX = 6;
   public static final int NORMAL_INDEX = 7;
   public static final int VERTEX_SIZE = 8;

   public static int vertexOffset(int vertexIndex) {
      return vertexIndex * 8;
   }
}
