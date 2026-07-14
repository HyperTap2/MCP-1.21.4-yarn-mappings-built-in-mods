package net.minecraft.nbt;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;

public class NbtList extends AbstractNbtList<NbtElement> {
   private static final int SIZE = 37;
   public static final NbtType<NbtList> TYPE = new NbtType.OfVariableSize<NbtList>() {
      public NbtList read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         nbtSizeTracker.pushStack();

         try {
            return readList(dataInput, nbtSizeTracker);
         } finally {
            nbtSizeTracker.popStack();
         }
      }

      private static NbtList readList(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(37L);
         byte b = input.readByte();
         int i = input.readInt();
         if (b == 0 && i > 0) {
            throw new InvalidNbtException("Missing type on ListTag");
         }

         tracker.add(4L, i);
         NbtType<?> nbtType = NbtTypes.byId(b);
         List<NbtElement> list = Lists.newArrayListWithCapacity(i);

         for (int j = 0; j < i; j++) {
            list.add(nbtType.read(input, tracker));
         }

         return new NbtList(list, b);
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         tracker.pushStack();

         try {
            return scanList(input, visitor, tracker);
         } finally {
            tracker.popStack();
         }
      }

      private static NbtScanner.Result scanList(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         tracker.add(37L);
         NbtType<?> nbtType = NbtTypes.byId(input.readByte());
         int i = input.readInt();
         switch (visitor.visitListMeta(nbtType, i)) {
            case HALT:
               return NbtScanner.Result.HALT;
            case BREAK:
               nbtType.skip(input, i, tracker);
               return visitor.endNested();
            default:
               tracker.add(4L, i);
               int j = 0;

               while (true) {
                  label41: {
                     if (j < i) {
                        switch (visitor.startListItem(nbtType, j)) {
                           case HALT:
                              return NbtScanner.Result.HALT;
                           case BREAK:
                              nbtType.skip(input, tracker);
                              break;
                           case SKIP:
                              nbtType.skip(input, tracker);
                              break label41;
                           default:
                              switch (nbtType.doAccept(input, visitor, tracker)) {
                                 case HALT:
                                    return NbtScanner.Result.HALT;
                                 case BREAK:
                                    break;
                                 default:
                                    break label41;
                              }
                        }
                     }

                     int k = i - 1 - j;
                     if (k > 0) {
                        nbtType.skip(input, k, tracker);
                     }

                     return visitor.endNested();
                  }

                  j++;
               }
         }
      }

      @Override
      public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.pushStack();

         try {
            NbtType<?> nbtType = NbtTypes.byId(input.readByte());
            int i = input.readInt();
            nbtType.skip(input, i, tracker);
         } finally {
            tracker.popStack();
         }
      }

      @Override
      public String getCrashReportName() {
         return "LIST";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_List";
      }
   };
   private final List<NbtElement> value;
   private byte type;

   NbtList(List<NbtElement> list, byte type) {
      this.value = list;
      this.type = type;
   }

   public NbtList() {
      this(Lists.newArrayList(), (byte)0);
   }

   @Override
   public void write(DataOutput output) throws IOException {
      if (this.value.isEmpty()) {
         this.type = 0;
      } else {
         this.type = this.value.get(0).getType();
      }

      output.writeByte(this.type);
      output.writeInt(this.value.size());

      for (NbtElement nbtElement : this.value) {
         nbtElement.write(output);
      }
   }

   @Override
   public int getSizeInBytes() {
      int i = 37;
      i += 4 * this.value.size();

      for (NbtElement nbtElement : this.value) {
         i += nbtElement.getSizeInBytes();
      }

      return i;
   }

   @Override
   public byte getType() {
      return 9;
   }

   @Override
   public NbtType<NbtList> getNbtType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.asString();
   }

   private void forgetTypeIfEmpty() {
      if (this.value.isEmpty()) {
         this.type = 0;
      }
   }

   @Override
   public NbtElement remove(int i) {
      NbtElement nbtElement = this.value.remove(i);
      this.forgetTypeIfEmpty();
      return nbtElement;
   }

   @Override
   public boolean isEmpty() {
      return this.value.isEmpty();
   }

   public NbtCompound getCompound(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 10) {
            return (NbtCompound)nbtElement;
         }
      }

      return new NbtCompound();
   }

   public NbtList getList(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 9) {
            return (NbtList)nbtElement;
         }
      }

      return new NbtList();
   }

   public short getShort(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 2) {
            return ((NbtShort)nbtElement).shortValue();
         }
      }

      return 0;
   }

   public int getInt(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 3) {
            return ((NbtInt)nbtElement).intValue();
         }
      }

      return 0;
   }

   public int[] getIntArray(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 11) {
            return ((NbtIntArray)nbtElement).getIntArray();
         }
      }

      return new int[0];
   }

   public long[] getLongArray(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 12) {
            return ((NbtLongArray)nbtElement).getLongArray();
         }
      }

      return new long[0];
   }

   public double getDouble(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 6) {
            return ((NbtDouble)nbtElement).doubleValue();
         }
      }

      return 0.0;
   }

   public float getFloat(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         if (nbtElement.getType() == 5) {
            return ((NbtFloat)nbtElement).floatValue();
         }
      }

      return 0.0F;
   }

   public String getString(int index) {
      if (index >= 0 && index < this.value.size()) {
         NbtElement nbtElement = this.value.get(index);
         return nbtElement.getType() == 8 ? nbtElement.asString() : nbtElement.toString();
      } else {
         return "";
      }
   }

   @Override
   public int size() {
      return this.value.size();
   }

   public NbtElement get(int i) {
      return this.value.get(i);
   }

   @Override
   public NbtElement set(int i, NbtElement nbtElement) {
      NbtElement nbtElement2 = this.get(i);
      if (!this.setElement(i, nbtElement)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", nbtElement.getType(), this.type));
      } else {
         return nbtElement2;
      }
   }

   @Override
   public void add(int i, NbtElement nbtElement) {
      if (!this.addElement(i, nbtElement)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", nbtElement.getType(), this.type));
      }
   }

   @Override
   public boolean setElement(int index, NbtElement element) {
      if (this.canAdd(element)) {
         this.value.set(index, element);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addElement(int index, NbtElement element) {
      if (this.canAdd(element)) {
         this.value.add(index, element);
         return true;
      } else {
         return false;
      }
   }

   private boolean canAdd(NbtElement element) {
      if (element.getType() == 0) {
         return false;
      } else if (this.type == 0) {
         this.type = element.getType();
         return true;
      } else {
         return this.type == element.getType();
      }
   }

   public NbtList copy() {
      Iterable<NbtElement> iterable = NbtTypes.byId(this.type).isImmutable() ? this.value : Iterables.transform(this.value, NbtElement::copy);
      List<NbtElement> list = Lists.newArrayList(iterable);
      return new NbtList(list, this.type);
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof NbtList && Objects.equals(this.value, ((NbtList)o).value);
   }

   @Override
   public int hashCode() {
      return this.value.hashCode();
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitList(this);
   }

   @Override
   public byte getHeldType() {
      return this.type;
   }

   @Override
   public void clear() {
      this.value.clear();
      this.type = 0;
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      switch (visitor.visitListMeta(NbtTypes.byId(this.type), this.value.size())) {
         case HALT:
            return NbtScanner.Result.HALT;
         case BREAK:
            return visitor.endNested();
         default:
            int i = 0;

            while (i < this.value.size()) {
               NbtElement nbtElement = this.value.get(i);
               switch (visitor.startListItem(nbtElement.getNbtType(), i)) {
                  case HALT:
                     return NbtScanner.Result.HALT;
                  case BREAK:
                     return visitor.endNested();
                  default:
                     switch (nbtElement.doAccept(visitor)) {
                        case HALT:
                           return NbtScanner.Result.HALT;
                        case BREAK:
                           return visitor.endNested();
                     }
                  case SKIP:
                     i++;
               }
            }

            return visitor.endNested();
      }
   }
}
