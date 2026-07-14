package net.minecraft.client.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatExtensions;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.programs.VertexFormatExtension;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import org.jetbrains.annotations.Nullable;

public class VertexFormat implements VertexFormatExtensions, VertexFormatExtension {
   public static final int field_52099 = -1;
   private final List<VertexFormatElement> elements;
   private final List<String> names;
   private final int vertexSizeByte;
   private final int requiredMask;
   private final int[] offsetsByElementId = new int[32];
   @Nullable
   private VertexBuffer buffer;
   private final int sodium$globalId;

   VertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSizeByte) {
      this.elements = elements;
      this.names = names;
      this.vertexSizeByte = vertexSizeByte;
      this.requiredMask = elements.stream().mapToInt(VertexFormatElement::getBit).reduce(0, (a, b) -> a | b);

      for (int i = 0; i < this.offsetsByElementId.length; i++) {
         VertexFormatElement vertexFormatElement = VertexFormatElement.get(i);
         int j = vertexFormatElement != null ? elements.indexOf(vertexFormatElement) : -1;
         this.offsetsByElementId[i] = j != -1 ? offsets.getInt(j) : -1;
      }
      this.sodium$globalId = VertexFormatRegistry.instance().allocateGlobalId(this);
   }

   @Override
   public int sodium$getGlobalId() {
      return this.sodium$globalId;
   }

   public static VertexFormat.Builder builder() {
      return new VertexFormat.Builder();
   }

   public void bindAttributes(int program) {
      int i = 0;

      for (String string : this.getAttributeNames()) {
         GlStateManager._glBindAttribLocation(program, i, string);
         i++;
      }
   }

   @Override
   public void bindAttributesIris(int program) {
      int index = 0;

      for (String name : this.getAttributeNames()) {
         boolean vanillaAttribute = name.equals("Position")
            || name.equals("Color")
            || name.equals("Normal")
            || name.equals("UV0")
            || name.equals("UV1")
            || name.equals("UV2");
         GlStateManager._glBindAttribLocation(program, index++, vanillaAttribute ? "iris_" + name : name);
      }
   }

   @Override
   public String toString() {
      return "VertexFormat" + this.names;
   }

   public int getVertexSizeByte() {
      return this.vertexSizeByte;
   }

   public List<VertexFormatElement> getElements() {
      return this.elements;
   }

   public List<String> getAttributeNames() {
      return this.names;
   }

   public int[] getOffsetsByElementId() {
      return this.offsetsByElementId;
   }

   public int getOffset(VertexFormatElement element) {
      return this.offsetsByElementId[element.id()];
   }

   public boolean has(VertexFormatElement element) {
      return (this.requiredMask & element.getBit()) != 0;
   }

   public int getRequiredMask() {
      return this.requiredMask;
   }

   public String getName(VertexFormatElement element) {
      int i = this.elements.indexOf(element);
      if (i == -1) {
         throw new IllegalArgumentException(element + " is not contained in format");
      } else {
         return this.names.get(i);
      }
   }

   @Override
   public boolean equals(Object o) {
      return this == o
         ? true
         : o instanceof VertexFormat vertexFormat
            && this.requiredMask == vertexFormat.requiredMask
            && this.vertexSizeByte == vertexFormat.vertexSizeByte
            && this.names.equals(vertexFormat.names)
            && Arrays.equals(this.offsetsByElementId, vertexFormat.offsetsByElementId);
   }

   @Override
   public int hashCode() {
      return this.requiredMask * 31 + Arrays.hashCode(this.offsetsByElementId);
   }

   public void setupState() {
      VertexFormat extendedFormat = this.getIrisExtendedFormat();
      if (extendedFormat != null) {
         extendedFormat.setupState();
         return;
      }

      RenderSystem.assertOnRenderThread();
      int i = this.getVertexSizeByte();

      for (int j = 0; j < this.elements.size(); j++) {
         GlStateManager._enableVertexAttribArray(j);
         VertexFormatElement vertexFormatElement = this.elements.get(j);
         vertexFormatElement.setupState(j, this.getOffset(vertexFormatElement), i);
      }
   }

   public void clearState() {
      VertexFormat extendedFormat = this.getIrisExtendedFormat();
      if (extendedFormat != null) {
         extendedFormat.clearState();
         return;
      }

      RenderSystem.assertOnRenderThread();

      for (int i = 0; i < this.elements.size(); i++) {
         GlStateManager._disableVertexAttribArray(i);
      }
   }

   @Nullable
   private VertexFormat getIrisExtendedFormat() {
      if (!Iris.isPackInUseQuick() || !ImmediateState.renderWithExtendedVertexFormat) {
         return null;
      }

      if (this == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL) {
         return IrisVertexFormats.TERRAIN;
      } else if (this == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT) {
         return IrisVertexFormats.GLYPH;
      } else if (this == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
         return IrisVertexFormats.ENTITY;
      }

      return null;
   }

   public VertexBuffer getBuffer() {
      VertexBuffer vertexBuffer = this.buffer;
      if (vertexBuffer == null) {
         this.buffer = vertexBuffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);
      }

      return vertexBuffer;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
      private final IntList offsets = new IntArrayList();
      private int currentOffset;

      Builder() {
      }

      public VertexFormat.Builder add(String name, VertexFormatElement element) {
         this.elements.put(name, element);
         this.offsets.add(this.currentOffset);
         this.currentOffset = this.currentOffset + element.getSizeInBytes();
         return this;
      }

      public VertexFormat.Builder skip(int offset) {
         this.currentOffset += offset;
         return this;
      }

      public VertexFormat build() {
         ImmutableMap<String, VertexFormatElement> immutableMap = this.elements.buildOrThrow();
         ImmutableList<VertexFormatElement> immutableList = immutableMap.values().asList();
         ImmutableList<String> immutableList2 = immutableMap.keySet().asList();
         return new VertexFormat(immutableList, immutableList2, this.offsets, this.currentOffset);
      }
   }

   public enum DrawMode {
      LINES(4, 2, 2, false),
      LINE_STRIP(5, 2, 1, true),
      DEBUG_LINES(1, 2, 2, false),
      DEBUG_LINE_STRIP(3, 2, 1, true),
      TRIANGLES(4, 3, 3, false),
      TRIANGLE_STRIP(5, 3, 1, true),
      TRIANGLE_FAN(6, 3, 1, true),
      QUADS(4, 4, 4, false);

      public final int glMode;
      public final int firstVertexCount;
      public final int additionalVertexCount;
      public final boolean shareVertices;

      DrawMode(final int glMode, final int firstVertexCount, final int additionalVertexCount, final boolean shareVertices) {
         this.glMode = glMode;
         this.firstVertexCount = firstVertexCount;
         this.additionalVertexCount = additionalVertexCount;
         this.shareVertices = shareVertices;
      }

      public int getIndexCount(int vertexCount) {
         return switch (this) {
            case LINES, QUADS -> vertexCount / 4 * 6;
            case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> vertexCount;
            default -> 0;
         };
      }
   }

   public enum IndexType {
      SHORT(5123, 2),
      INT(5125, 4);

      public final int glType;
      public final int size;

      IndexType(final int glType, final int size) {
         this.glType = glType;
         this.size = size;
      }

      public static VertexFormat.IndexType smallestFor(int indexCount) {
         return (indexCount & -65536) != 0 ? INT : SHORT;
      }
   }
}
