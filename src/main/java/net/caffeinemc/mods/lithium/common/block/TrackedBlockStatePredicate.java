package net.caffeinemc.mods.lithium.common.block;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;

public abstract class TrackedBlockStatePredicate implements Predicate<BlockState> {
   public static final AtomicBoolean FULLY_INITIALIZED = new AtomicBoolean(false);
   private final int index;

   protected TrackedBlockStatePredicate(int index) {
      if (FULLY_INITIALIZED.get()) {
         throw new IllegalStateException("Cannot register a Lithium block-state flag after initialization");
      }

      this.index = index;
   }

   public int getIndex() {
      return this.index;
   }
}
