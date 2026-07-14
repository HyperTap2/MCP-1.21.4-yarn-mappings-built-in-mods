package net.caffeinemc.mods.sodium.client.render.immediate.model;

import java.util.Set;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public class ModelCuboid {
   public static final int FACE_NEG_Y = 0;
   public static final int FACE_POS_Y = 1;
   public static final int FACE_NEG_X = 2;
   public static final int FACE_NEG_Z = 3;
   public static final int FACE_POS_X = 4;
   public static final int FACE_POS_Z = 5;
   public final float x1;
   public final float y1;
   public final float z1;
   public final float x2;
   public final float y2;
   public final float z2;
   public final float u0;
   public final float u1;
   public final float u2;
   public final float u3;
   public final float u4;
   public final float u5;
   public final float v0;
   public final float v1;
   public final float v2;
   private final int cullBitmask;
   public final boolean mirror;

   public ModelCuboid(
      int u,
      int v,
      float x1,
      float y1,
      float z1,
      float sizeX,
      float sizeY,
      float sizeZ,
      float extraX,
      float extraY,
      float extraZ,
      boolean mirror,
      float textureWidth,
      float textureHeight,
      Set<Direction> renderDirections
   ) {
      float x2 = x1 + sizeX;
      float y2 = y1 + sizeY;
      float z2 = z1 + sizeZ;
      x1 -= extraX;
      y1 -= extraY;
      z1 -= extraZ;
      x2 += extraX;
      y2 += extraY;
      z2 += extraZ;
      if (mirror) {
         float tmp = x2;
         x2 = x1;
         x1 = tmp;
      }

      this.x1 = x1 / 16.0F;
      this.y1 = y1 / 16.0F;
      this.z1 = z1 / 16.0F;
      this.x2 = x2 / 16.0F;
      this.y2 = y2 / 16.0F;
      this.z2 = z2 / 16.0F;
      float scaleU = 1.0F / textureWidth;
      float scaleV = 1.0F / textureHeight;
      this.u0 = scaleU * u;
      this.u1 = scaleU * (u + sizeZ);
      this.u2 = scaleU * (u + sizeZ + sizeX);
      this.u3 = scaleU * (u + sizeZ + sizeX + sizeX);
      this.u4 = scaleU * (u + sizeZ + sizeX + sizeZ);
      this.u5 = scaleU * (u + sizeZ + sizeX + sizeZ + sizeX);
      this.v0 = scaleV * v;
      this.v1 = scaleV * (v + sizeZ);
      this.v2 = scaleV * (v + sizeZ + sizeY);
      this.mirror = mirror;
      int cullBitmask = 0;

      for (Direction direction : renderDirections) {
         cullBitmask |= 1 << getFaceIndex(direction);
      }

      this.cullBitmask = cullBitmask;
   }

   public boolean shouldDrawFace(int faceIndex) {
      return (this.cullBitmask & 1 << faceIndex) != 0;
   }

   public static int getFaceIndex(@NotNull Direction dir) {
      return switch (dir) {
         case DOWN -> 0;
         case UP -> 1;
         case NORTH -> 3;
         case SOUTH -> 5;
         case WEST -> 2;
         case EAST -> 4;
      };
   }
}
