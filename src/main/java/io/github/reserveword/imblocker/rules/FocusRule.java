package io.github.reserveword.imblocker.rules;

import io.github.reserveword.imblocker.common.Config;
import io.github.reserveword.imblocker.common.FocusableWidgetAccessor;
import io.github.reserveword.imblocker.common.IMManager;

public final class FocusRule implements Rule {
   public static FocusableWidgetAccessor focusedInputWidget;

   public static void focusChanged(FocusableWidgetAccessor widget, boolean focused) {
      if (focused) {
         focusedInputWidget = widget;
         if (!ScreenListRule.isBlacklistedScreen() && shouldEnable(widget)) {
            IMManager.setState(true);
         }
      } else if (focusedInputWidget == widget) {
         focusedInputWidget = null;
         IMManager.setState(false);
      }
   }

   @Override public double priority() { return 0.0; }

   @Override
   public boolean apply() {
      if (!ScreenListRule.isBlacklistedScreen() && focusedInputWidget != null && shouldEnable(focusedInputWidget)) {
         IMManager.setState(true);
         return true;
      }
      return false;
   }

   private static boolean shouldEnable(FocusableWidgetAccessor widget) {
      Class<?> widgetType = widget.getClass();
      if (Config.INSTANCE.inInputBlacklist(widgetType)) {
         return false;
      }
      return Config.INSTANCE.inInputWhitelist(widgetType) || widget.isWidgetEditable();
   }
}
