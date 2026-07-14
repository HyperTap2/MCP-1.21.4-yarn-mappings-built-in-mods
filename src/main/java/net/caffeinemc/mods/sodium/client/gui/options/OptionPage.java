package net.caffeinemc.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import net.minecraft.text.Text;

public class OptionPage {
   private final Text name;
   private final ImmutableList<OptionGroup> groups;
   private final ImmutableList<Option<?>> options;

   public OptionPage(Text name, ImmutableList<OptionGroup> groups) {
      this.name = name;
      this.groups = groups;
      Builder<Option<?>> builder = ImmutableList.builder();
      UnmodifiableIterator var4 = groups.iterator();

      while (var4.hasNext()) {
         OptionGroup group = (OptionGroup)var4.next();
         builder.addAll(group.getOptions());
      }

      this.options = builder.build();
   }

   public ImmutableList<OptionGroup> getGroups() {
      return this.groups;
   }

   public ImmutableList<Option<?>> getOptions() {
      return this.options;
   }

   public Text getName() {
      return this.name;
   }
}
