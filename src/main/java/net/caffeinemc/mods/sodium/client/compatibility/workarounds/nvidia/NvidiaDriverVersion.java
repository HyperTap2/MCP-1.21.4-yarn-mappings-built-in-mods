package net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia;

import net.caffeinemc.mods.sodium.client.platform.windows.WindowsFileVersion;

public record NvidiaDriverVersion(int major, int minor) {
   public static NvidiaDriverVersion parse(WindowsFileVersion version) {
      int merged = (version.z() - 10) * 10000 + version.w();
      int major = merged / 100;
      int minor = merged % 100;
      return new NvidiaDriverVersion(major, minor);
   }

   @Override
   public String toString() {
      return "%d.%d".formatted(this.major, this.minor);
   }
}
