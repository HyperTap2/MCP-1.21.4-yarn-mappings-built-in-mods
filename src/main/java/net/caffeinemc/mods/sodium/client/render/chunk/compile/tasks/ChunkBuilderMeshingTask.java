package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.ExtendedBlockEntityType;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder.Vertex;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.PresentTranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.Sorter;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelRenderHooks;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.LightBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Vector3dc;

public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
   private final ChunkRenderContext renderContext;
   private final Vertex[] iris$vertices = Vertex.uninitializedQuad();

   public ChunkBuilderMeshingTask(RenderSection render, int buildTime, Vector3dc absoluteCameraPos, ChunkRenderContext renderContext) {
      super(render, buildTime, absoluteCameraPos);
      this.renderContext = renderContext;
   }

   public ChunkBuildOutput execute(ChunkBuildContext buildContext, CancellationToken cancellationToken) {
      Profiler profiler = Profilers.get();
      BuiltSectionInfo.Builder renderData = new BuiltSectionInfo.Builder();
      ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();
      ChunkBuildBuffers buffers = buildContext.buffers;
      buffers.init(renderData, this.render.getSectionIndex());
      BlockRenderCache cache = buildContext.cache;
      cache.init(this.renderContext);
      LevelSlice slice = cache.getWorldSlice();
      int minX = this.render.getOriginX();
      int minY = this.render.getOriginY();
      int minZ = this.render.getOriginZ();
      int maxX = minX + 16;
      int maxY = minY + 16;
      int maxZ = minZ + 16;
      Mutable blockPos = new Mutable(minX, minY, minZ);
      Mutable modelOffset = new Mutable();
      TranslucentGeometryCollector collector;
      if (SodiumClientMod.options().debug.getSortBehavior() != SortBehavior.OFF) {
         collector = new TranslucentGeometryCollector(this.render.getPosition());
      } else {
         collector = null;
      }

      BlockRenderer blockRenderer = cache.getBlockRenderer();
      blockRenderer.prepare(buffers, slice, collector);
      profiler.push("render blocks");

      try {
         for (int y = minY; y < maxY; y++) {
            if (cancellationToken.isCancelled()) {
               return null;
            }

            for (int z = minZ; z < maxZ; z++) {
               for (int x = minX; x < maxX; x++) {
                  BlockState blockState = slice.getBlockState(x, y, z);
                  if (!blockState.isAir() || blockState.hasBlockEntity()) {
                     blockPos.set(x, y, z);
                     modelOffset.set(x & 15, y & 15, z & 15);
                     try {
                        this.iris$voxelizeLightBlock(buffers, blockState, blockPos);
                        if (blockState.getRenderType() == BlockRenderType.MODEL) {
                           if (WorldRenderingSettings.INSTANCE.getBlockStateIds() != null) {
                              ((BlockSensitiveBufferBuilder)buffers)
                                 .beginBlock(
                                    WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(blockState, -1),
                                    (byte)0,
                                    (byte)blockState.getLuminance(),
                                    blockPos.getX(),
                                    blockPos.getY(),
                                    blockPos.getZ()
                                 );
                           }

                           BakedModel model = cache.getBlockModels().getModel(blockState);
                           blockRenderer.renderModel(model, blockState, blockPos, modelOffset);
                        }

                        FluidState fluidState = blockState.getFluidState();
                        if (!fluidState.isEmpty()) {
                           if (WorldRenderingSettings.INSTANCE.getBlockStateIds() != null) {
                              ((BlockSensitiveBufferBuilder)buffers)
                                 .beginBlock(
                                    WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(fluidState.getBlockState()),
                                    (byte)1,
                                    (byte)blockState.getLuminance(),
                                    blockPos.getX(),
                                    blockPos.getY(),
                                    blockPos.getZ()
                                 );
                           }

                           cache.getFluidRenderer().render(slice, blockState, fluidState, blockPos, modelOffset, collector, buffers);
                        }

                        if (blockState.hasBlockEntity()) {
                           BlockEntity entity = slice.getBlockEntity(blockPos);
                           if (entity != null && ExtendedBlockEntityType.shouldRender(entity.getType(), slice, blockPos, entity)) {
                              BlockEntityRenderer<BlockEntity> renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(entity);
                              if (renderer != null) {
                                 renderData.addBlockEntity(entity, !renderer.rendersOutsideBoundingBox(entity));
                              }
                           }
                        }
                     } finally {
                        ((BlockSensitiveBufferBuilder)buffers).endBlock();
                     }

                     if (blockState.isOpaqueFullCube()) {
                        occluder.markClosed(blockPos);
                     }
                  }
               }
            }
         }
      } catch (CrashException var26) {
         throw this.fillCrashInfo(var26.getReport(), slice, blockPos);
      } catch (Exception var27) {
         throw this.fillCrashInfo(CrashReport.create(var27, "Encountered exception while building chunk meshes"), slice, blockPos);
      }

      profiler.swap("mesh appenders");
      PlatformLevelRenderHooks.INSTANCE
         .runChunkMeshAppenders(
            this.renderContext.getRenderers(),
            type -> buffers.get(DefaultMaterials.forRenderLayer(type)).asFallbackVertexConsumer(DefaultMaterials.forRenderLayer(type), collector),
            slice
         );
      blockRenderer.release();
      SortType sortType = SortType.NONE;
      if (collector != null) {
         sortType = collector.finishRendering();
      }

      Map<TerrainRenderPass, BuiltSectionMeshParts> meshes = new Reference2ReferenceOpenHashMap();
      profiler.swap("meshing");

      for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
         BuiltSectionMeshParts mesh = buffers.createMesh(pass, pass.isTranslucent() && sortType.needsDirectionMixing);
         if (mesh != null) {
            meshes.put(pass, mesh);
            renderData.addRenderPass(pass);
         }
      }

      if (cancellationToken.isCancelled()) {
         meshes.forEach((passx, mesh) -> mesh.getVertexData().free());
         profiler.pop();
         return null;
      } else {
         renderData.setOcclusionData(occluder.build());
         profiler.swap("translucency sorting");
         boolean reuseUploadedData = false;
         TranslucentData translucentData = null;
         if (collector != null) {
            TranslucentData oldData = this.render.getTranslucentData();
            translucentData = collector.getTranslucentData(oldData, meshes.get(DefaultTerrainRenderPasses.TRANSLUCENT), this);
            reuseUploadedData = translucentData == oldData;
         }

         ChunkBuildOutput output = new ChunkBuildOutput(this.render, this.submitTime, translucentData, renderData.build(), meshes);
         if (collector != null) {
            if (reuseUploadedData) {
               output.markAsReusingUploadedData();
            } else if (translucentData instanceof PresentTranslucentData present) {
               Sorter sorter = present.getSorter();
               sorter.writeIndexBuffer(this, true);
               output.copyResultFrom(sorter);
            }
         }

         profiler.pop();
         return output;
      }
   }

   private void iris$voxelizeLightBlock(ChunkBuildBuffers buffers, BlockState blockState, Mutable blockPos) {
      if (WorldRenderingSettings.INSTANCE.getBlockStateIds() == null
         || !WorldRenderingSettings.INSTANCE.shouldVoxelizeLightBlocks()
         || !(blockState.getBlock() instanceof LightBlock)) {
         return;
      }

      ChunkModelBuilder builder = buffers.get(DefaultMaterials.CUTOUT);
      BlockSensitiveBufferBuilder context = buffers;
      context.ignoreMidBlock(true);
      try {
         context.beginBlock(
            WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(blockState), (byte)0, (byte)blockState.getLuminance(), 0, 0, 0
         );

         for (Vertex vertex : this.iris$vertices) {
            vertex.x = (blockPos.getX() & 15) + 0.25F;
            vertex.y = (blockPos.getY() & 15) + 0.25F;
            vertex.z = (blockPos.getZ() & 15) + 0.25F;
            vertex.u = 0.0F;
            vertex.v = 0.0F;
            vertex.color = 0;
            vertex.light = blockState.getLuminance() << 4 | blockState.getLuminance() << 20;
         }

         builder.getVertexBuffer(ModelQuadFacing.UNASSIGNED).push(this.iris$vertices, DefaultMaterials.CUTOUT);
      } finally {
         context.ignoreMidBlock(false);
      }
   }

   private CrashException fillCrashInfo(CrashReport report, LevelSlice slice, BlockPos pos) {
      CrashReportSection crashReportSection = report.addElement("Block being rendered", 1);
      BlockState state = null;

      try {
         state = slice.getBlockState(pos);
      } catch (Exception var7) {
      }

      CrashReportSection.addBlockInfo(crashReportSection, slice, pos, state);
      crashReportSection.add("Chunk section", this.render);
      if (this.renderContext != null) {
         crashReportSection.add("Render context volume", this.renderContext.getVolume());
      }

      return new CrashException(report);
   }

   @Override
   public int getEffort() {
      return 10;
   }
}
