package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.nbt.io.TagRegistry;
import com.viaversion.nbt.limiter.TagLimiter;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.type.OptionalType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import java.io.IOException;

public class TagType extends Type<Tag> {
   public TagType() {
      super(Tag.class);
   }

   @Override
   public Tag read(ByteBuf buffer) {
      byte id = buffer.readByte();
      if (id == 0) {
         return null;
      }
      try {
         return TagRegistry.read(id, new ByteBufInputStream(buffer), TagLimiter.noop(), 0);
      } catch (IOException exception) {
         if (Via.getManager().isDebug()) {
            throw new RuntimeException(exception);
         }
         throw new RuntimeException("Error reading tag: " + exception.getMessage());
      }
   }

   @Override
   public void write(ByteBuf buffer, Tag tag) {
      try {
         NamedCompoundTagType.write(buffer, tag, null);
      } catch (IOException exception) {
         throw new RuntimeException(exception);
      }
   }

   public static final class OptionalTagType extends OptionalType<Tag> {
      public OptionalTagType() {
         super(Types.TAG);
      }
   }
}
