package net.irisshaders.iris.layer;

import java.util.function.Function;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

public class BufferSourceWrapper implements VertexConsumerProvider, Groupable {
   private final VertexConsumerProvider bufferSource;
   private final Function<RenderLayer, RenderLayer> typeChanger;

   public BufferSourceWrapper(VertexConsumerProvider bufferSource, Function<RenderLayer, RenderLayer> typeChanger) {
      this.bufferSource = bufferSource;
      this.typeChanger = typeChanger;
   }

   public VertexConsumerProvider getOriginal() {
      return this.bufferSource;
   }

   @Override
   public void startGroup() {
      if (this.bufferSource instanceof Groupable groupable) {
         groupable.startGroup();
      }
   }

   @Override
   public boolean maybeStartGroup() {
      return this.bufferSource instanceof Groupable groupable ? groupable.maybeStartGroup() : false;
   }

   @Override
   public void endGroup() {
      if (this.bufferSource instanceof Groupable groupable) {
         groupable.endGroup();
      }
   }

   public VertexConsumer getBuffer(RenderLayer renderType) {
      return this.bufferSource.getBuffer(this.typeChanger.apply(renderType));
   }
}
