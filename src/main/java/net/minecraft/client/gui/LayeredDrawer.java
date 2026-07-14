package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.render.RenderTickCounter;

public class LayeredDrawer {
   public static final float LAYER_Z_PADDING = 200.0F;
   private final List<LayeredDrawer.Layer> layers = new ArrayList<>();

   public LayeredDrawer addLayer(LayeredDrawer.Layer layer) {
      this.layers.add(layer);
      return this;
   }

   public LayeredDrawer addSubDrawer(LayeredDrawer drawer, BooleanSupplier shouldRender) {
      return this.addLayer((context, tickCounter) -> {
         if (shouldRender.getAsBoolean()) {
            drawer.renderInternal(context, tickCounter);
         }
      });
   }

   public void render(DrawContext context, RenderTickCounter tickCounter) {
      context.getMatrices().push();
      this.renderInternal(context, tickCounter);
      context.getMatrices().pop();
   }

   private void renderInternal(DrawContext context, RenderTickCounter tickCounter) {
      for (LayeredDrawer.Layer layer : this.layers) {
         layer.render(context, tickCounter);
         context.getMatrices().translate(0.0F, 0.0F, 200.0F);
      }
   }

   public interface Layer {
      void render(DrawContext context, RenderTickCounter tickCounter);
   }
}
