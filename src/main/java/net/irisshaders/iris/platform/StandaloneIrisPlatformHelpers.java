package net.irisshaders.iris.platform;

import com.vdurmont.semver4j.Semver;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public final class StandaloneIrisPlatformHelpers implements IrisPlatformHelpers {
   private static final String VERSION = "1.8.8+mc1.21.4";
   private static final List<KeyBinding> REGISTERED_KEY_BINDINGS = new ArrayList<>();

   public static KeyBinding[] getRegisteredKeyBindings() {
      synchronized (REGISTERED_KEY_BINDINGS) {
         return REGISTERED_KEY_BINDINGS.toArray(KeyBinding[]::new);
      }
   }

   @Override
   public boolean isModLoaded(String modId) {
      return "iris".equals(modId) || "sodium".equals(modId);
   }

   @Override
   public String getVersion() {
      return VERSION;
   }

   @Override
   public boolean isDevelopmentEnvironment() {
      return false;
   }

   @Override
   public Path getGameDir() {
      return MinecraftClient.getInstance().runDirectory.toPath();
   }

   @Override
   public Path getConfigDir() {
      return this.getGameDir().resolve("config");
   }

   @Override
   public int compareVersions(String currentVersion, String candidateVersion) {
      return new Semver(currentVersion, Semver.SemverType.LOOSE).compareTo(new Semver(candidateVersion, Semver.SemverType.LOOSE));
   }

   @Override
   public KeyBinding registerKeyBinding(KeyBinding keyBinding) {
      synchronized (REGISTERED_KEY_BINDINGS) {
         REGISTERED_KEY_BINDINGS.add(keyBinding);
         return keyBinding;
      }
   }

   @Override
   public boolean useELS() {
      return false;
   }

   @Override
   public BlockState getBlockAppearance(BlockRenderView world, BlockState state, Direction face, BlockPos pos) {
      return state;
   }
}
