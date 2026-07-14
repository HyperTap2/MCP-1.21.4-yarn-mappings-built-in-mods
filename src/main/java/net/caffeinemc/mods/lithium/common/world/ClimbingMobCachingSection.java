package net.caffeinemc.mods.lithium.common.world;

import java.util.ArrayList;
import net.caffeinemc.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import net.caffeinemc.mods.lithium.common.entity.pushable.FeetBlockCachingEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public interface ClimbingMobCachingSection {
   LazyIterationConsumer.NextIteration lithium$collectPushableEntities(
      World world, Entity except, Box box, EntityPushablePredicate<? super Entity> predicate, ArrayList<Entity> entities
   );

   void lithium$onEntityModifiedCachedBlock(FeetBlockCachingEntity entity, BlockState newBlockState);
}
