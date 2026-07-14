package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.GraphDirectionSet;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenderSection {
   private final RenderRegion region;
   private final int sectionIndex;
   private final int chunkX;
   private final int chunkY;
   private final int chunkZ;
   private long visibilityData = 0L;
   private int incomingDirections;
   private int lastVisibleFrame = -1;
   private int iris$lastVisibleFrameShadow = -1;
   private int adjacentMask;
   public RenderSection adjacentDown;
   public RenderSection adjacentUp;
   public RenderSection adjacentNorth;
   public RenderSection adjacentSouth;
   public RenderSection adjacentWest;
   public RenderSection adjacentEast;
   private boolean built = false;
   private int flags = 0;
   @Nullable
   private BlockEntity[] globalBlockEntities;
   @Nullable
   private BlockEntity[] culledBlockEntities;
   @Nullable
   private Sprite[] animatedSprites;
   @Nullable
   private TranslucentData translucentData;
   @Nullable
   private CancellationToken taskCancellationToken = null;
   @Nullable
   private ChunkUpdateType pendingUpdateType;
   private int lastUploadFrame = -1;
   private int lastSubmittedFrame = -1;
   private boolean disposed;

   public RenderSection(RenderRegion region, int chunkX, int chunkY, int chunkZ) {
      this.chunkX = chunkX;
      this.chunkY = chunkY;
      this.chunkZ = chunkZ;
      int rX = this.getChunkX() & 7;
      int rY = this.getChunkY() & 3;
      int rZ = this.getChunkZ() & 7;
      this.sectionIndex = LocalSectionIndex.pack(rX, rY, rZ);
      this.region = region;
   }

   public RenderSection getAdjacent(int direction) {
      return switch (direction) {
         case 0 -> this.adjacentDown;
         case 1 -> this.adjacentUp;
         case 2 -> this.adjacentNorth;
         case 3 -> this.adjacentSouth;
         case 4 -> this.adjacentWest;
         case 5 -> this.adjacentEast;
         default -> null;
      };
   }

   public void setAdjacentNode(int direction, RenderSection node) {
      if (node == null) {
         this.adjacentMask = this.adjacentMask & ~GraphDirectionSet.of(direction);
      } else {
         this.adjacentMask = this.adjacentMask | GraphDirectionSet.of(direction);
      }

      switch (direction) {
         case 0:
            this.adjacentDown = node;
            break;
         case 1:
            this.adjacentUp = node;
            break;
         case 2:
            this.adjacentNorth = node;
            break;
         case 3:
            this.adjacentSouth = node;
            break;
         case 4:
            this.adjacentWest = node;
            break;
         case 5:
            this.adjacentEast = node;
      }
   }

   public int getAdjacentMask() {
      return this.adjacentMask;
   }

   public TranslucentData getTranslucentData() {
      return this.translucentData;
   }

   public void setTranslucentData(TranslucentData translucentData) {
      if (translucentData == null) {
         throw new IllegalArgumentException("new translucentData cannot be null");
      } else {
         this.translucentData = translucentData;
      }
   }

   public void delete() {
      if (this.taskCancellationToken != null) {
         this.taskCancellationToken.setCancelled();
         this.taskCancellationToken = null;
      }

      this.clearRenderState();
      this.disposed = true;
   }

   public boolean setInfo(@Nullable BuiltSectionInfo info) {
      return info != null ? this.setRenderState(info) : this.clearRenderState();
   }

   private boolean setRenderState(@NotNull BuiltSectionInfo info) {
      boolean prevBuilt = this.built;
      int prevFlags = this.flags;
      long prevVisibilityData = this.visibilityData;
      this.built = true;
      this.flags = info.flags;
      this.visibilityData = info.visibilityData;
      this.globalBlockEntities = info.globalBlockEntities;
      this.culledBlockEntities = info.culledBlockEntities;
      this.animatedSprites = info.animatedSprites;
      return !prevBuilt || prevFlags != this.flags || prevVisibilityData != this.visibilityData;
   }

   private boolean clearRenderState() {
      boolean wasBuilt = this.built;
      this.built = false;
      this.flags = 0;
      this.visibilityData = 0L;
      this.globalBlockEntities = null;
      this.culledBlockEntities = null;
      this.animatedSprites = null;
      return wasBuilt;
   }

   public ChunkSectionPos getPosition() {
      return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
   }

   public int getOriginX() {
      return this.chunkX << 4;
   }

   public int getOriginY() {
      return this.chunkY << 4;
   }

   public int getOriginZ() {
      return this.chunkZ << 4;
   }

   public float getSquaredDistance(BlockPos pos) {
      return this.getSquaredDistance(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
   }

   public float getSquaredDistance(float x, float y, float z) {
      float xDist = x - this.getCenterX();
      float yDist = y - this.getCenterY();
      float zDist = z - this.getCenterZ();
      return xDist * xDist + yDist * yDist + zDist * zDist;
   }

   public int getCenterX() {
      return this.getOriginX() + 8;
   }

   public int getCenterY() {
      return this.getOriginY() + 8;
   }

   public int getCenterZ() {
      return this.getOriginZ() + 8;
   }

   public int getChunkX() {
      return this.chunkX;
   }

   public int getChunkY() {
      return this.chunkY;
   }

   public int getChunkZ() {
      return this.chunkZ;
   }

   public boolean isDisposed() {
      return this.disposed;
   }

   @Override
   public String toString() {
      return String.format(
         "RenderSection at chunk (%d, %d, %d) from (%d, %d, %d) to (%d, %d, %d)",
         this.chunkX,
         this.chunkY,
         this.chunkZ,
         this.getOriginX(),
         this.getOriginY(),
         this.getOriginZ(),
         this.getOriginX() + 15,
         this.getOriginY() + 15,
         this.getOriginZ() + 15
      );
   }

   public boolean isBuilt() {
      return this.built;
   }

   public int getSectionIndex() {
      return this.sectionIndex;
   }

   public RenderRegion getRegion() {
      return this.region;
   }

   public void setLastVisibleFrame(int frame) {
      if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
         this.iris$lastVisibleFrameShadow = frame;
      } else {
         this.lastVisibleFrame = frame;
      }
   }

   public int getLastVisibleFrame() {
      return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? this.iris$lastVisibleFrameShadow : this.lastVisibleFrame;
   }

   public int getIncomingDirections() {
      return this.incomingDirections;
   }

   public void addIncomingDirections(int directions) {
      this.incomingDirections |= directions;
   }

   public void setIncomingDirections(int directions) {
      this.incomingDirections = directions;
   }

   public int getFlags() {
      return this.flags;
   }

   public long getVisibilityData() {
      return this.visibilityData;
   }

   @Nullable
   public Sprite[] getAnimatedSprites() {
      return this.animatedSprites;
   }

   @Nullable
   public BlockEntity[] getCulledBlockEntities() {
      return this.culledBlockEntities;
   }

   @Nullable
   public BlockEntity[] getGlobalBlockEntities() {
      return this.globalBlockEntities;
   }

   @Nullable
   public CancellationToken getTaskCancellationToken() {
      return this.taskCancellationToken;
   }

   public void setTaskCancellationToken(@Nullable CancellationToken token) {
      this.taskCancellationToken = token;
   }

   @Nullable
   public ChunkUpdateType getPendingUpdate() {
      return this.pendingUpdateType;
   }

   public void setPendingUpdate(@Nullable ChunkUpdateType type) {
      this.pendingUpdateType = type;
   }

   public void prepareTrigger(boolean isDirectTrigger) {
      if (this.translucentData != null) {
         this.translucentData.prepareTrigger(isDirectTrigger);
      }
   }

   public int getLastUploadFrame() {
      return this.lastUploadFrame;
   }

   public void setLastUploadFrame(int lastSortFrame) {
      this.lastUploadFrame = lastSortFrame;
   }

   public int getLastSubmittedFrame() {
      return this.lastSubmittedFrame;
   }

   public void setLastSubmittedFrame(int lastSubmittedFrame) {
      this.lastSubmittedFrame = lastSubmittedFrame;
   }
}
