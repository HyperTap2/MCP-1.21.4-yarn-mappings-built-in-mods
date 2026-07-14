package net.minecraft.world.explosion;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class EntityExplosionBehavior extends ExplosionBehavior {
   private final Entity entity;

   public EntityExplosionBehavior(Entity entity) {
      this.entity = entity;
   }

   @Override
   public Optional<Float> getBlastResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState) {
      Optional<Float> resistance = super.getBlastResistance(explosion, world, pos, blockState, fluidState);
      if (resistance.isPresent()) {
         float original = resistance.get();
         float effective = this.entity.getEffectiveExplosionResistance(explosion, world, pos, blockState, fluidState, original);
         if (effective != original) {
            return Optional.of(effective);
         }
      }
      return resistance;
   }

   @Override
   public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
      return this.entity.canExplosionDestroyBlock(explosion, world, pos, state, power);
   }
}
