package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;

public interface BrowseUrlHandler {
   void browseTo(String var1) throws IOException;

   static BrowseUrlHandler createImplementation() {
      if (XDGImpl.isSupported()) {
         return new XDGImpl();
      } else {
         return CrossPlatformImpl.isSupported() ? new CrossPlatformImpl() : null;
      }
   }
}
