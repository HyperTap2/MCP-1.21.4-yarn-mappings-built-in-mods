package net.irisshaders.iris.gui.element.widget;

import java.util.List;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.StringOption;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuStringOptionElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Language;

public class StringElementWidget extends BaseOptionElementWidget<OptionMenuStringOptionElement> {
   protected final StringOption option;
   protected String appliedValue;
   protected int valueCount;
   protected int valueIndex;
   protected MutableText prefix;
   protected MutableText suffix;

   public StringElementWidget(OptionMenuStringOptionElement element) {
      super(element);
      this.option = element.option;
   }

   @Override
   public void init(ShaderPackScreen screen, NavigationController navigation) {
      super.init(screen, navigation);
      String actualPendingValue = this.element.getPendingOptionValues().getStringValueOrDefault(this.option.getName());
      this.appliedValue = this.element.getAppliedOptionValues().getStringValueOrDefault(this.option.getName());
      this.prefix = Text.literal(
         Language.getInstance().hasTranslation("prefix." + this.option.getName()) ? Language.getInstance().get("prefix." + this.option.getName()) : ""
      );
      this.suffix = Text.literal(
         Language.getInstance().hasTranslation("suffix." + this.option.getName()) ? Language.getInstance().get("suffix." + this.option.getName()) : ""
      );
      this.setLabel(GuiUtil.translateOrDefault(Text.literal(this.option.getName()), "option." + this.option.getName()));
      List<String> values = this.option.getAllowedValues();
      this.valueCount = values.size();
      this.valueIndex = values.indexOf(actualPendingValue);
   }

   @Override
   public void render(DrawContext guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
      this.updateRenderParams(0);
      this.renderOptionWithValue(guiGraphics, hovered || this.isFocused());
      if (this.usedKeyboard) {
         this.tryRenderTooltip(guiGraphics, this.bounds.getBoundingCoordinate(NavigationDirection.RIGHT), this.bounds.position().y(), hovered);
      } else {
         this.tryRenderTooltip(guiGraphics, mouseX, mouseY, hovered);
      }
   }

   private void increment(int amount) {
      this.valueIndex = Math.max(this.valueIndex, 0);
      this.valueIndex = Math.floorMod(this.valueIndex + amount, this.valueCount);
   }

   @Override
   protected Text createValueLabel() {
      return this.prefix
         .copy()
         .append(GuiUtil.translateOrDefault(Text.literal(this.getValue()).append(this.suffix), "value." + this.option.getName() + "." + this.getValue()))
         .styled(style -> style.withColor(TextColor.fromRgb(6719743)));
   }

   @Override
   public String getCommentKey() {
      return "option." + this.option.getName() + ".comment";
   }

   public String getValue() {
      return this.valueIndex < 0 ? this.appliedValue : (String)this.option.getAllowedValues().get(this.valueIndex);
   }

   protected void queue() {
      Iris.getShaderPackOptionQueue().put(this.option.getName(), this.getValue());
   }

   @Override
   public boolean applyNextValue() {
      this.increment(1);
      this.queue();
      return true;
   }

   @Override
   public boolean applyPreviousValue() {
      this.increment(-1);
      this.queue();
      return true;
   }

   @Override
   public boolean applyOriginalValue() {
      this.valueIndex = this.option.getAllowedValues().indexOf(this.option.getDefaultValue());
      this.queue();
      return true;
   }

   @Override
   public boolean isValueModified() {
      return !this.appliedValue.equals(this.getValue());
   }
}
