package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.mixinterface.AbstractTextureExtended;
import net.irisshaders.iris.pbr.TextureTracker;
import net.minecraft.util.TriState;

public abstract class AbstractTexture implements AutoCloseable, AbstractTextureExtended {
   public static final int DEFAULT_ID = -1;
   protected int glId = -1;
   protected boolean bilinear;
   private int wrapS = 10497;
   private int wrapT = 10497;
   private int minFilter = 9986;
   private int magFilter = 9729;

   public void setClamp(boolean clamp) {
      RenderSystem.assertOnRenderThreadOrInit();
      int i;
      int j;
      if (clamp) {
         i = 33071;
         j = 33071;
      } else {
         i = 10497;
         j = 10497;
      }

      boolean bl = this.wrapS != i;
      boolean bl2 = this.wrapT != j;
      if (bl || bl2) {
         this.bindTexture();
         if (bl) {
            GlStateManager._texParameter(3553, 10242, i);
            this.wrapS = i;
         }

         if (bl2) {
            GlStateManager._texParameter(3553, 10243, j);
            this.wrapT = j;
         }
      }
   }

   public void setFilter(TriState bilinear, boolean mipmap) {
      this.setFilter(bilinear.asBoolean(this.bilinear), mipmap);
   }

   public void setFilter(boolean bilinear, boolean mipmap) {
      RenderSystem.assertOnRenderThreadOrInit();
      int i;
      int j;
      if (bilinear) {
         i = mipmap ? 9987 : 9729;
         j = 9729;
      } else {
         i = mipmap ? 9986 : 9728;
         j = 9728;
      }

      boolean bl = this.minFilter != i;
      boolean bl2 = this.magFilter != j;
      if (bl2 || bl) {
         this.bindTexture();
         if (bl) {
            GlStateManager._texParameter(3553, 10241, i);
            this.minFilter = i;
         }

         if (bl2) {
            GlStateManager._texParameter(3553, 10240, j);
            this.magFilter = j;
         }
      }
   }

   public int getGlId() {
      RenderSystem.assertOnRenderThreadOrInit();
      if (this.glId == -1) {
         this.glId = TextureUtil.generateTextureId();
         TextureTracker.INSTANCE.trackTexture(this.glId, this);
      }

      return this.glId;
   }

   @Override
   public void setNearestFilter() {
      RenderSystem.assertOnRenderThreadOrInit();
      boolean mipmap = this.minFilter >= 9984;
      int min = mipmap ? 9984 : 9728;
      int mag = 9728;
      boolean minChanged = this.minFilter != min;
      boolean magChanged = this.magFilter != mag;
      if (minChanged || magChanged) {
         this.bindTexture();
         if (minChanged) {
            GlStateManager._texParameter(3553, 10241, min);
            this.minFilter = min;
         }

         if (magChanged) {
            GlStateManager._texParameter(3553, 10240, mag);
            this.magFilter = mag;
         }
      }
   }

   public void clearGlId() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            if (this.glId != -1) {
               TextureUtil.releaseTextureId(this.glId);
               this.glId = -1;
            }
         });
      } else if (this.glId != -1) {
         TextureUtil.releaseTextureId(this.glId);
         this.glId = -1;
      }
   }

   public void bindTexture() {
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> GlStateManager._bindTexture(this.getGlId()));
      } else {
         GlStateManager._bindTexture(this.getGlId());
      }
   }

   @Override
   public void close() {
   }
}
