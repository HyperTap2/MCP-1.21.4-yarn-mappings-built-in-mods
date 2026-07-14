package net.irisshaders.batchedentityrendering.impl.ordering;

import java.util.List;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.minecraft.client.render.RenderLayer;

public interface RenderOrderManager {
   void begin(RenderLayer var1);

   void startGroup();

   boolean maybeStartGroup();

   boolean isInGroup();

   void endGroup();

   void reset();

   void resetType(TransparencyType var1);

   List<RenderLayer> getRenderOrder();
}
