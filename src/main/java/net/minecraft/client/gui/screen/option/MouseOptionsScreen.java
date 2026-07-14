package net.minecraft.client.gui.screen.option;

import com.viaversion.viafabricplus.features.mouse_sensitivity.MouseSensitivity1_13_2;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class MouseOptionsScreen extends GameOptionsScreen {
   private static final Text TITLE = Text.translatable("options.mouse_settings.title");

   private static SimpleOption<?>[] getOptions(GameOptions gameOptions) {
      return new SimpleOption[]{
         gameOptions.getMouseSensitivity(),
         gameOptions.getInvertYMouse(),
         gameOptions.getMouseWheelSensitivity(),
         gameOptions.getDiscreteMouseScroll(),
         gameOptions.getTouchscreen()
      };
   }

   public MouseOptionsScreen(Screen parent, GameOptions gameOptions) {
      super(parent, gameOptions, TITLE);
   }

   @Override
   protected void addOptions() {
      if (InputUtil.isRawMouseMotionSupported()) {
         this.body
            .addAll(Stream.concat(Arrays.stream(getOptions(this.gameOptions)), Stream.of(this.gameOptions.getRawMouseInput())).toArray(SimpleOption[]::new));
      } else {
         this.body.addAll(getOptions(this.gameOptions));
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)
         && this.body.getWidgetFor(this.gameOptions.getMouseSensitivity()).isHovered()) {
         int sensitivity = MouseSensitivity1_13_2
            .get1_13SliderValue(this.gameOptions.getMouseSensitivity().getValue().floatValue())
            .valueInt();
         context.drawTooltip(this.textRenderer, Text.of("<=1.13.2 Sensitivity: " + sensitivity + "%"), mouseX, mouseY);
      }
   }
}
