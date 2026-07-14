package net.minecraft.client.sound;

import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import javax.sound.sampled.AudioFormat;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;

public class StaticSound {
   @Nullable
   private ByteBuffer sample;
   private final AudioFormat format;
   private boolean hasBuffer;
   private int streamBufferPointer;

   public StaticSound(ByteBuffer sample, AudioFormat format) {
      this.sample = sample;
      this.format = format;
      if (ProtocolTranslator.getTargetVersion().equals(AprilFoolsProtocolVersion.s3d_shareware)) {
         this.apply8BitSound(sample);
      }
   }

   private void apply8BitSound(ByteBuffer byteBuffer) {
      if (byteBuffer == null) {
         return;
      }

      if (this.format.getChannels() == 1) {
         this.apply8BitMono(byteBuffer);
      } else {
         this.apply8BitStereo(byteBuffer);
      }
   }

   private void apply8BitMono(ByteBuffer byteBuffer) {
      short sample = 0;
      int repeat = 0;

      while (byteBuffer.hasRemaining()) {
         if (repeat == 0) {
            byteBuffer.mark();
            sample = (short)(byteBuffer.getShort() & -4);
            byteBuffer.reset();
            repeat = 15;
         } else {
            repeat--;
         }

         byteBuffer.putShort(sample);
      }

      byteBuffer.flip();
   }

   private void apply8BitStereo(ByteBuffer byteBuffer) {
      short leftSample = 0;
      short rightSample = 0;
      int repeat = 0;

      while (byteBuffer.hasRemaining()) {
         if (repeat == 0) {
            byteBuffer.mark();
            leftSample = (short)(byteBuffer.getShort() & -4);
            rightSample = (short)(byteBuffer.getShort() & -4);
            byteBuffer.reset();
            repeat = 15;
         } else {
            repeat--;
         }

         byteBuffer.putShort(leftSample);
         byteBuffer.putShort(rightSample);
      }

      byteBuffer.flip();
   }

   OptionalInt getStreamBufferPointer() {
      if (!this.hasBuffer) {
         if (this.sample == null) {
            return OptionalInt.empty();
         }

         int i = AlUtil.getFormatId(this.format);
         int[] is = new int[1];
         AL10.alGenBuffers(is);
         if (AlUtil.checkErrors("Creating buffer")) {
            return OptionalInt.empty();
         }

         AL10.alBufferData(is[0], i, this.sample, (int)this.format.getSampleRate());
         if (AlUtil.checkErrors("Assigning buffer data")) {
            return OptionalInt.empty();
         }

         this.streamBufferPointer = is[0];
         this.hasBuffer = true;
         this.sample = null;
      }

      return OptionalInt.of(this.streamBufferPointer);
   }

   public void close() {
      if (this.hasBuffer) {
         AL10.alDeleteBuffers(new int[]{this.streamBufferPointer});
         if (AlUtil.checkErrors("Deleting stream buffers")) {
            return;
         }
      }

      this.hasBuffer = false;
   }

   public OptionalInt takeStreamBufferPointer() {
      OptionalInt optionalInt = this.getStreamBufferPointer();
      this.hasBuffer = false;
      return optionalInt;
   }
}
