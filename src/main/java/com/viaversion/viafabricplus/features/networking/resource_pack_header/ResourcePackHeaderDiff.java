package com.viaversion.viafabricplus.features.networking.resource_pack_header;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.GameVersion;
import net.minecraft.SaveVersion;
import net.minecraft.SharedConstants;
import net.minecraft.resource.ResourceType;

public final class ResourcePackHeaderDiff {
   private static final Map<ProtocolVersion, GameVersion> GAME_VERSION_DIFF = new HashMap<>();

   public static void init() {
   }

   public static GameVersion get(ProtocolVersion version) {
      return !GAME_VERSION_DIFF.containsKey(version) ? SharedConstants.getGameVersion() : GAME_VERSION_DIFF.get(version);
   }

   private static void registerVersion(ProtocolVersion version, int packFormat, String name) {
      registerVersion(version, packFormat, name, name);
   }

   private static void registerVersion(ProtocolVersion version, int packFormat, String name, String id) {
      GAME_VERSION_DIFF.put(version, new GameVersion() {
         public SaveVersion getSaveVersion() {
            return null;
         }

         public String getId() {
            return id;
         }

         public String getName() {
            return name;
         }

         public int getProtocolVersion() {
            return version.getOriginalVersion();
         }

         public int getResourceVersion(ResourceType type) {
            if (type == ResourceType.CLIENT_RESOURCES) {
               return packFormat;
            } else {
               throw new UnsupportedOperationException();
            }
         }

         public Date getBuildTime() {
            return null;
         }

         public boolean isStable() {
            return true;
         }
      });
   }

   static {
      registerVersion(ProtocolVersion.v1_21_5, 55, "1.21.5");
      registerVersion(ProtocolVersion.v1_21_4, 46, "1.21.4");
      registerVersion(ProtocolVersion.v1_21_2, 42, "1.21.3");
      registerVersion(ProtocolVersion.v1_21, 34, "1.21.1");
      registerVersion(ProtocolVersion.v1_20_5, 32, "1.20.6");
      registerVersion(ProtocolVersion.v1_20_3, 22, "1.20.4");
      registerVersion(ProtocolVersion.v1_20_2, 18, "1.20.2");
      registerVersion(ProtocolVersion.v1_20, 15, "1.20.1");
      registerVersion(ProtocolVersion.v1_19_4, 13, "1.19.4");
      registerVersion(ProtocolVersion.v1_19_3, 12, "1.19.3");
      registerVersion(ProtocolVersion.v1_19_1, 9, "1.19.2");
      registerVersion(ProtocolVersion.v1_19, 9, "1.19");
      registerVersion(ProtocolVersion.v1_18_2, 8, "1.18.2");
      registerVersion(ProtocolVersion.v1_18, 8, "1.18.1");
      registerVersion(ProtocolVersion.v1_17_1, 7, "1.17.1");
      registerVersion(ProtocolVersion.v1_17, 7, "1.17");
      registerVersion(ProtocolVersion.v1_16_4, 6, "1.16.5");
      registerVersion(ProtocolVersion.v1_16_3, 6, "1.16.3");
      registerVersion(ProtocolVersion.v1_16_2, 6, "1.16.2");
      registerVersion(ProtocolVersion.v1_16_1, 5, "1.16.1");
      registerVersion(ProtocolVersion.v1_16, 5, "1.16");
      registerVersion(ProtocolVersion.v1_15_2, 5, "1.15.2");
      registerVersion(ProtocolVersion.v1_15_1, 5, "1.15.1");
      registerVersion(ProtocolVersion.v1_15, 5, "1.15");
      registerVersion(ProtocolVersion.v1_14_4, 4, "1.14.4");
      registerVersion(ProtocolVersion.v1_14_3, 4, "1.14.3");
      registerVersion(ProtocolVersion.v1_14_2, 4, "1.14.2");
      registerVersion(ProtocolVersion.v1_14_1, 4, "1.14.1");
      registerVersion(ProtocolVersion.v1_14, 4, "1.14");
      registerVersion(ProtocolVersion.v1_13_2, 4, "1.13.2");
      registerVersion(ProtocolVersion.v1_13_1, 4, "1.13.1");
      registerVersion(ProtocolVersion.v1_13, 4, "1.13");
      registerVersion(ProtocolVersion.v1_12_2, 3, "1.12.2");
      registerVersion(ProtocolVersion.v1_12_1, 3, "1.12.1");
      registerVersion(ProtocolVersion.v1_12, 3, "1.12");
      registerVersion(ProtocolVersion.v1_11_1, 3, "1.11.2");
      registerVersion(ProtocolVersion.v1_11, 3, "1.11");
      registerVersion(ProtocolVersion.v1_10, 2, "1.10.2");
      registerVersion(ProtocolVersion.v1_9_3, 2, "1.9.4");
      registerVersion(ProtocolVersion.v1_9_2, 2, "1.9.2");
      registerVersion(ProtocolVersion.v1_9_1, 2, "1.9.1");
      registerVersion(ProtocolVersion.v1_9, 2, "1.9");
      registerVersion(ProtocolVersion.v1_8, 1, "1.8.9");
      registerVersion(ProtocolVersion.v1_7_6, 1, "1.7.10");
      registerVersion(ProtocolVersion.v1_7_2, 1, "1.7.5");
   }
}
