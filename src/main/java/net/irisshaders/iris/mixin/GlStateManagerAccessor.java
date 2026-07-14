package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.BlendFuncState;
import com.mojang.blaze3d.platform.GlStateManager.ColorMask;
import com.mojang.blaze3d.platform.GlStateManager.DepthTestState;
import com.mojang.blaze3d.platform.GlStateManager.Texture2DState;

public final class GlStateManagerAccessor {
   private GlStateManagerAccessor() {
   }

   public static BlendFuncState getBLEND() {
      return GlStateManager.iris$getBlendState();
   }

   public static ColorMask getCOLOR_MASK() {
      return GlStateManager.iris$getColorMask();
   }

   public static DepthTestState getDEPTH() {
      return GlStateManager.iris$getDepthState();
   }

   public static int getActiveTexture() {
      return GlStateManager.activeTexture;
   }

   public static Texture2DState[] getTEXTURES() {
      return GlStateManager.TEXTURES;
   }
}
