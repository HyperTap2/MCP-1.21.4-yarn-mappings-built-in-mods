package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

public interface NbtElement {
   int field_33246 = 8;
   int field_33247 = 12;
   int field_33248 = 4;
   int field_33249 = 28;
   byte END_TYPE = 0;
   byte BYTE_TYPE = 1;
   byte SHORT_TYPE = 2;
   byte INT_TYPE = 3;
   byte LONG_TYPE = 4;
   byte FLOAT_TYPE = 5;
   byte DOUBLE_TYPE = 6;
   byte BYTE_ARRAY_TYPE = 7;
   byte STRING_TYPE = 8;
   byte LIST_TYPE = 9;
   byte COMPOUND_TYPE = 10;
   byte INT_ARRAY_TYPE = 11;
   byte LONG_ARRAY_TYPE = 12;
   byte NUMBER_TYPE = 99;
   int MAX_DEPTH = 512;

   void write(DataOutput output) throws IOException;

   @Override
   String toString();

   byte getType();

   NbtType<?> getNbtType();

   NbtElement copy();

   int getSizeInBytes();

   default String asString() {
      return new StringNbtWriter().apply(this);
   }

   void accept(NbtElementVisitor visitor);

   NbtScanner.Result doAccept(NbtScanner visitor);

   default void accept(NbtScanner visitor) {
      NbtScanner.Result result = visitor.start(this.getNbtType());
      if (result == NbtScanner.Result.CONTINUE) {
         this.doAccept(visitor);
      }
   }
}
