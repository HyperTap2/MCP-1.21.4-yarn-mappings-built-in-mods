package net.caffeinemc.caffeineconfig;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;

public final class Option {
   private final String name;
   private Object2BooleanLinkedOpenHashMap<Option> dependencies;
   private Set<String> modDefined = null;
   private boolean enabled;
   private boolean userDefined;
   private boolean overrideable;

   Option(String name, boolean enabled, boolean userDefined, boolean overrideable) {
      this.name = name;
      this.enabled = enabled;
      this.userDefined = userDefined;
      this.overrideable = overrideable;
   }

   public String getName() {
      return this.name;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isEnabledRecursive(CaffeineConfig config) {
      return this.enabled && (config.getParent(this) == null || config.getParent(this).isEnabledRecursive(config));
   }

   public boolean isOverridden() {
      return this.isUserDefined() || this.isModDefined();
   }

   public boolean isUserDefined() {
      return this.userDefined;
   }

   public boolean isOverrideable() {
      return this.overrideable;
   }

   public boolean isModDefined() {
      return this.modDefined != null;
   }

   public Collection<String> getDefiningMods() {
      return this.modDefined != null ? Collections.unmodifiableCollection(this.modDefined) : Collections.emptyList();
   }

   void setOverrideable(boolean overrideable) {
      this.overrideable = overrideable;
   }

   void setEnabled(boolean enabled, boolean userDefined) {
      this.enabled = enabled;
      this.userDefined = userDefined;
   }

   void addModOverride(boolean enabled, String modId) {
      this.enabled = enabled;
      if (this.modDefined == null) {
         this.modDefined = new LinkedHashSet<>();
      }

      this.modDefined.add(modId);
   }

   void clearModsDefiningValue() {
      this.modDefined = null;
   }

   void addDependency(Option dependencyOption, boolean requiredValue) {
      if (this.dependencies == null) {
         this.dependencies = new Object2BooleanLinkedOpenHashMap(1);
      }

      this.dependencies.put(dependencyOption, requiredValue);
   }

   boolean disableIfDependenciesNotMet(Logger logger, CaffeineConfig config) {
      if (this.dependencies != null && this.isEnabled()) {
         ObjectBidirectionalIterator var3 = this.dependencies.object2BooleanEntrySet().iterator();

         while (var3.hasNext()) {
            Entry<Option> dependency = (Entry<Option>)var3.next();
            Option option = (Option)dependency.getKey();
            boolean requiredValue = dependency.getBooleanValue();
            if (option.isEnabledRecursive(config) != requiredValue) {
               this.enabled = false;
               logger.warn(
                  "Option '{}' requires '{}={}' but found '{}'. Setting '{}={}'.",
                  new Object[]{this.name, option.name, requiredValue, option.isEnabled(), this.name, this.enabled}
               );
               return true;
            }
         }
      }

      return false;
   }
}
