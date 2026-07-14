package net.minecraft.block.entity;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.caffeinemc.mods.lithium.api.inventory.LithiumInventory;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.caffeinemc.mods.lithium.common.hopper.InventoryHelper;
import net.caffeinemc.mods.lithium.common.hopper.LithiumStackList;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public abstract class LockableContainerBlockEntity extends BlockEntity
   implements Inventory, NamedScreenHandlerFactory, Nameable, LithiumInventory, InventoryChangeTracker {
   private ContainerLock lock = ContainerLock.EMPTY;
   @Nullable
   private Text customName;
   private ReferenceArraySet<InventoryChangeListener> lithium$contentListeners;
   private ReferenceArraySet<InventoryChangeListener> lithium$majorListeners;

   protected LockableContainerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
      super(blockEntityType, blockPos, blockState);
   }

   @Override
   protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      super.readNbt(nbt, registries);
      this.lock = ContainerLock.fromNbt(nbt, registries);
      if (nbt.contains("CustomName", 8)) {
         this.customName = tryParseCustomName(nbt.getString("CustomName"), registries);
      }
   }

   @Override
   protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      super.writeNbt(nbt, registries);
      this.lock.writeNbt(nbt, registries);
      if (this.customName != null) {
         nbt.putString("CustomName", Text.Serialization.toJsonString(this.customName, registries));
      }
   }

   @Override
   public Text getName() {
      return this.customName != null ? this.customName : this.getContainerName();
   }

   @Override
   public Text getDisplayName() {
      return this.getName();
   }

   @Nullable
   @Override
   public Text getCustomName() {
      return this.customName;
   }

   protected abstract Text getContainerName();

   public boolean checkUnlocked(PlayerEntity player) {
      return checkUnlocked(player, this.lock, this.getDisplayName());
   }

   public static boolean checkUnlocked(PlayerEntity player, ContainerLock lock, Text containerName) {
      if (!player.isSpectator() && !lock.canOpen(player.getMainHandStack())) {
         player.sendMessage(Text.translatable("container.isLocked", containerName), true);
         player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 1.0F);
         return false;
      } else {
         return true;
      }
   }

   protected abstract DefaultedList<ItemStack> getHeldStacks();

   protected abstract void setHeldStacks(DefaultedList<ItemStack> inventory);

   @Override
   public DefaultedList<ItemStack> getInventoryLithium() {
      return this.getHeldStacks();
   }

   @Override
   public void setInventoryLithium(DefaultedList<ItemStack> inventory) {
      this.setHeldStacks(inventory);
      this.lithium$emitStackListReplaced();
   }

   @Override
   public void lithium$emitContentModified() {
      if (this.lithium$contentListeners != null) {
         for (InventoryChangeListener listener : this.lithium$contentListeners) {
            listener.lithium$handleInventoryContentModified(this);
         }
         this.lithium$contentListeners.clear();
      }
   }

   @Override
   public void lithium$emitStackListReplaced() {
      this.lithium$notifyMajorListeners(true);
      this.lithium$invalidateChangeListening();
   }

   @Override
   public void lithium$emitRemoved() {
      this.lithium$notifyMajorListeners(false);
      this.lithium$invalidateChangeListening();
   }

   private void lithium$notifyMajorListeners(boolean replaced) {
      if (this.lithium$majorListeners != null) {
         for (InventoryChangeListener listener : this.lithium$majorListeners) {
            if (replaced) {
               listener.handleStackListReplaced(this);
            } else {
               listener.lithium$handleInventoryRemoved(this);
            }
         }
         this.lithium$majorListeners.clear();
      }
      if (this instanceof InventoryChangeListener listener) {
         if (replaced) {
            listener.handleStackListReplaced(this);
         } else {
            listener.lithium$handleInventoryRemoved(this);
         }
      }
   }

   private void lithium$invalidateChangeListening() {
      if (this.lithium$contentListeners != null) {
         this.lithium$contentListeners.clear();
      }
      LithiumStackList stacks = InventoryHelper.getLithiumStackListOrNull(this);
      if (stacks != null) {
         stacks.removeInventoryModificationCallback(this);
      }
   }

   @Override
   public void lithium$emitFirstComparatorAdded() {
      if (this.lithium$contentListeners != null && !this.lithium$contentListeners.isEmpty()) {
         this.lithium$contentListeners.removeIf(listener -> listener.lithium$handleComparatorAdded(this));
      }
   }

   @Override
   public void lithium$forwardContentChangeOnce(InventoryChangeListener listener, LithiumStackList stackList, InventoryChangeTracker tracker) {
      if (this.lithium$contentListeners == null) {
         this.lithium$contentListeners = new ReferenceArraySet<>(1);
      }
      stackList.setInventoryModificationCallback(tracker);
      this.lithium$contentListeners.add(listener);
   }

   @Override
   public void lithium$forwardMajorInventoryChanges(InventoryChangeListener listener) {
      if (this.lithium$majorListeners == null) {
         this.lithium$majorListeners = new ReferenceArraySet<>(1);
      }
      this.lithium$majorListeners.add(listener);
   }

   @Override
   public void lithium$stopForwardingMajorInventoryChanges(InventoryChangeListener listener) {
      if (this.lithium$majorListeners != null) {
         this.lithium$majorListeners.remove(listener);
      }
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemStack : this.getHeldStacks()) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getStack(int slot) {
      return this.getHeldStacks().get(slot);
   }

   @Override
   public ItemStack removeStack(int slot, int amount) {
      ItemStack itemStack = Inventories.splitStack(this.getHeldStacks(), slot, amount);
      if (!itemStack.isEmpty()) {
         this.markDirty();
      }

      return itemStack;
   }

   @Override
   public ItemStack removeStack(int slot) {
      return Inventories.removeStack(this.getHeldStacks(), slot);
   }

   @Override
   public void setStack(int slot, ItemStack stack) {
      this.getHeldStacks().set(slot, stack);
      stack.capCount(this.getMaxCount(stack));
      this.markDirty();
   }

   @Override
   public boolean canPlayerUse(PlayerEntity player) {
      return Inventory.canPlayerUse(this, player);
   }

   @Override
   public void clear() {
      this.getHeldStacks().clear();
   }

   @Nullable
   @Override
   public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
      return this.checkUnlocked(playerEntity) ? this.createScreenHandler(i, playerInventory) : null;
   }

   protected abstract ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory);

   @Override
   protected void readComponents(BlockEntity.ComponentsAccess components) {
      super.readComponents(components);
      this.customName = components.get(DataComponentTypes.CUSTOM_NAME);
      this.lock = components.getOrDefault(DataComponentTypes.LOCK, ContainerLock.EMPTY);
      components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(this.getHeldStacks());
   }

   @Override
   protected void addComponents(ComponentMap.Builder builder) {
      super.addComponents(builder);
      builder.add(DataComponentTypes.CUSTOM_NAME, this.customName);
      if (!this.lock.equals(ContainerLock.EMPTY)) {
         builder.add(DataComponentTypes.LOCK, this.lock);
      }

      builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(this.getHeldStacks()));
   }

   @Override
   public void removeFromCopiedStackNbt(NbtCompound nbt) {
      nbt.remove("CustomName");
      nbt.remove("lock");
      nbt.remove("Items");
   }
}
