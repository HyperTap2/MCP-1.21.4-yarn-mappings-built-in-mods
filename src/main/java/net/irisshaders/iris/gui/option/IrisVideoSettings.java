package net.irisshaders.iris.gui.option;

import java.io.IOException;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.SimpleOption.ValidatingIntSliderCallbacks;
import net.minecraft.text.Text;

public class IrisVideoSettings {
   private static final Tooltip DISABLED_TOOLTIP = Tooltip.of(Text.translatable("options.iris.shadowDistance.disabled"));
   private static final Tooltip ENABLED_TOOLTIP = Tooltip.of(Text.translatable("options.iris.shadowDistance.enabled"));
   public static int shadowDistance = 32;
   public static ColorSpace colorSpace = ColorSpace.SRGB;
   public static final SimpleOption<Integer> RENDER_DISTANCE = new ShadowDistanceOption<>(
      "options.iris.shadowDistance",
      mc -> {
         WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
         Tooltip tooltip;
         if (pipeline != null) {
            if (pipeline.getForcedShadowRenderDistanceChunksForDisplay().isPresent()) {
               tooltip = DISABLED_TOOLTIP;
            } else {
               tooltip = ENABLED_TOOLTIP;
            }
         } else {
            tooltip = ENABLED_TOOLTIP;
         }

         return tooltip;
      },
      (arg, d) -> {
         WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
         if (pipeline != null) {
            d = pipeline.getForcedShadowRenderDistanceChunksForDisplay().orElse(d);
         }

         return d.intValue() <= 0.0
            ? Text.translatable("options.generic_value", new Object[]{Text.translatable("options.iris.shadowDistance"), "0 (disabled)"})
            : Text.translatable(
               "options.generic_value", new Object[]{Text.translatable("options.iris.shadowDistance"), Text.translatable("options.chunks", new Object[]{d})}
            );
      },
      new ValidatingIntSliderCallbacks(0, 32),
      getOverriddenShadowDistance(shadowDistance),
      integer -> {
         shadowDistance = integer;

         try {
            Iris.getIrisConfig().save();
         } catch (IOException e) {
            Iris.logger.fatal("Failed to save config!", e);
         }
      }
   );

   public static int getOverriddenShadowDistance(int base) {
      return Iris.getPipelineManager().getPipeline().map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().orElse(base)).orElse(base);
   }

   public static boolean isShadowDistanceSliderEnabled() {
      return Iris.getPipelineManager().getPipeline().map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().isEmpty()).orElse(true);
   }
}
