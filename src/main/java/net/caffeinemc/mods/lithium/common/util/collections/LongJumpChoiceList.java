package net.caffeinemc.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.bytes.ByteBytePair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.ai.brain.task.LongJumpTask.Target;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class LongJumpChoiceList extends AbstractList<Target> {
   private static final ConcurrentHashMap<ByteBytePair, LongJumpChoiceList> CHOICE_LISTS = new ConcurrentHashMap<>();
   private static final LongJumpChoiceList FROG_JUMP = new LongJumpChoiceList((byte)4, (byte)2);
   private static final LongJumpChoiceList GOAT_JUMP = new LongJumpChoiceList((byte)5, (byte)5);
   private final BlockPos origin;
   private final IntArrayList[] packedOffsetsByDistanceSq;
   private final int[] weightByDistanceSq;
   private int totalWeight;

   private LongJumpChoiceList(byte horizontalRange, byte verticalRange) {
      if (horizontalRange < 0 || verticalRange < 0) {
         throw new IllegalArgumentException("The ranges must be within 0..127");
      }
      this.origin = BlockPos.ORIGIN;
      int maxSquaredDistance = horizontalRange * horizontalRange * 2 + verticalRange * verticalRange;
      this.packedOffsetsByDistanceSq = new IntArrayList[maxSquaredDistance];
      this.weightByDistanceSq = new int[maxSquaredDistance];

      for (int x = -horizontalRange; x <= horizontalRange; x++) {
         for (int y = -verticalRange; y <= verticalRange; y++) {
            for (int z = -horizontalRange; z <= horizontalRange; z++) {
               int squaredDistance = x * x + y * y + z * z;
               if (squaredDistance == 0) {
                  continue;
               }
               int index = squaredDistance - 1;
               IntArrayList offsets = this.packedOffsetsByDistanceSq[index];
               if (offsets == null) {
                  offsets = new IntArrayList();
                  this.packedOffsetsByDistanceSq[index] = offsets;
               }
               offsets.add(packOffset(x, y, z));
               this.weightByDistanceSq[index] += squaredDistance;
               this.totalWeight += squaredDistance;
            }
         }
      }
   }

   private LongJumpChoiceList(BlockPos origin, IntArrayList[] offsets, int[] weights, int totalWeight) {
      this.origin = origin;
      this.packedOffsetsByDistanceSq = offsets;
      this.weightByDistanceSq = weights;
      this.totalWeight = totalWeight;
   }

   public static LongJumpChoiceList forCenter(BlockPos center, byte horizontalRange, byte verticalRange) {
      if (horizontalRange < 0 || verticalRange < 0) {
         throw new IllegalArgumentException("The ranges must be within 0..127");
      }
      int packedRange = horizontalRange << 8 | verticalRange;
      LongJumpChoiceList template = packedRange == (4 << 8 | 2)
         ? FROG_JUMP
         : packedRange == (5 << 8 | 5)
            ? GOAT_JUMP
            : CHOICE_LISTS.computeIfAbsent(ByteBytePair.of(horizontalRange, verticalRange), pair -> new LongJumpChoiceList(pair.leftByte(), pair.rightByte()));
      return template.offsetCopy(center);
   }

   private LongJumpChoiceList offsetCopy(BlockPos offset) {
      IntArrayList[] offsets = new IntArrayList[this.packedOffsetsByDistanceSq.length];
      for (int i = 0; i < offsets.length; i++) {
         if (this.packedOffsetsByDistanceSq[i] != null) {
            offsets[i] = this.packedOffsetsByDistanceSq[i].clone();
         }
      }
      return new LongJumpChoiceList(this.origin.add(offset), offsets, Arrays.copyOf(this.weightByDistanceSq, this.weightByDistanceSq.length), this.totalWeight);
   }

   public Target removeRandomWeightedByDistanceSq(Random random) {
      if (this.totalWeight == 0) {
         return null;
      }
      int targetWeight = random.nextInt(this.totalWeight);
      for (int index = 0; index < this.weightByDistanceSq.length; index++) {
         targetWeight -= this.weightByDistanceSq[index];
         if (targetWeight < 0) {
            return this.removeFromBucket(index, random.nextInt(this.packedOffsetsByDistanceSq[index].size()));
         }
      }
      throw new IllegalStateException("Long jump candidate weights are inconsistent");
   }

   private Target removeFromBucket(int bucketIndex, int elementIndex) {
      IntArrayList offsets = this.packedOffsetsByDistanceSq[bucketIndex];
      int packedOffset = offsets.getInt(elementIndex);
      offsets.set(elementIndex, offsets.getInt(offsets.size() - 1));
      offsets.removeInt(offsets.size() - 1);
      int squaredDistance = bucketIndex + 1;
      this.weightByDistanceSq[bucketIndex] -= squaredDistance;
      this.totalWeight -= squaredDistance;
      return new Target(this.origin.add(unpackX(packedOffset), unpackY(packedOffset), unpackZ(packedOffset)), squaredDistance);
   }

   @Override
   public Target get(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index);
      }
      for (int bucket = 0; bucket < this.packedOffsetsByDistanceSq.length; bucket++) {
         IntArrayList offsets = this.packedOffsetsByDistanceSq[bucket];
         if (offsets != null) {
            if (index < offsets.size()) {
               int packedOffset = offsets.getInt(index);
               return new Target(this.origin.add(unpackX(packedOffset), unpackY(packedOffset), unpackZ(packedOffset)), bucket + 1);
            }
            index -= offsets.size();
         }
      }
      throw new IndexOutOfBoundsException(index);
   }

   @Override
   public Target remove(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index);
      }
      for (int bucket = 0; bucket < this.packedOffsetsByDistanceSq.length; bucket++) {
         IntArrayList offsets = this.packedOffsetsByDistanceSq[bucket];
         if (offsets != null) {
            if (index < offsets.size()) {
               return this.removeFromBucket(bucket, index);
            }
            index -= offsets.size();
         }
      }
      throw new IndexOutOfBoundsException(index);
   }

   @Override
   public boolean isEmpty() {
      return this.totalWeight == 0;
   }

   @Override
   public int size() {
      int size = 0;
      for (IntArrayList offsets : this.packedOffsetsByDistanceSq) {
         if (offsets != null) {
            size += offsets.size();
         }
      }
      return size;
   }

   private static int packOffset(int x, int y, int z) {
      return x + 128 | y + 128 << 8 | z + 128 << 16;
   }

   private static int unpackX(int packedOffset) {
      return (packedOffset & 255) - 128;
   }

   private static int unpackY(int packedOffset) {
      return (packedOffset >>> 8 & 255) - 128;
   }

   private static int unpackZ(int packedOffset) {
      return (packedOffset >>> 16 & 255) - 128;
   }
}
