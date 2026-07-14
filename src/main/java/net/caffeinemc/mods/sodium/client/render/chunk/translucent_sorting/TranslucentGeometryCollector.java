package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPBuildFailureException;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.AnyOrderData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.CombinedCameraPos;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicBSPData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.NoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.PresentTranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.StaticNormalRelativeData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.StaticTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class TranslucentGeometryCollector {
   private final ChunkSectionPos sectionPos;
   private boolean hasUnaligned = false;
   private int alignedFacingBitmap = 0;
   private final float[] extents = new float[]{
      Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY
   };
   private boolean alignedExtentsMultiple = false;
   private final float[] alignedExtremes = new float[]{
      Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY
   };
   private int unalignedANormal = -1;
   private float unalignedADistance1 = Float.NaN;
   private float unalignedADistance2 = Float.NaN;
   private int unalignedBNormal = -1;
   private float unalignedBDistance1 = Float.NaN;
   private float unalignedBDistance2 = Float.NaN;
   private ReferenceArrayList<TQuad>[] quadLists = new ReferenceArrayList[ModelQuadFacing.COUNT];
   private TQuad[] quads;
   private SortType sortType;
   private boolean quadHashPresent = false;
   private int quadHash = 0;
   private static final float INV_QUANTIZE_EPSILON = 256.0F;
   private static final float QUANTIZE_EPSILON = 0.00390625F;
   private static final int[] STATIC_TOPO_SORT_ATTEMPT_LIMITS;
   public static final int STATIC_TOPO_UNKNOWN_FALLBACK_LIMIT;

   public TranslucentGeometryCollector(ChunkSectionPos sectionPos) {
      this.sectionPos = sectionPos;
   }

   public void appendQuad(int packedNormal, ChunkVertexEncoder.Vertex[] vertices, ModelQuadFacing facing) {
      float xSum = 0.0F;
      float ySum = 0.0F;
      float zSum = 0.0F;
      float lastX = vertices[3].x;
      float lastY = vertices[3].y;
      float lastZ = vertices[3].z;
      int uniqueVertexes = 0;
      float posXExtent = Float.NEGATIVE_INFINITY;
      float posYExtent = Float.NEGATIVE_INFINITY;
      float posZExtent = Float.NEGATIVE_INFINITY;
      float negXExtent = Float.POSITIVE_INFINITY;
      float negYExtent = Float.POSITIVE_INFINITY;
      float negZExtent = Float.POSITIVE_INFINITY;

      for (int i = 0; i < 4; i++) {
         float x = vertices[i].x;
         float y = vertices[i].y;
         float z = vertices[i].z;
         posXExtent = Math.max(posXExtent, x);
         posYExtent = Math.max(posYExtent, y);
         posZExtent = Math.max(posZExtent, z);
         negXExtent = Math.min(negXExtent, x);
         negYExtent = Math.min(negYExtent, y);
         negZExtent = Math.min(negZExtent, z);
         if (x != lastX || y != lastY || z != lastZ) {
            xSum += x;
            ySum += y;
            zSum += z;
            uniqueVertexes++;
         }

         if (i != 3) {
            lastX = x;
            lastY = y;
            lastZ = z;
         }
      }

      if (facing != ModelQuadFacing.POS_X && facing != ModelQuadFacing.NEG_X) {
         posXExtent -= 0.00390625F;
         negXExtent += 0.00390625F;
         if (negXExtent > posXExtent) {
            negXExtent = posXExtent;
         }
      }

      if (facing != ModelQuadFacing.POS_Y && facing != ModelQuadFacing.NEG_Y) {
         posYExtent -= 0.00390625F;
         negYExtent += 0.00390625F;
         if (negYExtent > posYExtent) {
            negYExtent = posYExtent;
         }
      }

      if (facing != ModelQuadFacing.POS_Z && facing != ModelQuadFacing.NEG_Z) {
         posZExtent -= 0.00390625F;
         negZExtent += 0.00390625F;
         if (negZExtent > posZExtent) {
            negZExtent = posZExtent;
         }
      }

      float[] extents = new float[]{posXExtent, posYExtent, posZExtent, negXExtent, negYExtent, negZExtent};
      int direction = facing.ordinal();
      ReferenceArrayList<TQuad> quadList = this.quadLists[direction];
      if (quadList == null) {
         quadList = new ReferenceArrayList();
         this.quadLists[direction] = quadList;
      }

      Vector3fc center = null;
      if (!facing.isAligned() || uniqueVertexes != 4) {
         float centerX = xSum / uniqueVertexes;
         float centerY = ySum / uniqueVertexes;
         float centerZ = zSum / uniqueVertexes;
         center = new Vector3f(centerX, centerY, centerZ);
      }

      boolean needsVertexPositions = uniqueVertexes != 4 || !facing.isAligned();
      if (!needsVertexPositions) {
         for (int i = 0; i < 4; i++) {
            ChunkVertexEncoder.Vertex vertex = vertices[i];
            if (vertex.x != posYExtent && vertex.x != negYExtent
               || vertex.y != posZExtent && vertex.y != negZExtent
               || vertex.z != posXExtent && vertex.z != negXExtent) {
               needsVertexPositions = true;
               break;
            }
         }
      }

      float[] vertexPositions = null;
      if (needsVertexPositions) {
         vertexPositions = new float[12];
         int ix = 0;

         for (int itemIndex = 0; ix < 4; ix++) {
            ChunkVertexEncoder.Vertex vertex = vertices[ix];
            vertexPositions[itemIndex++] = vertex.x;
            vertexPositions[itemIndex++] = vertex.y;
            vertexPositions[itemIndex++] = vertex.z;
         }
      }

      if (facing.isAligned()) {
         if (!this.hasUnaligned) {
            this.extents[0] = Math.max(this.extents[0], posXExtent);
            this.extents[1] = Math.max(this.extents[1], posYExtent);
            this.extents[2] = Math.max(this.extents[2], posZExtent);
            this.extents[3] = Math.min(this.extents[3], negXExtent);
            this.extents[4] = Math.min(this.extents[4], negYExtent);
            this.extents[5] = Math.min(this.extents[5], negZExtent);
         }

         TQuad quad = TQuad.fromAligned(facing, extents, vertexPositions, center);
         quadList.add(quad);
         float extreme = this.alignedExtremes[direction];
         float distance = quad.getAccurateDotProduct();
         float existingExtreme = this.alignedExtremes[direction];
         if (!this.alignedExtentsMultiple && !Float.isInfinite(existingExtreme) && existingExtreme != distance) {
            this.alignedExtentsMultiple = true;
         }

         if (facing.getSign() > 0) {
            this.alignedExtremes[direction] = Math.max(extreme, distance);
         } else {
            this.alignedExtremes[direction] = Math.min(extreme, distance);
         }
      } else {
         this.hasUnaligned = true;
         TQuad quadx = TQuad.fromUnaligned(facing, extents, vertexPositions, center, packedNormal);
         quadList.add(quadx);
         float distancex = quadx.getAccurateDotProduct();
         if (packedNormal == this.unalignedANormal) {
            if (Float.isNaN(this.unalignedADistance1)) {
               this.unalignedADistance1 = distancex;
            } else {
               this.unalignedADistance2 = distancex;
            }
         } else if (packedNormal == this.unalignedBNormal) {
            if (Float.isNaN(this.unalignedBDistance1)) {
               this.unalignedBDistance1 = distancex;
            } else {
               this.unalignedBDistance2 = distancex;
            }
         } else if (this.unalignedANormal == -1) {
            this.unalignedANormal = packedNormal;
            this.unalignedADistance1 = distancex;
         } else if (this.unalignedBNormal == -1) {
            this.unalignedBNormal = packedNormal;
            this.unalignedBDistance1 = distancex;
         }
      }
   }

   private static SortType filterSortType(SortType sortType) {
      SortBehavior sortBehavior = SodiumClientMod.options().debug.getSortBehavior();
      switch (sortBehavior) {
         case OFF:
            return SortType.NONE;
         case STATIC:
            if (sortType != SortType.STATIC_NORMAL_RELATIVE && sortType != SortType.STATIC_TOPO) {
               return SortType.NONE;
            }

            return sortType;
         default:
            return sortType;
      }
   }

   private SortType sortTypeHeuristic() {
      if (this.quads.length <= 1) {
         return SortType.NONE;
      } else {
         SortBehavior sortBehavior = SodiumClientMod.options().debug.getSortBehavior();
         if (sortBehavior.getSortMode() == SortBehavior.SortMode.NONE) {
            return SortType.NONE;
         } else {
            int alignedNormalCount = Integer.bitCount(this.alignedFacingBitmap);
            int planeCount = this.getPlaneCount(alignedNormalCount);
            int unalignedNormalCount = 0;
            if (this.unalignedANormal != -1) {
               unalignedNormalCount++;
            }

            if (this.unalignedBNormal != -1) {
               unalignedNormalCount++;
            }

            int normalCount = alignedNormalCount + unalignedNormalCount;
            if (planeCount <= 1) {
               return SortType.NONE;
            } else {
               if (!this.hasUnaligned) {
                  boolean opposingAlignedNormals = ModelQuadFacing.bitmapIsOpposingAligned(this.alignedFacingBitmap);
                  if (planeCount == 2 && opposingAlignedNormals) {
                     return SortType.NONE;
                  }

                  if (!this.alignedExtentsMultiple) {
                     boolean passesBoundingBoxTest = true;

                     for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
                        float extreme = this.alignedExtremes[direction];
                        if (!Float.isInfinite(extreme)) {
                           int sign = direction < 3 ? 1 : -1;
                           if (sign * extreme != this.extents[direction]) {
                              passesBoundingBoxTest = false;
                              break;
                           }
                        }
                     }

                     if (passesBoundingBoxTest) {
                        return SortType.NONE;
                     }
                  }

                  if (opposingAlignedNormals || alignedNormalCount == 1) {
                     return SortType.STATIC_NORMAL_RELATIVE;
                  }
               } else if (alignedNormalCount == 0) {
                  if (unalignedNormalCount == 1 || unalignedNormalCount == 2 && NormI8.isOpposite(this.unalignedANormal, this.unalignedBNormal)) {
                     return SortType.STATIC_NORMAL_RELATIVE;
                  }
               } else if (planeCount == 2) {
                  int alignedDirection = Integer.numberOfTrailingZeros(this.alignedFacingBitmap);
                  if (NormI8.isOpposite(this.unalignedANormal, ModelQuadFacing.PACKED_ALIGNED_NORMALS[alignedDirection])) {
                     return SortType.STATIC_NORMAL_RELATIVE;
                  }
               }

               int attemptLimitIndex = MathHelper.clamp(normalCount, 2, STATIC_TOPO_SORT_ATTEMPT_LIMITS.length - 1);
               return this.quads.length <= STATIC_TOPO_SORT_ATTEMPT_LIMITS[attemptLimitIndex] ? SortType.STATIC_TOPO : SortType.DYNAMIC;
            }
         }
      }
   }

   private int getPlaneCount(int alignedNormalCount) {
      int alignedPlaneCount = alignedNormalCount;
      if (this.alignedExtentsMultiple) {
         alignedPlaneCount = 100;
      }

      int unalignedPlaneCount = 0;
      if (!Float.isNaN(this.unalignedADistance1)) {
         unalignedPlaneCount++;
      }

      if (!Float.isNaN(this.unalignedADistance2)) {
         unalignedPlaneCount++;
      }

      if (!Float.isNaN(this.unalignedBDistance1)) {
         unalignedPlaneCount++;
      }

      if (!Float.isNaN(this.unalignedBDistance2)) {
         unalignedPlaneCount++;
      }

      return alignedPlaneCount + unalignedPlaneCount;
   }

   public SortType finishRendering() {
      int totalQuadCount = 0;

      for (ReferenceArrayList<TQuad> quadList : this.quadLists) {
         if (quadList != null) {
            totalQuadCount += quadList.size();
         }
      }

      this.quads = new TQuad[totalQuadCount];
      int quadIndex = 0;

      for (int direction = 0; direction < ModelQuadFacing.COUNT; direction++) {
         ReferenceArrayList<TQuad> quadListx = this.quadLists[direction];
         if (quadListx != null) {
            ObjectListIterator var10 = quadListx.iterator();

            while (var10.hasNext()) {
               TQuad quad = (TQuad)var10.next();
               this.quads[quadIndex++] = quad;
            }

            if (direction < ModelQuadFacing.DIRECTIONS) {
               this.alignedFacingBitmap |= 1 << direction;
            }
         }
      }

      this.quadLists = null;
      this.sortType = filterSortType(this.sortTypeHeuristic());
      return this.sortType;
   }

   private static int ensureUnassignedVertexCount(int[] vertexCounts) {
      int vertexCount = vertexCounts[ModelQuadFacing.UNASSIGNED.ordinal()];
      if (vertexCount == 0) {
         throw new IllegalStateException("No unassigned data in mesh");
      } else {
         return vertexCount;
      }
   }

   private TranslucentData makeNewTranslucentData(int[] vertexCounts, CombinedCameraPos cameraPos, TranslucentData oldData) {
      if (this.sortType == SortType.NONE) {
         return AnyOrderData.fromMesh(vertexCounts, this.quads, this.sectionPos);
      } else if (this.sortType == SortType.STATIC_NORMAL_RELATIVE) {
         boolean isDoubleUnaligned = this.alignedFacingBitmap == 0;
         return StaticNormalRelativeData.fromMesh(vertexCounts, this.quads, this.sectionPos, isDoubleUnaligned);
      } else {
         if (this.sortType == SortType.STATIC_TOPO) {
            int vertexCount = ensureUnassignedVertexCount(vertexCounts);
            StaticTopoData result = StaticTopoData.fromMesh(vertexCount, this.quads, this.sectionPos);
            if (result != null) {
               return result;
            }

            this.sortType = SortType.DYNAMIC;
         }

         this.sortType = filterSortType(this.sortType);
         if (this.sortType == SortType.NONE) {
            return AnyOrderData.fromMesh(vertexCounts, this.quads, this.sectionPos);
         } else if (this.sortType == SortType.DYNAMIC) {
            int vertexCount = ensureUnassignedVertexCount(vertexCounts);

            try {
               return DynamicBSPData.fromMesh(vertexCount, cameraPos, this.quads, this.sectionPos, oldData);
            } catch (BSPBuildFailureException var7) {
               GeometryPlanes geometryPlanes = GeometryPlanes.fromQuadLists(this.sectionPos, this.quads);
               return DynamicTopoData.fromMesh(vertexCount, cameraPos, this.quads, this.sectionPos, geometryPlanes);
            }
         } else {
            throw new IllegalStateException("Unknown sort type: " + this.sortType);
         }
      }
   }

   private int getQuadHash(TQuad[] quads) {
      if (this.quadHashPresent) {
         return this.quadHash;
      } else {
         for (int i = 0; i < quads.length; i++) {
            TQuad quad = quads[i];
            this.quadHash = this.quadHash * 31 + quad.getQuadHash() + i * 3;
         }

         this.quadHashPresent = true;
         return this.quadHash;
      }
   }

   public TranslucentData getTranslucentData(TranslucentData oldData, BuiltSectionMeshParts translucentMesh, CombinedCameraPos cameraPos) {
      if (translucentMesh == null) {
         return NoData.forNoTranslucent(this.sectionPos);
      } else {
         int[] vertexCounts = translucentMesh.getVertexCounts();
         if (oldData != null) {
            if (this.sortType == SortType.NONE
               && oldData instanceof AnyOrderData oldAnyData
               && oldAnyData.getQuadCount() == this.quads.length
               && Arrays.equals(oldAnyData.getVertexCounts(), vertexCounts)) {
               return oldAnyData;
            }

            if (oldData instanceof PresentTranslucentData oldPresentData
               && oldPresentData.getQuadCount() == this.quads.length
               && oldPresentData.getQuadHash() == this.getQuadHash(this.quads)) {
               return oldPresentData;
            }
         }

         TranslucentData newData = this.makeNewTranslucentData(vertexCounts, cameraPos, oldData);
         if (newData instanceof PresentTranslucentData presentData) {
            presentData.setQuadHash(this.getQuadHash(this.quads));
         }

         return newData;
      }
   }

   static {
      float targetEpsilon = 0.0021F;
      if (0.00390625F <= targetEpsilon && Integer.bitCount(256) == 1) {
         throw new RuntimeException("epsilon is invalid: 0.00390625");
      } else {
         STATIC_TOPO_SORT_ATTEMPT_LIMITS = new int[]{-1, -1, 250, 100, 50, 30};
         STATIC_TOPO_UNKNOWN_FALLBACK_LIMIT = STATIC_TOPO_SORT_ATTEMPT_LIMITS[STATIC_TOPO_SORT_ATTEMPT_LIMITS.length - 1];
      }
   }
}
