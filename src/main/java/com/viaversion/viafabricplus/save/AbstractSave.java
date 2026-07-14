package com.viaversion.viafabricplus.save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class AbstractSave {
   public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final Path path;

   public AbstractSave(String name) {
      this.path = ViaFabricPlusImpl.INSTANCE.rootPath().resolve(name + ".json");
   }

   public void init() {
      if (Files.exists(this.path)) {
         try {
            JsonObject object = (JsonObject)GSON.fromJson(Files.readString(this.path), JsonObject.class);
            if (object != null) {
               this.read(object);
            } else {
               ViaFabricPlusImpl.INSTANCE.logger().error("The file {} is empty!", this.path.getFileName());
            }
         } catch (Exception e) {
            ViaFabricPlusImpl.INSTANCE.logger().error("Failed to read file: {}!", this.path.getFileName(), e);
         }
      }
   }

   public void save() {
      try {
         JsonObject object = new JsonObject();
         this.write(object);
         Files.writeString(this.path, GSON.toJson(object), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (Exception e) {
         ViaFabricPlusImpl.INSTANCE.logger().error("Failed to write file: {}!", this.path.getFileName(), e);
      }
   }

   public abstract void write(JsonObject var1);

   public abstract void read(JsonObject var1);

   public void postInit() {
   }

   public Path getPath() {
      return this.path;
   }
}
