package net.minecraft.client.gui.navigation;


public interface Navigable {
   default int getNavigationOrder() {
      return 0;
   }
}
