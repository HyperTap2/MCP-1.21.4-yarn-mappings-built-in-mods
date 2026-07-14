package net.irisshaders.iris.pbr.loader;

import net.irisshaders.iris.pbr.texture.PBRType;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class SimplePBRLoader implements PBRTextureLoader<ResourceTexture> {
   public void load(ResourceTexture texture, ResourceManager resourceManager, PBRTextureLoader.PBRTextureConsumer pbrTextureConsumer) {
      Identifier location = texture.getId();
      AbstractTexture normalTexture = this.createPBRTexture(location, resourceManager, PBRType.NORMAL);
      AbstractTexture specularTexture = this.createPBRTexture(location, resourceManager, PBRType.SPECULAR);
      if (normalTexture != null) {
         pbrTextureConsumer.acceptNormalTexture(normalTexture);
      }

      if (specularTexture != null) {
         pbrTextureConsumer.acceptSpecularTexture(specularTexture);
      }
   }

   @Nullable
   protected AbstractTexture createPBRTexture(Identifier imageLocation, ResourceManager resourceManager, PBRType pbrType) {
      Identifier pbrImageLocation = imageLocation.withPath(pbrType::appendSuffix);
      ResourceTexture pbrTexture = new ResourceTexture(pbrImageLocation);
      TextureContents contents = this.loadContentsSafe(pbrTexture, resourceManager);
      if (contents == null) {
         pbrTexture.clearGlId();
         return null;
      } else {
         pbrTexture.reload(contents);
         return pbrTexture;
      }
   }

   private TextureContents loadContentsSafe(ReloadableTexture texture, ResourceManager manager) {
      try {
         return texture.loadContents(manager);
      } catch (Exception var4) {
         return null;
      }
   }
}
