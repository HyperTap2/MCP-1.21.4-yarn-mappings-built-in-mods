package net.minecraft.block.entity;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.caffeinemc.mods.lithium.api.inventory.LithiumInventory;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.caffeinemc.mods.lithium.common.hopper.HopperHelper;
import net.caffeinemc.mods.lithium.common.hopper.HopperCachingState;
import net.caffeinemc.mods.lithium.common.hopper.BlockStateOnlyInventory;
import net.caffeinemc.mods.lithium.common.hopper.InventoryHelper;
import net.caffeinemc.mods.lithium.common.hopper.LithiumStackList;
import net.caffeinemc.mods.lithium.common.hopper.UpdateReceiver;
import net.caffeinemc.mods.lithium.common.tracking.entity.SectionedEntityMovementListener;
import net.caffeinemc.mods.lithium.common.tracking.entity.SectionedInventoryEntityMovementTracker;
import net.caffeinemc.mods.lithium.common.tracking.entity.SectionedItemEntityMovementTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class HopperBlockEntity extends LootableContainerBlockEntity
   implements Hopper, UpdateReceiver, InventoryChangeListener, SectionedEntityMovementListener, SleepingBlockEntity {
   public static final int TRANSFER_COOLDOWN = 8;
   public static final int INVENTORY_SIZE = 5;
   private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];
   private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
   private int transferCooldown = -1;
   private long lastTickTime;
   private Direction facing;
   @Nullable
   private Inventory lithium$failedInsertInventory;
   private long lithium$hopperModCountAtFailedInsert = Long.MIN_VALUE;
   private long lithium$targetModCountAtFailedInsert = Long.MIN_VALUE;
   @Nullable
   private Inventory lithium$failedExtractInventory;
   private long lithium$hopperModCountAtFailedExtract = Long.MIN_VALUE;
   private long lithium$sourceModCountAtFailedExtract = Long.MIN_VALUE;
   @Nullable
   private InventoryChangeTracker lithium$insertFailureTracker;
   @Nullable
   private InventoryChangeTracker lithium$extractFailureTracker;
   private HopperCachingState.BlockInventory lithium$insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
   private HopperCachingState.BlockInventory lithium$extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
   @Nullable
   private Inventory lithium$insertBlockInventory;
   @Nullable
   private Inventory lithium$extractBlockInventory;
   @Nullable
   private SectionedInventoryEntityMovementTracker<Inventory> lithium$insertInventoryEntityTracker;
   @Nullable
   private SectionedInventoryEntityMovementTracker<Inventory> lithium$extractInventoryEntityTracker;
   @Nullable
   private SectionedItemEntityMovementTracker<ItemEntity> lithium$collectItemEntityTracker;
   @Nullable
   private Box lithium$insertInventoryEntityBox;
   @Nullable
   private Box lithium$extractInventoryEntityBox;
   @Nullable
   private Box lithium$collectItemEntityBox;
   private long lithium$insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
   private long lithium$extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
   private long lithium$collectItemEntityAttemptTime = Long.MIN_VALUE;
   private long lithium$myModCountAtLastItemCollect = Long.MIN_VALUE;
   private boolean lithium$collectItemEntityTrackerWasEmpty;
   private WorldChunk.WrappedBlockEntityTickInvoker lithium$tickWrapper;
   private BlockEntityTickInvoker lithium$sleepingTicker;

   public HopperBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.HOPPER, pos, state);
      this.facing = state.get(HopperBlock.FACING);
   }

   @Override
   protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      super.readNbt(nbt, registries);
      this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
      if (!this.readLootTable(nbt)) {
         Inventories.readNbt(nbt, this.inventory, registries);
      }

      this.transferCooldown = nbt.getInt("TransferCooldown");
   }

   @Override
   protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      super.writeNbt(nbt, registries);
      if (!this.writeLootTable(nbt)) {
         Inventories.writeNbt(nbt, this.inventory, registries);
      }

      nbt.putInt("TransferCooldown", this.transferCooldown);
   }

   @Override
   public int size() {
      return this.inventory.size();
   }

   @Override
   public ItemStack removeStack(int slot, int amount) {
      this.generateLoot(null);
      return Inventories.splitStack(this.getHeldStacks(), slot, amount);
   }

   @Override
   public void setStack(int slot, ItemStack stack) {
      this.generateLoot(null);
      this.getHeldStacks().set(slot, stack);
      stack.capCount(this.getMaxCount(stack));
   }

   @Override
   public void setCachedState(BlockState state) {
      if (this.world != null && !this.world.isClient() && state.get(HopperBlock.FACING) != this.facing) {
         this.lithium$invalidateTrackedData();
      }
      super.setCachedState(state);
      this.facing = state.get(HopperBlock.FACING);
   }

   @Override
   protected Text getContainerName() {
      return Text.translatable("container.hopper");
   }

   public static void serverTick(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity) {
      blockEntity.transferCooldown--;
      blockEntity.lastTickTime = world.getTime();
      if (!blockEntity.needsCooldown()) {
         blockEntity.setTransferCooldown(0);
         insertAndExtract(world, pos, state, blockEntity, () -> extract(world, blockEntity));
      }
   }

   private static boolean insertAndExtract(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
      if (world.isClient) {
         return false;
      }

      if (!blockEntity.needsCooldown() && state.get(HopperBlock.ENABLED)) {
         boolean bl = false;
         if (!blockEntity.isEmpty()) {
            bl = insert(world, pos, blockEntity);
         }

         if (!blockEntity.isFull()) {
            bl |= booleanSupplier.getAsBoolean();
         }

         if (bl) {
            blockEntity.setTransferCooldown(8);
            markDirty(world, pos, state);
            return true;
         }
      }

      if (!blockEntity.needsCooldown() && !blockEntity.lithium$isSleeping() && !state.get(HopperBlock.ENABLED)) {
         blockEntity.lithium$startSleeping();
      }

      return false;
   }

   private boolean isFull() {
      LithiumStackList stacks = InventoryHelper.getLithiumStackList(this);
      return stacks.getFullSlots() == stacks.size();
   }

   private static boolean insert(World world, BlockPos pos, HopperBlockEntity blockEntity) {
      Inventory inventory = getOutputInventory(world, pos, blockEntity);
      if (inventory == null) {
         return false;
      }

      LithiumStackList hopperStacks = InventoryHelper.getLithiumStackList(blockEntity);
      LithiumStackList targetStacks = inventory instanceof LithiumInventory lithiumInventory
         ? InventoryHelper.getLithiumStackList(lithiumInventory)
         : null;
      if (inventory == blockEntity.lithium$failedInsertInventory
         && hopperStacks.getModCount() == blockEntity.lithium$hopperModCountAtFailedInsert
         && targetStacks != null
         && targetStacks.getModCount() == blockEntity.lithium$targetModCountAtFailedInsert) {
         return false;
      }

      Direction direction = blockEntity.facing.getOpposite();
      if (targetStacks != null && targetStacks.getFullSlots() == targetStacks.size() || isInventoryFull(inventory, direction)) {
         return false;
      }

      for (int i = 0; i < hopperStacks.size(); i++) {
         ItemStack itemStack = hopperStacks.get(i);
         if (!itemStack.isEmpty()) {
            if (HopperHelper.tryMoveSingleItem(inventory, itemStack, direction)) {
               inventory.markDirty();
               blockEntity.lithium$invalidateInsertCache();
               return true;
            }
         }
      }

      blockEntity.lithium$failedInsertInventory = inventory;
      blockEntity.lithium$hopperModCountAtFailedInsert = hopperStacks.getModCount();
      blockEntity.lithium$targetModCountAtFailedInsert = targetStacks == null ? Long.MIN_VALUE : targetStacks.getModCount();
      if (targetStacks != null && inventory instanceof InventoryChangeTracker tracker) {
         blockEntity.lithium$listenForInsertChanges(tracker, targetStacks);
      }
      return false;
   }

   private static int[] getAvailableSlots(Inventory inventory, Direction side) {
      if (inventory instanceof SidedInventory sidedInventory) {
         return sidedInventory.getAvailableSlots(side);
      } else {
         int i = inventory.size();
         if (i < AVAILABLE_SLOTS_CACHE.length) {
            int[] is = AVAILABLE_SLOTS_CACHE[i];
            if (is != null) {
               return is;
            }

            int[] js = indexArray(i);
            AVAILABLE_SLOTS_CACHE[i] = js;
            return js;
         } else {
            return indexArray(i);
         }
      }
   }

   private static int[] indexArray(int size) {
      int[] is = new int[size];
      int i = 0;

      while (i < is.length) {
         is[i] = i++;
      }

      return is;
   }

   private static boolean isInventoryFull(Inventory inventory, Direction direction) {
      int[] is = getAvailableSlots(inventory, direction);

      for (int i : is) {
         ItemStack itemStack = inventory.getStack(i);
         if (itemStack.getCount() < itemStack.getMaxCount()) {
            return false;
         }
      }

      return true;
   }

   public static boolean extract(World world, Hopper hopper) {
      BlockPos blockPos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
      BlockState blockState = world.getBlockState(blockPos);
      Inventory inventory = getInputInventory(world, hopper, blockPos, blockState);
      if (inventory != null) {
         Direction direction = Direction.DOWN;

         if (hopper instanceof HopperBlockEntity blockEntity && inventory instanceof LithiumInventory lithiumInventory) {
            LithiumStackList hopperStacks = InventoryHelper.getLithiumStackList(blockEntity);
            LithiumStackList sourceStacks = InventoryHelper.getLithiumStackList(lithiumInventory);
            if (inventory == blockEntity.lithium$failedExtractInventory
               && hopperStacks.getModCount() == blockEntity.lithium$hopperModCountAtFailedExtract
               && sourceStacks.getModCount() == blockEntity.lithium$sourceModCountAtFailedExtract) {
               if (!(inventory instanceof ComparatorTracker tracker) || tracker.lithium$hasAnyComparatorNearby()) {
                  sourceStacks.runComparatorUpdatePatternOnFailedExtract(sourceStacks, inventory);
               }
               return false;
            }

            int[] available = getAvailableSlots(inventory, direction);
            for (int slot : available) {
               ItemStack source = sourceStacks.get(slot);
               if (!source.isEmpty() && canExtract(hopper, inventory, source, slot, direction)
                  && HopperHelper.tryMoveSingleItem(hopper, source, null)) {
                  hopper.markDirty();
                  inventory.markDirty();
                  blockEntity.lithium$invalidateExtractCache();
                  return true;
               }
            }
            blockEntity.lithium$failedExtractInventory = inventory;
            blockEntity.lithium$hopperModCountAtFailedExtract = hopperStacks.getModCount();
            blockEntity.lithium$sourceModCountAtFailedExtract = sourceStacks.getModCount();
            if (inventory instanceof InventoryChangeTracker tracker) {
               blockEntity.lithium$listenForExtractChanges(tracker, sourceStacks);
            }
            return false;
         }

         for (int i : getAvailableSlots(inventory, direction)) {
            if (extract(hopper, inventory, i, direction)) {
               return true;
            }
         }

         return false;
      } else {
         boolean bl = hopper.canBlockFromAbove() && blockState.isFullCube(world, blockPos) && !blockState.isIn(BlockTags.DOES_NOT_BLOCK_HOPPERS);
         if (!bl) {
            for (ItemEntity itemEntity : getInputItemEntities(world, hopper)) {
               if (extract(hopper, itemEntity)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side) {
      ItemStack itemStack = inventory.getStack(slot);
      if (!itemStack.isEmpty() && canExtract(hopper, inventory, itemStack, slot, side)) {
         int i = itemStack.getCount();
         ItemStack itemStack2 = transfer(inventory, hopper, inventory.removeStack(slot, 1), null);
         if (itemStack2.isEmpty()) {
            inventory.markDirty();
            return true;
         }

         itemStack.setCount(i);
         if (i == 1) {
            inventory.setStack(slot, itemStack);
         }
      }

      return false;
   }

   public static boolean extract(Inventory inventory, ItemEntity itemEntity) {
      boolean bl = false;
      ItemStack itemStack = itemEntity.getStack().copy();
      ItemStack itemStack2 = transfer(null, inventory, itemStack, null);
      if (itemStack2.isEmpty()) {
         bl = true;
         itemEntity.setStack(ItemStack.EMPTY);
         itemEntity.discard();
      } else {
         itemEntity.setStack(itemStack2);
      }

      return bl;
   }

   public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
      if (to instanceof SidedInventory sidedInventory && side != null) {
         int[] is = sidedInventory.getAvailableSlots(side);

         for (int i = 0; i < is.length && !stack.isEmpty(); i++) {
            stack = transfer(from, to, stack, is[i], side);
         }
      } else {
         int j = to.size();

         for (int i = 0; i < j && !stack.isEmpty(); i++) {
            stack = transfer(from, to, stack, i, side);
         }
      }

      return stack;
   }

   private static boolean canInsert(Inventory inventory, ItemStack stack, int slot, @Nullable Direction side) {
      return !inventory.isValid(slot, stack) ? false : !(inventory instanceof SidedInventory sidedInventory && !sidedInventory.canInsert(slot, stack, side));
   }

   private static boolean canExtract(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing) {
      return !fromInventory.canTransferTo(hopperInventory, slot, stack)
         ? false
         : !(fromInventory instanceof SidedInventory sidedInventory && !sidedInventory.canExtract(slot, stack, facing));
   }

   private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction side) {
      ItemStack itemStack = to.getStack(slot);
      if (canInsert(to, stack, slot, side)) {
         boolean bl = false;
         boolean bl2 = to.isEmpty();
         if (itemStack.isEmpty()) {
            to.setStack(slot, stack);
            stack = ItemStack.EMPTY;
            bl = true;
         } else if (canMergeItems(itemStack, stack)) {
            int i = stack.getMaxCount() - itemStack.getCount();
            int j = Math.min(stack.getCount(), i);
            stack.decrement(j);
            itemStack.increment(j);
            bl = j > 0;
         }

         if (bl) {
            if (bl2 && to instanceof HopperBlockEntity hopperBlockEntity && !hopperBlockEntity.isDisabled()) {
               int j = 0;
               if (from instanceof HopperBlockEntity hopperBlockEntity2 && hopperBlockEntity.lastTickTime >= hopperBlockEntity2.lastTickTime) {
                  j = 1;
               }

               hopperBlockEntity.setTransferCooldown(8 - j);
            }

            to.markDirty();
         }
      }

      return stack;
   }

   @Nullable
   private static Inventory getOutputInventory(World world, BlockPos pos, HopperBlockEntity blockEntity) {
      BlockPos outputPos = pos.offset(blockEntity.facing);
      Inventory blockInventory = blockEntity.lithium$getCachedBlockInventory(world, outputPos, false);
      return blockInventory != null ? blockInventory : blockEntity.lithium$getEntityInventory(world, outputPos, false);
   }

   @Nullable
   private static Inventory getInputInventory(World world, Hopper hopper, BlockPos pos, BlockState state) {
      if (hopper instanceof HopperBlockEntity blockEntity) {
         Inventory blockInventory = blockEntity.lithium$getCachedBlockInventory(world, pos, true);
         return blockInventory != null ? blockInventory : blockEntity.lithium$getEntityInventory(world, pos, true);
      }
      return getInventoryAt(world, pos, state, hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
   }

   public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
      Box box = hopper.getInputAreaShape().offset(hopper.getHopperX() - 0.5, hopper.getHopperY() - 0.5, hopper.getHopperZ() - 0.5);
      if (hopper instanceof HopperBlockEntity blockEntity && world instanceof ServerWorld serverWorld) {
         if (blockEntity.lithium$collectItemEntityTracker == null) {
            blockEntity.lithium$collectItemEntityBox = box;
            blockEntity.lithium$collectItemEntityTracker = SectionedItemEntityMovementTracker.registerAt(serverWorld, box, ItemEntity.class);
         }
         long modCount = InventoryHelper.getLithiumStackList(blockEntity).getModCount();
         if ((blockEntity.lithium$collectItemEntityTrackerWasEmpty || blockEntity.lithium$myModCountAtLastItemCollect == modCount)
            && blockEntity.lithium$collectItemEntityTracker.isUnchangedSince(blockEntity.lithium$collectItemEntityAttemptTime)) {
            blockEntity.lithium$collectItemEntityAttemptTime = world.getTime();
            return Collections.emptyList();
         }
         blockEntity.lithium$myModCountAtLastItemCollect = modCount;
         List<ItemEntity> entities = blockEntity.lithium$collectItemEntityTracker.getEntities(box);
         blockEntity.lithium$collectItemEntityAttemptTime = world.getTime();
         blockEntity.lithium$collectItemEntityTrackerWasEmpty = entities.isEmpty();
         return entities;
      }
      return world.getEntitiesByClass(ItemEntity.class, box, EntityPredicates.VALID_ENTITY);
   }

   @Nullable
   private Inventory lithium$getCachedBlockInventory(World world, BlockPos targetPos, boolean extracting) {
      HopperCachingState.BlockInventory mode = extracting ? this.lithium$extractionMode : this.lithium$insertionMode;
      Inventory cached = extracting ? this.lithium$extractBlockInventory : this.lithium$insertBlockInventory;
      if (mode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY
         || mode == HopperCachingState.BlockInventory.BLOCK_STATE
         || mode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY) {
         return cached;
      }
      if (mode == HopperCachingState.BlockInventory.BLOCK_ENTITY
         && cached instanceof BlockEntity blockEntity
         && !blockEntity.isRemoved()
         && blockEntity.getPos().equals(targetPos)) {
         return cached;
      }

      Inventory inventory = HopperHelper.replaceDoubleInventory(getBlockInventoryAt(world, targetPos, world.getBlockState(targetPos)));
      this.lithium$cacheBlockInventory(inventory, extracting);
      return inventory;
   }

   private void lithium$cacheBlockInventory(@Nullable Inventory inventory, boolean extracting) {
      Inventory oldInventory = extracting ? this.lithium$extractBlockInventory : this.lithium$insertBlockInventory;
      HopperCachingState.BlockInventory oldMode = extracting ? this.lithium$extractionMode : this.lithium$insertionMode;
      if (oldMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY
         && oldInventory instanceof InventoryChangeTracker oldTracker
         && oldInventory != inventory) {
         oldTracker.stopListenForMajorInventoryChanges(this);
      }
      HopperCachingState.BlockInventory mode;
      if (inventory == null) {
         mode = HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY;
      } else if (inventory instanceof InventoryChangeTracker tracker) {
         mode = HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY;
         tracker.listenForMajorInventoryChanges(this);
      } else if (inventory instanceof BlockEntity) {
         mode = HopperCachingState.BlockInventory.BLOCK_ENTITY;
      } else if (inventory instanceof BlockStateOnlyInventory) {
         mode = HopperCachingState.BlockInventory.BLOCK_STATE;
      } else {
         mode = HopperCachingState.BlockInventory.UNKNOWN;
      }
      if (extracting) {
         this.lithium$extractBlockInventory = inventory;
         this.lithium$extractionMode = mode;
      } else {
         this.lithium$insertBlockInventory = inventory;
         this.lithium$insertionMode = mode;
      }
   }

   @Nullable
   private Inventory lithium$getEntityInventory(World world, BlockPos targetPos, boolean extracting) {
      if (!(world instanceof ServerWorld serverWorld)) {
         return getEntityInventoryAt(world, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
      }
      SectionedInventoryEntityMovementTracker<Inventory> tracker = extracting
         ? this.lithium$extractInventoryEntityTracker
         : this.lithium$insertInventoryEntityTracker;
      Box box = extracting ? this.lithium$extractInventoryEntityBox : this.lithium$insertInventoryEntityBox;
      if (tracker == null) {
         box = new Box(targetPos);
         tracker = SectionedInventoryEntityMovementTracker.registerAt(serverWorld, box, Inventory.class);
         if (extracting) {
            this.lithium$extractInventoryEntityTracker = tracker;
            this.lithium$extractInventoryEntityBox = box;
         } else {
            this.lithium$insertInventoryEntityTracker = tracker;
            this.lithium$insertInventoryEntityBox = box;
         }
      }

      long failedAt = extracting ? this.lithium$extractInventoryEntityFailedSearchTime : this.lithium$insertInventoryEntityFailedSearchTime;
      if (tracker.isUnchangedSince(failedAt)) {
         if (extracting) {
            this.lithium$extractInventoryEntityFailedSearchTime = world.getTime();
         } else {
            this.lithium$insertInventoryEntityFailedSearchTime = world.getTime();
         }
         return null;
      }
      List<Inventory> inventories = tracker.getEntities(box);
      long nextFailedAt = inventories.isEmpty() ? world.getTime() : Long.MIN_VALUE;
      if (extracting) {
         this.lithium$extractInventoryEntityFailedSearchTime = nextFailedAt;
      } else {
         this.lithium$insertInventoryEntityFailedSearchTime = nextFailedAt;
      }
      return inventories.isEmpty() ? null : inventories.get(world.random.nextInt(inventories.size()));
   }

   @Nullable
   public static Inventory getInventoryAt(World world, BlockPos pos) {
      return getInventoryAt(world, pos, world.getBlockState(pos), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   @Nullable
   private static Inventory getInventoryAt(World world, BlockPos pos, BlockState state, double x, double y, double z) {
      Inventory inventory = getBlockInventoryAt(world, pos, state);
      if (inventory == null) {
         inventory = getEntityInventoryAt(world, x, y, z);
      }

      return inventory;
   }

   @Nullable
   private static Inventory getBlockInventoryAt(World world, BlockPos pos, BlockState state) {
      Block block = state.getBlock();
      if (block instanceof InventoryProvider) {
         return ((InventoryProvider)block).getInventory(state, world, pos);
      }

      if (state.hasBlockEntity() && world.getBlockEntity(pos) instanceof Inventory inventory) {
         if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
            inventory = ChestBlock.getInventory((ChestBlock)block, state, world, pos, true);
         }

         return HopperHelper.replaceDoubleInventory(inventory);
      } else {
         return null;
      }
   }

   @Nullable
   private static Inventory getEntityInventoryAt(World world, double x, double y, double z) {
      List<Entity> list = world.getOtherEntities(
         (Entity)null, new Box(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntityPredicates.VALID_INVENTORIES
      );
      return !list.isEmpty() ? (Inventory)list.get(world.random.nextInt(list.size())) : null;
   }

   private static boolean canMergeItems(ItemStack first, ItemStack second) {
      return first.getCount() <= first.getMaxCount() && ItemStack.areItemsAndComponentsEqual(first, second);
   }

   @Override
   public double getHopperX() {
      return this.pos.getX() + 0.5;
   }

   @Override
   public double getHopperY() {
      return this.pos.getY() + 0.5;
   }

   @Override
   public double getHopperZ() {
      return this.pos.getZ() + 0.5;
   }

   @Override
   public boolean canBlockFromAbove() {
      return true;
   }

   private void setTransferCooldown(int transferCooldown) {
      if (transferCooldown == 7) {
         if (this.lastTickTime == Long.MAX_VALUE) {
            this.lithium$sleepOnlyCurrentTick();
         } else {
            this.lithium$wakeUpNow();
         }
      } else if (transferCooldown > 0 && this.lithium$sleepingTicker != null) {
         this.lithium$wakeUpNow();
      }
      this.transferCooldown = transferCooldown;
   }

   @Override
   public boolean lithium$startSleeping() {
      if (SleepingBlockEntity.super.lithium$startSleeping()) {
         this.lastTickTime = Long.MAX_VALUE;
         return true;
      }
      return false;
   }

   @Override public WorldChunk.WrappedBlockEntityTickInvoker lithium$getTickWrapper() { return this.lithium$tickWrapper; }
   @Override public void lithium$setTickWrapper(WorldChunk.WrappedBlockEntityTickInvoker wrapper) { this.lithium$tickWrapper = wrapper; this.lithium$setSleepingTicker(null); }
   @Override public BlockEntityTickInvoker lithium$getSleepingTicker() { return this.lithium$sleepingTicker; }
   @Override public void lithium$setSleepingTicker(BlockEntityTickInvoker ticker) { this.lithium$sleepingTicker = ticker; }

   private boolean needsCooldown() {
      return this.transferCooldown > 0;
   }

   private boolean isDisabled() {
      return this.transferCooldown > 8;
   }

   private void lithium$invalidateInsertCache() {
      if (this.lithium$insertFailureTracker != null) {
         this.lithium$insertFailureTracker.stopListenForMajorInventoryChanges(this);
         this.lithium$insertFailureTracker = null;
      }
      this.lithium$failedInsertInventory = null;
      this.lithium$hopperModCountAtFailedInsert = Long.MIN_VALUE;
      this.lithium$targetModCountAtFailedInsert = Long.MIN_VALUE;
   }

   private void lithium$invalidateBlockInsertionData() {
      if (this.lithium$insertionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY
         && this.lithium$insertBlockInventory instanceof InventoryChangeTracker tracker) {
         tracker.stopListenForMajorInventoryChanges(this);
      }
      this.lithium$insertionMode = HopperCachingState.BlockInventory.UNKNOWN;
      this.lithium$insertBlockInventory = null;
      this.lithium$invalidateInsertCache();
   }

   private void lithium$invalidateExtractCache() {
      if (this.lithium$extractFailureTracker != null) {
         this.lithium$extractFailureTracker.stopListenForMajorInventoryChanges(this);
         this.lithium$extractFailureTracker = null;
      }
      this.lithium$failedExtractInventory = null;
      this.lithium$hopperModCountAtFailedExtract = Long.MIN_VALUE;
      this.lithium$sourceModCountAtFailedExtract = Long.MIN_VALUE;
   }

   private void lithium$invalidateBlockExtractionData() {
      if (this.lithium$extractionMode == HopperCachingState.BlockInventory.REMOVAL_TRACKING_BLOCK_ENTITY
         && this.lithium$extractBlockInventory instanceof InventoryChangeTracker tracker) {
         tracker.stopListenForMajorInventoryChanges(this);
      }
      this.lithium$extractionMode = HopperCachingState.BlockInventory.UNKNOWN;
      this.lithium$extractBlockInventory = null;
      this.lithium$invalidateExtractCache();
   }

   private void lithium$invalidateTrackedData() {
      if (this.world instanceof ServerWorld serverWorld) {
         if (this.lithium$insertInventoryEntityTracker != null) {
            this.lithium$insertInventoryEntityTracker.unRegister(serverWorld);
         }
         if (this.lithium$extractInventoryEntityTracker != null) {
            this.lithium$extractInventoryEntityTracker.unRegister(serverWorld);
         }
         if (this.lithium$collectItemEntityTracker != null) {
            this.lithium$collectItemEntityTracker.unRegister(serverWorld);
         }
      }
      this.lithium$insertInventoryEntityTracker = null;
      this.lithium$extractInventoryEntityTracker = null;
      this.lithium$collectItemEntityTracker = null;
      this.lithium$insertInventoryEntityBox = null;
      this.lithium$extractInventoryEntityBox = null;
      this.lithium$collectItemEntityBox = null;
      this.lithium$insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
      this.lithium$extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
      this.lithium$collectItemEntityAttemptTime = Long.MIN_VALUE;
      this.lithium$collectItemEntityTrackerWasEmpty = false;
      this.lithium$invalidateBlockInsertionData();
      this.lithium$invalidateBlockExtractionData();
   }

   @Override
   public void markRemoved() {
      this.lithium$invalidateTrackedData();
      super.markRemoved();
   }

   private void lithium$listenForInsertChanges(InventoryChangeTracker tracker, LithiumStackList stacks) {
      if (this.lithium$insertFailureTracker != tracker) {
         if (this.lithium$insertFailureTracker != null) {
            this.lithium$insertFailureTracker.stopListenForMajorInventoryChanges(this);
         }
         this.lithium$insertFailureTracker = tracker;
         tracker.listenForMajorInventoryChanges(this);
      }
      tracker.listenForContentChangesOnce(stacks, this);
   }

   private void lithium$listenForExtractChanges(InventoryChangeTracker tracker, LithiumStackList stacks) {
      if (this.lithium$extractFailureTracker != tracker) {
         if (this.lithium$extractFailureTracker != null) {
            this.lithium$extractFailureTracker.stopListenForMajorInventoryChanges(this);
         }
         this.lithium$extractFailureTracker = tracker;
         tracker.listenForMajorInventoryChanges(this);
      }
      tracker.listenForContentChangesOnce(stacks, this);
   }

   @Override
   public void lithium$handleInventoryContentModified(Inventory inventory) {
      if (inventory == this.lithium$failedInsertInventory) {
         this.lithium$invalidateInsertCache();
      }
      if (inventory == this.lithium$failedExtractInventory) {
         this.lithium$invalidateExtractCache();
      }
   }

   @Override
   public void lithium$handleInventoryRemoved(Inventory inventory) {
      this.lithium$handleInventoryContentModified(inventory);
      if (inventory == this.lithium$insertBlockInventory) {
         this.lithium$invalidateBlockInsertionData();
      }
      if (inventory == this.lithium$extractBlockInventory) {
         this.lithium$invalidateBlockExtractionData();
      }
      if (inventory == this) {
         this.lithium$invalidateTrackedData();
      }
   }

   @Override
   public boolean lithium$handleComparatorAdded(Inventory inventory) {
      if (inventory == this.lithium$failedExtractInventory) {
         this.lithium$invalidateExtractCache();
         return true;
      }
      return false;
   }

   @Override
   public void lithium$invalidateCacheOnNeighborUpdate(boolean fromAbove) {
      if (fromAbove) {
         if (this.lithium$extractionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY
            || this.lithium$extractionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            this.lithium$invalidateBlockExtractionData();
         }
      } else {
         if (this.lithium$insertionMode == HopperCachingState.BlockInventory.NO_BLOCK_INVENTORY
            || this.lithium$insertionMode == HopperCachingState.BlockInventory.BLOCK_STATE) {
            this.lithium$invalidateBlockInsertionData();
         }
      }
   }

   @Override
   public void lithium$invalidateCacheOnUndirectedNeighborUpdate() {
      this.lithium$invalidateCacheOnNeighborUpdate(true);
      this.lithium$invalidateCacheOnNeighborUpdate(false);
   }

   @Override
   public void lithium$invalidateCacheOnNeighborUpdate(Direction fromDirection) {
      if (fromDirection == Direction.UP || this.facing == fromDirection) {
         this.lithium$invalidateCacheOnNeighborUpdate(fromDirection == Direction.UP);
      }
   }

   @Override
   public void lithium$handleEntityMovement(Object category) {
      this.lithium$insertInventoryEntityFailedSearchTime = Long.MIN_VALUE;
      this.lithium$extractInventoryEntityFailedSearchTime = Long.MIN_VALUE;
      this.lithium$collectItemEntityAttemptTime = Long.MIN_VALUE;
      this.lithium$collectItemEntityTrackerWasEmpty = false;
   }


   @Override
   protected DefaultedList<ItemStack> getHeldStacks() {
      return this.inventory;
   }

   @Override
   protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
      this.inventory = inventory;
   }

   public static void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity, HopperBlockEntity blockEntity) {
      if (entity instanceof ItemEntity itemEntity
         && !itemEntity.getStack().isEmpty()
         && entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(blockEntity.getInputAreaShape())) {
         insertAndExtract(world, pos, state, blockEntity, () -> extract(blockEntity, itemEntity));
      }
   }

   @Override
   protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
      return new HopperScreenHandler(syncId, playerInventory, this);
   }
}
