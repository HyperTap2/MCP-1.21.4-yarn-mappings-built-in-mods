package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BlockRenderContext {
   private final LevelSlice slice;
   public final TranslucentGeometryCollector collector;
   private final Mutable pos = new Mutable();
   private final Vector3f origin = new Vector3f();
   private BlockState state;
   private BakedModel model;
   private long seed;

   public BlockRenderContext(LevelSlice slice, TranslucentGeometryCollector collector) {
      this.slice = slice;
      this.collector = collector;
   }

   public void update(BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed) {
      this.pos.set(pos);
      this.origin.set(origin.getX(), origin.getY(), origin.getZ());
      this.state = state;
      this.model = model;
      this.seed = seed;
   }

   public TranslucentGeometryCollector collector() {
      return this.collector;
   }

   public BlockPos pos() {
      return this.pos;
   }

   public LevelSlice slice() {
      return this.slice;
   }

   public BlockState state() {
      return this.state;
   }

   public BakedModel model() {
      return this.model;
   }

   public Vector3fc origin() {
      return this.origin;
   }

   public long seed() {
      return this.seed;
   }
}
