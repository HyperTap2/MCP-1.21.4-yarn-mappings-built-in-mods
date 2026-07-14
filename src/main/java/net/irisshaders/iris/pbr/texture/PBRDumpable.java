package net.irisshaders.iris.pbr.texture;

import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.util.Identifier;

public interface PBRDumpable extends DynamicTexture {
   Identifier getDefaultDumpLocation();
}
