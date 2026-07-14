package net.caffeinemc.mods.sodium.api.vertex.serializer;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.client.render.VertexFormat;

public interface VertexSerializerRegistry {
   VertexSerializerRegistry INSTANCE = DependencyInjection.load(
      VertexSerializerRegistry.class, "net.caffeinemc.mods.sodium.client.render.vertex.serializers.VertexSerializerRegistryImpl"
   );

   static VertexSerializerRegistry instance() {
      return INSTANCE;
   }

   VertexSerializer get(VertexFormat var1, VertexFormat var2);

   void registerSerializer(VertexFormat var1, VertexFormat var2, VertexSerializer var3);
}
