package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.client.render.BufferRenderer;

public class BufferSegmentRenderer {
   public void draw(BufferSegment segment) {
      if (segment.meshData() != null) {
         segment.type().startDrawing();
         this.drawInner(segment);
         segment.type().endDrawing();
      }
   }

   public void drawInner(BufferSegment segment) {
      BufferRenderer.drawWithGlobalProgram(segment.meshData());
   }
}
