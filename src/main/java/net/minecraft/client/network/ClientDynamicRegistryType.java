package net.minecraft.client.network;

import java.util.List;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.DynamicRegistryManager.Immutable;

public enum ClientDynamicRegistryType {
   STATIC,
   REMOTE;

   private static final List<ClientDynamicRegistryType> VALUES = List.of(values());
   private static final Immutable STATIC_REGISTRY_MANAGER = DynamicRegistryManager.of(Registries.REGISTRIES);

   public static CombinedDynamicRegistries<ClientDynamicRegistryType> createCombinedDynamicRegistries() {
      return new CombinedDynamicRegistries(VALUES).with(STATIC, new Immutable[]{STATIC_REGISTRY_MANAGER});
   }
}
