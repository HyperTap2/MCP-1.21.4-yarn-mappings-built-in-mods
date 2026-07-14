package net.minecraft.client;


public interface WindowEventHandler {
   void onWindowFocusChanged(boolean focused);

   void onResolutionChanged();

   void onCursorEnterChanged();
}
