package net.caffeinemc.mods.sodium.client.model.light.data;

import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;

public abstract class LightDataAccess {
   private final Mutable pos = new Mutable();
   protected BlockRenderView level;

   public int get(int x, int y, int z, Direction d1, Direction d2) {
      return this.get(x + d1.getOffsetX() + d2.getOffsetX(), y + d1.getOffsetY() + d2.getOffsetY(), z + d1.getOffsetZ() + d2.getOffsetZ());
   }

   public int get(int x, int y, int z, Direction dir) {
      return this.get(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
   }

   public int get(BlockPos pos, Direction dir) {
      return this.get(pos.getX(), pos.getY(), pos.getZ(), dir);
   }

   public int get(BlockPos pos) {
      return this.get(pos.getX(), pos.getY(), pos.getZ());
   }

   public abstract int get(int var1, int var2, int var3);

   protected int compute(int x, int y, int z) {
      BlockPos pos = this.pos.set(x, y, z);
      BlockRenderView level = this.level;
      BlockState state = level.getBlockState(pos);
      boolean em = state.hasEmissiveLighting(level, pos);
      boolean op = state.shouldBlockVision(level, pos) && state.getOpacity() != 0;
      boolean fo = state.isOpaqueFullCube();
      boolean fc = state.isFullCube(level, pos);
      int lu = PlatformBlockAccess.getInstance().getLightEmission(state, level, pos);
      int bl;
      int sl;
      if (fo && lu == 0) {
         bl = 0;
         sl = 0;
      } else if (em) {
         bl = level.getLightLevel(LightType.BLOCK, pos);
         sl = level.getLightLevel(LightType.SKY, pos);
      } else {
         int light = WorldRenderer.getLightmapCoordinates(level, state, pos);
         bl = LightmapTextureManager.getBlockLightCoordinates(light);
         sl = LightmapTextureManager.getSkyLightCoordinates(light);
      }

      float ao;
      if (lu == 0) {
         ao = state.getAmbientOcclusionLightLevel(level, pos);
      } else {
         ao = 1.0F;
      }

      return packFC(fc) | packFO(fo) | packOP(op) | packEM(em) | packAO(ao) | packLU(lu) | packSL(sl) | packBL(bl);
   }

   public static int packBL(int blockLight) {
      return blockLight & 15;
   }

   public static int unpackBL(int word) {
      return word & 15;
   }

   public static int packSL(int skyLight) {
      return (skyLight & 15) << 4;
   }

   public static int unpackSL(int word) {
      return word >>> 4 & 15;
   }

   public static int packLU(int luminance) {
      return (luminance & 15) << 8;
   }

   public static int unpackLU(int word) {
      return word >>> 8 & 15;
   }

   public static int packAO(float ao) {
      int aoi = (int)(ao * 4096.0F);
      return (aoi & 65535) << 12;
   }

   public static float unpackAO(int word) {
      int aoi = word >>> 12 & 65535;
      return aoi * 2.4414062E-4F;
   }

   public static int packEM(boolean emissive) {
      return (emissive ? 1 : 0) << 28;
   }

   public static boolean unpackEM(int word) {
      return (word >>> 28 & 1) != 0;
   }

   public static int packOP(boolean opaque) {
      return (opaque ? 1 : 0) << 29;
   }

   public static boolean unpackOP(int word) {
      return (word >>> 29 & 1) != 0;
   }

   public static int packFO(boolean opaque) {
      return (opaque ? 1 : 0) << 30;
   }

   public static boolean unpackFO(int word) {
      return (word >>> 30 & 1) != 0;
   }

   public static int packFC(boolean fullCube) {
      return (fullCube ? 1 : 0) << 31;
   }

   public static boolean unpackFC(int word) {
      return (word >>> 31 & 1) != 0;
   }

   public static int getLightmap(int word) {
      return LightmapTextureManager.pack(Math.max(unpackBL(word), unpackLU(word)), unpackSL(word));
   }

   public static int getEmissiveLightmap(int word) {
      return unpackEM(word) ? 15728880 : getLightmap(word);
   }

   public BlockRenderView getLevel() {
      return this.level;
   }
}
