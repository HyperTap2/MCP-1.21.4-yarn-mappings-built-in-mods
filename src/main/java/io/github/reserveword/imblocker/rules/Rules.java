package io.github.reserveword.imblocker.rules;

import io.github.reserveword.imblocker.common.IMManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public final class Rules {
   private static final ArrayList<Rule> RULES = new ArrayList<>();

   static {
      register(new ChatRule(), new FocusRule(), new ScreenListRule());
   }

   private Rules() {
   }

   public static void register(Rule... rules) {
      RULES.addAll(Arrays.asList(rules));
      RULES.sort(Comparator.comparingDouble(Rule::priority).reversed());
   }

   public static void apply() {
      for (Rule rule : RULES) {
         if (rule.apply()) return;
      }
      IMManager.setState(false);
   }
}
