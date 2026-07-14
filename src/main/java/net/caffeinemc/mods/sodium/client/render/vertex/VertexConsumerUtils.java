package net.caffeinemc.mods.sodium.client.render.vertex;

import javax.annotation.Nullable;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.VertexConsumer;

public class VertexConsumerUtils {
   @Nullable
   public static VertexBufferWriter convertOrLog(VertexConsumer consumer) {
      VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);
      if (writer == null) {
         VertexConsumerTracker.logBadConsumer(consumer);
      }

      return writer;
   }
}
