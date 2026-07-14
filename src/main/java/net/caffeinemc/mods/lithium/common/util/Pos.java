package net.caffeinemc.mods.lithium.common.util;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;

public final class Pos {
   private Pos() {
   }

   public static final class SectionYCoord {
      private SectionYCoord() {
      }

      public static int getMinYSection(HeightLimitView view) {
         return view.getBottomSectionCoord();
      }

      public static int getNumYSections(HeightLimitView view) {
         return view.countVerticalSections();
      }

      public static int getMaxYSectionInclusive(HeightLimitView view) {
         return view.getTopSectionCoord();
      }

      public static int getMaxYSectionExclusive(HeightLimitView view) {
         return view.getTopSectionCoord() + 1;
      }

      public static int fromSectionIndex(HeightLimitView view, int sectionIndex) {
         return sectionIndex + getMinYSection(view);
      }
   }

   public static final class ChunkCoord {
      private ChunkCoord() {
      }

      public static int fromBlockCoord(int blockCoord) {
         return ChunkSectionPos.getSectionCoord(blockCoord);
      }
   }

   public static final class SectionYIndex {
      private SectionYIndex() {
      }

      public static int fromSectionCoord(HeightLimitView view, int sectionCoord) {
         return sectionCoord - SectionYCoord.getMinYSection(view);
      }

      public static int fromBlockCoord(HeightLimitView view, int blockCoord) {
         return fromSectionCoord(view, ChunkSectionPos.getSectionCoord(blockCoord));
      }

      public static int getNumYSections(HeightLimitView view) {
         return view.countVerticalSections();
      }

      public static int getMinYSectionIndex(HeightLimitView view) {
         return 0;
      }

      public static int getMaxYSectionIndexInclusive(HeightLimitView view) {
         return view.countVerticalSections() - 1;
      }
   }

   public static final class BlockCoord {
      private BlockCoord() {
      }

      public static int getMinY(HeightLimitView view) {
         return view.getBottomY();
      }

      public static int getMaxYInclusive(HeightLimitView view) {
         return view.getTopYInclusive();
      }

      public static int getMinInSectionCoord(int sectionCoord) {
         return sectionCoord << 4;
      }

      public static int getMaxInSectionCoord(int sectionCoord) {
         return (sectionCoord << 4) + 15;
      }

      public static int getMinYInSectionIndex(HeightLimitView view, int sectionIndex) {
         return view.sectionIndexToCoord(sectionIndex) << 4;
      }

      public static int getMaxYInSectionIndex(HeightLimitView view, int sectionIndex) {
         return getMinYInSectionIndex(view, sectionIndex) + 15;
      }
   }
}
