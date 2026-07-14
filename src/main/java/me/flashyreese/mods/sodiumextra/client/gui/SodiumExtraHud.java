package me.flashyreese.mods.sodiumextra.client.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class SodiumExtraHud {
   private final List<Text> textList = new ObjectArrayList();
   private final MinecraftClient client = MinecraftClient.getInstance();

   public void onStartTick(MinecraftClient client) {
      this.textList.clear();
      if (SodiumExtraClientMod.options().extraSettings.showFps) {
         int currentFPS = this.client.getCurrentFps();
         Text text = Text.translatable("sodium-extra.overlay.fps", new Object[]{currentFPS});
         if (SodiumExtraClientMod.options().extraSettings.showFPSExtended) {
            text = Text.literal(
               String.format(
                  "%s %s",
                  text.getString(),
                  Text.translatable(
                        "sodium-extra.overlay.fps_extended",
                        new Object[]{
                           SodiumExtraClientMod.getClientTickHandler().getHighestFps(),
                           SodiumExtraClientMod.getClientTickHandler().getAverageFps(),
                           SodiumExtraClientMod.getClientTickHandler().getLowestFps()
                        }
                     )
                     .getString()
               )
            );
         }

         this.textList.add(text);
      }

      if (SodiumExtraClientMod.options().extraSettings.showCoords && !this.client.hasReducedDebugInfo() && this.client.player != null) {
         Vec3d pos = this.client.player.getPos();
         Text text = Text.translatable(
            "sodium-extra.overlay.coordinates", new Object[]{String.format("%.2f", pos.x), String.format("%.2f", pos.y), String.format("%.2f", pos.z)}
         );
         this.textList.add(text);
      }

      if (!SodiumExtraClientMod.options().renderSettings.lightUpdates) {
         Text text = Text.translatable("sodium-extra.overlay.light_updates");
         this.textList.add(text);
      }
   }

   public void onHudRender(DrawContext guiGraphics, RenderTickCounter deltaTracker) {
      if (!this.client.getDebugHud().shouldShowDebugHud() && !this.client.options.hudHidden) {
         SodiumExtraGameOptions.OverlayCorner overlayCorner = SodiumExtraClientMod.options().extraSettings.overlayCorner;
         int y = overlayCorner != SodiumExtraGameOptions.OverlayCorner.BOTTOM_LEFT && overlayCorner != SodiumExtraGameOptions.OverlayCorner.BOTTOM_RIGHT
            ? 2
            : this.client.getWindow().getScaledHeight() - 9 - 2;

         for (Text text : this.textList) {
            int x;
            if (overlayCorner != SodiumExtraGameOptions.OverlayCorner.TOP_RIGHT && overlayCorner != SodiumExtraGameOptions.OverlayCorner.BOTTOM_RIGHT) {
               x = 2;
            } else {
               x = this.client.getWindow().getScaledWidth() - this.client.textRenderer.getWidth(text) - 2;
            }

            this.drawString(guiGraphics, text, x, y);
            if (overlayCorner != SodiumExtraGameOptions.OverlayCorner.BOTTOM_LEFT && overlayCorner != SodiumExtraGameOptions.OverlayCorner.BOTTOM_RIGHT) {
               y += 9 + 2;
            } else {
               y -= 9 + 2;
            }
         }
      }
   }

   private void drawString(DrawContext guiGraphics, Text text, int x, int y) {
      int textColor = -1;
      if (SodiumExtraClientMod.options().extraSettings.textContrast == SodiumExtraGameOptions.TextContrast.BACKGROUND) {
         guiGraphics.fill(x - 1, y - 1, x + this.client.textRenderer.getWidth(text) + 1, y + 9 + 1, -1873784752);
      }

      guiGraphics.drawText(
         this.client.textRenderer,
         text,
         x,
         y,
         textColor,
         SodiumExtraClientMod.options().extraSettings.textContrast == SodiumExtraGameOptions.TextContrast.SHADOW
      );
   }
}
