package net.irisshaders.iris.api.v0;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public interface IrisApi {
   static IrisApi getInstance() {
      return IrisApiInternal.INSTANCE;
   }

   int getMinorApiRevision();

   boolean isShaderPackInUse();

   boolean isRenderingShadowPass();

   Object openMainIrisScreenObj(Object var1);

   String getMainScreenLanguageKey();

   IrisApiConfig getConfig();

   IrisTextVertexSink createTextVertexSink(int var1, IntFunction<ByteBuffer> var2);

   float getSunPathRotation();
}
