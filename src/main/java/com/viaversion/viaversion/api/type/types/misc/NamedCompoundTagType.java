package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.nbt.limiter.TagLimiter;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.type.OptionalType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NamedCompoundTagType extends Type<CompoundTag> {
   public static final int MAX_NBT_BYTES = 2097152;
   public static final int MAX_NESTING_LEVEL = 512;

   public NamedCompoundTagType() {
      super(CompoundTag.class);
   }

   @Override
   public CompoundTag read(ByteBuf buffer) {
      try {
         return read(buffer, true);
      } catch (IOException exception) {
         throw new RuntimeException(exception);
      }
   }

   @Override
   public void write(ByteBuf buffer, CompoundTag object) {
      try {
         write(buffer, object, "");
      } catch (IOException exception) {
         throw new RuntimeException(exception);
      }
   }

   public static CompoundTag read(ByteBuf buffer, boolean readName) throws IOException {
      byte id = buffer.readByte();
      if (id == 0) {
         return null;
      }
      if (id != CompoundTag.ID) {
         throw new IOException(String.format("Expected root tag to be a CompoundTag, was %s", id));
      }
      if (readName) {
         buffer.skipBytes(buffer.readUnsignedShort());
      }
      return CompoundTag.read(new ByteBufInputStream(buffer), TagLimiter.noop(), 0);
   }

   public static void write(ByteBuf buffer, Tag tag, @Nullable String name) throws IOException {
      if (tag == null) {
         buffer.writeByte(0);
         return;
      }
      ByteBufOutputStream out = new ByteBufOutputStream(buffer);
      out.writeByte(tag.getTagId());
      if (name != null) {
         out.writeUTF(name);
      }
      tag.write(out);
   }

   public static final class OptionalNamedCompoundTagType extends OptionalType<CompoundTag> {
      public OptionalNamedCompoundTagType() {
         super(Types.NAMED_COMPOUND_TAG);
      }
   }
}
