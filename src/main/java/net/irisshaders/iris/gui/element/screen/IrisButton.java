package net.irisshaders.iris.gui.element.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.uniform.FloatSupplier;
import net.irisshaders.iris.gui.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.NarrationSupplier;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class IrisButton extends ButtonWidget {
   private final FloatSupplier alphaSupplier;

   public IrisButton(
      int pButton0,
      int pInt1,
      int pInt2,
      int pInt3,
      Text pComponent4,
      PressAction pButton$OnPress5,
      NarrationSupplier pButton$CreateNarration6,
      FloatSupplier alpha
   ) {
      super(pButton0, pInt1, pInt2, pInt3, pComponent4, pButton$OnPress5, pButton$CreateNarration6);
      this.alphaSupplier = alpha;
   }

   public static IrisButton.Builder iris$builder(Text pComponent0, PressAction pButton$OnPress1, FloatSupplier alpha) {
      return new IrisButton.Builder(pComponent0, pButton$OnPress1, alpha);
   }

   protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      MinecraftClient lvMinecraft5 = MinecraftClient.getInstance();
      context.draw();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.isSelected() ? this.alphaSupplier.getAsFloat() * 1.8F : this.alphaSupplier.getAsFloat());
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      GuiUtil.bindIrisWidgetsTexture();
      GuiUtil.drawButton(context, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isSelected(), !this.isNarratable());
      context.draw();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alphaSupplier.getAsFloat());
      int lvInt6 = this.active ? 16777215 : 10526880;
      this.drawMessage(context, lvMinecraft5.textRenderer, lvInt6 | MathHelper.ceil(this.alphaSupplier.getAsFloat() * 255.0F) << 24);
      context.draw();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static class Builder {
      private final Text message;
      private final PressAction onPress;
      private final FloatSupplier alpha;
      @Nullable
      private Tooltip tooltip;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private NarrationSupplier createNarration = IrisButton.DEFAULT_NARRATION_SUPPLIER;

      public Builder(Text pButton$Builder0, PressAction pButton$OnPress1, FloatSupplier alpha) {
         this.message = pButton$Builder0;
         this.onPress = pButton$OnPress1;
         this.alpha = alpha;
      }

      public IrisButton.Builder pos(int pButton$Builder0, int pInt1) {
         this.x = pButton$Builder0;
         this.y = pInt1;
         return this;
      }

      public IrisButton.Builder width(int pButton$Builder0) {
         this.width = pButton$Builder0;
         return this;
      }

      public IrisButton.Builder size(int pButton$Builder0, int pInt1) {
         this.width = pButton$Builder0;
         this.height = pInt1;
         return this;
      }

      public IrisButton.Builder bounds(int pButton$Builder0, int pInt1, int pInt2, int pInt3) {
         return this.pos(pButton$Builder0, pInt1).size(pInt2, pInt3);
      }

      public IrisButton.Builder tooltip(@Nullable Tooltip pButton$Builder0) {
         this.tooltip = pButton$Builder0;
         return this;
      }

      public IrisButton.Builder createNarration(NarrationSupplier pButton$Builder0) {
         this.createNarration = pButton$Builder0;
         return this;
      }

      public IrisButton build() {
         IrisButton lvButton1 = new IrisButton(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration, this.alpha);
         lvButton1.setTooltip(this.tooltip);
         lvButton1.active = true;
         return lvButton1;
      }
   }
}
