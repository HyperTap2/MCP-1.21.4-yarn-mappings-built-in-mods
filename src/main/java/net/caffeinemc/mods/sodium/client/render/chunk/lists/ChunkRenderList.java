package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteArrayIterator;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.caffeinemc.mods.sodium.client.util.iterator.ReversibleByteArrayIterator;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class ChunkRenderList {
   private final RenderRegion region;
   private final byte[] sectionsWithGeometry = new byte[256];
   private int sectionsWithGeometryCount = 0;
   private final byte[] sectionsWithSprites = new byte[256];
   private int sectionsWithSpritesCount = 0;
   private final byte[] sectionsWithEntities = new byte[256];
   private int sectionsWithEntitiesCount = 0;
   private int size;
   private int lastVisibleFrame;
   private static final int SORTING_HISTOGRAM_SIZE = 18;

   public ChunkRenderList(RenderRegion region) {
      this.region = region;
   }

   public void reset(int frame) {
      this.sectionsWithGeometryCount = 0;
      this.sectionsWithSpritesCount = 0;
      this.sectionsWithEntitiesCount = 0;
      this.size = 0;
      this.lastVisibleFrame = frame;
   }

   public void sortSections(ChunkSectionPos cameraPos, int[] sortItems) {
      int cameraX = MathHelper.clamp(cameraPos.getX() - this.region.getChunkX(), 0, 7);
      int cameraY = MathHelper.clamp(cameraPos.getY() - this.region.getChunkY(), 0, 3);
      int cameraZ = MathHelper.clamp(cameraPos.getZ() - this.region.getChunkZ(), 0, 7);
      int[] histogram = new int[18];

      for (int i = 0; i < this.sectionsWithGeometryCount; i++) {
         int index = this.sectionsWithGeometry[i] & 255;
         int x = Math.abs(LocalSectionIndex.unpackX(index) - cameraX);
         int y = Math.abs(LocalSectionIndex.unpackY(index) - cameraY);
         int z = Math.abs(LocalSectionIndex.unpackZ(index) - cameraZ);
         int distance = x + y + z;
         histogram[distance]++;
         sortItems[i] = distance << 8 | index;
      }

      for (int i = 1; i < 18; i++) {
         histogram[i] += histogram[i - 1];
      }

      for (int i = 0; i < this.sectionsWithGeometryCount; i++) {
         int item = sortItems[i];
         int distance = item >>> 8;
         this.sectionsWithGeometry[--histogram[distance]] = (byte)item;
      }
   }

   public void add(RenderSection render) {
      if (this.size >= 256) {
         throw new ArrayIndexOutOfBoundsException("Render list is full");
      } else {
         this.size++;
         int index = render.getSectionIndex();
         int flags = render.getFlags();
         this.sectionsWithGeometry[this.sectionsWithGeometryCount] = (byte)index;
         this.sectionsWithGeometryCount += flags >>> 0 & 1;
         this.sectionsWithSprites[this.sectionsWithSpritesCount] = (byte)index;
         this.sectionsWithSpritesCount += flags >>> 2 & 1;
         this.sectionsWithEntities[this.sectionsWithEntitiesCount] = (byte)index;
         this.sectionsWithEntitiesCount += flags >>> 1 & 1;
      }
   }

   @Nullable
   public ByteIterator sectionsWithGeometryIterator(boolean reverse) {
      return this.sectionsWithGeometryCount == 0 ? null : new ReversibleByteArrayIterator(this.sectionsWithGeometry, this.sectionsWithGeometryCount, reverse);
   }

   @Nullable
   public ByteIterator sectionsWithSpritesIterator() {
      return this.sectionsWithSpritesCount == 0 ? null : new ByteArrayIterator(this.sectionsWithSprites, this.sectionsWithSpritesCount);
   }

   @Nullable
   public ByteIterator sectionsWithEntitiesIterator() {
      return this.sectionsWithEntitiesCount == 0 ? null : new ByteArrayIterator(this.sectionsWithEntities, this.sectionsWithEntitiesCount);
   }

   public int getSectionsWithGeometryCount() {
      return this.sectionsWithGeometryCount;
   }

   public int getSectionsWithSpritesCount() {
      return this.sectionsWithSpritesCount;
   }

   public int getSectionsWithEntitiesCount() {
      return this.sectionsWithEntitiesCount;
   }

   public int getLastVisibleFrame() {
      return this.lastVisibleFrame;
   }

   public RenderRegion getRegion() {
      return this.region;
   }

   public int size() {
      return this.size;
   }
}
