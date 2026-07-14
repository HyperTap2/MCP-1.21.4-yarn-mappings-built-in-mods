package net.irisshaders.batchedentityrendering.impl.ordering;

import java.util.LinkedHashSet;
import java.util.List;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.minecraft.client.render.RenderLayer;

public class SimpleRenderOrderManager implements RenderOrderManager {
   private final LinkedHashSet<RenderLayer> renderTypes = new LinkedHashSet<>();

   @Override
   public void begin(RenderLayer type) {
      this.renderTypes.add(type);
   }

   @Override
   public void startGroup() {
   }

   @Override
   public boolean maybeStartGroup() {
      return false;
   }

   @Override
   public boolean isInGroup() {
      return false;
   }

   @Override
   public void endGroup() {
   }

   @Override
   public void reset() {
      this.renderTypes.clear();
   }

   @Override
   public void resetType(TransparencyType type) {
   }

   @Override
   public List<RenderLayer> getRenderOrder() {
      return List.copyOf(this.renderTypes);
   }
}
