package net.irisshaders.iris.vertices;

import net.irisshaders.iris.Iris;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormatElement.ComponentType;
import net.minecraft.client.render.VertexFormatElement.Usage;

public class IrisVertexFormats {
   public static final VertexFormatElement ENTITY_ELEMENT;
   public static final VertexFormatElement ENTITY_ID_ELEMENT;
   public static final VertexFormatElement MID_TEXTURE_ELEMENT;
   public static final VertexFormatElement TANGENT_ELEMENT;
   public static final VertexFormatElement MID_BLOCK_ELEMENT;
   public static final VertexFormat TERRAIN;
   public static final VertexFormat ENTITY;
   public static final VertexFormat GLYPH;
   public static final VertexFormat CLOUDS;

   private static void debug(VertexFormat format) {
      Iris.logger.info("Vertex format: " + format + " with byte size " + format.getVertexSizeByte());
      int byteIndex = 0;

      for (VertexFormatElement element : format.getElements()) {
         Iris.logger.info(element + " @ " + byteIndex + " is " + element.type() + " " + element.usage());
         byteIndex += element.getSizeInBytes();
      }
   }

   private static int getNextVertexFormatElementId() {
      int id = 0;

      while (VertexFormatElement.get(id) != null) {
         if (++id >= 32) {
            throw new RuntimeException("Too many mods registering VertexFormatElements");
         }
      }

      return id;
   }

   static {
      int LAST_UV = 0;

      for (int i = 0; i < 32; i++) {
         VertexFormatElement element = VertexFormatElement.get(i);
         if (element != null && element.usage() == Usage.UV) {
            LAST_UV = Math.max(LAST_UV, element.uvIndex());
         }
      }

      ENTITY_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, ComponentType.SHORT, Usage.GENERIC, 2);
      ENTITY_ID_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), LAST_UV + 1, ComponentType.USHORT, Usage.UV, 3);
      MID_TEXTURE_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, ComponentType.FLOAT, Usage.GENERIC, 2);
      TANGENT_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, ComponentType.BYTE, Usage.GENERIC, 4);
      MID_BLOCK_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, ComponentType.BYTE, Usage.GENERIC, 3);
      TERRAIN = VertexFormat.builder()
         .add("Position", VertexFormatElement.POSITION)
         .add("Color", VertexFormatElement.COLOR)
         .add("UV0", VertexFormatElement.UV_0)
         .add("UV2", VertexFormatElement.UV_2)
         .add("Normal", VertexFormatElement.NORMAL)
         .skip(1)
         .add("mc_Entity", ENTITY_ELEMENT)
         .add("mc_midTexCoord", MID_TEXTURE_ELEMENT)
         .add("at_tangent", TANGENT_ELEMENT)
         .add("at_midBlock", MID_BLOCK_ELEMENT)
         .skip(1)
         .build();
      ENTITY = VertexFormat.builder()
         .add("Position", VertexFormatElement.POSITION)
         .add("Color", VertexFormatElement.COLOR)
         .add("UV0", VertexFormatElement.UV_0)
         .add("UV1", VertexFormatElement.UV_1)
         .add("UV2", VertexFormatElement.UV_2)
         .add("Normal", VertexFormatElement.NORMAL)
         .skip(1)
         .add("iris_Entity", ENTITY_ID_ELEMENT)
         .skip(2)
         .add("mc_midTexCoord", MID_TEXTURE_ELEMENT)
         .add("at_tangent", TANGENT_ELEMENT)
         .build();
      GLYPH = VertexFormat.builder()
         .add("Position", VertexFormatElement.POSITION)
         .add("Color", VertexFormatElement.COLOR)
         .add("UV0", VertexFormatElement.UV_0)
         .add("UV2", VertexFormatElement.UV_2)
         .add("Normal", VertexFormatElement.NORMAL)
         .skip(1)
         .add("iris_Entity", ENTITY_ID_ELEMENT)
         .skip(2)
         .add("mc_midTexCoord", MID_TEXTURE_ELEMENT)
         .add("at_tangent", TANGENT_ELEMENT)
         .build();
      CLOUDS = VertexFormat.builder()
         .add("Position", VertexFormatElement.POSITION)
         .add("Color", VertexFormatElement.COLOR)
         .add("Normal", VertexFormatElement.NORMAL)
         .skip(1)
         .build();
   }
}
