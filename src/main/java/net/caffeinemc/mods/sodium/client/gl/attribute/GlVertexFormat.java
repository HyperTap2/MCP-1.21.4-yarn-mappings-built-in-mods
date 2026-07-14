package net.caffeinemc.mods.sodium.client.gl.attribute;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute;

public class GlVertexFormat {
   private final Map<VertexFormatAttribute, GlVertexAttribute> attributesKeyed;
   private final int stride;
   private final GlVertexAttributeBinding[] bindings;

   public GlVertexFormat(Map<VertexFormatAttribute, GlVertexAttribute> attributesKeyed, GlVertexAttributeBinding[] bindings, int stride) {
      this.attributesKeyed = attributesKeyed;
      this.bindings = bindings;
      this.stride = stride;
   }

   public static GlVertexFormat.Builder builder(int stride) {
      return new GlVertexFormat.Builder(stride);
   }

   public GlVertexAttribute getAttribute(VertexFormatAttribute name) {
      GlVertexAttribute attr = this.attributesKeyed.get(name);
      if (attr == null) {
         throw new NullPointerException("No attribute exists for " + name.toString());
      } else {
         return attr;
      }
   }

   public int getStride() {
      return this.stride;
   }

   @Override
   public String toString() {
      return String.format("GlVertexFormat{attributes=%d,stride=%d}", this.attributesKeyed.size(), this.stride);
   }

   public GlVertexAttributeBinding[] getShaderBindings() {
      return this.bindings;
   }

   public static class Builder {
      private final Map<VertexFormatAttribute, GlVertexAttribute> attributes = new Object2ObjectArrayMap();
      private final Object2IntMap<GlVertexAttribute> bindings = new Object2IntArrayMap();
      private final int stride;

      public Builder(int stride) {
         this.stride = stride;
      }

      public GlVertexFormat.Builder addElement(VertexFormatAttribute attribute, int binding, int pointer) {
         return this.addElement(
            attribute, binding, new GlVertexAttribute(attribute.format(), attribute.count(), attribute.normalized(), pointer, this.stride, attribute.intType())
         );
      }

      private GlVertexFormat.Builder addElement(VertexFormatAttribute type, int binding, GlVertexAttribute attribute) {
         if (attribute.getPointer() >= this.stride) {
            throw new IllegalArgumentException("Element starts outside vertex format");
         } else if (attribute.getPointer() + attribute.getSize() > this.stride) {
            throw new IllegalArgumentException("Element extends outside vertex format");
         } else if (this.attributes.put(type, attribute) != null) {
            throw new IllegalStateException("Generic attribute " + type.name() + " already defined in vertex format");
         } else {
            if (binding != -1) {
               this.bindings.put(attribute, binding);
            }

            return this;
         }
      }

      public GlVertexFormat build() {
         int size = 0;

         for (GlVertexAttribute attribute : this.attributes.values()) {
            size = Math.max(size, attribute.getPointer() + attribute.getSize());
         }

         if (this.stride < size) {
            throw new IllegalArgumentException("Stride is too small");
         } else {
            GlVertexAttributeBinding[] bindings = this.bindings
               .object2IntEntrySet()
               .stream()
               .map(entry -> new GlVertexAttributeBinding(entry.getIntValue(), (GlVertexAttribute)entry.getKey()))
               .toArray(GlVertexAttributeBinding[]::new);
            return new GlVertexFormat(this.attributes, bindings, this.stride);
         }
      }
   }
}
