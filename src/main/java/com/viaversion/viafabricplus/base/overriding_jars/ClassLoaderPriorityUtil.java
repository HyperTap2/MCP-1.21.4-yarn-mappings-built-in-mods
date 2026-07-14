package com.viaversion.viafabricplus.base.overriding_jars;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import net.lenni0451.reflect.ClassLoaders;
import net.lenni0451.reflect.stream.RStream;
import org.apache.logging.log4j.Logger;

public final class ClassLoaderPriorityUtil {
   public static void loadOverridingJars(Path path, Logger logger) {
      try {
         Path jars = path.resolve("jars");
         if (!Files.exists(jars)) {
            Files.createDirectory(jars);
            return;
         }

         File[] files = jars.toFile().listFiles();
         if (files != null && files.length > 0) {
            ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

            try {
               ClassLoader actualLoader = (ClassLoader)RStream.of(oldLoader).fields().by("urlLoader").get();
               Thread.currentThread().setContextClassLoader(actualLoader);
               logger.warn("================================");
               logger.warn("OVERRIDING JARS LOADING! THIS CAN CAUSE UNEXPECTED BEHAVIOR AND ISSUES!");

               for (File file : files) {
                  if (file.getName().endsWith(".jar")) {
                     ClassLoaders.loadToFront(file.toURI().toURL());
                     logger.warn(" -> {}", file.getName());
                  }
               }

               logger.warn("================================");
            } finally {
               Thread.currentThread().setContextClassLoader(oldLoader);
            }
         }
      } catch (Throwable e) {
         logger.error("Failed to load overriding jars", e);
      }
   }
}
