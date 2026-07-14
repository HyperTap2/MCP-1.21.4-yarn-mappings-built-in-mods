package net.caffeinemc.mods.lithium.common.util.change_tracking;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ChangeSubscriber<T> {
   static <T> ChangeSubscriber<T> combine(ChangeSubscriber<T> previous, int previousData, ChangeSubscriber<T> added, int addedData) {
      if (previous == null) {
         return added;
      }
      if (previous instanceof Multi<T> multi) {
         ArrayList<ChangeSubscriber<T>> subscribers = new ArrayList<>(multi.subscribers);
         IntArrayList data = new IntArrayList(multi.subscriberData);
         subscribers.add(added);
         data.add(addedData);
         return new Multi<>(subscribers, data);
      }
      ArrayList<ChangeSubscriber<T>> subscribers = new ArrayList<>();
      subscribers.add(previous);
      subscribers.add(added);
      IntArrayList data = new IntArrayList();
      data.add(previousData);
      data.add(addedData);
      return new Multi<>(subscribers, data);
   }

   static <T> ChangeSubscriber<T> without(ChangeSubscriber<T> previous, ChangeSubscriber<T> removed) {
      return without(previous, removed, 0, false);
   }

   static <T> ChangeSubscriber<T> without(ChangeSubscriber<T> previous, ChangeSubscriber<T> removed, int removedData, boolean matchData) {
      if (previous == removed) {
         return null;
      }
      if (previous instanceof Multi<T> multi) {
         int index = multi.indexOf(removed, removedData, matchData);
         if (index < 0) {
            return previous;
         }
         if (multi.subscribers.size() == 2) {
            return multi.subscribers.get(1 - index);
         }
         ArrayList<ChangeSubscriber<T>> subscribers = new ArrayList<>(multi.subscribers);
         IntArrayList data = new IntArrayList(multi.subscriberData);
         subscribers.remove(index);
         data.removeInt(index);
         return new Multi<>(subscribers, data);
      }
      return previous;
   }

   static <T> int dataWithout(ChangeSubscriber<T> previous, ChangeSubscriber<T> removed, int data) {
      return dataWithout(previous, removed, data, 0, false);
   }

   static <T> int dataWithout(ChangeSubscriber<T> previous, ChangeSubscriber<T> removed, int data, int removedData, boolean matchData) {
      if (previous instanceof Multi<T> multi) {
         int index = multi.indexOf(removed, removedData, matchData);
         return index >= 0 && multi.subscribers.size() == 2 ? multi.subscriberData.getInt(1 - index) : data;
      }
      return previous == removed ? 0 : data;
   }

   static int dataOf(ChangeSubscriber<?> subscribers, ChangeSubscriber<?> subscriber, int data) {
      return subscribers instanceof Multi<?> multi ? multi.subscriberData.getInt(multi.subscribers.indexOf(subscriber)) : data;
   }

   static boolean containsSubscriber(ChangeSubscriber<ItemStack> subscribers, int data, ChangeSubscriber<ItemStack> expected, int expectedData) {
      return subscribers instanceof Multi<ItemStack> multi
         ? multi.indexOf(expected, expectedData, true) >= 0
         : subscribers == expected && data == expectedData;
   }

   void lithium$notify(@Nullable T publisher, int subscriberData);

   void lithium$forceUnsubscribe(T publisher, int subscriberData);

   interface CountChangeSubscriber<T> extends ChangeSubscriber<T> {
      void lithium$notifyCount(T publisher, int subscriberData, int newCount);
   }

   interface EnchantmentSubscriber<T> extends ChangeSubscriber<T> {
      void lithium$notifyAfterEnchantmentChange(T publisher, int subscriberData);
   }

   final class Multi<T> implements CountChangeSubscriber<T>, EnchantmentSubscriber<T> {
      private final ArrayList<ChangeSubscriber<T>> subscribers;
      private final IntArrayList subscriberData;

      private Multi(ArrayList<ChangeSubscriber<T>> subscribers, IntArrayList subscriberData) {
         this.subscribers = subscribers;
         this.subscriberData = subscriberData;
      }

      @Override
      public void lithium$notify(T publisher, int ignored) {
         for (int i = 0; i < this.subscribers.size(); i++) {
            this.subscribers.get(i).lithium$notify(publisher, this.subscriberData.getInt(i));
         }
      }

      @Override
      public void lithium$forceUnsubscribe(T publisher, int ignored) {
         for (int i = 0; i < this.subscribers.size(); i++) {
            this.subscribers.get(i).lithium$forceUnsubscribe(publisher, this.subscriberData.getInt(i));
         }
      }

      @Override
      public void lithium$notifyCount(T publisher, int ignored, int newCount) {
         for (int i = 0; i < this.subscribers.size(); i++) {
            if (this.subscribers.get(i) instanceof CountChangeSubscriber<T> subscriber) {
               subscriber.lithium$notifyCount(publisher, this.subscriberData.getInt(i), newCount);
            }
         }
      }

      @Override
      public void lithium$notifyAfterEnchantmentChange(T publisher, int ignored) {
         for (int i = 0; i < this.subscribers.size(); i++) {
            if (this.subscribers.get(i) instanceof EnchantmentSubscriber<T> subscriber) {
               subscriber.lithium$notifyAfterEnchantmentChange(publisher, this.subscriberData.getInt(i));
            }
         }
      }

      private int indexOf(ChangeSubscriber<T> subscriber, int data, boolean matchData) {
         for (int i = 0; i < this.subscribers.size(); i++) {
            if (this.subscribers.get(i) == subscriber && (!matchData || this.subscriberData.getInt(i) == data)) {
               return i;
            }
         }
         return -1;
      }
   }
}
