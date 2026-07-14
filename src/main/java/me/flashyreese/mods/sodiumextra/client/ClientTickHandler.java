package me.flashyreese.mods.sodiumextra.client;

import net.minecraft.client.MinecraftClient;

public class ClientTickHandler {
   private static final int SAMPLE_COUNT = 200;
   private final int[] fpsSamples = new int[SAMPLE_COUNT];
   private int sampleCursor;
   private int samplesFilled;
   private int fpsSum;
   private int averageFps;
   private int lowestFps;
   private int highestFps;

   public void onClientTick(MinecraftClient client) {
      int currentFps = client.getCurrentFps();
      if (this.samplesFilled == SAMPLE_COUNT) {
         this.fpsSum -= this.fpsSamples[this.sampleCursor];
      } else {
         this.samplesFilled++;
      }

      this.fpsSamples[this.sampleCursor] = currentFps;
      this.fpsSum += currentFps;
      this.sampleCursor = (this.sampleCursor + 1) % SAMPLE_COUNT;
      this.averageFps = this.fpsSum / this.samplesFilled;

      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      for (int i = 0; i < this.samplesFilled; i++) {
         int fps = this.fpsSamples[i];
         min = Math.min(min, fps);
         max = Math.max(max, fps);
      }
      this.lowestFps = min;
      this.highestFps = max;
   }

   public int getAverageFps() {
      return this.averageFps;
   }

   public int getLowestFps() {
      return this.lowestFps;
   }

   public int getHighestFps() {
      return this.highestFps;
   }
}
