package net.caffeinemc.mods.sodium.client.checks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;

public record SodiumResourcePackMetadata(List<String> ignoredShaders) {
   public static final Codec<SodiumResourcePackMetadata> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(Codec.STRING.listOf().fieldOf("ignored_shaders").forGetter(SodiumResourcePackMetadata::ignoredShaders))
         .apply(instance, SodiumResourcePackMetadata::new)
   );
   public static final ResourceMetadataSerializer<SodiumResourcePackMetadata> SERIALIZER = new ResourceMetadataSerializer<>("sodium", CODEC);
}
