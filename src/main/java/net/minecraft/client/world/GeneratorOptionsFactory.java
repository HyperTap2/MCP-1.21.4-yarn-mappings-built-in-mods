package net.minecraft.client.world;

import net.minecraft.client.gui.screen.world.WorldCreationSettings;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.DataPackContents;

@FunctionalInterface
public interface GeneratorOptionsFactory {
   GeneratorOptionsHolder apply(
      DataPackContents dataPackContents, CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries, WorldCreationSettings settings
   );
}
