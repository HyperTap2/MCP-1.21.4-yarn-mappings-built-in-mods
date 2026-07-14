package net.caffeinemc.mods.lithium.common.tracking.entity;

import java.util.ArrayList;
import java.util.List;
import net.caffeinemc.mods.lithium.common.util.tuples.WorldSectionBox;
import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

public final class SectionedInventoryEntityMovementTracker<S> extends SectionedEntityMovementTracker<Entity> {
   private final Class<S> entityClass;

   private SectionedInventoryEntityMovementTracker(WorldSectionBox sections, Class<S> entityClass) {
      super(sections, entityClass);
      this.entityClass = entityClass;
   }

   public static <S> SectionedInventoryEntityMovementTracker<S> registerAt(ServerWorld world, Box box, Class<S> entityClass) {
      SectionedInventoryEntityMovementTracker<S> tracker = new SectionedInventoryEntityMovementTracker<>(WorldSectionBox.entityAccessBox(world, box), entityClass);
      tracker = ((LithiumData)world).lithium$getData().entityMovementTrackers().getCanonical(tracker);
      tracker.register(world);
      return tracker;
   }

   public List<S> getEntities(Box box) {
      List<S> result = new ArrayList<>();
      for (int i = 0; i < this.sortedSections.size(); i++) {
         if (!this.sectionVisible[i]) {
            continue;
         }
         for (S candidate : this.sortedSections.get(i).getCollection().getAllOfType(this.entityClass)) {
            if (candidate instanceof Entity entity && entity.isAlive() && entity.getBoundingBox().intersects(box)) {
               result.add(candidate);
            }
         }
      }
      return result;
   }
}
