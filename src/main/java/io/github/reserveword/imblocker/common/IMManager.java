package io.github.reserveword.imblocker.common;

import ca.weblite.objc.Runtime;
import ca.weblite.objc.RuntimeUtils;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.minecraft.client.MinecraftClient;

public final class IMManager {
   private static final PlatformIMManager INSTANCE = createManager();

   private IMManager() {
   }

   public static void setEnglish(boolean english) {
      INSTANCE.setEnglishState(english);
   }

   public static void syncState() {
      INSTANCE.syncState();
   }

   public static boolean getState() {
      return INSTANCE.getState();
   }

   public static void setState(boolean enabled) {
      INSTANCE.setState(enabled);
   }

   private static PlatformIMManager createManager() {
      try {
         if (Platform.isWindows()) {
            return new WindowsManager();
         }
         if (Platform.isMac()) {
            return new MacManager();
         }
         if (Platform.isLinux()) {
            return new LinuxManager();
         }
      } catch (Throwable exception) {
         Common.LOGGER.error("Failed to initialize native input method integration", exception);
      }
      return new StubManager();
   }

   private interface PlatformIMManager {
      void setEnglishState(boolean english);
      void syncState();
      boolean getState();
      void setState(boolean enabled);
   }

   private static final class WindowsManager implements PlatformIMManager {
      private static boolean state = true;
      private static long cooldown = System.currentTimeMillis();
      private static Boolean englishState;

      private static native HANDLE ImmGetContext(HWND window);
      private static native HANDLE ImmAssociateContext(HWND window, HANDLE context);
      private static native boolean ImmReleaseContext(HWND window, HANDLE context);
      private static native HANDLE ImmCreateContext();
      private static native boolean ImmDestroyContext(HANDLE context);
      private static native boolean ImmSetConversionStatus(HANDLE context, int conversion, int sentence);

      private WindowsManager() {
         Native.register(WindowsManager.class, "imm32");
      }

      @Override
      public void setEnglishState(boolean english) {
         if (englishState == null || englishState != english) {
            if (System.currentTimeMillis() - cooldown < 50L) {
               return;
            }
            HWND window = getMinecraftWindow();
            if (window == null) {
               return;
            }
            HANDLE context = ImmGetContext(window);
            if (context != null) {
               ImmSetConversionStatus(context, english ? 0 : 1, 0);
               ImmReleaseContext(window, context);
               englishState = english;
            }
         }
      }

      @Override
      public void syncState() {
         englishState = null;
      }

      @Override
      public boolean getState() {
         return state;
      }

      @Override
      public void setState(boolean enabled) {
         HWND window = getMinecraftWindow();
         if (window == null) {
            return;
         }
         HANDLE currentContext = ImmGetContext(window);
         boolean active = currentContext != null;
         if (currentContext != null) {
            ImmReleaseContext(window, currentContext);
         }
         if (active == enabled) {
            state = enabled;
            return;
         }
         cooldown = System.currentTimeMillis();
         if (enabled) {
            ImmAssociateContext(window, ImmCreateContext());
         } else {
            HANDLE context = ImmAssociateContext(window, null);
            if (context != null) {
               ImmDestroyContext(context);
            }
         }
         englishState = null;
         state = enabled;
      }

      private static HWND getMinecraftWindow() {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client == null || client.getWindow() == null) {
            return null;
         }
         long handle = client.getWindow().getWin32Handle();
         return handle == 0L ? null : new HWND(Pointer.createConstant(handle));
      }
   }

   private static final class LinuxManager implements PlatformIMManager {
      private Boolean state;

      private LinuxManager() {
         this.syncState();
      }

      @Override public void setEnglishState(boolean english) { }

      @Override
      public void syncState() {
         if (isFcitxRunning()) {
            String output = processOutput("fcitx5-remote");
            this.state = output == null ? null : "2".equals(output.trim());
         } else {
            String engine = processOutput("ibus", "engine");
            this.state = engine == null ? null : !engine.trim().startsWith("xkb:");
         }
      }

      @Override public boolean getState() { return Boolean.TRUE.equals(this.state); }

      @Override
      public void setState(boolean enabled) {
         if (this.state != null && this.state == enabled) return;
         if (isFcitxRunning()) {
            run("fcitx5-remote", enabled ? "-o" : "-c");
         } else {
            run("ibus", "engine", enabled ? "libpinyin" : "xkb:us::eng");
         }
         this.state = enabled;
      }

      private static boolean isFcitxRunning() {
         return processOutput("pgrep", "-l", "fcitx5") != null;
      }

      private static String processOutput(String... command) {
         try {
            Process process = new ProcessBuilder(command).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
               return reader.readLine();
            }
         } catch (IOException exception) {
            return null;
         }
      }

      private static void run(String... command) {
         try {
            new ProcessBuilder(command).start();
         } catch (IOException exception) {
            Common.LOGGER.debug("Failed to execute input method command", exception);
         }
      }
   }

   private static final class MacManager implements PlatformIMManager {
      private final InterpretKeyEventsCallback original;
      private final InterpretKeyEventsCallback replacement;
      private boolean state;

      private MacManager() {
         Pointer viewClass = Runtime.INSTANCE.objc_getClass("GLFWContentView");
         Pointer selector = RuntimeUtils.sel("interpretKeyEvents:");
         Pointer method = Runtime.INSTANCE.class_getInstanceMethod(viewClass, selector);
         this.original = ObjC.INSTANCE.method_getImplementation(method);
         this.replacement = (self, sel, events) -> {
            if (!this.state) {
               Pointer inputContext = RuntimeUtils.cls("NSTextInputContext");
               Pointer current = RuntimeUtils.msgPointer(inputContext, "currentInputContext");
               RuntimeUtils.msg(current, "discardMarkedText");
            } else {
               this.original.invoke(self, sel, events);
            }
         };
         ObjC.INSTANCE.class_replaceMethod(viewClass, selector, this.replacement, "v@:@");
      }

      @Override public void setEnglishState(boolean english) { }
      @Override public void syncState() { }
      @Override public boolean getState() { return this.state; }
      @Override public void setState(boolean enabled) { this.state = enabled; }
   }

   private static final class StubManager implements PlatformIMManager {
      private boolean state = true;
      @Override public void setEnglishState(boolean english) { }
      @Override public void syncState() { }
      @Override public boolean getState() { return this.state; }
      @Override public void setState(boolean enabled) { this.state = enabled; }
   }

   private interface InterpretKeyEventsCallback extends Callback {
      void invoke(Pointer self, Pointer selector, Pointer events);
   }

   private interface ObjC extends Library {
      ObjC INSTANCE = Native.load("objc.A", ObjC.class);
      void class_replaceMethod(Pointer type, Pointer selector, InterpretKeyEventsCallback callback, String signature);
      InterpretKeyEventsCallback method_getImplementation(Pointer method);
   }
}
