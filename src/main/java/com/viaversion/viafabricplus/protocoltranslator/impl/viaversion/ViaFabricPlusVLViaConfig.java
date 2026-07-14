package com.viaversion.viafabricplus.protocoltranslator.impl.viaversion;

import com.viaversion.vialoader.impl.viaversion.VLViaConfig;
import java.io.File;
import java.util.logging.Logger;

public final class ViaFabricPlusVLViaConfig extends VLViaConfig {
   public ViaFabricPlusVLViaConfig(File configFile, Logger logger) {
      super(configFile, logger);
      this.UNSUPPORTED.add("simulate-pt");
      this.UNSUPPORTED.add("fix-1_21-placement-rotation");
   }

   public boolean isSimulatePlayerTick() {
      return false;
   }

   public boolean fix1_21PlacementRotation() {
      return false;
   }
}
