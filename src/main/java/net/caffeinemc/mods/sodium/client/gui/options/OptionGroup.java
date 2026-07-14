package net.caffeinemc.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class OptionGroup {
   private final ImmutableList<Option<?>> options;

   private OptionGroup(ImmutableList<Option<?>> options) {
      this.options = options;
   }

   public static OptionGroup.Builder createBuilder() {
      return new OptionGroup.Builder();
   }

   public ImmutableList<Option<?>> getOptions() {
      return this.options;
   }

   public static class Builder {
      private final List<Option<?>> options = new ArrayList<>();

      public OptionGroup.Builder add(Option<?> option) {
         this.options.add(option);
         return this;
      }

      public OptionGroup build() {
         Validate.notEmpty(this.options, "At least one option must be specified", new Object[0]);
         return new OptionGroup(ImmutableList.copyOf(this.options));
      }
   }
}
