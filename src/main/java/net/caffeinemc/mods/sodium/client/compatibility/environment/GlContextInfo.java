package net.caffeinemc.mods.sodium.client.compatibility.environment;

import java.util.Objects;
import org.lwjgl.opengl.GL11C;

public record GlContextInfo(String vendor, String renderer, String version) {
   public static GlContextInfo create() {
      String vendor = Objects.requireNonNull(GL11C.glGetString(7936), "GL_VENDOR is NULL");
      String renderer = Objects.requireNonNull(GL11C.glGetString(7937), "GL_RENDERER is NULL");
      String version = Objects.requireNonNull(GL11C.glGetString(7938), "GL_VERSION is NULL");
      return new GlContextInfo(vendor, renderer, version);
   }
}
