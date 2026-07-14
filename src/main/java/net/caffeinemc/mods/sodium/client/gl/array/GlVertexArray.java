package net.caffeinemc.mods.sodium.client.gl.array;

import net.caffeinemc.mods.sodium.client.gl.GlObject;
import org.lwjgl.opengl.GL30C;

public class GlVertexArray extends GlObject {
   public static final int NULL_ARRAY_ID = 0;

   public GlVertexArray() {
      this.setHandle(GL30C.glGenVertexArrays());
   }
}
