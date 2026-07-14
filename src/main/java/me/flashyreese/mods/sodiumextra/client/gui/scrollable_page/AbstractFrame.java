package me.flashyreese.mods.sodiumextra.client.gui.scrollable_page;

import java.util.ArrayList;
import java.util.List;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFrame extends AbstractWidget implements ParentElement {
   protected final Dim2i dim;
   protected final List<AbstractWidget> children = new ArrayList<>();
   protected final List<ControlElement<?>> controlElements = new ArrayList<>();
   private Element focused;
   private boolean dragging;

   public AbstractFrame(Dim2i dim) {
      this.dim = dim;
   }

   public void buildFrame() {
      for (Element element : this.children) {
         if (element instanceof AbstractFrame abstractFrame) {
            this.controlElements.addAll(abstractFrame.controlElements);
         }

         if (element instanceof ControlElement) {
            this.controlElements.add((ControlElement<?>)element);
         }
      }
   }

   public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
      for (Drawable renderable : this.children) {
         renderable.render(context, mouseX, mouseY, delta);
      }
   }

   public void applyScissor(@NotNull DrawContext guiGraphics, int x, int y, int width, int height, Runnable action) {
      guiGraphics.enableScissor(x, y, x + width, y + height);
      action.run();
      guiGraphics.disableScissor();
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   @Nullable
   public Element getFocused() {
      return this.focused;
   }

   public void setFocused(@Nullable Element focused) {
      this.focused = focused;
   }

   public List<? extends Element> children() {
      return this.children;
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.dim.containsCursor(mouseX, mouseY);
   }

   @Nullable
   public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
      return super.getNavigationPath(navigation);
   }

   public ScreenRect getNavigationFocus() {
      return new ScreenRect(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
   }
}
