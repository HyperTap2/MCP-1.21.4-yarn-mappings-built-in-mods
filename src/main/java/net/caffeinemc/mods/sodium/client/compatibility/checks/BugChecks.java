package net.caffeinemc.mods.sodium.client.compatibility.checks;

class BugChecks {
   public static final boolean ISSUE_899 = configureCheck("issue899", true);
   public static final boolean ISSUE_1486 = configureCheck("issue1486", true);
   public static final boolean ISSUE_2048 = configureCheck("issue2048", true);
   public static final boolean ISSUE_2561 = configureCheck("issue2561", true);
   public static final boolean ISSUE_2637 = configureCheck("issue2637", true);

   private static boolean configureCheck(String name, boolean defaultValue) {
      String propertyValue = System.getProperty(getPropertyKey(name), null);
      return propertyValue == null ? defaultValue : Boolean.parseBoolean(propertyValue);
   }

   private static String getPropertyKey(String name) {
      return "sodium.checks." + name;
   }
}
