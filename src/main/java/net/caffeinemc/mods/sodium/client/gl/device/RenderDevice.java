package net.caffeinemc.mods.sodium.client.gl.device;

import net.caffeinemc.mods.sodium.client.gl.functions.DeviceFunctions;
import org.lwjgl.opengl.GLCapabilities;

public interface RenderDevice {
   RenderDevice INSTANCE = new GLRenderDevice();

   CommandList createCommandList();

   static void enterManagedCode() {
      INSTANCE.makeActive();
   }

   static void exitManagedCode() {
      INSTANCE.makeInactive();
   }

   void makeActive();

   void makeInactive();

   GLCapabilities getCapabilities();

   DeviceFunctions getDeviceFunctions();

   int getSubTexelPrecisionBits();
}
