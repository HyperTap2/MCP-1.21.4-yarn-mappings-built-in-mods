package net.minecraft.client;

import dev.isxander.zoomify.Zoomify;
import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.features.mouse_sensitivity.MouseSensitivity1_13_2;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Scroller;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Smoother;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;

public class Mouse {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftClient client;
   public boolean leftButtonClicked;
   private boolean middleButtonClicked;
   public boolean rightButtonClicked;
   private double x;
   private double y;
   private int controlLeftClicks;
   private int activeButton = -1;
   private boolean hasResolutionChanged = true;
   private int field_1796;
   private double glfwTime;
   private final Smoother cursorXSmoother = new Smoother();
   private final Smoother cursorYSmoother = new Smoother();
   private double cursorDeltaX;
   private double cursorDeltaY;
   private final Scroller scroller;
   private double lastTickTime = Double.MIN_VALUE;
   private boolean cursorLocked;
   private final Queue<Runnable> pendingScreenEvents = new ConcurrentLinkedQueue<>();

   public Mouse(MinecraftClient client) {
      this.client = client;
      this.scroller = new Scroller();
   }

   private void onMouseButton(long window, int button, int action, int mods) {
      if (window == this.client.getWindow().getHandle()) {
         this.client.getInactivityFpsLimiter().onInput();
         if (this.client.currentScreen != null) {
            this.client.setNavigationType(GuiNavigationType.MOUSE);
         }

         boolean bl = action == 1;
         if (MinecraftClient.IS_SYSTEM_MAC && button == 0) {
            if (bl) {
               if ((mods & 2) == 2) {
                  button = 1;
                  this.controlLeftClicks++;
               }
            } else if (this.controlLeftClicks > 0) {
               button = 1;
               this.controlLeftClicks--;
            }
         }

         int i = button;
         if (bl) {
            if (this.client.options.getTouchscreen().getValue() && this.field_1796++ > 0) {
               return;
            }

            this.activeButton = i;
            this.glfwTime = GlfwUtil.getTime();
         } else if (this.activeButton != -1) {
            if (this.client.options.getTouchscreen().getValue() && --this.field_1796 > 0) {
               return;
            }

            this.activeButton = -1;
         }

         if (this.client.getOverlay() == null) {
            if (this.client.currentScreen == null) {
               if (!this.cursorLocked && bl) {
                  this.lockCursor();
               }
            } else {
               double d = this.x * this.client.getWindow().getScaledWidth() / this.client.getWindow().getWidth();
               double e = this.y * this.client.getWindow().getScaledHeight() / this.client.getWindow().getHeight();
               Screen screen = this.client.currentScreen;
               if (bl) {
                  screen.applyMousePressScrollNarratorDelay();

                  try {
                     if (screen.mouseClicked(d, e, i)) {
                        return;
                     }
                  } catch (Throwable throwable) {
                     CrashReport crashReport = CrashReport.create(throwable, "mouseClicked event handler");
                     screen.addCrashReportSection(crashReport);
                     CrashReportSection crashReportSection = crashReport.addElement("Mouse");
                     crashReportSection.add("Scaled X", d);
                     crashReportSection.add("Scaled Y", e);
                     crashReportSection.add("Button", i);
                     throw new CrashException(crashReport);
                  }
               } else {
                  try {
                     if (screen.mouseReleased(d, e, i)) {
                        return;
                     }
                  } catch (Throwable throwable) {
                     CrashReport crashReport = CrashReport.create(throwable, "mouseReleased event handler");
                     screen.addCrashReportSection(crashReport);
                     CrashReportSection crashReportSection = crashReport.addElement("Mouse");
                     crashReportSection.add("Scaled X", d);
                     crashReportSection.add("Scaled Y", e);
                     crashReportSection.add("Button", i);
                     throw new CrashException(crashReport);
                  }
               }
            }
         }

         if (this.client.currentScreen == null && this.client.getOverlay() == null) {
            if (i == 0) {
               this.leftButtonClicked = bl;
            } else if (i == 2) {
               this.middleButtonClicked = bl;
            } else if (i == 1) {
               this.rightButtonClicked = bl;
            }

            KeyBinding.setKeyPressed(InputUtil.Type.MOUSE.createFromCode(i), bl);
            if (bl) {
               if (this.client.player.isSpectator() && i == 2) {
                  this.client.inGameHud.getSpectatorHud().useSelectedCommand();
               } else {
                  KeyBinding.onKeyPressed(InputUtil.Type.MOUSE.createFromCode(i));
               }
            }
         }
      }
   }

   private void onMouseScroll(long window, double horizontal, double vertical) {
      if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
         this.client.getInactivityFpsLimiter().onInput();
         boolean bl = this.client.options.getDiscreteMouseScroll().getValue();
         double d = this.client.options.getMouseWheelSensitivity().getValue();
         double e = (bl ? Math.signum(horizontal) : horizontal) * d;
         double f = (bl ? Math.signum(vertical) : vertical) * d;
         if (this.client.getOverlay() == null) {
            if (this.client.currentScreen != null) {
               double g = this.x * this.client.getWindow().getScaledWidth() / this.client.getWindow().getWidth();
               double h = this.y * this.client.getWindow().getScaledHeight() / this.client.getWindow().getHeight();
               this.client.currentScreen.mouseScrolled(g, h, e, f);
               this.client.currentScreen.applyMousePressScrollNarratorDelay();
            } else if (this.client.player != null) {
               if (Zoomify.onScroll(f)) {
                  return;
               }
               Vector2i vector2i = this.scroller.update(e, f);
               if (vector2i.x == 0 && vector2i.y == 0) {
                  return;
               }

               int i = vector2i.y == 0 ? -vector2i.x : vector2i.y;
               if (this.client.player.isSpectator()) {
                  if (this.client.inGameHud.getSpectatorHud().isOpen()) {
                     this.client.inGameHud.getSpectatorHud().cycleSlot(-i);
                  } else {
                     float j = MathHelper.clamp(this.client.player.getAbilities().getFlySpeed() + vector2i.y * 0.005F, 0.0F, 0.2F);
                     this.client.player.getAbilities().setFlySpeed(j);
                  }
               } else {
                  PlayerInventory playerInventory = this.client.player.getInventory();
                  playerInventory.setSelectedSlot(Scroller.scrollCycling(i, playerInventory.selectedSlot, PlayerInventory.getHotbarSize()));
               }
            }
         }
      }
   }

   private void onFilesDropped(long window, List<Path> paths, int invalidFilesCount) {
      this.client.getInactivityFpsLimiter().onInput();
      if (this.client.currentScreen != null) {
         this.client.currentScreen.onFilesDropped(paths);
      }

      if (invalidFilesCount > 0) {
         SystemToast.addFileDropFailure(this.client, invalidFilesCount);
      }
   }

   public void setup(long window) {
      InputUtil.setMouseCallbacks(
         window,
         (windowx, x, y) -> this.executeScreenEvent(() -> this.onCursorPos(windowx, x, y)),
         (windowx, button, action, modifiers) -> this.executeScreenEvent(() -> this.onMouseButton(windowx, button, action, modifiers)),
         (windowx, offsetX, offsetY) -> this.client.execute(() -> this.onMouseScroll(windowx, offsetX, offsetY)),
         (windowx, count, names) -> {
            List<Path> list = new ArrayList<>(count);
            int i = 0;

            for (int j = 0; j < count; j++) {
               String string = GLFWDropCallback.getName(names, j);

               try {
                  list.add(Paths.get(string));
               } catch (InvalidPathException invalidPathException) {
                  i++;
                  LOGGER.error("Failed to parse path '{}'", string, invalidPathException);
               }
            }

            if (!list.isEmpty()) {
               int j = i;
               this.client.execute(() -> this.onFilesDropped(windowx, list, j));
            }
         }
      );
   }

   private void executeScreenEvent(Runnable event) {
      if (this.client.getNetworkHandler() != null
         && this.client.currentScreen != null
         && DebugSettings.INSTANCE.executeInputsSynchronously.isEnabled()) {
         this.pendingScreenEvents.offer(event);
      } else {
         this.client.execute(event);
      }
   }

   Queue<Runnable> getPendingScreenEvents() {
      return this.pendingScreenEvents;
   }

   private void onCursorPos(long window, double x, double y) {
      if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
         if (this.hasResolutionChanged) {
            this.x = x;
            this.y = y;
            this.hasResolutionChanged = false;
         } else {
            if (this.client.isWindowFocused()) {
               this.cursorDeltaX = this.cursorDeltaX + (x - this.x);
               this.cursorDeltaY = this.cursorDeltaY + (y - this.y);
            }

            this.x = x;
            this.y = y;
         }
      }
   }

   public void tick() {
      double d = GlfwUtil.getTime();
      double e = d - this.lastTickTime;
      this.lastTickTime = d;
      if (this.client.isWindowFocused()) {
         Screen screen = this.client.currentScreen;
         boolean bl = this.cursorDeltaX != 0.0 || this.cursorDeltaY != 0.0;
         if (bl) {
            this.client.getInactivityFpsLimiter().onInput();
         }

         if (screen != null && this.client.getOverlay() == null && bl) {
            double f = this.x * this.client.getWindow().getScaledWidth() / this.client.getWindow().getWidth();
            double g = this.y * this.client.getWindow().getScaledHeight() / this.client.getWindow().getHeight();

            try {
               screen.mouseMoved(f, g);
            } catch (Throwable throwable) {
               CrashReport crashReport = CrashReport.create(throwable, "mouseMoved event handler");
               screen.addCrashReportSection(crashReport);
               CrashReportSection crashReportSection = crashReport.addElement("Mouse");
               crashReportSection.add("Scaled X", f);
               crashReportSection.add("Scaled Y", g);
               throw new CrashException(crashReport);
            }

            if (this.activeButton != -1 && this.glfwTime > 0.0) {
               double h = this.cursorDeltaX * this.client.getWindow().getScaledWidth() / this.client.getWindow().getWidth();
               double i = this.cursorDeltaY * this.client.getWindow().getScaledHeight() / this.client.getWindow().getHeight();

               try {
                  screen.mouseDragged(f, g, this.activeButton, h, i);
               } catch (Throwable throwable2) {
                  CrashReport crashReport2 = CrashReport.create(throwable2, "mouseDragged event handler");
                  screen.addCrashReportSection(crashReport2);
                  CrashReportSection crashReportSection2 = crashReport2.addElement("Mouse");
                  crashReportSection2.add("Scaled X", f);
                  crashReportSection2.add("Scaled Y", g);
                  throw new CrashException(crashReport2);
               }
            }

            screen.applyMouseMoveNarratorDelay();
         }

         if (this.isCursorLocked() && this.client.player != null) {
            this.updateMouse(e);
         }
      }

      this.cursorDeltaX = 0.0;
      this.cursorDeltaY = 0.0;
   }

   private void updateMouse(double timeDelta) {
      double sensitivity = this.client.options.getMouseSensitivity().getValue();
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
         sensitivity = MouseSensitivity1_13_2.get1_13SliderValue((float)sensitivity).keyFloat();
      }

      double d = sensitivity * 0.6F + 0.2F;
      double e = d * d * d;
      double f = e * 8.0;
      double i;
      double j;
      if (this.client.options.smoothCameraEnabled || Zoomify.shouldUseCinematicCamera()) {
         double smoothness = Zoomify.shouldUseCinematicCamera() ? 0.35 : 1.0;
         double g = this.cursorXSmoother.smooth(this.cursorDeltaX * f, timeDelta * f * smoothness);
         double h = this.cursorYSmoother.smooth(this.cursorDeltaY * f, timeDelta * f * smoothness);
         i = g;
         j = h;
      } else if (this.client.options.getPerspective().isFirstPerson() && this.client.player.isUsingSpyglass()) {
         this.cursorXSmoother.clear();
         this.cursorYSmoother.clear();
         i = this.cursorDeltaX * e;
         j = this.cursorDeltaY * e;
      } else {
         this.cursorXSmoother.clear();
         this.cursorYSmoother.clear();
         i = this.cursorDeltaX * f;
         j = this.cursorDeltaY * f;
      }

      int k = 1;
      if (this.client.options.getInvertYMouse().getValue()) {
         k = -1;
      }

      double zoomScale = Zoomify.mouseScale();
      i *= zoomScale;
      j *= zoomScale;

      this.client.getTutorialManager().onUpdateMouse(i, j);
      if (this.client.player != null) {
         this.client.player.changeLookDirection(i, j * k);
      }
   }

   public boolean wasLeftButtonClicked() {
      return this.leftButtonClicked;
   }

   public boolean wasMiddleButtonClicked() {
      return this.middleButtonClicked;
   }

   public boolean wasRightButtonClicked() {
      return this.rightButtonClicked;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public void onResolutionChanged() {
      this.hasResolutionChanged = true;
   }

   public boolean isCursorLocked() {
      return this.cursorLocked;
   }

   public void lockCursor() {
      if (this.client.isWindowFocused()) {
         if (!this.cursorLocked) {
            if (!MinecraftClient.IS_SYSTEM_MAC) {
               KeyBinding.updatePressedStates();
            }

            this.cursorLocked = true;
            this.x = this.client.getWindow().getWidth() / 2;
            this.y = this.client.getWindow().getHeight() / 2;
            InputUtil.setCursorParameters(this.client.getWindow().getHandle(), 212995, this.x, this.y);
            this.client.setScreen(null);
            this.client.attackCooldown = 10000;
            this.hasResolutionChanged = true;
         }
      }
   }

   public void unlockCursor() {
      if (this.cursorLocked) {
         this.cursorLocked = false;
         this.x = this.client.getWindow().getWidth() / 2;
         this.y = this.client.getWindow().getHeight() / 2;
         InputUtil.setCursorParameters(this.client.getWindow().getHandle(), 212993, this.x, this.y);
      }
   }

   public void setResolutionChanged() {
      this.hasResolutionChanged = true;
   }
}
