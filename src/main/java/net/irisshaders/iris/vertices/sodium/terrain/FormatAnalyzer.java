package net.irisshaders.iris.vertices.sodium.terrain;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat.Builder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.DefaultChunkMeshAttributes;

public class FormatAnalyzer {
   private static final Byte2ObjectMap<ChunkVertexType> classMap = new Byte2ObjectOpenHashMap();

   public static ChunkVertexType createFormat(boolean blockId, boolean normal, boolean midUV, boolean midBlock) {
      byte key = 0;
      if (blockId) {
         key = (byte)(key | 1);
      }

      if (normal) {
         key = (byte)(key | 2);
      }

      if (midUV) {
         key = (byte)(key | 4);
      }

      if (midBlock) {
         key = (byte)(key | 8);
      }

      if (classMap.containsKey(key)) {
         return (ChunkVertexType)classMap.get(key);
      }

      int offset = 20;
      int blockIdOffset;
      if (blockId) {
         blockIdOffset = offset;
         offset += 4;
      } else {
         blockIdOffset = 0;
      }

      int normalOffset;
      if (normal) {
         normalOffset = offset;
         offset += 4;
      } else {
         normalOffset = 0;
      }

      int midUvOffset;
      if (midUV) {
         midUvOffset = offset;
         offset += 4;
      } else {
         midUvOffset = 0;
      }

      int midBlockOffset;
      if (midBlock) {
         midBlockOffset = offset;
         offset += 4;
      } else {
         midBlockOffset = 0;
      }

      Builder VERTEX_FORMAT = GlVertexFormat.builder(offset)
         .addElement(DefaultChunkMeshAttributes.POSITION, 0, 0)
         .addElement(DefaultChunkMeshAttributes.COLOR, 1, 8)
         .addElement(DefaultChunkMeshAttributes.TEXTURE, 2, 12)
         .addElement(DefaultChunkMeshAttributes.LIGHT_MATERIAL_INDEX, 3, 16);
      if (blockId) {
         VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.BLOCK_ID, 11, blockIdOffset);
      }

      if (normal) {
         VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.NORMAL, 10, normalOffset);
      }

      if (midUV) {
         VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.MID_TEX_COORD, 12, midUvOffset);
      }

      if (midBlock) {
         VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.MID_BLOCK, 14, midBlockOffset);
      }

      return (ChunkVertexType)classMap.computeIfAbsent(
         key, k -> new XHFPModelVertexType(VERTEX_FORMAT.build(), blockIdOffset, normalOffset, midUvOffset, midBlockOffset)
      );
   }

   static {
      classMap.put((byte)0, ChunkMeshFormats.COMPACT);
   }
}
