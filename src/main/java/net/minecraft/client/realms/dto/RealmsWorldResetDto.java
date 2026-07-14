package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import net.minecraft.client.realms.RealmsSerializable;

public class RealmsWorldResetDto extends ValueObject implements RealmsSerializable {
   @SerializedName("seed")
   private final String seed;
   @SerializedName("worldTemplateId")
   private final long worldTemplateId;
   @SerializedName("levelType")
   private final int levelType;
   @SerializedName("generateStructures")
   private final boolean generateStructures;
   @SerializedName("experiments")
   private final Set<String> experiments;

   public RealmsWorldResetDto(String seed, long worldTemplateId, int levelType, boolean generateStructures, Set<String> experiments) {
      this.seed = seed;
      this.worldTemplateId = worldTemplateId;
      this.levelType = levelType;
      this.generateStructures = generateStructures;
      this.experiments = experiments;
   }
}
