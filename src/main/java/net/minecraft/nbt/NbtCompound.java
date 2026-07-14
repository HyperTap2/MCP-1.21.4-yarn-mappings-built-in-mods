package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;

public class NbtCompound implements NbtElement {
   public static final Codec<NbtCompound> CODEC = Codec.PASSTHROUGH
      .comapFlatMap(
         dynamic -> {
            NbtElement nbtElement = (NbtElement)dynamic.convert(NbtOps.INSTANCE).getValue();
            return nbtElement instanceof NbtCompound nbtCompound
               ? DataResult.success(nbtCompound == dynamic.getValue() ? nbtCompound.copy() : nbtCompound)
               : DataResult.error(() -> "Not a compound tag: " + nbtElement);
         },
         nbt -> new Dynamic(NbtOps.INSTANCE, nbt.copy())
      );
   private static final int SIZE = 48;
   private static final int field_41719 = 32;
   public static final NbtType<NbtCompound> TYPE = new NbtType.OfVariableSize<NbtCompound>() {
      public NbtCompound read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         nbtSizeTracker.pushStack();

         try {
            return readCompound(dataInput, nbtSizeTracker);
         } finally {
            nbtSizeTracker.popStack();
         }
      }

      private static NbtCompound readCompound(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(48L);
         Map<String, NbtElement> map = new Object2ObjectOpenHashMap<>();

         byte b;
         while ((b = input.readByte()) != 0) {
            String string = readString(input, tracker);
            NbtElement nbtElement = NbtCompound.read(NbtTypes.byId(b), string, input, tracker);
            if (map.put(string, nbtElement) == null) {
               tracker.add(36L);
            }
         }

         return new NbtCompound(map);
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         tracker.pushStack();

         try {
            return scanCompound(input, visitor, tracker);
         } finally {
            tracker.popStack();
         }
      }

      private static NbtScanner.Result scanCompound(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         tracker.add(48L);

         byte b;
         label35:
         while ((b = input.readByte()) != 0) {
            NbtType<?> nbtType = NbtTypes.byId(b);
            switch (visitor.visitSubNbtType(nbtType)) {
               case HALT:
                  return NbtScanner.Result.HALT;
               case BREAK:
                  NbtString.skip(input);
                  nbtType.skip(input, tracker);
                  break label35;
               case SKIP:
                  NbtString.skip(input);
                  nbtType.skip(input, tracker);
                  break;
               default:
                  String string = readString(input, tracker);
                  switch (visitor.startSubNbt(nbtType, string)) {
                     case HALT:
                        return NbtScanner.Result.HALT;
                     case BREAK:
                        nbtType.skip(input, tracker);
                        break label35;
                     case SKIP:
                        nbtType.skip(input, tracker);
                        break;
                     default:
                        tracker.add(36L);
                        switch (nbtType.doAccept(input, visitor, tracker)) {
                           case HALT:
                              return NbtScanner.Result.HALT;
                           case BREAK:
                        }
                  }
            }
         }

         if (b != 0) {
            while ((b = input.readByte()) != 0) {
               NbtString.skip(input);
               NbtTypes.byId(b).skip(input, tracker);
            }
         }

         return visitor.endNested();
      }

      private static String readString(DataInput input, NbtSizeTracker tracker) throws IOException {
         String string = input.readUTF();
         tracker.add(28L);
         tracker.add(2L, string.length());
         return string;
      }

      @Override
      public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.pushStack();

         byte b;
         try {
            while ((b = input.readByte()) != 0) {
               NbtString.skip(input);
               NbtTypes.byId(b).skip(input, tracker);
            }
         } finally {
            tracker.popStack();
         }
      }

      @Override
      public String getCrashReportName() {
         return "COMPOUND";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_Compound";
      }
   };
   private final Map<String, NbtElement> entries;

   protected NbtCompound(Map<String, NbtElement> entries) {
      this.entries = entries;
   }

   public NbtCompound() {
      this(new Object2ObjectOpenHashMap<>());
   }

   @Override
   public void write(DataOutput output) throws IOException {
      for (String string : this.entries.keySet()) {
         NbtElement nbtElement = this.entries.get(string);
         write(string, nbtElement, output);
      }

      output.writeByte(0);
   }

   @Override
   public int getSizeInBytes() {
      int i = 48;

      for (Entry<String, NbtElement> entry : this.entries.entrySet()) {
         i += 28 + 2 * entry.getKey().length();
         i += 36;
         i += entry.getValue().getSizeInBytes();
      }

      return i;
   }

   public Set<String> getKeys() {
      return this.entries.keySet();
   }

   @Override
   public byte getType() {
      return 10;
   }

   @Override
   public NbtType<NbtCompound> getNbtType() {
      return TYPE;
   }

   public int getSize() {
      return this.entries.size();
   }

   @Nullable
   public NbtElement put(String key, NbtElement element) {
      return this.entries.put(key, element);
   }

   public void putByte(String key, byte value) {
      this.entries.put(key, NbtByte.of(value));
   }

   public void putShort(String key, short value) {
      this.entries.put(key, NbtShort.of(value));
   }

   public void putInt(String key, int value) {
      this.entries.put(key, NbtInt.of(value));
   }

   public void putLong(String key, long value) {
      this.entries.put(key, NbtLong.of(value));
   }

   public void putUuid(String key, UUID value) {
      this.entries.put(key, NbtHelper.fromUuid(value));
   }

   public UUID getUuid(String key) {
      return NbtHelper.toUuid(this.get(key));
   }

   public boolean containsUuid(String key) {
      NbtElement nbtElement = this.get(key);
      return nbtElement != null && nbtElement.getNbtType() == NbtIntArray.TYPE && ((NbtIntArray)nbtElement).getIntArray().length == 4;
   }

   public void putFloat(String key, float value) {
      this.entries.put(key, NbtFloat.of(value));
   }

   public void putDouble(String key, double value) {
      this.entries.put(key, NbtDouble.of(value));
   }

   public void putString(String key, String value) {
      this.entries.put(key, NbtString.of(value));
   }

   public void putByteArray(String key, byte[] value) {
      this.entries.put(key, new NbtByteArray(value));
   }

   public void putByteArray(String key, List<Byte> value) {
      this.entries.put(key, new NbtByteArray(value));
   }

   public void putIntArray(String key, int[] value) {
      this.entries.put(key, new NbtIntArray(value));
   }

   public void putIntArray(String key, List<Integer> value) {
      this.entries.put(key, new NbtIntArray(value));
   }

   public void putLongArray(String key, long[] value) {
      this.entries.put(key, new NbtLongArray(value));
   }

   public void putLongArray(String key, List<Long> value) {
      this.entries.put(key, new NbtLongArray(value));
   }

   public void putBoolean(String key, boolean value) {
      this.entries.put(key, NbtByte.of(value));
   }

   @Nullable
   public NbtElement get(String key) {
      return this.entries.get(key);
   }

   public byte getType(String key) {
      NbtElement nbtElement = this.entries.get(key);
      return nbtElement == null ? 0 : nbtElement.getType();
   }

   public boolean contains(String key) {
      return this.entries.containsKey(key);
   }

   public boolean contains(String key, int type) {
      int i = this.getType(key);
      if (i == type) {
         return true;
      } else {
         return type != 99 ? false : i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6;
      }
   }

   public byte getByte(String key) {
      try {
         if (this.contains(key, 99)) {
            return ((AbstractNbtNumber)this.entries.get(key)).byteValue();
         }
      } catch (ClassCastException var3) {
      }

      return 0;
   }

   public short getShort(String key) {
      try {
         if (this.contains(key, 99)) {
            return ((AbstractNbtNumber)this.entries.get(key)).shortValue();
         }
      } catch (ClassCastException var3) {
      }

      return 0;
   }

   public int getInt(String key) {
      try {
         if (this.contains(key, 99)) {
            return ((AbstractNbtNumber)this.entries.get(key)).intValue();
         }
      } catch (ClassCastException var3) {
      }

      return 0;
   }

   public long getLong(String key) {
      try {
         if (this.contains(key, 99)) {
            return ((AbstractNbtNumber)this.entries.get(key)).longValue();
         }
      } catch (ClassCastException var3) {
      }

      return 0L;
   }

   public float getFloat(String key) {
      try {
         if (this.contains(key, 99)) {
            return ((AbstractNbtNumber)this.entries.get(key)).floatValue();
         }
      } catch (ClassCastException var3) {
      }

      return 0.0F;
   }

   public double getDouble(String key) {
      try {
         if (this.contains(key, 99)) {
            return ((AbstractNbtNumber)this.entries.get(key)).doubleValue();
         }
      } catch (ClassCastException var3) {
      }

      return 0.0;
   }

   public String getString(String key) {
      try {
         if (this.contains(key, 8)) {
            return this.entries.get(key).asString();
         }
      } catch (ClassCastException var3) {
      }

      return "";
   }

   public byte[] getByteArray(String key) {
      try {
         if (this.contains(key, 7)) {
            return ((NbtByteArray)this.entries.get(key)).getByteArray();
         }
      } catch (ClassCastException classCastException) {
         throw new CrashException(this.createCrashReport(key, NbtByteArray.TYPE, classCastException));
      }

      return new byte[0];
   }

   public int[] getIntArray(String key) {
      try {
         if (this.contains(key, 11)) {
            return ((NbtIntArray)this.entries.get(key)).getIntArray();
         }
      } catch (ClassCastException classCastException) {
         throw new CrashException(this.createCrashReport(key, NbtIntArray.TYPE, classCastException));
      }

      return new int[0];
   }

   public long[] getLongArray(String key) {
      try {
         if (this.contains(key, 12)) {
            return ((NbtLongArray)this.entries.get(key)).getLongArray();
         }
      } catch (ClassCastException classCastException) {
         throw new CrashException(this.createCrashReport(key, NbtLongArray.TYPE, classCastException));
      }

      return new long[0];
   }

   public NbtCompound getCompound(String key) {
      try {
         if (this.contains(key, 10)) {
            return (NbtCompound)this.entries.get(key);
         }
      } catch (ClassCastException classCastException) {
         throw new CrashException(this.createCrashReport(key, TYPE, classCastException));
      }

      return new NbtCompound();
   }

   public NbtList getList(String key, int type) {
      try {
         if (this.getType(key) == 9) {
            NbtList nbtList = (NbtList)this.entries.get(key);
            if (!nbtList.isEmpty() && nbtList.getHeldType() != type) {
               return new NbtList();
            }

            return nbtList;
         }
      } catch (ClassCastException classCastException) {
         throw new CrashException(this.createCrashReport(key, NbtList.TYPE, classCastException));
      }

      return new NbtList();
   }

   public boolean getBoolean(String key) {
      return this.getByte(key) != 0;
   }

   public void remove(String key) {
      this.entries.remove(key);
   }

   @Override
   public String toString() {
      return this.asString();
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   private CrashReport createCrashReport(String key, NbtType<?> reader, ClassCastException exception) {
      CrashReport crashReport = CrashReport.create(exception, "Reading NBT data");
      CrashReportSection crashReportSection = crashReport.addElement("Corrupt NBT tag", 1);
      crashReportSection.add("Tag type found", () -> this.entries.get(key).getNbtType().getCrashReportName());
      crashReportSection.add("Tag type expected", reader::getCrashReportName);
      crashReportSection.add("Tag name", key);
      return crashReport;
   }

   protected NbtCompound shallowCopy() {
      return new NbtCompound(new HashMap<>(this.entries));
   }

   public NbtCompound copy() {
      Map<String, NbtElement> map = new Object2ObjectOpenHashMap<>(Maps.transformValues(this.entries, NbtElement::copy));
      return new NbtCompound(map);
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof NbtCompound && Objects.equals(this.entries, ((NbtCompound)o).entries);
   }

   @Override
   public int hashCode() {
      return this.entries.hashCode();
   }

   private static void write(String key, NbtElement element, DataOutput output) throws IOException {
      output.writeByte(element.getType());
      if (element.getType() != 0) {
         output.writeUTF(key);
         element.write(output);
      }
   }

   static NbtElement read(NbtType<?> reader, String key, DataInput input, NbtSizeTracker tracker) {
      try {
         return reader.read(input, tracker);
      } catch (IOException iOException) {
         CrashReport crashReport = CrashReport.create(iOException, "Loading NBT data");
         CrashReportSection crashReportSection = crashReport.addElement("NBT Tag");
         crashReportSection.add("Tag name", key);
         crashReportSection.add("Tag type", reader.getCrashReportName());
         throw new NbtCrashException(crashReport);
      }
   }

   public NbtCompound copyFrom(NbtCompound source) {
      for (String string : source.entries.keySet()) {
         NbtElement nbtElement = source.entries.get(string);
         if (nbtElement.getType() == 10) {
            if (this.contains(string, 10)) {
               NbtCompound nbtCompound = this.getCompound(string);
               nbtCompound.copyFrom((NbtCompound)nbtElement);
            } else {
               this.put(string, nbtElement.copy());
            }
         } else {
            this.put(string, nbtElement.copy());
         }
      }

      return this;
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitCompound(this);
   }

   protected Set<Entry<String, NbtElement>> entrySet() {
      return this.entries.entrySet();
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      for (Entry<String, NbtElement> entry : this.entries.entrySet()) {
         NbtElement nbtElement = entry.getValue();
         NbtType<?> nbtType = nbtElement.getNbtType();
         NbtScanner.NestedResult nestedResult = visitor.visitSubNbtType(nbtType);
         switch (nestedResult) {
            case HALT:
               return NbtScanner.Result.HALT;
            case BREAK:
               return visitor.endNested();
            case SKIP:
               break;
            default:
               nestedResult = visitor.startSubNbt(nbtType, entry.getKey());
               switch (nestedResult) {
                  case HALT:
                     return NbtScanner.Result.HALT;
                  case BREAK:
                     return visitor.endNested();
                  case SKIP:
                     break;
                  default:
                     NbtScanner.Result result = nbtElement.doAccept(visitor);
                     switch (result) {
                        case HALT:
                           return NbtScanner.Result.HALT;
                        case BREAK:
                           return visitor.endNested();
                     }
               }
         }
      }

      return visitor.endNested();
   }
}
