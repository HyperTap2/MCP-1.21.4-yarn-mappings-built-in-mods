package net.irisshaders.iris.layer;

import java.util.function.Function;
import net.minecraft.client.render.RenderLayer;

public interface WrappingMultiBufferSource {
   void pushWrappingFunction(Function<RenderLayer, RenderLayer> var1);

   void popWrappingFunction();

   void assertWrapStackEmpty();
}
