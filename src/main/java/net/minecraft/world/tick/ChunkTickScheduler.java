package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.caffeinemc.mods.lithium.common.world.scheduler.OrderedTickQueue;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;

public class ChunkTickScheduler<T> implements SerializableTickScheduler<T>, BasicTickScheduler<T> {
   private static volatile Reference2IntOpenHashMap<Object> TYPE_INDICES = createTypeIndices();
   private final Long2ReferenceAVLTreeMap<OrderedTickQueue<T>> queues = new Long2ReferenceAVLTreeMap<>();
   private final IntOpenHashSet allTicks = new IntOpenHashSet();
   @Nullable
   private List<Tick<T>> ticks;
   @Nullable
   private BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> tickConsumer;
   @Nullable
   private OrderedTickQueue<T> nextQueue;

   public ChunkTickScheduler() {
   }

   public ChunkTickScheduler(List<Tick<T>> ticks) {
      this.ticks = ticks;
      for (Tick<T> tick : ticks) {
         this.allTicks.add(tickToInt(tick.pos(), tick.type()));
      }
   }

   private static Reference2IntOpenHashMap<Object> createTypeIndices() {
      Reference2IntOpenHashMap<Object> map = new Reference2IntOpenHashMap<>();
      map.defaultReturnValue(-1);
      return map;
   }

   private static int tickToInt(BlockPos pos, Object type) {
      int typeIndex = TYPE_INDICES.getInt(type);
      if (typeIndex == -1) {
         typeIndex = addType(type);
      }
      return (pos.getX() & 15) << 16 | (pos.getY() & 4095) << 4 | pos.getZ() & 15 | typeIndex << 20;
   }

   private static synchronized int addType(Object type) {
      int existing = TYPE_INDICES.getInt(type);
      if (existing != -1) {
         return existing;
      }
      Reference2IntOpenHashMap<Object> copy = TYPE_INDICES.clone();
      int index = copy.size();
      if (index >= 4096) {
         throw new IllegalStateException("Lithium tick scheduler supports at most 4096 tickable types");
      }
      copy.put(type, index);
      TYPE_INDICES = copy;
      return index;
   }

   private static long bucketKey(long time, TickPriority priority) {
      return time << 4 | priority.ordinal() & 15;
   }

   public void setTickConsumer(@Nullable BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> tickConsumer) {
      this.tickConsumer = tickConsumer;
   }

   @Nullable
   public OrderedTick<T> peekNextTick() {
      return this.nextQueue == null ? null : this.nextQueue.peek();
   }

   @Nullable
   public OrderedTick<T> pollNextTick() {
      if (this.nextQueue == null) {
         return null;
      }
      OrderedTick<T> tick = this.nextQueue.poll();
      if (tick != null) {
         this.allTicks.remove(tickToInt(tick.pos(), tick.type()));
         if (this.nextQueue.isEmpty()) {
            this.updateNextQueue(true);
         }
      }
      return tick;
   }

   @Override
   public void scheduleTick(OrderedTick<T> tick) {
      if (this.allTicks.add(tickToInt(tick.pos(), tick.type()))) {
         this.queueTick(tick);
      }
   }

   private void queueTick(OrderedTick<T> tick) {
      OrderedTickQueue<T> queue = this.queues.computeIfAbsent(bucketKey(tick.triggerTick(), tick.priority()), key -> new OrderedTickQueue<>());
      boolean wasEmpty = queue.isEmpty();
      queue.offer(tick);
      if (wasEmpty) {
         this.updateNextQueue(false);
      }
      if (this.tickConsumer != null) {
         this.tickConsumer.accept(this, tick);
      }
   }

   private void updateNextQueue(boolean removeEmpty) {
      if (removeEmpty && !this.queues.isEmpty()) {
         OrderedTickQueue<T> removed = this.queues.remove(this.queues.firstLongKey());
         if (removed != this.nextQueue) {
            throw new IllegalStateException("Tick queue order was corrupted");
         }
      }
      this.nextQueue = this.queues.isEmpty() ? null : this.queues.get(this.queues.firstLongKey());
   }

   @Override
   public boolean isQueued(BlockPos pos, T type) {
      return this.allTicks.contains(tickToInt(pos, type));
   }

   public void removeTicksIf(Predicate<OrderedTick<T>> predicate) {
      ObjectIterator<OrderedTickQueue<T>> iterator = this.queues.values().iterator();
      while (iterator.hasNext()) {
         OrderedTickQueue<T> queue = iterator.next();
         queue.sort();
         boolean removed = false;
         int size = queue.size();
         for (int index = 0; index < size; index++) {
            OrderedTick<T> tick = queue.getTickAtIndex(index);
            if (predicate.test(tick)) {
               queue.setTickAtIndex(index, null);
               this.allTicks.remove(tickToInt(tick.pos(), tick.type()));
               removed = true;
            }
         }
         if (removed) {
            queue.removeNullsAndConsumed();
         }
         if (queue.isEmpty()) {
            iterator.remove();
         }
      }
      this.updateNextQueue(false);
   }

   public Stream<OrderedTick<T>> getQueueAsStream() {
      return this.queues.values().stream().flatMap(Collection::stream);
   }

   @Override
   public int getTickCount() {
      return this.allTicks.size();
   }

   @Override
   public List<Tick<T>> collectTicks(long time) {
      List<Tick<T>> collected = new ArrayList<>(this.getTickCount());
      if (this.ticks != null) {
         collected.addAll(this.ticks);
      }
      for (OrderedTickQueue<T> queue : this.queues.values()) {
         for (OrderedTick<T> tick : queue) {
            collected.add(tick.toTick(time));
         }
      }
      return collected;
   }

   public NbtList toNbt(long time, Function<T, String> typeToNameFunction) {
      NbtList nbt = new NbtList();
      for (Tick<T> tick : this.collectTicks(time)) {
         nbt.add(tick.toNbt(typeToNameFunction));
      }
      return nbt;
   }

   public void disable(long time) {
      if (this.ticks != null) {
         int order = -this.ticks.size();
         for (Tick<T> tick : this.ticks) {
            this.queueTick(tick.createOrderedTick(time, order++));
         }
      }
      this.ticks = null;
   }

   public static <T> ChunkTickScheduler<T> create(NbtList tickQueue, Function<String, Optional<T>> nameToTypeFunction, ChunkPos pos) {
      return new ChunkTickScheduler<>(Tick.tick(tickQueue, nameToTypeFunction, pos));
   }
}
