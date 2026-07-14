package net.irisshaders.iris.gui.element.widget;

import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.Selectable.SelectionType;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractElementWidget<T extends OptionMenuElement> implements Element, Selectable {
   public static final AbstractElementWidget<OptionMenuElement> EMPTY = new AbstractElementWidget<OptionMenuElement>(null) {
      @Override
      public void render(DrawContext guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
      }

      @Nullable
      @Override
      public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
         return null;
      }

      @NotNull
      @Override
      public ScreenRect getNavigationFocus() {
         return ScreenRect.empty();
      }
   };
   protected final T element;
   public ScreenRect bounds = ScreenRect.empty();
   private boolean focused;

   public AbstractElementWidget(T element) {
      this.element = element;
   }

   public void init(ShaderPackScreen screen, NavigationController navigation) {
   }

   public abstract void render(DrawContext var1, int var2, int var3, float var4, boolean var5);

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return false;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void setFocused(boolean focused) {
      this.focused = focused;
   }

   @Nullable
   public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
      return !this.isFocused() ? GuiNavigationPath.of(this) : null;
   }

   public ScreenRect getNavigationFocus() {
      return this.bounds;
   }

   public SelectionType getType() {
      return SelectionType.NONE;
   }

   public void appendNarrations(NarrationMessageBuilder builder) {
   }
}
