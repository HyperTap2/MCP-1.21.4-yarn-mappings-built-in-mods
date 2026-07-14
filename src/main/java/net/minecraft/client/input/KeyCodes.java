package net.minecraft.client.input;


public class KeyCodes {
   public static boolean isToggle(int keyCode) {
      return keyCode == 257 || keyCode == 32 || keyCode == 335;
   }
}
