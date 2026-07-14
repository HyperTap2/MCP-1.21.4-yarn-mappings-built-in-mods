package com.viaversion.viafabricplus.screen;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class VFPScreen extends Screen {
   private static final String MOD_URL = "https://github.com/ViaVersion/ViaFabricPlus";
   private final boolean backButton;
   public Screen prevScreen;
   private Text subtitle;
   private PressAction subtitlePressAction;
   private PressableTextWidget subtitleWidget;

   public VFPScreen(String title, boolean backButton) {
      this(Text.of(title), backButton);
   }

   public VFPScreen(Text title, boolean backButton) {
      super(title);
      this.backButton = backButton;
   }

   public void setupDefaultSubtitle() {
      this.setupUrlSubtitle("https://github.com/ViaVersion/ViaFabricPlus");
   }

   public void setupUrlSubtitle(String subtitle) {
      this.setupSubtitle(Text.of(subtitle), ConfirmLinkScreen.opening(this, subtitle));
   }

   public void setupSubtitle(@Nullable Text subtitle) {
      this.setupSubtitle(subtitle, null);
   }

   public void setupSubtitle(@Nullable Text subtitle, @Nullable PressAction subtitlePressAction) {
      this.subtitle = subtitle;
      this.subtitlePressAction = subtitlePressAction;
      if (this.subtitleWidget != null) {
         this.remove(this.subtitleWidget);
         this.subtitleWidget = null;
      }

      if (subtitlePressAction != null) {
         int subtitleWidth = this.textRenderer.getWidth(subtitle);
         this.addDrawableChild(
            this.subtitleWidget = new PressableTextWidget(
               this.width / 2 - subtitleWidth / 2, (9 + 2) * 2 + 3, subtitleWidth, 9 + 2, subtitle, subtitlePressAction, this.textRenderer
            )
         );
      }
   }

   public void open(Screen prevScreen) {
      this.prevScreen = prevScreen;
      setScreen(this);
   }

   public static void setScreen(Screen screen) {
      MinecraftClient client = MinecraftClient.getInstance();
      client.execute(() -> client.setScreen(screen));
   }

   protected void init() {
      if (this.backButton) {
         this.addDrawableChild(ButtonWidget.builder(Text.of("<-"), button -> this.close()).position(5, 5).size(20, 20).build());
      }
   }

   public void addRefreshButton(Runnable click) {
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("base.viafabricplus.refresh"), button -> {
         click.run();
         this.client.setScreen(this);
      }).position(this.width - 60 - 5, 5).size(60, 20).build());
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.renderTitle(context);
   }

   public void close() {
      if (this.prevScreen instanceof VFPScreen vfpScreen) {
         vfpScreen.open(vfpScreen.prevScreen);
      } else {
         MinecraftClient.getInstance().setScreen(this.prevScreen);
      }
   }

   public void renderTitle(DrawContext context) {
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.scale(2.0F, 2.0F, 2.0F);
      context.drawCenteredTextWithShadow(this.textRenderer, "ViaFabricPlus", this.width / 4, 3, Color.ORANGE.getRGB());
      matrices.pop();
      this.renderSubtitle(context);
   }

   public void renderSubtitle(DrawContext context) {
      if (this.subtitle != null && this.subtitlePressAction == null) {
         int startY = (9 + 2) * 2 + 3;
         context.drawCenteredTextWithShadow(this.textRenderer, this.subtitle, this.width / 2, this.subtitleCentered() ? this.height / 2 - startY : startY, -1);
      }
   }

   protected boolean subtitleCentered() {
      return false;
   }

   public void renderScreenTitle(DrawContext context) {
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 70, 16777215);
   }

   public static void showErrorScreen(Text title, Throwable throwable, Screen next) {
      ViaFabricPlusImpl.INSTANCE.logger().error("Something went wrong!", throwable);
      MinecraftClient client = MinecraftClient.getInstance();
      client.execute(
         () -> client.setScreen(
            new NoticeScreen(
               () -> client.setScreen(next),
               title,
               Text.translatable("base.viafabricplus.something_went_wrong").append("\n" + throwable.getMessage()),
               Text.translatable("base.viafabricplus.cancel"),
               false
            )
         )
      );
   }
}
