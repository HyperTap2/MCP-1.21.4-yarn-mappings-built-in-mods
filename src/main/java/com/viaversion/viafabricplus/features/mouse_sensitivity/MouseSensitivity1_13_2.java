package com.viaversion.viafabricplus.features.mouse_sensitivity;

import it.unimi.dsi.fastutil.floats.FloatIntPair;

public final class MouseSensitivity1_13_2 {
   public static FloatIntPair get1_13SliderValue(float value1_14) {
      int oldSliderWidth = 142;
      int mousePos = (int)(142.0F * value1_14);
      float oldValue = mousePos / 142.0F;
      int oldDisplay = (int)(oldValue * 200.0F);
      return FloatIntPair.of(oldValue, oldDisplay);
   }
}
