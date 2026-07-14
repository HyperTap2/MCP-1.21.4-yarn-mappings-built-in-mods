package net.minecraft.client.texture;

import java.io.IOException;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class ResourceTexture extends ReloadableTexture {
   public ResourceTexture(Identifier location) {
      super(location);
   }

   @Override
   public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
      return TextureContents.load(resourceManager, this.getId());
   }
}
