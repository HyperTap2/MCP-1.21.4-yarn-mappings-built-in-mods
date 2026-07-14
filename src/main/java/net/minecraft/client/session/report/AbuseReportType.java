package net.minecraft.client.session.report;

import java.util.Locale;

public enum AbuseReportType {
   CHAT("chat"),
   SKIN("skin"),
   USERNAME("username");

   private final String name;

   AbuseReportType(final String name) {
      this.name = name.toUpperCase(Locale.ROOT);
   }

   public String getName() {
      return this.name;
   }
}
