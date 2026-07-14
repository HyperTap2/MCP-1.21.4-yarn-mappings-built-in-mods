package com.github.argon4w.acceleratedrendering.features.filter;

public enum FilterType {
   BLACKLIST,
   WHITELIST;

   public boolean test(boolean matches) {
      return this == WHITELIST ? matches : !matches;
   }
}
