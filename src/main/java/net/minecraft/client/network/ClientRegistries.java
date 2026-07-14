package net.minecraft.client.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.SerializableRegistries;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.registry.Registry.PendingTagLoad;
import net.minecraft.registry.RegistryLoader.ElementsAndTags;
import net.minecraft.registry.RegistryWrapper.Impl;
import net.minecraft.registry.SerializableRegistries.SerializedRegistryEntry;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagPacketSerializer.Serialized;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;

public class ClientRegistries {
   @Nullable
   private ClientRegistries.DynamicRegistries dynamicRegistries;
   @Nullable
   private ClientRegistries.Tags tags;

   public void putDynamicRegistry(RegistryKey<? extends Registry<?>> registryRef, List<SerializedRegistryEntry> entries) {
      if (this.dynamicRegistries == null) {
         this.dynamicRegistries = new ClientRegistries.DynamicRegistries();
      }

      this.dynamicRegistries.put(registryRef, entries);
   }

   public void putTags(Map<RegistryKey<? extends Registry<?>>, Serialized> tags) {
      if (this.tags == null) {
         this.tags = new ClientRegistries.Tags();
      }

      tags.forEach(this.tags::put);
   }

   private static <T> PendingTagLoad<T> startTagReload(Immutable registryManager, RegistryKey<? extends Registry<? extends T>> registryRef, Serialized tags) {
      Registry<T> registry = registryManager.getOrThrow(registryRef);
      return registry.startTagReload(tags.toRegistryTags(registry));
   }

   private DynamicRegistryManager createRegistryManager(ResourceFactory resourceFactory, ClientRegistries.DynamicRegistries dynamicRegistries, boolean local) {
      CombinedDynamicRegistries<ClientDynamicRegistryType> combinedDynamicRegistries = ClientDynamicRegistryType.createCombinedDynamicRegistries();
      Immutable immutable = combinedDynamicRegistries.getPrecedingRegistryManagers(ClientDynamicRegistryType.REMOTE);
      Map<RegistryKey<? extends Registry<?>>, ElementsAndTags> map = new HashMap<>();
      dynamicRegistries.dynamicRegistries
         .forEach((registryRef, entries) -> map.put((RegistryKey<? extends Registry<?>>)registryRef, new ElementsAndTags(entries, Serialized.NONE)));
      List<PendingTagLoad<?>> list = new ArrayList<>();
      if (this.tags != null) {
         this.tags.forEach((registryRef, tags) -> {
            if (!tags.isEmpty()) {
               if (SerializableRegistries.isSynced(registryRef)) {
                  map.compute((RegistryKey<? extends Registry<?>>)registryRef, (key, value) -> {
                     List<SerializedRegistryEntry> listxx = value != null ? value.elements() : List.of();
                     return new ElementsAndTags(listxx, tags);
                  });
               } else if (!local) {
                  list.add(startTagReload(immutable, registryRef, tags));
               }
            }
         });
      }

      List<Impl<?>> list2 = TagGroupLoader.collectRegistries(immutable, list);

      Immutable immutable2;
      try {
         immutable2 = RegistryLoader.loadFromNetwork(map, resourceFactory, list2, RegistryLoader.SYNCED_REGISTRIES).toImmutable();
      } catch (Exception exception) {
         CrashReport crashReport = CrashReport.create(exception, "Network Registry Load");
         addCrashReportSection(crashReport, map, list);
         throw new CrashException(crashReport);
      }

      DynamicRegistryManager dynamicRegistryManager = combinedDynamicRegistries.with(ClientDynamicRegistryType.REMOTE, new Immutable[]{immutable2})
         .getCombinedRegistryManager();
      list.forEach(PendingTagLoad::apply);
      return dynamicRegistryManager;
   }

   private static void addCrashReportSection(
      CrashReport crashReport, Map<RegistryKey<? extends Registry<?>>, ElementsAndTags> data, List<PendingTagLoad<?>> tags
   ) {
      CrashReportSection crashReportSection = crashReport.addElement("Received Elements and Tags");
      crashReportSection.add(
         "Dynamic Registries",
         () -> data.entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> ((RegistryKey)entry.getKey()).getValue()))
            .map(
               entry -> String.format(
                  Locale.ROOT, "\n\t\t%s: elements=%d tags=%d", entry.getKey().getValue(), entry.getValue().elements().size(), entry.getValue().tags().size()
               )
            )
            .collect(Collectors.joining())
      );
      crashReportSection.add(
         "Static Registries",
         () -> tags.stream()
            .sorted(Comparator.comparing(tag -> tag.getKey().getValue()))
            .map(tag -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", tag.getKey().getValue(), tag.size()))
            .collect(Collectors.joining())
      );
   }

   private void loadTags(ClientRegistries.Tags tags, Immutable registryManager, boolean local) {
      tags.forEach((registryRef, serialized) -> {
         if (local || SerializableRegistries.isSynced(registryRef)) {
            startTagReload(registryManager, registryRef, serialized).apply();
         }
      });
   }

   public Immutable createRegistryManager(ResourceFactory resourceFactory, Immutable registryManager, boolean local) {
      DynamicRegistryManager dynamicRegistryManager;
      if (this.dynamicRegistries != null) {
         dynamicRegistryManager = this.createRegistryManager(resourceFactory, this.dynamicRegistries, local);
      } else {
         if (this.tags != null) {
            this.loadTags(this.tags, registryManager, !local);
         }

         dynamicRegistryManager = registryManager;
      }

      return dynamicRegistryManager.toImmutable();
   }

   static class DynamicRegistries {
      final Map<RegistryKey<? extends Registry<?>>, List<SerializedRegistryEntry>> dynamicRegistries = new HashMap<>();

      public void put(RegistryKey<? extends Registry<?>> registryRef, List<SerializedRegistryEntry> entries) {
         this.dynamicRegistries.computeIfAbsent(registryRef, registries -> new ArrayList<>()).addAll(entries);
      }
   }

   static class Tags {
      private final Map<RegistryKey<? extends Registry<?>>, Serialized> tags = new HashMap<>();

      public void put(RegistryKey<? extends Registry<?>> registryRef, Serialized tags) {
         this.tags.put(registryRef, tags);
      }

      public void forEach(BiConsumer<? super RegistryKey<? extends Registry<?>>, ? super Serialized> consumer) {
         this.tags.forEach(consumer);
      }
   }
}
