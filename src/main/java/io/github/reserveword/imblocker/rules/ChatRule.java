package io.github.reserveword.imblocker.rules;

import io.github.reserveword.imblocker.common.IMManager;

public final class ChatRule implements Rule {
   public static boolean isChatScreenShowing;

   @Override public double priority() { return 10.0; }

   @Override
   public boolean apply() {
      if (!isChatScreenShowing || FocusRule.focusedInputWidget == null) return false;
      IMManager.setState(true);
      IMManager.setEnglish(FocusRule.focusedInputWidget.getText().trim().startsWith("/"));
      return true;
   }
}
