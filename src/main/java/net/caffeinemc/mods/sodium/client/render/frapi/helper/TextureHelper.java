package net.caffeinemc.mods.sodium.client.render.frapi.helper;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public class TextureHelper {
   private static final float NORMALIZER = 0.0625F;
   private static final TextureHelper.VertexModifier[] ROTATIONS = new TextureHelper.VertexModifier[]{
      null, (q, i) -> q.uv(i, q.v(i), 1.0F - q.u(i)), (q, i) -> q.uv(i, 1.0F - q.u(i), 1.0F - q.v(i)), (q, i) -> q.uv(i, 1.0F - q.v(i), q.u(i))
   };
   private static final TextureHelper.VertexModifier[] UVLOCKERS = new TextureHelper.VertexModifier[6];

   private TextureHelper() {
   }

   public static void bakeSprite(MutableQuadView quad, Sprite sprite, int bakeFlags) {
      if (quad.nominalFace() != null && (4 & bakeFlags) != 0) {
         applyModifier(quad, UVLOCKERS[quad.nominalFace().getId()]);
      } else if ((32 & bakeFlags) == 0) {
         applyModifier(quad, (q, i) -> q.uv(i, q.u(i) * 0.0625F, q.v(i) * 0.0625F));
      }

      int rotation = bakeFlags & 3;
      if (rotation != 0) {
         applyModifier(quad, ROTATIONS[rotation]);
      }

      if ((8 & bakeFlags) != 0) {
         applyModifier(quad, (q, i) -> q.uv(i, 1.0F - q.u(i), q.v(i)));
      }

      if ((16 & bakeFlags) != 0) {
         applyModifier(quad, (q, i) -> q.uv(i, q.u(i), 1.0F - q.v(i)));
      }

      interpolate(quad, sprite);
   }

   private static void interpolate(MutableQuadView q, Sprite sprite) {
      float uMin = sprite.getMinU();
      float uSpan = sprite.getMaxU() - uMin;
      float vMin = sprite.getMinV();
      float vSpan = sprite.getMaxV() - vMin;

      for (int i = 0; i < 4; i++) {
         q.uv(i, uMin + q.u(i) * uSpan, vMin + q.v(i) * vSpan);
      }
   }

   private static void applyModifier(MutableQuadView quad, TextureHelper.VertexModifier modifier) {
      for (int i = 0; i < 4; i++) {
         modifier.apply(quad, i);
      }
   }

   static {
      UVLOCKERS[Direction.EAST.getId()] = (q, i) -> q.uv(i, 1.0F - q.z(i), 1.0F - q.y(i));
      UVLOCKERS[Direction.WEST.getId()] = (q, i) -> q.uv(i, q.z(i), 1.0F - q.y(i));
      UVLOCKERS[Direction.NORTH.getId()] = (q, i) -> q.uv(i, 1.0F - q.x(i), 1.0F - q.y(i));
      UVLOCKERS[Direction.SOUTH.getId()] = (q, i) -> q.uv(i, q.x(i), 1.0F - q.y(i));
      UVLOCKERS[Direction.DOWN.getId()] = (q, i) -> q.uv(i, q.x(i), 1.0F - q.z(i));
      UVLOCKERS[Direction.UP.getId()] = (q, i) -> q.uv(i, q.x(i), q.z(i));
   }

   @FunctionalInterface
   private interface VertexModifier {
      void apply(MutableQuadView var1, int var2);
   }
}
