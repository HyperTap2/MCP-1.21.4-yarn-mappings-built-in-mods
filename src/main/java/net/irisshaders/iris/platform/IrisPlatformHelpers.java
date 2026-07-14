package net.irisshaders.iris.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public interface IrisPlatformHelpers {
   IrisPlatformHelpers INSTANCE = ServiceLoader.load(IrisPlatformHelpers.class).findFirst().get();

   static IrisPlatformHelpers getInstance() {
      return INSTANCE;
   }

   boolean isModLoaded(String var1);

   String getVersion();

   boolean isDevelopmentEnvironment();

   Path getGameDir();

   Path getConfigDir();

   int compareVersions(String var1, String var2) throws Exception;

   KeyBinding registerKeyBinding(KeyBinding var1);

   boolean useELS();

   BlockState getBlockAppearance(BlockRenderView var1, BlockState var2, Direction var3, BlockPos var4);
}
