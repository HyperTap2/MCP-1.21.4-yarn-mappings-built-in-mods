package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat.DrawMode;

public class RenderTypeUtil {
   public static boolean isTriangleStripDrawMode(RenderLayer renderType) {
      return renderType.getDrawMode() == DrawMode.TRIANGLE_STRIP;
   }
}
