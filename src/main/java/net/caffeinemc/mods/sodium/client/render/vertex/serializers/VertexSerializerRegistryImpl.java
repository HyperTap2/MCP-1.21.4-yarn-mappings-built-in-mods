package net.caffeinemc.mods.sodium.client.render.vertex.serializers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.StampedLock;
import me.flashyreese.mods.sodiumextra.compat.IrisCompat;
import me.flashyreese.mods.sodiumextra.compat.ModelVertexToTerrainSerializer;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatExtensions;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.caffeinemc.mods.sodium.client.render.vertex.serializers.generated.VertexSerializerFactory;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertexSerializerRegistryImpl implements VertexSerializerRegistry {
   private static final Logger LOGGER = LoggerFactory.getLogger(VertexSerializerRegistryImpl.class);
   private static final Path CLASS_DUMP_PATH;
   private final Long2ReferenceMap<VertexSerializer> cache = new Long2ReferenceOpenHashMap();
   private final StampedLock lock = new StampedLock();

   public VertexSerializerRegistryImpl() {
      if (SodiumExtraClientMod.isMixinEnabled("optimizations.beacon_beam_rendering.MixinVertexSerializerRegistryImpl")
         && IrisCompat.isIrisPresent()) {
         this.registerSerializer(
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            IrisCompat.getTerrainFormat(),
            new ModelVertexToTerrainSerializer()
         );
      }
   }

   @Override
   public VertexSerializer get(VertexFormat srcFormat, VertexFormat dstFormat) {
      long identifier = createKey(srcFormat, dstFormat);
      VertexSerializer serializer = this.find(identifier);
      if (serializer == null) {
         serializer = this.create(identifier, srcFormat, dstFormat);
      }

      return serializer;
   }

   @Override
   public void registerSerializer(VertexFormat srcFormat, VertexFormat dstFormat, VertexSerializer serializer) {
      this.cache.put(createKey(srcFormat, dstFormat), serializer);
   }

   private VertexSerializer create(long identifier, VertexFormat srcFormat, VertexFormat dstFormat) {
      long stamp = this.lock.writeLock();

      VertexSerializer serializer;
      try {
         VertexSerializer cached = (VertexSerializer)this.cache.get(identifier);
         if (cached == null) {
            serializer = createSerializer(srcFormat, dstFormat);
            this.cache.put(identifier, serializer);
            return serializer;
         }

         serializer = cached;
      } finally {
         this.lock.unlockWrite(stamp);
      }

      return serializer;
   }

   private VertexSerializer find(long identifier) {
      long stamp = this.lock.readLock();

      VertexSerializer var5;
      try {
         var5 = (VertexSerializer)this.cache.get(identifier);
      } finally {
         this.lock.unlockRead(stamp);
      }

      return var5;
   }

   private static VertexSerializer createSerializer(VertexFormat srcVertexFormat, VertexFormat dstVertexFormat) {
      String identifier = String.format("%04X$%04X", getGlobalId(srcVertexFormat), getGlobalId(dstVertexFormat));
      VertexSerializerFactory.Bytecode bytecode = VertexSerializerFactory.generate(srcVertexFormat, dstVertexFormat, identifier);
      if (CLASS_DUMP_PATH != null) {
         dumpClass(identifier, bytecode);
      }

      Class<?> clazz = VertexSerializerFactory.define(bytecode);

      Constructor<?> constructor;
      try {
         constructor = clazz.getConstructor();
      } catch (NoSuchMethodException var11) {
         throw new RuntimeException("Failed to find constructor of generated class", var11);
      }

      Object instance;
      try {
         instance = constructor.newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException var10) {
         throw new RuntimeException("Failed to instantiate generated class", var10);
      }

      try {
         return (VertexSerializer)instance;
      } catch (ClassCastException var9) {
         throw new RuntimeException("Failed to cast generated class to interface type", var9);
      }
   }

   private static void dumpClass(String id, VertexSerializerFactory.Bytecode bytecode) {
      Path path = CLASS_DUMP_PATH.resolve("VertexSerializer$Impl$%s.class".formatted(id));

      try {
         Files.write(path, bytecode.copy());
      } catch (IOException var4) {
         LOGGER.warn("Could not dump bytecode to location: {}", path, var4);
      }
   }

   private static long createKey(VertexFormat a, VertexFormat b) {
      return getGlobalId(a) & 4294967295L | (getGlobalId(b) & 4294967295L) << 32;
   }

   private static int getGlobalId(VertexFormat format) {
      return ((VertexFormatExtensions)format).sodium$getGlobalId();
   }

   static {
      String classDumpPath = System.getProperty("sodium.codegen.dump", null);
      if (classDumpPath != null) {
         CLASS_DUMP_PATH = Path.of(classDumpPath);
      } else {
         CLASS_DUMP_PATH = null;
      }
   }
}
