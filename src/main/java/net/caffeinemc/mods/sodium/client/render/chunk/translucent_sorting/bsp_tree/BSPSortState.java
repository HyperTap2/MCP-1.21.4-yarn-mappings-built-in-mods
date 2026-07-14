package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.IntBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

class BSPSortState {
   static final int NO_FIXED_OFFSET = Integer.MIN_VALUE;
   private IntBuffer indexBuffer;
   private int indexModificationsRemaining;
   private int[] indexMap;
   private int fixedIndexOffset = Integer.MIN_VALUE;
   private static final int INDEX_COMPRESSION_MIN_LENGTH = 32;
   private static final int HEADER_LENGTH = 2;
   private static final int[] WIDTHS = new int[]{1, 2, 3, 4, 5, 6, 8, 10, 16, 32};
   private static final int CONSTANT_DELTA_WIDTH_INDEX = 15;
   private IntConsumer indexConsumer = index -> TranslucentData.writeQuadVertexIndexes(this.indexBuffer, index);
   private IntConsumer indexMapConsumer = index -> TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.indexMap[index]);

   BSPSortState(NativeBuffer nativeBuffer) {
      this.indexBuffer = nativeBuffer.getDirectBuffer().asIntBuffer();
   }

   void startNode(InnerPartitionBSPNode node) {
      if (node.indexMap != null) {
         if (this.indexMap != null || this.fixedIndexOffset != Integer.MIN_VALUE) {
            throw new IllegalStateException("Index modification already in progress");
         }

         this.indexMap = node.indexMap;
         this.indexModificationsRemaining = node.reuseData.indexCount();
      } else if (node.fixedIndexOffset != Integer.MIN_VALUE) {
         if (this.indexMap != null || this.fixedIndexOffset != Integer.MIN_VALUE) {
            throw new IllegalStateException("Index modification already in progress");
         }

         this.fixedIndexOffset = node.fixedIndexOffset;
         this.indexModificationsRemaining = node.reuseData.indexCount();
      }
   }

   private void checkModificationCounter(int reduceBy) {
      this.indexModificationsRemaining -= reduceBy;
      if (this.indexModificationsRemaining <= 0) {
         this.indexMap = null;
         this.fixedIndexOffset = Integer.MIN_VALUE;
      }
   }

   void writeIndex(int index) {
      if (this.indexMap != null) {
         TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.indexMap[index]);
         this.checkModificationCounter(1);
      } else if (this.fixedIndexOffset != Integer.MIN_VALUE) {
         TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.fixedIndexOffset + index);
         this.checkModificationCounter(1);
      } else {
         TranslucentData.writeQuadVertexIndexes(this.indexBuffer, index);
      }
   }

   private static int ceilDiv(int x, int y) {
      return -Math.floorDiv(-x, y);
   }

   private static boolean isOutOfBounds(int size) {
      return size < 32 || size > 1024;
   }

   static int[] compressIndexesInPlace(int[] indexes, boolean doSort) {
      return isOutOfBounds(indexes.length) ? indexes : compressIndexes(IntArrayList.wrap(indexes), doSort);
   }

   static int[] compressIndexes(IntArrayList indexes) {
      return compressIndexes(indexes, true);
   }

   static int[] compressIndexes(IntArrayList indexes, boolean doSort) {
      if (isOutOfBounds(indexes.size())) {
         return indexes.toIntArray();
      } else {
         IntArrayList workingList = new IntArrayList(indexes);
         if (doSort) {
            workingList.sort(null);
         }

         int last = workingList.getInt(0);
         int minDelta = Integer.MAX_VALUE;
         int maxDelta = 0;

         for (int i = 1; i < workingList.size(); i++) {
            int current = workingList.getInt(i);
            int delta = current - last;
            workingList.set(i, delta);
            last = current;
            if (delta < minDelta) {
               minDelta = delta;
            }

            if (delta > maxDelta) {
               maxDelta = delta;
            }
         }

         int deltaRangeWidth = 32 - Integer.numberOfLeadingZeros(maxDelta - minDelta);
         int firstIndex = workingList.getInt(0);
         if (firstIndex > 131072) {
            return indexes.toIntArray();
         } else {
            int deltaCount = workingList.size() - 1;
            if (deltaRangeWidth == 0) {
               return new int[]{-134217728 | deltaCount << 17 | firstIndex, minDelta};
            } else if (deltaRangeWidth > 16) {
               return indexes.toIntArray();
            } else {
               int widthIndex = 0;

               while (WIDTHS[widthIndex] < deltaRangeWidth) {
                  widthIndex++;
               }

               int width = WIDTHS[widthIndex];
               int countPerInt = WIDTHS[WIDTHS.length - widthIndex - 1];
               int size = 2 + ceilDiv(deltaCount, countPerInt);
               int[] compressed = new int[size];
               compressed[0] = -2147483648 | widthIndex << 27 | deltaCount << 17 | firstIndex;
               compressed[1] = minDelta;
               int positionLimit = 32 - width;
               int outputIndex = 2;
               int gatherInt = 0;
               int bitPosition = 0;

               for (int i = 1; i < workingList.size(); i++) {
                  int shiftedDelta = workingList.getInt(i) - minDelta;
                  gatherInt |= shiftedDelta << bitPosition;
                  bitPosition += width;
                  if (bitPosition > positionLimit) {
                     compressed[outputIndex++] = gatherInt;
                     gatherInt = 0;
                     bitPosition = 0;
                  }
               }

               if (bitPosition > 0) {
                  compressed[outputIndex++] = gatherInt;
               }

               return compressed;
            }
         }
      }
   }

   static int decompressOrRead(int[] indexes, IntConsumer consumer) {
      if (isCompressed(indexes)) {
         return decompress(indexes, consumer);
      } else {
         for (int i = 0; i < indexes.length; i++) {
            consumer.accept(indexes[i]);
         }

         return indexes.length;
      }
   }

   private static int decompress(int[] indexes, IntConsumer consumer) {
      return decompressWithOffset(indexes, 0, consumer);
   }

   private static int decompressWithOffset(int[] indexes, int fixedIndexOffset, IntConsumer consumer) {
      int header = indexes[0];
      int widthIndex = header >> 27 & 15;
      int currentValue = header & 131071 + fixedIndexOffset;
      int valueCount = (header >> 17 & 1023) + 1;
      int baseDelta = indexes[1];
      if (widthIndex == 15) {
         for (int i = 0; i < valueCount; i++) {
            consumer.accept(currentValue);
            currentValue += baseDelta;
         }

         return valueCount;
      } else {
         int width = WIDTHS[widthIndex];
         int mask = (1 << width) - 1;
         int positionLimit = 32 - width;
         int readIndex = 2;
         int splitInt = indexes[readIndex++];
         int splitIntBitPosition = 0;
         int totalValueCount = valueCount;

         while (valueCount-- > 0) {
            consumer.accept(currentValue);
            if (valueCount == 0) {
               break;
            }

            int delta = splitInt >> splitIntBitPosition & mask;
            splitIntBitPosition += width;
            if (splitIntBitPosition > positionLimit && valueCount > 1) {
               splitInt = indexes[readIndex++];
               splitIntBitPosition = 0;
            }

            currentValue += baseDelta + delta;
         }

         return totalValueCount;
      }
   }

   static boolean isCompressed(int[] indexes) {
      return indexes[0] < 0;
   }

   void writeIndexes(int[] indexes) {
      boolean useIndexMap = this.indexMap != null;
      boolean useFixedIndexOffset = this.fixedIndexOffset != Integer.MIN_VALUE;
      int valueCount;
      if (isCompressed(indexes)) {
         if (useFixedIndexOffset) {
            valueCount = decompressWithOffset(indexes, this.fixedIndexOffset, this.indexConsumer);
         } else {
            valueCount = decompress(indexes, useIndexMap ? this.indexMapConsumer : this.indexConsumer);
         }
      } else {
         if (useIndexMap) {
            for (int i = 0; i < indexes.length; i++) {
               TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.indexMap[indexes[i]]);
            }
         } else if (useFixedIndexOffset) {
            for (int i = 0; i < indexes.length; i++) {
               TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.fixedIndexOffset + indexes[i]);
            }
         } else {
            TranslucentData.writeQuadVertexIndexes(this.indexBuffer, indexes);
         }

         valueCount = indexes.length;
      }

      if (useIndexMap || useFixedIndexOffset) {
         this.checkModificationCounter(valueCount);
      }
   }
}
