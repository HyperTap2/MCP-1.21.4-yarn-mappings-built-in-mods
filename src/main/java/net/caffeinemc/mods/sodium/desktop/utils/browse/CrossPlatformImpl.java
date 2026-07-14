package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;

class CrossPlatformImpl implements BrowseUrlHandler {
   public static boolean isSupported() {
      return Desktop.getDesktop().isSupported(Action.BROWSE);
   }

   @Override
   public void browseTo(String url) throws IOException {
      Desktop.getDesktop().browse(URI.create(url));
   }
}
