package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;

public class NbtString implements NbtElement {
   private static final int SIZE = 36;
   public static final NbtType<NbtString> TYPE = new NbtType.OfVariableSize<NbtString>() {
      public NbtString read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         return NbtString.of(readString(dataInput, nbtSizeTracker));
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         return visitor.visitString(readString(input, tracker));
      }

      private static String readString(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(36L);
         String string = input.readUTF();
         tracker.add(2L, string.length());
         return string;
      }

      @Override
      public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
         NbtString.skip(input);
      }

      @Override
      public String getCrashReportName() {
         return "STRING";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_String";
      }

      @Override
      public boolean isImmutable() {
         return true;
      }
   };
   private static final NbtString EMPTY = new NbtString("");
   private static final char DOUBLE_QUOTE = '"';
   private static final char SINGLE_QUOTE = '\'';
   private static final char BACKSLASH = '\\';
   private static final char NULL = '\u0000';
   private final String value;

   public static void skip(DataInput input) throws IOException {
      input.skipBytes(input.readUnsignedShort());
   }

   private NbtString(String value) {
      Objects.requireNonNull(value, "Null string not allowed");
      this.value = value;
   }

   public static NbtString of(String value) {
      return value.isEmpty() ? EMPTY : new NbtString(value);
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeUTF(this.value);
   }

   @Override
   public int getSizeInBytes() {
      return 36 + 2 * this.value.length();
   }

   @Override
   public byte getType() {
      return 8;
   }

   @Override
   public NbtType<NbtString> getNbtType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return NbtElement.super.asString();
   }

   public NbtString copy() {
      return this;
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof NbtString && Objects.equals(this.value, ((NbtString)o).value);
   }

   @Override
   public int hashCode() {
      return this.value.hashCode();
   }

   @Override
   public String asString() {
      return this.value;
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitString(this);
   }

   public static String escape(String value) {
      StringBuilder stringBuilder = new StringBuilder(" ");
      char c = 0;

      for (int i = 0; i < value.length(); i++) {
         char d = value.charAt(i);
         if (d == '\\') {
            stringBuilder.append('\\');
         } else if (d == '"' || d == '\'') {
            if (c == 0) {
               c = (char)(d == '"' ? 39 : 34);
            }

            if (c == d) {
               stringBuilder.append('\\');
            }
         }

         stringBuilder.append(d);
      }

      if (c == 0) {
         c = '"';
      }

      stringBuilder.setCharAt(0, c);
      stringBuilder.append(c);
      return stringBuilder.toString();
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      return visitor.visitString(this.value);
   }
}
