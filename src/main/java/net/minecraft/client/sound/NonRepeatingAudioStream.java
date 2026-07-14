package net.minecraft.client.sound;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface NonRepeatingAudioStream extends AudioStream {
   ByteBuffer readAll() throws IOException;
}
