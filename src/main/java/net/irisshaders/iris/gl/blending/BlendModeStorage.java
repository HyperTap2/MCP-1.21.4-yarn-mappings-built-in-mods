package net.irisshaders.iris.gl.blending;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.BlendFuncState;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.statelisteners.BooleanStateAccessor;

public class BlendModeStorage {
   private static boolean originalBlendEnable;
   private static BlendMode originalBlend;
   private static boolean blendLocked;

   public static boolean isBlendLocked() {
      return blendLocked;
   }

   public static void overrideBlend(BlendMode override) {
      if (!blendLocked) {
         BlendFuncState blendState = GlStateManagerAccessor.getBLEND();
         originalBlendEnable = ((BooleanStateAccessor)blendState.capState).isEnabled();
         originalBlend = new BlendMode(blendState.srcFactorRGB, blendState.dstFactorRGB, blendState.srcFactorAlpha, blendState.dstFactorAlpha);
      }

      blendLocked = false;
      if (override == null) {
         GlStateManager._disableBlend();
      } else {
         GlStateManager._enableBlend();
         GlStateManager._blendFuncSeparate(override.srcRgb(), override.dstRgb(), override.srcAlpha(), override.dstAlpha());
      }

      blendLocked = true;
   }

   public static void overrideBufferBlend(int index, BlendMode override) {
      if (!blendLocked) {
         BlendFuncState blendState = GlStateManagerAccessor.getBLEND();
         originalBlendEnable = ((BooleanStateAccessor)blendState.capState).isEnabled();
         originalBlend = new BlendMode(blendState.srcFactorRGB, blendState.dstFactorRGB, blendState.srcFactorAlpha, blendState.dstFactorAlpha);
      }

      if (override == null) {
         IrisRenderSystem.disableBufferBlend(index);
      } else {
         IrisRenderSystem.enableBufferBlend(index);
         IrisRenderSystem.blendFuncSeparatei(index, override.srcRgb(), override.dstRgb(), override.srcAlpha(), override.dstAlpha());
      }

      blendLocked = true;
   }

   public static void deferBlendModeToggle(boolean enabled) {
      originalBlendEnable = enabled;
   }

   public static void deferBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
      originalBlend = new BlendMode(srcRgb, dstRgb, srcAlpha, dstAlpha);
   }

   public static void restoreBlend() {
      if (blendLocked) {
         blendLocked = false;
         if (originalBlendEnable) {
            GlStateManager._enableBlend();
         } else {
            GlStateManager._disableBlend();
         }

         GlStateManager._blendFuncSeparate(originalBlend.srcRgb(), originalBlend.dstRgb(), originalBlend.srcAlpha(), originalBlend.dstAlpha());
      }
   }
}
