package me.flashyreese.mods.sodiumextra.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.render.VertexFormat;

public class IrisCompat {
   private static boolean irisPresent;
   private static MethodHandle handleRenderingShadowPass;
   private static Object apiInstance;
   private static VertexFormat terrainFormat;

   public static boolean isRenderingShadowPass() {
      if (irisPresent) {
         try {
            return (boolean)handleRenderingShadowPass.invoke((Object)apiInstance);
         } catch (Throwable throwable) {
            throwable.printStackTrace();
         }
      }

      return false;
   }

   public static VertexFormat getTerrainFormat() {
      return terrainFormat;
   }

   public static boolean isIrisPresent() {
      return irisPresent;
   }

   static {
      try {
         Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         apiInstance = api.cast(api.getDeclaredMethod("getInstance").invoke(null));
         handleRenderingShadowPass = MethodHandles.lookup().findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class));
         Class<?> irisVertexFormatsClass = Class.forName("net.irisshaders.iris.vertices.IrisVertexFormats");
         Field terrainField = irisVertexFormatsClass.getDeclaredField("TERRAIN");
         terrainFormat = (VertexFormat)terrainField.get(null);
         irisPresent = true;
      } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
         irisPresent = false;
      }
   }
}
