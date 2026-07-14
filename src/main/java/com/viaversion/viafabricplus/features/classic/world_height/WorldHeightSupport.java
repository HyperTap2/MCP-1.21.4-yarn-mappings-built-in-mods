package com.viaversion.viafabricplus.features.classic.world_height;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.model.ClassicLevel;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicWorldHeightProvider;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.storage.ClassicLevelStorage;

public final class WorldHeightSupport {
   public static PacketHandler handleJoinGame(PacketHandler parentHandler) {
      return wrapper -> {
         parentHandler.handle(wrapper);
         if (!wrapper.isCancelled()) {
            if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
               for (CompoundTag dimension : ((CompoundTag)wrapper.get(Types.NAMED_COMPOUND_TAG, 0))
                  .getCompoundTag("minecraft:dimension_type")
                  .getListTag("value", CompoundTag.class)) {
                  changeDimensionTagHeight(wrapper.user(), dimension.getCompoundTag("element"));
               }

               changeDimensionTagHeight(wrapper.user(), (CompoundTag)wrapper.get(Types.NAMED_COMPOUND_TAG, 1));
            }
         }
      };
   }

   public static PacketHandler handleRespawn(PacketHandler parentHandler) {
      return wrapper -> {
         parentHandler.handle(wrapper);
         if (!wrapper.isCancelled()) {
            if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
               changeDimensionTagHeight(wrapper.user(), (CompoundTag)wrapper.get(Types.NAMED_COMPOUND_TAG, 0));
            }
         }
      };
   }

   public static PacketHandler handleChunkData(PacketHandler parentHandler) {
      return wrapper -> {
         parentHandler.handle(wrapper);
         if (!wrapper.isCancelled()) {
            if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
               wrapper.resetReader();
               Chunk chunk = (Chunk)wrapper.read(new ChunkType1_17(16));
               wrapper.write(new ChunkType1_17(chunk.getSections().length), chunk);
               ClassicWorldHeightProvider heightProvider = (ClassicWorldHeightProvider)Via.getManager().getProviders().get(ClassicWorldHeightProvider.class);
               if (chunk.getSections().length < heightProvider.getMaxChunkSectionCount(wrapper.user())) {
                  ChunkSection[] newArray = new ChunkSection[heightProvider.getMaxChunkSectionCount(wrapper.user())];
                  System.arraycopy(chunk.getSections(), 0, newArray, 0, chunk.getSections().length);
                  chunk.setSections(newArray);
               }

               BitSet chunkMask = new BitSet();

               for (int i = 0; i < chunk.getSections().length; i++) {
                  if (chunk.getSections()[i] != null) {
                     chunkMask.set(i);
                  }
               }

               chunk.setChunkMask(chunkMask);
               int[] newBiomeData = new int[chunk.getSections().length * 4 * 4 * 4];
               System.arraycopy(chunk.getBiomeData(), 0, newBiomeData, 0, chunk.getBiomeData().length);

               for (int i = 64; i < chunk.getSections().length * 4; i++) {
                  System.arraycopy(chunk.getBiomeData(), chunk.getBiomeData().length - 16, newBiomeData, i * 16, 16);
               }

               chunk.setBiomeData(newBiomeData);
               chunk.setHeightMap(new CompoundTag());
            }
         }
      };
   }

   public static PacketHandler handleUpdateLight(PacketHandler parentHandler) {
      PacketHandler classicLightHandler = new PacketHandlers() {
         public void register() {
            this.map(Types.VAR_INT);
            this.map(Types.VAR_INT);
            this.map(Types.BOOLEAN);
            this.handler(wrapper -> {
               wrapper.read(Types.VAR_INT);
               wrapper.read(Types.VAR_INT);
               int emptySkyLightMask = (Integer)wrapper.read(Types.VAR_INT);
               int emptyBlockLightMask = (Integer)wrapper.read(Types.VAR_INT);
               ClassicLevel level = ((ClassicLevelStorage)wrapper.user().get(ClassicLevelStorage.class)).getClassicLevel();
               ClassicWorldHeightProvider heightProvider = (ClassicWorldHeightProvider)Via.getManager().getProviders().get(ClassicWorldHeightProvider.class);
               int sectionYCount = level.getSizeY() >> 4;
               if (level.getSizeY() % 16 != 0) {
                  sectionYCount++;
               }

               if (sectionYCount > heightProvider.getMaxChunkSectionCount(wrapper.user())) {
                  sectionYCount = heightProvider.getMaxChunkSectionCount(wrapper.user());
               }

               List<byte[]> lightArrays = new ArrayList<>();

               while (wrapper.isReadable(Types.BYTE_ARRAY_PRIMITIVE, 0)) {
                  lightArrays.add((byte[])wrapper.read(Types.BYTE_ARRAY_PRIMITIVE));
               }

               int skyLightCount = 16;
               int blockLightCount = sectionYCount;
               if (lightArrays.size() == 18) {
                  blockLightCount = 0;
               } else if (lightArrays.size() != 16 + sectionYCount + 2 && lightArrays.size() == sectionYCount + sectionYCount + 2) {
                  skyLightCount = sectionYCount;
               }

               skyLightCount += 2;
               BitSet skyLightMask = new BitSet();
               BitSet blockLightMask = new BitSet();
               skyLightMask.set(0, skyLightCount);
               blockLightMask.set(0, blockLightCount);
               wrapper.write(Types.LONG_ARRAY_PRIMITIVE, skyLightMask.toLongArray());
               wrapper.write(Types.LONG_ARRAY_PRIMITIVE, blockLightMask.toLongArray());
               wrapper.write(Types.LONG_ARRAY_PRIMITIVE, new long[emptySkyLightMask]);
               wrapper.write(Types.LONG_ARRAY_PRIMITIVE, new long[emptyBlockLightMask]);
               wrapper.write(Types.VAR_INT, skyLightCount);

               for (int i = 0; i < skyLightCount; i++) {
                  wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, lightArrays.remove(0));
               }

               wrapper.write(Types.VAR_INT, blockLightCount);

               for (int i = 0; i < blockLightCount; i++) {
                  wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, lightArrays.remove(0));
               }
            });
         }
      };
      return wrapper -> {
         if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
            classicLightHandler.handle(wrapper);
         } else {
            parentHandler.handle(wrapper);
         }
      };
   }

   private static void changeDimensionTagHeight(UserConnection connection, CompoundTag tag) {
      tag.putInt(
         "height", ((ClassicWorldHeightProvider)Via.getManager().getProviders().get(ClassicWorldHeightProvider.class)).getMaxChunkSectionCount(connection) << 4
      );
   }
}
