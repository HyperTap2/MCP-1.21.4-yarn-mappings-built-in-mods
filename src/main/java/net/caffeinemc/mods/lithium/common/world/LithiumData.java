package net.caffeinemc.mods.lithium.common.world;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.caffeinemc.mods.lithium.common.tracking.block.ChunkSectionChangeCallback;
import net.caffeinemc.mods.lithium.common.tracking.block.SectionedBlockChangeTracker;
import net.caffeinemc.mods.lithium.common.tracking.entity.SectionedEntityMovementTracker;
import net.caffeinemc.mods.lithium.common.util.deduplication.LithiumInterner;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.village.raid.Raid;

public interface LithiumData {
   Data lithium$getData();

   final class Data {
      private final LithiumInterner<SectionedBlockChangeTracker> blockChangeTrackers = new LithiumInterner<>();
      private final LithiumInterner<SectionedEntityMovementTracker<?>> entityMovementTrackers = new LithiumInterner<>();
      private final Long2ReferenceOpenHashMap<ChunkSectionChangeCallback> chunkSectionChangeCallbacks = new Long2ReferenceOpenHashMap<>();
      private volatile ItemStack ominousBanner;
      private final ReferenceOpenHashSet<EntityNavigation> activeNavigations = new ReferenceOpenHashSet<>();
      private final GameEventDispatcherStorage gameEventDispatchers = new GameEventDispatcherStorage();

      public LithiumInterner<SectionedBlockChangeTracker> blockChangeTrackers() {
         return this.blockChangeTrackers;
      }

      public LithiumInterner<SectionedEntityMovementTracker<?>> entityMovementTrackers() {
         return this.entityMovementTrackers;
      }

      public Long2ReferenceOpenHashMap<ChunkSectionChangeCallback> chunkSectionChangeCallbacks() {
         return this.chunkSectionChangeCallbacks;
      }

      public ReferenceOpenHashSet<EntityNavigation> activeNavigations() {
         return this.activeNavigations;
      }

      public GameEventDispatcherStorage gameEventDispatchers() {
         return this.gameEventDispatchers;
      }

      public ItemStack ominousBanner(RegistryEntryLookup<BannerPattern> bannerPatterns) {
         ItemStack banner = this.ominousBanner;
         if (banner == null) {
            synchronized (this) {
               banner = this.ominousBanner;
               if (banner == null) {
                  banner = Raid.createOminousBanner(bannerPatterns);
                  this.ominousBanner = banner;
               }
            }
         }
         return banner;
      }
   }
}
