package net.minecraft.client.util;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

public class GlfwUtil {
   public static void makeJvmCrash() {
      MemoryUtil.memSet(0L, 0, 1L);
   }

   public static double getTime() {
      return GLFW.glfwGetTime();
   }

   private GlfwUtil() {
   }
}
