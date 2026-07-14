package net.caffeinemc.mods.lithium.common.hopper;

import net.minecraft.block.entity.BlockEntity;

public enum ComparatorUpdatePattern {
   NO_UPDATE {
      @Override
      public ComparatorUpdatePattern thenUpdate() {
         return UPDATE;
      }

      @Override
      public ComparatorUpdatePattern thenDecrementUpdateIncrementUpdate() {
         return DECREMENT_UPDATE_INCREMENT_UPDATE;
      }
   },
   UPDATE {
      @Override
      public void apply(BlockEntity blockEntity, LithiumStackList stackList) {
         blockEntity.markDirty();
      }

      @Override
      public ComparatorUpdatePattern thenDecrementUpdateIncrementUpdate() {
         return UPDATE_DECREMENT_UPDATE_INCREMENT_UPDATE;
      }
   },
   DECREMENT_UPDATE_INCREMENT_UPDATE {
      @Override
      public void apply(BlockEntity blockEntity, LithiumStackList stackList) {
         stackList.setReducedSignalStrengthOverride();
         blockEntity.markDirty();
         stackList.clearSignalStrengthOverride();
         blockEntity.markDirty();
      }

      @Override
      public boolean isChainable() {
         return false;
      }
   },
   UPDATE_DECREMENT_UPDATE_INCREMENT_UPDATE {
      @Override
      public void apply(BlockEntity blockEntity, LithiumStackList stackList) {
         blockEntity.markDirty();
         stackList.setReducedSignalStrengthOverride();
         blockEntity.markDirty();
         stackList.clearSignalStrengthOverride();
         blockEntity.markDirty();
      }

      @Override
      public boolean isChainable() {
         return false;
      }
   };

   public void apply(BlockEntity blockEntity, LithiumStackList stackList) {
   }

   public ComparatorUpdatePattern thenUpdate() {
      return this;
   }

   public ComparatorUpdatePattern thenDecrementUpdateIncrementUpdate() {
      return this;
   }

   public boolean isChainable() {
      return true;
   }
}
