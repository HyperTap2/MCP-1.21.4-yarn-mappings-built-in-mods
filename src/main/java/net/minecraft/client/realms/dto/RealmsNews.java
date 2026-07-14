package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.realms.util.JsonUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsNews extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   public String newsLink;

   public static RealmsNews parse(String json) {
      RealmsNews realmsNews = new RealmsNews();

      try {
         JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
         realmsNews.newsLink = JsonUtils.getNullableStringOr("newsLink", jsonObject, null);
      } catch (Exception exception) {
         LOGGER.error("Could not parse RealmsNews: {}", exception.getMessage());
      }

      return realmsNews;
   }
}
