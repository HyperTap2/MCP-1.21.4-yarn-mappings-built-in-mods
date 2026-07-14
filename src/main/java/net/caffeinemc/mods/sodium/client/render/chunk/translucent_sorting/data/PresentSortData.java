package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public interface PresentSortData {
   NativeBuffer getIndexBuffer();

   default IntBuffer getIntBuffer() {
      return this.getIndexBuffer().getDirectBuffer().asIntBuffer();
   }
}
