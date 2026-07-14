package net.irisshaders.iris.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class OldImageButton extends ButtonWidget {
   protected final Identifier resourceLocation;
   protected final int xTexStart;
   protected final int yTexStart;
   protected final int yDiffTex;
   protected final int textureWidth;
   protected final int textureHeight;

   public OldImageButton(int pImageButton0, int pInt1, int pInt2, int pInt3, int pInt4, int pInt5, Identifier pResourceLocation6, PressAction pButton$OnPress7) {
      this(pImageButton0, pInt1, pInt2, pInt3, pInt4, pInt5, pInt3, pResourceLocation6, 256, 256, pButton$OnPress7);
   }

   public OldImageButton(
      int pImageButton0, int pInt1, int pInt2, int pInt3, int pInt4, int pInt5, int pInt6, Identifier pResourceLocation7, PressAction pButton$OnPress8
   ) {
      this(pImageButton0, pInt1, pInt2, pInt3, pInt4, pInt5, pInt6, pResourceLocation7, 256, 256, pButton$OnPress8);
   }

   public OldImageButton(
      int pImageButton0,
      int pInt1,
      int pInt2,
      int pInt3,
      int pInt4,
      int pInt5,
      int pInt6,
      Identifier pResourceLocation7,
      int pInt8,
      int pInt9,
      PressAction pButton$OnPress10
   ) {
      this(pImageButton0, pInt1, pInt2, pInt3, pInt4, pInt5, pInt6, pResourceLocation7, pInt8, pInt9, pButton$OnPress10, ScreenTexts.EMPTY);
   }

   public OldImageButton(
      int pImageButton0,
      int pInt1,
      int pInt2,
      int pInt3,
      int pInt4,
      int pInt5,
      int pInt6,
      Identifier pResourceLocation7,
      int pInt8,
      int pInt9,
      PressAction pButton$OnPress10,
      Text pComponent11
   ) {
      super(pImageButton0, pInt1, pInt2, pInt3, pComponent11, pButton$OnPress10, DEFAULT_NARRATION_SUPPLIER);
      this.textureWidth = pInt8;
      this.textureHeight = pInt9;
      this.xTexStart = pInt4;
      this.yTexStart = pInt5;
      this.yDiffTex = pInt6;
      this.resourceLocation = pResourceLocation7;
   }

   public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderTexture(
         context,
         this.resourceLocation,
         this.getX(),
         this.getY(),
         this.xTexStart,
         this.yTexStart,
         this.yDiffTex,
         this.width,
         this.height,
         this.textureWidth,
         this.textureHeight
      );
   }

   public void renderTexture(
      DrawContext pAbstractWidget0,
      Identifier pResourceLocation1,
      int pInt2,
      int pInt3,
      int pInt4,
      int pInt5,
      int pInt6,
      int pInt7,
      int pInt8,
      int pInt9,
      int pInt10
   ) {
      int lvInt12 = pInt5;
      if (!this.isNarratable()) {
         lvInt12 = pInt5 + pInt6 * 2;
      } else if (this.isSelected()) {
         lvInt12 = pInt5 + pInt6;
      }

      RenderSystem.enableDepthTest();
      pAbstractWidget0.drawTexture(RenderLayer::getGuiTextured, pResourceLocation1, pInt2, pInt3, pInt4, lvInt12, pInt7, pInt8, pInt9, pInt10);
   }
}
