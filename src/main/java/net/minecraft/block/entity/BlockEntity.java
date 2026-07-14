package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.tr7zw.entityculling.Cullable;
import dev.tr7zw.entityculling.CullingState;
import java.util.HashSet;
import java.util.Set;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_comparator_tracking.ComparatorTracking;
import net.caffeinemc.mods.lithium.common.block.entity.SetChangedHandlingBlockEntity;
import net.caffeinemc.mods.lithium.common.world.blockentity.SupportCache;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockEntity implements Cullable, ComparatorTracker, SetChangedHandlingBlockEntity, SupportCache {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BlockEntityType<?> type;
   private final CullingState entityCulling$state = new CullingState();
   @Nullable
   protected World world;
   protected final BlockPos pos;
   protected boolean removed;
   private BlockState cachedState;
   private ComponentMap components = ComponentMap.EMPTY;
   private byte lithium$hasComparators = -1;
   private boolean lithium$supportTestResult;

   public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      this.type = type;
      this.pos = pos.toImmutable();
      this.validateSupports(state);
      this.cachedState = state;
      this.lithium$supportTestResult = type.supports(state);
   }

   private void validateSupports(BlockState state) {
      if (!this.supports(state)) {
         throw new IllegalStateException("Invalid block entity " + this.getNameForReport() + " state at " + this.pos + ", got " + state);
      }
   }

   public boolean supports(BlockState state) {
      return this.type.supports(state);
   }

   public static BlockPos posFromNbt(NbtCompound nbt) {
      return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
   }

   @Nullable
   public World getWorld() {
      return this.world;
   }

   public void setWorld(World world) {
      this.world = world;
   }

   public boolean hasWorld() {
      return this.world != null;
   }

   protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
   }

   public final void read(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      this.readNbt(nbt, registries);
      if (this instanceof InventoryChangeTracker tracker) {
         tracker.lithium$emitStackListReplaced();
      }
      BlockEntity.Components.CODEC
         .parse(registries.getOps(NbtOps.INSTANCE), nbt)
         .resultOrPartial(error -> LOGGER.warn("Failed to load components: {}", error))
         .ifPresent(components -> this.components = components);
   }

   public final void readComponentlessNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      this.readNbt(nbt, registries);
      if (this instanceof InventoryChangeTracker tracker) {
         tracker.lithium$emitStackListReplaced();
      }
   }

   protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
   }

   public final NbtCompound createNbtWithIdentifyingData(RegistryWrapper.WrapperLookup registries) {
      NbtCompound nbtCompound = this.createNbt(registries);
      this.writeIdentifyingData(nbtCompound);
      return nbtCompound;
   }

   public final NbtCompound createNbtWithId(RegistryWrapper.WrapperLookup registries) {
      NbtCompound nbtCompound = this.createNbt(registries);
      this.writeIdToNbt(nbtCompound);
      return nbtCompound;
   }

   public final NbtCompound createNbt(RegistryWrapper.WrapperLookup registries) {
      NbtCompound nbtCompound = new NbtCompound();
      this.writeNbt(nbtCompound, registries);
      BlockEntity.Components.CODEC
         .encodeStart(registries.getOps(NbtOps.INSTANCE), this.components)
         .resultOrPartial(snbt -> LOGGER.warn("Failed to save components: {}", snbt))
         .ifPresent(nbt -> nbtCompound.copyFrom((NbtCompound)nbt));
      return nbtCompound;
   }

   public final NbtCompound createComponentlessNbt(RegistryWrapper.WrapperLookup registries) {
      NbtCompound nbtCompound = new NbtCompound();
      this.writeNbt(nbtCompound, registries);
      return nbtCompound;
   }

   public final NbtCompound createComponentlessNbtWithIdentifyingData(RegistryWrapper.WrapperLookup registries) {
      NbtCompound nbtCompound = this.createComponentlessNbt(registries);
      this.writeIdentifyingData(nbtCompound);
      return nbtCompound;
   }

   private void writeIdToNbt(NbtCompound nbt) {
      Identifier identifier = BlockEntityType.getId(this.getType());
      if (identifier == null) {
         throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
      }

      nbt.putString("id", identifier.toString());
   }

   public static void writeIdToNbt(NbtCompound nbt, BlockEntityType<?> type) {
      nbt.putString("id", BlockEntityType.getId(type).toString());
   }

   private void writeIdentifyingData(NbtCompound nbt) {
      this.writeIdToNbt(nbt);
      nbt.putInt("x", this.pos.getX());
      nbt.putInt("y", this.pos.getY());
      nbt.putInt("z", this.pos.getZ());
   }

   @Nullable
   public static BlockEntity createFromNbt(BlockPos pos, BlockState state, NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      String string = nbt.getString("id");
      Identifier identifier = Identifier.tryParse(string);
      if (identifier == null) {
         LOGGER.error("Block entity has invalid type: {}", string);
         return null;
      } else {
         return Registries.BLOCK_ENTITY_TYPE.getOptionalValue(identifier).map(type -> {
            try {
               return type.instantiate(pos, state);
            } catch (Throwable throwable) {
               LOGGER.error("Failed to create block entity {}", string, throwable);
               return null;
            }
         }).map(blockEntity -> {
            try {
               blockEntity.read(nbt, registries);
               return (BlockEntity)blockEntity;
            } catch (Throwable throwable) {
               LOGGER.error("Failed to load data for block entity {}", string, throwable);
               return null;
            }
         }).orElseGet(() -> {
            LOGGER.warn("Skipping BlockEntity with id {}", string);
            return null;
         });
      }
   }

   public void markDirty() {
      if (this.world != null) {
         markDirty(this.world, this.pos, this.cachedState);
      }
      this.lithium$handleSetChanged();
   }

   protected static void markDirty(World world, BlockPos pos, BlockState state) {
      world.markDirty(pos);
      if (!state.isAir()) {
         world.updateComparators(pos, state.getBlock());
      }
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getCachedState() {
      return this.cachedState;
   }

   @Override
   public boolean lithium$isSupported() {
      return this.lithium$supportTestResult;
   }

   @Nullable
   public Packet<ClientPlayPacketListener> toUpdatePacket() {
      return null;
   }

   public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
      return new NbtCompound();
   }

   public boolean isRemoved() {
      return this.removed;
   }

   public void markRemoved() {
      this.removed = true;
      this.lithium$hasComparators = -1;
      if (this.world != null && !this.world.isClient() && this instanceof InventoryChangeTracker tracker) {
         tracker.lithium$emitRemoved();
      }
   }


   @Override
   public void lithium$onComparatorAdded(Direction direction, int offset) {
      if (direction.getAxis() != Direction.Axis.Y && this.lithium$hasComparators != 1 && offset >= 1 && offset <= 2) {
         this.lithium$hasComparators = 1;
         if (this instanceof InventoryChangeTracker tracker) {
            tracker.lithium$emitFirstComparatorAdded();
         }
      }
   }

   @Override
   public boolean lithium$hasAnyComparatorNearby() {
      if (this.lithium$hasComparators == -1) {
         this.lithium$hasComparators = (byte)(ComparatorTracking.findNearbyComparators(this.world, this.pos) ? 1 : 0);
      }
      return this.lithium$hasComparators == 1;
   }

   public void cancelRemoval() {
      this.removed = false;
   }

   public boolean onSyncedBlockEvent(int type, int data) {
      return false;
   }

   public void populateCrashReport(CrashReportSection crashReportSection) {
      crashReportSection.add("Name", this::getNameForReport);
      if (this.world != null) {
         CrashReportSection.addBlockInfo(crashReportSection, this.world, this.pos, this.getCachedState());
         CrashReportSection.addBlockInfo(crashReportSection, this.world, this.pos, this.world.getBlockState(this.pos));
      }
   }

   private String getNameForReport() {
      return Registries.BLOCK_ENTITY_TYPE.getId(this.getType()) + " // " + this.getClass().getCanonicalName();
   }

   public BlockEntityType<?> getType() {
      return this.type;
   }

   @Override
   public CullingState entityCulling$getState() {
      return this.entityCulling$state;
   }

   @Deprecated
   public void setCachedState(BlockState state) {
      this.validateSupports(state);
      this.cachedState = state;
      this.lithium$supportTestResult = this.type.supports(state);
   }

   protected void readComponents(BlockEntity.ComponentsAccess components) {
   }

   public final void readComponents(ItemStack stack) {
      this.readComponents(stack.getDefaultComponents(), stack.getComponentChanges());
   }

   public final void readComponents(ComponentMap defaultComponents, ComponentChanges components) {
      final Set<ComponentType<?>> set = new HashSet<>();
      set.add(DataComponentTypes.BLOCK_ENTITY_DATA);
      set.add(DataComponentTypes.BLOCK_STATE);
      final ComponentMap componentMap = MergedComponentMap.create(defaultComponents, components);
      this.readComponents(new BlockEntity.ComponentsAccess() {
         @Nullable
         @Override
         public <T> T get(ComponentType<T> type) {
            set.add(type);
            return componentMap.get(type);
         }

         @Override
         public <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
            set.add(type);
            return componentMap.getOrDefault(type, fallback);
         }
      });
      ComponentChanges componentChanges = components.withRemovedIf(set::contains);
      this.components = componentChanges.toAddedRemovedPair().added();
   }

   protected void addComponents(ComponentMap.Builder builder) {
   }

   @Deprecated
   public void removeFromCopiedStackNbt(NbtCompound nbt) {
   }

   public final ComponentMap createComponentMap() {
      ComponentMap.Builder builder = ComponentMap.builder();
      builder.addAll(this.components);
      this.addComponents(builder);
      return builder.build();
   }

   public ComponentMap getComponents() {
      return this.components;
   }

   public void setComponents(ComponentMap components) {
      this.components = components;
   }

   @Nullable
   public static Text tryParseCustomName(String json, RegistryWrapper.WrapperLookup registries) {
      try {
         return Text.Serialization.fromJson(json, registries);
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse custom name from string '{}', discarding", json, exception);
         return null;
      }
   }

   static class Components {
      public static final Codec<ComponentMap> CODEC = ComponentMap.CODEC.optionalFieldOf("components", ComponentMap.EMPTY).codec();

      private Components() {
      }
   }

   protected interface ComponentsAccess {
      @Nullable
      <T> T get(ComponentType<T> type);

      <T> T getOrDefault(ComponentType<? extends T> type, T fallback);
   }
}
