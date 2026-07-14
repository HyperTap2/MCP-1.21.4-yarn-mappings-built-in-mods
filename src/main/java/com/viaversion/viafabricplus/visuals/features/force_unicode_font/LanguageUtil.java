// SPDX-License-Identifier: GPL-3.0-or-later
package com.viaversion.viafabricplus.visuals.features.force_unicode_font;

import java.util.Map;

public final class LanguageUtil {
   private static final int NON_ASCII_THRESHOLD = 256;

   private LanguageUtil() {
   }

   public static boolean isUnicodeFont1_12_2(Map<String, String> translations) {
      int nonAsciiCharacters = 0;
      int totalCharacters = 0;
      for (String value : translations.values()) {
         totalCharacters += value.length();
         for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) >= NON_ASCII_THRESHOLD) {
               nonAsciiCharacters++;
            }
         }
      }
      return totalCharacters != 0 && (float)nonAsciiCharacters / totalCharacters > 0.1F;
   }
}
