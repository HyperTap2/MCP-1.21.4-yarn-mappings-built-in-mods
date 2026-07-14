package net.minecraft.client.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

public class SimpleModelSupplier implements ModelSupplier {
   private final Identifier parent;

   public SimpleModelSupplier(Identifier parent) {
      this.parent = parent;
   }

   public JsonElement get() {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("parent", this.parent.toString());
      return jsonObject;
   }
}
