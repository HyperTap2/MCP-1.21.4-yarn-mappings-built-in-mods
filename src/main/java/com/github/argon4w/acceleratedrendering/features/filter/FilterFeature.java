package com.github.argon4w.acceleratedrendering.features.filter;

import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.Nullable;

/** Source-integrated replacement for AcceleratedRendering's registry filters. */
public final class FilterFeature {
   private static final boolean ENABLED = enabled("enabled", true);
   private static final Filter MENU = filter("menu", true, FilterType.WHITELIST, "minecraft:.*");
   private static final Filter ENTITY = filter("entities", false, FilterType.BLACKLIST, "");
   private static final Filter BLOCK_ENTITY = filter("blockEntities", false, FilterType.BLACKLIST, "");
   private static final Filter ITEM = filter("items", false, FilterType.BLACKLIST, "");

   private FilterFeature() {
   }

   public static boolean beginEntity(Entity entity) {
      return begin(ENTITY.rejects(Registries.ENTITY_TYPE.getId(entity.getType()).toString()));
   }

   public static boolean beginBlockEntity(BlockEntity blockEntity) {
      return begin(BLOCK_ENTITY.rejects(Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()).toString()));
   }

   public static boolean beginItem(@Nullable Item item) {
      return begin(item != null && ITEM.rejects(Registries.ITEM.getId(item).toString()));
   }

   public static boolean beginMenu(@Nullable Screen screen) {
      if (!(screen instanceof HandledScreen<?> handledScreen)) {
         return false;
      }

      ScreenHandlerType<?> type;
      try {
         type = handledScreen.getScreenHandler().getType();
      } catch (UnsupportedOperationException ignored) {
         return false;
      }
      return begin(MENU.rejects(Registries.SCREEN_HANDLER.getId(type).toString()));
   }

   public static void end(boolean filtered) {
      if (!filtered) {
         return;
      }

      AcceleratedTextRenderingFeature.resetPipelineSetting();
      AcceleratedItemRenderingFeature.resetPipelineSetting();
      AcceleratedEntityRenderingFeature.resetPipelineSetting();
   }

   private static boolean begin(boolean filtered) {
      if (!ENABLED || !filtered) {
         return false;
      }

      AcceleratedEntityRenderingFeature.useVanillaPipeline();
      AcceleratedItemRenderingFeature.useVanillaPipeline();
      AcceleratedTextRenderingFeature.useVanillaPipeline();
      return true;
   }

   private static Filter filter(String name, boolean defaultEnabled, FilterType defaultType, String defaultValues) {
      boolean enabled = enabled(name + ".enabled", defaultEnabled);
      FilterType type;
      try {
         type = FilterType.valueOf(
            System.getProperty(key(name + ".type"), defaultType.name()).toUpperCase(Locale.ROOT)
         );
      } catch (IllegalArgumentException ignored) {
         type = defaultType;
      }

      String configured = System.getProperty(key(name + ".values"), defaultValues);
      List<Pattern> patterns = new ArrayList<>();
      for (String value : configured.split(",")) {
         String expression = value.trim();
         if (!expression.isEmpty()) {
            try {
               patterns.add(Pattern.compile(expression));
            } catch (PatternSyntaxException ignored) {
               patterns.add(Pattern.compile(Pattern.quote(expression)));
            }
         }
      }
      return new Filter(enabled, type, List.copyOf(patterns));
   }

   private static boolean enabled(String name, boolean fallback) {
      return Boolean.parseBoolean(System.getProperty(key(name), Boolean.toString(fallback)));
   }

   private static String key(String name) {
      return "acceleratedrendering.filter." + name;
   }

   private record Filter(boolean enabled, FilterType type, List<Pattern> patterns) {
      boolean rejects(String id) {
         if (!this.enabled) {
            return false;
         }

         boolean matches = this.patterns.stream().anyMatch(pattern -> pattern.matcher(id).matches());
         return !this.type.test(matches);
      }
   }
}
