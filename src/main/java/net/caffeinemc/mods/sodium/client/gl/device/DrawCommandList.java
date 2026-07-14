package net.caffeinemc.mods.sodium.client.gl.device;

import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType;

public interface DrawCommandList extends AutoCloseable {
   void multiDrawElementsBaseVertex(MultiDrawBatch var1, GlIndexType var2);

   void endTessellating();

   void flush();

   @Override
   default void close() {
      this.flush();
   }
}
