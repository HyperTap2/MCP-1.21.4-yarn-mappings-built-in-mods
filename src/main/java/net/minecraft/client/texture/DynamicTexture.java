package net.minecraft.client.texture;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.Identifier;

public interface DynamicTexture {
   void save(Identifier id, Path path) throws IOException;
}
