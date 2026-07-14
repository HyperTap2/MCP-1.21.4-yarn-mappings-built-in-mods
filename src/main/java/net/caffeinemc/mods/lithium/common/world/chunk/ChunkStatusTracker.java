package net.caffeinemc.mods.lithium.common.world.chunk;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import net.caffeinemc.mods.lithium.common.tracking.block.ChunkSectionChangeCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

public final class ChunkStatusTracker {
   private static final ArrayList<BiConsumer<ServerWorld, ChunkPos>> UNLOAD_CALLBACKS = new ArrayList<>();
   private static final ArrayList<BiConsumer<ServerWorld, WorldChunk>> LOAD_CALLBACKS = new ArrayList<>();

   private ChunkStatusTracker() {
   }

   public static synchronized void registerLoadCallback(BiConsumer<ServerWorld, WorldChunk> callback) {
      LOAD_CALLBACKS.add(callback);
   }

   public static synchronized void registerUnloadCallback(BiConsumer<ServerWorld, ChunkPos> callback) {
      UNLOAD_CALLBACKS.add(callback);
   }

   public static void onChunkAccessible(ServerWorld world, WorldChunk chunk) {
      checkThread(world);
      for (BiConsumer<ServerWorld, WorldChunk> callback : LOAD_CALLBACKS) {
         callback.accept(world, chunk);
      }
   }

   public static void onChunkInaccessible(ServerWorld world, ChunkPos pos) {
      checkThread(world);
      for (BiConsumer<ServerWorld, ChunkPos> callback : UNLOAD_CALLBACKS) {
         callback.accept(world, pos);
      }
   }

   private static void checkThread(ServerWorld world) {
      if (world.getServer() != null && !world.getServer().isOnThread()) {
         throw new IllegalStateException("Chunk status callback ran off the server thread");
      }
   }

   static {
      ChunkSectionChangeCallback.init();
   }
}
