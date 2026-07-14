package net.minecraft.util.dynamic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.ListBuilder.Builder;
import com.mojang.serialization.RecordBuilder.MapBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class ForwardingDynamicOps<T> implements DynamicOps<T> {
   protected final DynamicOps<T> delegate;

   protected ForwardingDynamicOps(DynamicOps<T> delegate) {
      this.delegate = delegate;
   }

   public T empty() {
      return (T)this.delegate.empty();
   }

   public T emptyMap() {
      return (T)this.delegate.emptyMap();
   }

   public T emptyList() {
      return (T)this.delegate.emptyList();
   }

   public <U> U convertTo(DynamicOps<U> outputOps, T input) {
      return (U)this.delegate.convertTo(outputOps, input);
   }

   public DataResult<Number> getNumberValue(T input) {
      return this.delegate.getNumberValue(input);
   }

   public T createNumeric(Number number) {
      return (T)this.delegate.createNumeric(number);
   }

   public T createByte(byte b) {
      return (T)this.delegate.createByte(b);
   }

   public T createShort(short s) {
      return (T)this.delegate.createShort(s);
   }

   public T createInt(int i) {
      return (T)this.delegate.createInt(i);
   }

   public T createLong(long l) {
      return (T)this.delegate.createLong(l);
   }

   public T createFloat(float f) {
      return (T)this.delegate.createFloat(f);
   }

   public T createDouble(double d) {
      return (T)this.delegate.createDouble(d);
   }

   public DataResult<Boolean> getBooleanValue(T input) {
      return this.delegate.getBooleanValue(input);
   }

   public T createBoolean(boolean bl) {
      return (T)this.delegate.createBoolean(bl);
   }

   public DataResult<String> getStringValue(T input) {
      return this.delegate.getStringValue(input);
   }

   public T createString(String string) {
      return (T)this.delegate.createString(string);
   }

   public DataResult<T> mergeToList(T list, T value) {
      return this.delegate.mergeToList(list, value);
   }

   public DataResult<T> mergeToList(T list, List<T> values) {
      return this.delegate.mergeToList(list, values);
   }

   public DataResult<T> mergeToMap(T map, T key, T value) {
      return this.delegate.mergeToMap(map, key, value);
   }

   public DataResult<T> mergeToMap(T map, MapLike<T> values) {
      return this.delegate.mergeToMap(map, values);
   }

   public DataResult<T> mergeToMap(T object, Map<T, T> map) {
      return this.delegate.mergeToMap(object, map);
   }

   public DataResult<T> mergeToPrimitive(T object, T object2) {
      return this.delegate.mergeToPrimitive(object, object2);
   }

   public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
      return this.delegate.getMapValues(input);
   }

   public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
      return this.delegate.getMapEntries(input);
   }

   public T createMap(Map<T, T> map) {
      return (T)this.delegate.createMap(map);
   }

   public T createMap(Stream<Pair<T, T>> map) {
      return (T)this.delegate.createMap(map);
   }

   public DataResult<MapLike<T>> getMap(T input) {
      return this.delegate.getMap(input);
   }

   public DataResult<Stream<T>> getStream(T input) {
      return this.delegate.getStream(input);
   }

   public DataResult<Consumer<Consumer<T>>> getList(T input) {
      return this.delegate.getList(input);
   }

   public T createList(Stream<T> stream) {
      return (T)this.delegate.createList(stream);
   }

   public DataResult<ByteBuffer> getByteBuffer(T input) {
      return this.delegate.getByteBuffer(input);
   }

   public T createByteList(ByteBuffer buf) {
      return (T)this.delegate.createByteList(buf);
   }

   public DataResult<IntStream> getIntStream(T input) {
      return this.delegate.getIntStream(input);
   }

   public T createIntList(IntStream stream) {
      return (T)this.delegate.createIntList(stream);
   }

   public DataResult<LongStream> getLongStream(T input) {
      return this.delegate.getLongStream(input);
   }

   public T createLongList(LongStream stream) {
      return (T)this.delegate.createLongList(stream);
   }

   public T remove(T input, String key) {
      return (T)this.delegate.remove(input, key);
   }

   public boolean compressMaps() {
      return this.delegate.compressMaps();
   }

   public ListBuilder<T> listBuilder() {
      return new Builder(this);
   }

   public RecordBuilder<T> mapBuilder() {
      return new MapBuilder(this);
   }
}
