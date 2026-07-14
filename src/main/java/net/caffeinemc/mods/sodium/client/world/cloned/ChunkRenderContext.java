package net.caffeinemc.mods.sodium.client.world.cloned;

import java.util.List;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderContext {
   private final ChunkSectionPos origin;
   private final ClonedChunkSection[] sections;
   private final BlockBox volume;
   private final List<?> renderers;

   public ChunkRenderContext(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume, List<?> renderers) {
      this.origin = origin;
      this.sections = sections;
      this.volume = volume;
      this.renderers = renderers;
   }

   public ClonedChunkSection[] getSections() {
      return this.sections;
   }

   public ChunkSectionPos getOrigin() {
      return this.origin;
   }

   public BlockBox getVolume() {
      return this.volume;
   }

   public List<?> getRenderers() {
      return this.renderers;
   }
}
