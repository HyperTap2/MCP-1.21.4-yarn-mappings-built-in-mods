package net.caffeinemc.mods.sodium.api.util;

import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class NormI8 {
   private static final int X_COMPONENT_OFFSET = 0;
   private static final int Y_COMPONENT_OFFSET = 8;
   private static final int Z_COMPONENT_OFFSET = 16;
   private static final float COMPONENT_RANGE = 127.0F;
   private static final float NORM = 0.007874016F;

   public static int pack(Vector3fc normal) {
      return pack(normal.x(), normal.y(), normal.z());
   }

   public static int pack(float x, float y, float z) {
      int normX = encode(x);
      int normY = encode(y);
      int normZ = encode(z);
      return normZ << 16 | normY << 8 | normX << 0;
   }

   private static int encode(float comp) {
      return (int)(MathHelper.clamp(comp, -1.0F, 1.0F) * 127.0F) & 0xFF;
   }

   public static float unpackX(int norm) {
      return (byte)(norm >> 0 & 0xFF) * 0.007874016F;
   }

   public static float unpackY(int norm) {
      return (byte)(norm >> 8 & 0xFF) * 0.007874016F;
   }

   public static float unpackZ(int norm) {
      return (byte)(norm >> 16 & 0xFF) * 0.007874016F;
   }

   public static int flipPacked(int norm) {
      int normX = (norm >> 0 & 0xFF) * -1 & 0xFF;
      int normY = (norm >> 8 & 0xFF) * -1 & 0xFF;
      int normZ = (norm >> 16 & 0xFF) * -1 & 0xFF;
      return normZ << 16 | normY << 8 | normX << 0;
   }

   public static boolean isOpposite(int normA, int normB) {
      byte normAX = (byte)(normA >> 0);
      byte normAY = (byte)(normA >> 8);
      byte normAZ = (byte)(normA >> 16);
      byte normBX = (byte)(normB >> 0);
      byte normBY = (byte)(normB >> 8);
      byte normBZ = (byte)(normB >> 16);
      return normAX == -normBX && normAY == -normBY && normAZ == -normBZ;
   }

   public static Vector3f unpack(int packed, Vector3f output) {
      return output.set(unpackX(packed), unpackY(packed), unpackZ(packed));
   }
}
