package net.caffeinemc.mods.sodium.client.render.frapi.mesh;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import org.jetbrains.annotations.Range;

public class MeshImpl implements Mesh {
   private static final ThreadLocal<ObjectArrayList<QuadViewImpl>> CURSOR_POOLS = ThreadLocal.withInitial(ObjectArrayList::new);
   int[] data;
   int limit;

   MeshImpl(int[] data) {
      this.data = data;
      this.limit = data.length;
   }

   MeshImpl() {
   }

   @Range(
      from = 0L,
      to = 2147483647L
   )
   public int size() {
      return this.limit / EncodingFormat.TOTAL_STRIDE;
   }

   public void forEach(Consumer<? super QuadView> action) {
      ObjectArrayList<QuadViewImpl> pool = CURSOR_POOLS.get();
      QuadViewImpl cursor;
      if (pool.isEmpty()) {
         cursor = new QuadViewImpl();
      } else {
         cursor = (QuadViewImpl)pool.pop();
      }

      this.forEach(action, cursor);
      pool.push(cursor);
   }

   <C extends QuadViewImpl> void forEach(Consumer<? super C> action, C cursor) {
      int limit = this.limit;
      int index = 0;

      for (cursor.data = this.data; index < limit; index += EncodingFormat.TOTAL_STRIDE) {
         cursor.baseIndex = index;
         cursor.load();
         action.accept(cursor);
      }

      cursor.data = null;
   }

   public void outputTo(QuadEmitter emitter) {
      MutableQuadViewImpl e = (MutableQuadViewImpl)emitter;
      int[] data = this.data;
      int limit = this.limit;

      for (int index = 0; index < limit; index += EncodingFormat.TOTAL_STRIDE) {
         System.arraycopy(data, index, e.data, e.baseIndex, EncodingFormat.TOTAL_STRIDE);
         e.load();
         e.transformAndEmit();
      }

      e.clear();
   }
}
