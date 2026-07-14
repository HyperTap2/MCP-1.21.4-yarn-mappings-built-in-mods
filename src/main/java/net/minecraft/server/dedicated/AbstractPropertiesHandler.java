package net.minecraft.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.registry.DynamicRegistryManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPropertiesHandler<T extends AbstractPropertiesHandler<T>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Properties properties;

   public AbstractPropertiesHandler(Properties properties) {
      this.properties = properties;
   }

   public static Properties loadProperties(Path path) {
      try {
         try (InputStream inputStream = Files.newInputStream(path)) {
            CharsetDecoder charsetDecoder = StandardCharsets.UTF_8
               .newDecoder()
               .onMalformedInput(CodingErrorAction.REPORT)
               .onUnmappableCharacter(CodingErrorAction.REPORT);
            Properties properties = new Properties();
            properties.load(new InputStreamReader(inputStream, charsetDecoder));
            return properties;
         } catch (CharacterCodingException characterCodingException) {
            LOGGER.info("Failed to load properties as UTF-8 from file {}, trying ISO_8859_1", path);

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
               Properties properties = new Properties();
               properties.load(reader);
               return properties;
            }
         }
      } catch (IOException iOException) {
         LOGGER.error("Failed to load properties from file: {}", path, iOException);
         return new Properties();
      }
   }

   public void saveProperties(Path path) {
      try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
         this.properties.store(writer, "Minecraft server properties");
      } catch (IOException iOException) {
         LOGGER.error("Failed to store properties to file: {}", path);
      }
   }

   private static <V extends Number> Function<String, V> wrapNumberParser(Function<String, V> parser) {
      return string -> {
         try {
            return parser.apply(string);
         } catch (NumberFormatException numberFormatException) {
            return null;
         }
      };
   }

   protected static <V> Function<String, V> combineParser(IntFunction<V> intParser, Function<String, V> fallbackParser) {
      return string -> {
         try {
            return intParser.apply(Integer.parseInt(string));
         } catch (NumberFormatException numberFormatException) {
            return fallbackParser.apply(string);
         }
      };
   }

   @Nullable
   private String getStringValue(String key) {
      return (String)this.properties.get(key);
   }

   @Nullable
   protected <V> V getDeprecated(String key, Function<String, V> stringifier) {
      String string = this.getStringValue(key);
      if (string == null) {
         return null;
      }

      this.properties.remove(key);
      return stringifier.apply(string);
   }

   protected <V> V get(String key, Function<String, V> parser, Function<V, String> stringifier, V fallback) {
      String string = this.getStringValue(key);
      V object = (V)MoreObjects.firstNonNull(string != null ? parser.apply(string) : null, fallback);
      this.properties.put(key, stringifier.apply(object));
      return object;
   }

   protected <V> AbstractPropertiesHandler<T>.PropertyAccessor<V> accessor(String key, Function<String, V> parser, Function<V, String> stringifier, V fallback) {
      String string = this.getStringValue(key);
      V object = (V)MoreObjects.firstNonNull(string != null ? parser.apply(string) : null, fallback);
      this.properties.put(key, stringifier.apply(object));
      return new PropertyAccessor<V>(key, object, stringifier);
   }

   protected <V> V get(String key, Function<String, V> parser, UnaryOperator<V> parsedTransformer, Function<V, String> stringifier, V fallback) {
      return this.get(key, value -> {
         V object = parser.apply(value);
         return object != null ? parsedTransformer.apply(object) : null;
      }, stringifier, fallback);
   }

   protected <V> V get(String key, Function<String, V> parser, V fallback) {
      return this.get(key, parser, Objects::toString, fallback);
   }

   protected <V> AbstractPropertiesHandler<T>.PropertyAccessor<V> accessor(String key, Function<String, V> parser, V fallback) {
      return this.accessor(key, parser, Objects::toString, fallback);
   }

   protected String getString(String key, String fallback) {
      return this.get(key, Function.identity(), Function.identity(), fallback);
   }

   @Nullable
   protected String getDeprecatedString(String key) {
      return this.getDeprecated(key, Function.identity());
   }

   protected int getInt(String key, int fallback) {
      return this.get(key, wrapNumberParser(Integer::parseInt), fallback);
   }

   protected AbstractPropertiesHandler<T>.PropertyAccessor<Integer> intAccessor(String key, int fallback) {
      return this.accessor(key, wrapNumberParser(Integer::parseInt), fallback);
   }

   protected int transformedParseInt(String key, UnaryOperator<Integer> transformer, int fallback) {
      return this.get(key, wrapNumberParser(Integer::parseInt), transformer, Objects::toString, fallback);
   }

   protected long parseLong(String key, long fallback) {
      return this.get(key, wrapNumberParser(Long::parseLong), fallback);
   }

   protected boolean parseBoolean(String key, boolean fallback) {
      return this.get(key, Boolean::valueOf, fallback);
   }

   protected AbstractPropertiesHandler<T>.PropertyAccessor<Boolean> booleanAccessor(String key, boolean fallback) {
      return this.accessor(key, Boolean::valueOf, fallback);
   }

   @Nullable
   protected Boolean getDeprecatedBoolean(String key) {
      return this.getDeprecated(key, Boolean::valueOf);
   }

   protected Properties copyProperties() {
      Properties properties = new Properties();
      properties.putAll(this.properties);
      return properties;
   }

   protected abstract T create(DynamicRegistryManager registryManager, Properties properties);

   public class PropertyAccessor<V> implements Supplier<V> {
      private final String key;
      private final V value;
      private final Function<V, String> stringifier;

      PropertyAccessor(final String key, final V value, final Function<V, String> stringifier) {
         this.key = key;
         this.value = value;
         this.stringifier = stringifier;
      }

      @Override
      public V get() {
         return this.value;
      }

      public T set(DynamicRegistryManager registryManager, V value) {
         Properties properties = AbstractPropertiesHandler.this.copyProperties();
         properties.put(this.key, this.stringifier.apply(value));
         return AbstractPropertiesHandler.this.create(registryManager, properties);
      }
   }
}
