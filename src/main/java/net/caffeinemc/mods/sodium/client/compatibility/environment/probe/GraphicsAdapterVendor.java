package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import java.util.regex.Pattern;
import net.caffeinemc.mods.sodium.client.compatibility.environment.GlContextInfo;
import org.jetbrains.annotations.NotNull;

public enum GraphicsAdapterVendor {
   NVIDIA,
   AMD,
   INTEL,
   UNKNOWN;

   private static final Pattern INTEL_ICD_PATTERN = Pattern.compile("ig(4|7|75|8|9|11|12|(xe2?(hpg?|lpg?)))icd(32|64)\\.dll", 2);
   private static final Pattern NVIDIA_ICD_PATTERN = Pattern.compile("nvoglv(32|64)\\.dll", 2);
   private static final Pattern AMD_ICD_PATTERN = Pattern.compile("(atiglpxx|atig6pxx)\\.dll", 2);

   @NotNull
   static GraphicsAdapterVendor fromPciVendorId(String vendor) {
      if (vendor.contains("0x1002")) {
         return AMD;
      } else if (vendor.contains("0x10de")) {
         return NVIDIA;
      } else {
         return vendor.contains("0x8086") ? INTEL : UNKNOWN;
      }
   }

   @NotNull
   public static GraphicsAdapterVendor fromIcdName(String name) {
      if (matchesPattern(INTEL_ICD_PATTERN, name)) {
         return INTEL;
      } else if (matchesPattern(NVIDIA_ICD_PATTERN, name)) {
         return NVIDIA;
      } else {
         return matchesPattern(AMD_ICD_PATTERN, name) ? AMD : UNKNOWN;
      }
   }

   @NotNull
   public static GraphicsAdapterVendor fromContext(GlContextInfo context) {
      String vendor = context.vendor();

      return switch (vendor) {
         case "NVIDIA Corporation" -> NVIDIA;
         case "Intel", "Intel Open Source Technology Center" -> INTEL;
         case "AMD", "ATI Technologies Inc." -> AMD;
         default -> UNKNOWN;
      };
   }

   private static boolean matchesPattern(Pattern pattern, String name) {
      return pattern.matcher(name).matches();
   }
}
