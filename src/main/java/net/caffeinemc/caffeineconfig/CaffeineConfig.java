package net.caffeinemc.caffeineconfig;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CaffeineConfig {
   private final Map<String, Option> options = new HashMap<>();
   private final Set<Option> optionsWithDependencies = new ObjectLinkedOpenHashSet();
   private final String modName;
   private Logger logger;
   private static final CaffeineConfigPlatform PLATFORM = new CaffeineConfigStandalone();

   private CaffeineConfig(String modName) {
      this.modName = modName;
   }

   public static CaffeineConfig.Builder builder(String modName) {
      CaffeineConfig config = new CaffeineConfig(modName);
      config.logger = LoggerFactory.getLogger(modName + " Config");
      String jsonKey = modName.toLowerCase() + ":options";
      return config.new Builder().withSettingsKey(jsonKey);
   }

   public String getModName() {
      return this.modName;
   }

   public Logger getLogger() {
      return this.logger;
   }

   private void addOptionDependency(String optionName, String dependency, boolean requiredValue) {
      String mixinOptionName = getMixinOptionName(optionName);
      Option option = this.options.get(mixinOptionName);
      if (option == null) {
         throw new IllegalArgumentException(
            String.format("Option %s for dependency '%s depends on %s=%s' not found", optionName, optionName, dependency, requiredValue)
         );
      }

      String dependencyOptionName = getMixinOptionName(dependency);
      Option dependencyOption = this.options.get(dependencyOptionName);
      if (dependencyOption == null) {
         throw new IllegalArgumentException(
            String.format("Option %s for dependency '%s depends on %s=%s' not found", dependency, optionName, dependency, requiredValue)
         );
      }

      option.addDependency(dependencyOption, requiredValue);
      this.optionsWithDependencies.add(option);
   }

   private void addMixinOption(String mixin, boolean enabled) {
      this.addMixinOption(mixin, enabled, true);
   }

   public void addMixinOption(String mixin, boolean enabled, boolean overrideable) {
      String name = getMixinOptionName(mixin);
      if (this.options.putIfAbsent(name, new Option(name, enabled, false, overrideable)) != null) {
         throw new IllegalStateException("Mixin option already defined: " + mixin);
      }
   }

   private void readProperties(Properties props) {
      for (Entry<Object, Object> entry : props.entrySet()) {
         String key = (String)entry.getKey();
         String value = (String)entry.getValue();
         Option option = this.options.get(key);
         if (option == null) {
            this.logger.warn("No configuration key exists with name '{}', ignoring", key);
         } else if (!option.isOverrideable()) {
            this.logger.warn("User attempted to override option '{}' that is not overrideable, ignoring", key);
         } else {
            boolean enabled;
            if (value.equalsIgnoreCase("true")) {
               enabled = true;
            } else {
               if (!value.equalsIgnoreCase("false")) {
                  this.logger.warn("Invalid value '{}' encountered for configuration key '{}', ignoring", value, key);
                  continue;
               }

               enabled = false;
            }

            option.setEnabled(enabled, true);
         }
      }
   }

   private void applyOverrideableChecks() {
      for (Option parentOption : this.options.values()) {
         for (Option childOption : this.options.values()) {
            if (childOption.getName().startsWith(parentOption.getName() + ".")
               && childOption != parentOption
               && !parentOption.isOverrideable()
               && childOption.isOverrideable()) {
               this.logger
                  .warn(
                     "Mixin option '{}' cannot be set as overrideable because its parent option '{}' is not overrideable. The mixin option will be treated as not overrideable.",
                     childOption.getName(),
                     parentOption.getName()
                  );
               childOption.setOverrideable(false);
            }
         }
      }
   }

   private void applyChildOptionsStateChecks() {
      for (Option parentOption : this.options.values()) {
         for (Option childOption : this.options.values()) {
            if (childOption.getName().startsWith(parentOption.getName() + ".") && childOption != parentOption) {
               if (childOption.isOverrideable() && (parentOption.isUserDefined() || parentOption.isModDefined())) {
                  childOption.setEnabled(parentOption.isEnabled(), parentOption.isUserDefined());
                  if (parentOption.isModDefined()) {
                     parentOption.getDefiningMods().forEach(mod -> childOption.addModOverride(parentOption.isEnabled(), mod));
                  }
               } else if (parentOption.isUserDefined()) {
                  this.logger
                     .warn(
                        "User attempted to override option '{}' that is not overrideable by overriding '{}', ignoring",
                        childOption.getName(),
                        parentOption.getName()
                     );
               } else if (parentOption.isModDefined()) {
                  this.logger
                     .warn(
                        "Mod '{}' attempted to override option '{}' that is not overrideable by overriding '{}', ignoring",
                        new Object[]{parentOption.getDefiningMods(), childOption.getName(), parentOption.getName()}
                     );
               }
            }
         }
      }
   }

   void applyModOverride(String modId, String name, boolean enabled) {
      Option option = this.options.get(name);
      if (option == null) {
         this.logger.warn("Mod '{}' attempted to override option '{}', which doesn't exist, ignoring", modId, name);
      } else if (!option.isOverrideable()) {
         this.logger.warn("Mod '{}' attempted to override option '{}' that is not overrideable, ignoring", modId, name);
      } else {
         if (!enabled && option.isEnabled()) {
            option.clearModsDefiningValue();
         }

         if (!enabled || option.isEnabled() || option.getDefiningMods().isEmpty()) {
            option.addModOverride(enabled, modId);
         }
      }
   }

   public Option getEffectiveOptionForMixin(String mixinClassName) {
      int lastSplit = 0;
      Option option = null;

      int nextSplit;
      while ((nextSplit = mixinClassName.indexOf(46, lastSplit)) != -1) {
         String key = getMixinOptionName(mixinClassName.substring(0, nextSplit));
         Option candidate = this.options.get(key);
         if (candidate != null) {
            option = candidate;
            if (!option.isEnabled()) {
               return option;
            }
         }

         lastSplit = nextSplit + 1;
      }

      return option;
   }

   private boolean applyDependencies() {
      boolean changed = false;

      for (Option optionWithDependency : this.optionsWithDependencies) {
         changed |= optionWithDependency.disableIfDependenciesNotMet(this.logger, this);
      }

      return changed;
   }

   public Option getParent(Option option) {
      String optionName = option.getName();
      int split;
      if ((split = optionName.lastIndexOf(46)) != -1) {
         String key = optionName.substring(0, split);
         return this.options.get(key);
      } else {
         return null;
      }
   }

   private static void writeDefaultConfig(Path file, String modName, String infoUrl) throws IOException {
      Path dir = file.getParent();
      if (!Files.exists(dir)) {
         Files.createDirectories(dir);
      } else if (!Files.isDirectory(dir)) {
         throw new IOException("The parent file is not a directory");
      }

      try (BufferedWriter writer = Files.newBufferedWriter(file)) {
         writer.write(String.format("# This is the configuration file for %s.\n", modName));
         writer.write("# This file exists for debugging purposes and should not be configured otherwise.\n");
         writer.write("#\n");
         if (infoUrl != null) {
            writer.write("# You can find information on editing this file and all the available options here:\n");
            writer.write("# " + infoUrl + "\n");
            writer.write("#\n");
         }

         writer.write("# By default, this file will be empty except for this notice.\n");
      }
   }

   private static String getMixinOptionName(String name) {
      return "mixin." + name;
   }

   public int getOptionCount() {
      return this.options.size();
   }

   public int getOptionOverrideCount() {
      return (int)this.options.values().stream().filter(Option::isOverridden).count();
   }

   public Map<String, Option> getOptions() {
      return ImmutableMap.copyOf(this.options);
   }

   public final class Builder {
      private boolean alreadyBuilt = false;
      private String infoUrl;
      private String jsonKey;

      private Builder() {
      }

      public CaffeineConfig.Builder addMixinOption(String mixin, boolean enabled) {
         CaffeineConfig.this.addMixinOption(mixin, enabled);
         return this;
      }

      public CaffeineConfig.Builder addMixinOption(String mixin, boolean enabled, boolean overrideable) {
         CaffeineConfig.this.addMixinOption(mixin, enabled, overrideable);
         return this;
      }

      public CaffeineConfig.Builder addOptionDependency(String option, String dependency, boolean requiredValue) {
         CaffeineConfig.this.addOptionDependency(option, dependency, requiredValue);
         return this;
      }

      public CaffeineConfig.Builder withLogger(Logger logger) {
         CaffeineConfig.this.logger = logger;
         return this;
      }

      public CaffeineConfig.Builder withSettingsKey(String key) {
         this.jsonKey = key;
         return this;
      }

      public CaffeineConfig.Builder withInfoUrl(String url) {
         this.infoUrl = url;
         return this;
      }

      public CaffeineConfig build(Path path) {
         if (this.alreadyBuilt) {
            throw new IllegalStateException("Cannot build a CaffeineConfig twice from the same builder");
         }

         CaffeineConfig.this.applyOverrideableChecks();
         if (Files.exists(path)) {
            Properties props = new Properties();

            try (InputStream fin = Files.newInputStream(path)) {
               props.load(fin);
            } catch (IOException e) {
               throw new RuntimeException("Could not load config file", e);
            }

            CaffeineConfig.this.readProperties(props);
         } else {
            try {
               CaffeineConfig.writeDefaultConfig(path, CaffeineConfig.this.modName, this.infoUrl);
            } catch (IOException e) {
               CaffeineConfig.this.logger.warn("Could not write default configuration file", e);
            }
         }

         CaffeineConfig.PLATFORM.applyModOverrides(CaffeineConfig.this, this.jsonKey);

         while (CaffeineConfig.this.applyDependencies()) {
         }

         CaffeineConfig.this.applyChildOptionsStateChecks();
         this.alreadyBuilt = true;
         return CaffeineConfig.this;
      }
   }
}
