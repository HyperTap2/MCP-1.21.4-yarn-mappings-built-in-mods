package net.caffeinemc.mods.sodium.client.render.chunk.occlusion;

import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.MinecraftClient;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.collections.DoubleBufferedQueue;
import net.caffeinemc.mods.sodium.client.util.collections.ReadQueue;
import net.caffeinemc.mods.sodium.client.util.collections.WriteQueue;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class OcclusionCuller {
   private final Long2ReferenceMap<RenderSection> sections;
   private final World level;
   private final DoubleBufferedQueue<RenderSection> queue = new DoubleBufferedQueue<>();
   private static final long UP_DOWN_OCCLUDED = 1L << VisibilityEncoding.bit(0, 1) | 1L << VisibilityEncoding.bit(1, 0);
   private static final long NORTH_SOUTH_OCCLUDED = 1L << VisibilityEncoding.bit(2, 3) | 1L << VisibilityEncoding.bit(3, 2);
   private static final long WEST_EAST_OCCLUDED = 1L << VisibilityEncoding.bit(4, 5) | 1L << VisibilityEncoding.bit(5, 4);
   private static final float CHUNK_SECTION_RADIUS = 8.0F;
   private static final float CHUNK_SECTION_SIZE = 9.125F;
   private static final float CHUNK_SECTION_SIZE_NEARBY = 10.125F;

   public OcclusionCuller(Long2ReferenceMap<RenderSection> sections, World level) {
      this.sections = sections;
      this.level = level;
   }

   public void findVisible(OcclusionCuller.Visitor visitor, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame) {
      DoubleBufferedQueue<RenderSection> queues = this.queue;
      queues.reset();
      this.init(visitor, queues.write(), viewport, searchDistance, useOcclusionCulling, frame);

      while (queues.flip()) {
         processQueue(visitor, viewport, searchDistance, useOcclusionCulling, frame, queues.read(), queues.write());
      }

      this.addNearbySections(visitor, viewport, searchDistance, frame);
   }

   private static void processQueue(
      OcclusionCuller.Visitor visitor,
      Viewport viewport,
      float searchDistance,
      boolean useOcclusionCulling,
      int frame,
      ReadQueue<RenderSection> readQueue,
      WriteQueue<RenderSection> writeQueue
   ) {
      RenderSection section;
      while ((section = readQueue.dequeue()) != null) {
         if (isSectionVisible(section, viewport, searchDistance)) {
            visitor.visit(section);
            int connections;
            if (useOcclusionCulling) {
               long sectionVisibilityData = section.getVisibilityData();
               sectionVisibilityData &= getAngleVisibilityMask(viewport, section);
               connections = VisibilityEncoding.getConnections(sectionVisibilityData, section.getIncomingDirections());
            } else {
               connections = 63;
            }

            connections &= getOutwardDirections(viewport.getChunkCoord(), section);
            visitNeighbors(writeQueue, section, connections, frame);
         }
      }
   }

   private static long getAngleVisibilityMask(Viewport viewport, RenderSection section) {
      CameraTransform transform = viewport.getTransform();
      double dx = Math.abs(transform.x - section.getCenterX());
      double dy = Math.abs(transform.y - section.getCenterY());
      double dz = Math.abs(transform.z - section.getCenterZ());
      long angleOcclusionMask = 0L;
      if (dx > dy || dz > dy) {
         angleOcclusionMask |= UP_DOWN_OCCLUDED;
      }

      if (dx > dz || dy > dz) {
         angleOcclusionMask |= NORTH_SOUTH_OCCLUDED;
      }

      if (dy > dx || dz > dx) {
         angleOcclusionMask |= WEST_EAST_OCCLUDED;
      }

      return ~angleOcclusionMask;
   }

   private static boolean isSectionVisible(RenderSection section, Viewport viewport, float maxDistance) {
      return isWithinRenderDistance(viewport.getTransform(), section, maxDistance) && isWithinFrustum(viewport, section);
   }

   private static void visitNeighbors(WriteQueue<RenderSection> queue, RenderSection section, int outgoing, int frame) {
      outgoing &= section.getAdjacentMask();
      if (outgoing != 0) {
         queue.ensureCapacity(6);
         if (GraphDirectionSet.contains(outgoing, 0)) {
            visitNode(queue, section.adjacentDown, GraphDirectionSet.of(1), frame);
         }

         if (GraphDirectionSet.contains(outgoing, 1)) {
            visitNode(queue, section.adjacentUp, GraphDirectionSet.of(0), frame);
         }

         if (GraphDirectionSet.contains(outgoing, 2)) {
            visitNode(queue, section.adjacentNorth, GraphDirectionSet.of(3), frame);
         }

         if (GraphDirectionSet.contains(outgoing, 3)) {
            visitNode(queue, section.adjacentSouth, GraphDirectionSet.of(2), frame);
         }

         if (GraphDirectionSet.contains(outgoing, 4)) {
            visitNode(queue, section.adjacentWest, GraphDirectionSet.of(5), frame);
         }

         if (GraphDirectionSet.contains(outgoing, 5)) {
            visitNode(queue, section.adjacentEast, GraphDirectionSet.of(4), frame);
         }
      }
   }

   private static void visitNode(WriteQueue<RenderSection> queue, @NotNull RenderSection render, int incoming, int frame) {
      if (render.getLastVisibleFrame() != frame) {
         render.setLastVisibleFrame(frame);
         render.setIncomingDirections(0);
         queue.enqueue(render);
      }

      render.addIncomingDirections(incoming);
   }

   private static int getOutwardDirections(ChunkSectionPos origin, RenderSection section) {
      int planes = 0;
      planes |= section.getChunkX() <= origin.getX() ? 16 : 0;
      planes |= section.getChunkX() >= origin.getX() ? 32 : 0;
      planes |= section.getChunkY() <= origin.getY() ? 1 : 0;
      planes |= section.getChunkY() >= origin.getY() ? 2 : 0;
      planes |= section.getChunkZ() <= origin.getZ() ? 4 : 0;
      return planes | (section.getChunkZ() >= origin.getZ() ? 8 : 0);
   }

   private static boolean isWithinRenderDistance(CameraTransform camera, RenderSection section, float maxDistance) {
      int ox = section.getOriginX() - camera.intX;
      int oy = section.getOriginY() - camera.intY;
      int oz = section.getOriginZ() - camera.intZ;
      float dx = nearestToZero(ox, ox + 16) - camera.fracX;
      float dy = nearestToZero(oy, oy + 16) - camera.fracY;
      float dz = nearestToZero(oz, oz + 16) - camera.fracZ;
      if (SodiumExtraClientMod.isMixinEnabled("sodium.fog.MixinOcclusionCuller")) {
         int fogDistance = SodiumExtraClientMod.options().renderSettings.multiDimensionFogControl
            ? SodiumExtraClientMod.options().renderSettings.dimensionFogDistanceMap.getOrDefault(
               MinecraftClient.getInstance().world.getDimension().effects(), 0
            )
            : SodiumExtraClientMod.options().renderSettings.fogDistance;
         if (fogDistance == 33) {
            return dx * dx + dz * dz < maxDistance * maxDistance;
         }
      }

      return dx * dx + dz * dz < maxDistance * maxDistance && Math.abs(dy) < maxDistance;
   }

   private static int nearestToZero(int min, int max) {
      int clamped = 0;
      if (min > 0) {
         clamped = min;
      }

      if (max < 0) {
         clamped = max;
      }

      return clamped;
   }

   public static boolean isWithinFrustum(Viewport viewport, RenderSection section) {
      return viewport.isBoxVisible(section.getCenterX(), section.getCenterY(), section.getCenterZ(), 9.125F, 9.125F, 9.125F);
   }

   public static boolean isWithinNearbySectionFrustum(Viewport viewport, RenderSection section) {
      return viewport.isBoxVisible(section.getCenterX(), section.getCenterY(), section.getCenterZ(), 10.125F, 10.125F, 10.125F);
   }

   private void addNearbySections(OcclusionCuller.Visitor visitor, Viewport viewport, float searchDistance, int frame) {
      ChunkSectionPos origin = viewport.getChunkCoord();
      int originX = origin.getX();
      int originY = origin.getY();
      int originZ = origin.getZ();

      for (int dx = -1; dx <= 1; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx != 0 || dy != 0 || dz != 0) {
                  RenderSection section = this.getRenderSection(originX + dx, originY + dy, originZ + dz);
                  if (section != null && section.getLastVisibleFrame() != frame && isWithinNearbySectionFrustum(viewport, section)) {
                     section.setLastVisibleFrame(frame);
                     visitor.visit(section);
                  }
               }
            }
         }
      }
   }

   private void init(
      OcclusionCuller.Visitor visitor, WriteQueue<RenderSection> queue, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame
   ) {
      ChunkSectionPos origin = viewport.getChunkCoord();
      if (origin.getY() < this.level.getBottomSectionCoord()) {
         this.initOutsideWorldHeight(queue, viewport, searchDistance, frame, this.level.getBottomSectionCoord(), 0);
      } else if (origin.getY() > this.level.getTopSectionCoord()) {
         this.initOutsideWorldHeight(queue, viewport, searchDistance, frame, this.level.getTopSectionCoord(), 1);
      } else {
         this.initWithinWorld(visitor, queue, viewport, useOcclusionCulling, frame);
      }
   }

   private void initWithinWorld(OcclusionCuller.Visitor visitor, WriteQueue<RenderSection> queue, Viewport viewport, boolean useOcclusionCulling, int frame) {
      ChunkSectionPos origin = viewport.getChunkCoord();
      RenderSection section = this.getRenderSection(origin.getX(), origin.getY(), origin.getZ());
      if (section != null) {
         section.setLastVisibleFrame(frame);
         section.setIncomingDirections(0);
         visitor.visit(section);
         int outgoing;
         if (useOcclusionCulling) {
            outgoing = VisibilityEncoding.getConnections(section.getVisibilityData());
         } else {
            outgoing = 63;
         }

         visitNeighbors(queue, section, outgoing, frame);
      }
   }

   private void initOutsideWorldHeight(WriteQueue<RenderSection> queue, Viewport viewport, float searchDistance, int frame, int height, int direction) {
      ChunkSectionPos origin = viewport.getChunkCoord();
      int radius = MathHelper.floor(searchDistance / 16.0F);
      this.tryVisitNode(queue, origin.getX(), height, origin.getZ(), direction, frame, viewport);

      for (int layer = 1; layer <= radius; layer++) {
         for (int z = -layer; z < layer; z++) {
            int x = Math.abs(z) - layer;
            this.tryVisitNode(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
         }

         for (int z = layer; z > -layer; z--) {
            int x = layer - Math.abs(z);
            this.tryVisitNode(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
         }
      }

      for (int layer = radius + 1; layer <= 2 * radius; layer++) {
         int l = layer - radius;

         for (int z = -radius; z <= -l; z++) {
            int x = -z - layer;
            this.tryVisitNode(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
         }

         for (int z = l; z <= radius; z++) {
            int x = z - layer;
            this.tryVisitNode(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
         }

         for (int z = radius; z >= l; z--) {
            int x = layer - z;
            this.tryVisitNode(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
         }

         for (int z = -l; z >= -radius; z--) {
            int x = layer + z;
            this.tryVisitNode(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
         }
      }
   }

   private void tryVisitNode(WriteQueue<RenderSection> queue, int x, int y, int z, int direction, int frame, Viewport viewport) {
      RenderSection section = this.getRenderSection(x, y, z);
      if (section != null && isWithinFrustum(viewport, section)) {
         visitNode(queue, section, GraphDirectionSet.of(direction), frame);
      }
   }

   private RenderSection getRenderSection(int x, int y, int z) {
      return (RenderSection)this.sections.get(ChunkSectionPos.asLong(x, y, z));
   }

   public interface Visitor {
      void visit(RenderSection var1);
   }
}
