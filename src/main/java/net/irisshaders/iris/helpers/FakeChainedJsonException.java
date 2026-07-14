package net.irisshaders.iris.helpers;

import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.util.InvalidHierarchicalFileException;

public class FakeChainedJsonException extends InvalidHierarchicalFileException {
   private final ShaderCompileException trueException;

   public FakeChainedJsonException(ShaderCompileException e) {
      super("", e);
      this.trueException = e;
   }

   public ShaderCompileException getTrueException() {
      return this.trueException;
   }
}
