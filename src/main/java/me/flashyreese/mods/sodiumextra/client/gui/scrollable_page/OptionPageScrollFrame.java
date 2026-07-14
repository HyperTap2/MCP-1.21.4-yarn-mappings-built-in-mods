package me.flashyreese.mods.sodiumextra.client.gui.scrollable_page;

import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.List;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jetbrains.annotations.NotNull;

public class OptionPageScrollFrame extends AbstractFrame {
   protected final OptionPage page;
   private boolean canScroll;
   private ScrollBarComponent scrollBar = null;

   public OptionPageScrollFrame(Dim2i dim, OptionPage page) {
      super(dim);
      this.page = page;
      this.setupFrame();
      this.buildFrame();
   }

   public void setupFrame() {
      this.children.clear();
      this.controlElements.clear();
      int y = 0;
      if (!this.page.getGroups().isEmpty()) {
         OptionGroup lastGroup = (OptionGroup)this.page.getGroups().get(this.page.getGroups().size() - 1);
         UnmodifiableIterator var3 = this.page.getGroups().iterator();

         while (var3.hasNext()) {
            OptionGroup group = (OptionGroup)var3.next();
            y += group.getOptions().size() * 18;
            if (group != lastGroup) {
               y += 4;
            }
         }
      }

      this.canScroll = this.dim.height() < y;
      if (this.canScroll) {
         this.scrollBar = new ScrollBarComponent(
            new Dim2i(this.dim.getLimitX() - 10, this.dim.y(), 10, this.dim.height()), y, this.dim.height(), this::buildFrame, this.dim
         );
      }
   }

   @Override
   public void buildFrame() {
      if (this.page != null) {
         this.children.clear();
         this.controlElements.clear();
         int y = 0;

         for (UnmodifiableIterator var2 = this.page.getGroups().iterator(); var2.hasNext(); y += 4) {
            OptionGroup group = (OptionGroup)var2.next();

            for (UnmodifiableIterator var4 = group.getOptions().iterator(); var4.hasNext(); y += 18) {
               Option<?> option = (Option<?>)var4.next();
               Control<?> control = option.getControl();
               ControlElement<?> element = control.createElement(
                  new Dim2i(
                     this.dim.x(), this.dim.y() + y - (this.canScroll ? this.scrollBar.getOffset() : 0), this.dim.width() - (this.canScroll ? 11 : 0), 18
                  )
               );
               this.children.add(element);
            }
         }

         if (this.canScroll) {
            this.scrollBar.updateThumbPosition();
         }

         super.buildFrame();
      }
   }

   @Override
   public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
      ControlElement<?> hoveredElement = this.controlElements
         .stream()
         .filter(AbstractWidget::isHovered)
         .findFirst()
         .orElse(this.controlElements.stream().filter(AbstractWidget::isFocused).findFirst().orElse(null));
      this.applyScissor(context, this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height(), () -> super.render(context, mouseX, mouseY, delta));
      if (this.canScroll) {
         this.scrollBar.render(context, mouseX, mouseY, delta);
      }

      if (this.dim.containsCursor(mouseX, mouseY) && hoveredElement != null) {
         this.renderOptionTooltip(context, hoveredElement);
      }
   }

   private void renderOptionTooltip(DrawContext guiGraphics, ControlElement<?> element) {
      Dim2i dim = element.getDimensions();
      int textPadding = 3;
      int boxPadding = 3;
      int boxWidth = 240;
      int boxY = Math.max(dim.y(), this.dim.y());
      int boxX = this.dim.getLimitX() + boxPadding;
      Option<?> option = element.getOption();
      List<OrderedText> tooltip = new ArrayList<>(MinecraftClient.getInstance().textRenderer.wrapLines(option.getTooltip(), boxWidth - textPadding * 2));
      OptionImpact impact = option.getImpact();
      if (impact != null) {
         tooltip.add(
            Language.getInstance()
               .reorder(Text.translatable("sodium.options.performance_impact_string", new Object[]{impact.getLocalizedName()}).formatted(Formatting.GRAY))
         );
      }

      int boxHeight = tooltip.size() * 12 + boxPadding;
      int boxYLimit = boxY + boxHeight;
      int boxYCutoff = this.dim.getLimitY() - 25;
      if (boxYLimit > boxYCutoff) {
         boxY -= boxYLimit - boxYCutoff;
      }

      this.drawRect(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, -536870912);

      for (int i = 0; i < tooltip.size(); i++) {
         guiGraphics.drawText(MinecraftClient.getInstance().textRenderer, tooltip.get(i), boxX + textPadding, boxY + textPadding + i * 12, -1, false);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.dim.containsCursor(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         return this.canScroll ? this.scrollBar.mouseClicked(mouseX, mouseY, button) : false;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
         return true;
      } else {
         return this.canScroll ? this.scrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) : false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (super.mouseReleased(mouseX, mouseY, button)) {
         return true;
      } else {
         return this.canScroll ? this.scrollBar.mouseReleased(mouseX, mouseY, button) : false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
         return true;
      } else {
         return this.canScroll ? this.scrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) : false;
      }
   }
}
