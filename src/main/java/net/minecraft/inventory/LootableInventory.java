package net.minecraft.inventory;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface LootableInventory extends Inventory {
   String LOOT_TABLE_KEY = "LootTable";
   String LOOT_TABLE_SEED_KEY = "LootTableSeed";

   @Nullable
   RegistryKey<LootTable> getLootTable();

   void setLootTable(@Nullable RegistryKey<LootTable> lootTable);

   default void setLootTable(RegistryKey<LootTable> lootTableId, long lootTableSeed) {
      this.setLootTable(lootTableId);
      this.setLootTableSeed(lootTableSeed);
   }

   long getLootTableSeed();

   void setLootTableSeed(long lootTableSeed);

   BlockPos getPos();

   @Nullable
   World getWorld();

   static void setLootTable(BlockView world, Random random, BlockPos pos, RegistryKey<LootTable> lootTableId) {
      if (world.getBlockEntity(pos) instanceof LootableInventory lootableInventory) {
         lootableInventory.setLootTable(lootTableId, random.nextLong());
      }
   }

   default boolean readLootTable(NbtCompound nbt) {
      if (nbt.contains("LootTable", 8)) {
         this.setLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(nbt.getString("LootTable"))));
         if (nbt.contains("LootTableSeed", 4)) {
            this.setLootTableSeed(nbt.getLong("LootTableSeed"));
         } else {
            this.setLootTableSeed(0L);
         }

         return true;
      } else {
         return false;
      }
   }

   default boolean writeLootTable(NbtCompound nbt) {
      RegistryKey<LootTable> registryKey = this.getLootTable();
      if (registryKey == null) {
         return false;
      }

      nbt.putString("LootTable", registryKey.getValue().toString());
      long l = this.getLootTableSeed();
      if (l != 0L) {
         nbt.putLong("LootTableSeed", l);
      }

      return true;
   }

   default void generateLoot(@Nullable PlayerEntity player) {
      World world = this.getWorld();
      BlockPos blockPos = this.getPos();
      RegistryKey<LootTable> registryKey = this.getLootTable();
      if (registryKey != null && world != null && world.getServer() != null) {
         LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(registryKey);
         if (player instanceof ServerPlayerEntity) {
            Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger((ServerPlayerEntity)player, registryKey);
         }

         this.setLootTable(null);
         LootWorldContext.Builder builder = new LootWorldContext.Builder((ServerWorld)world).add(LootContextParameters.ORIGIN, Vec3d.ofCenter(blockPos));
         if (player != null) {
            builder.luck(player.getLuck()).add(LootContextParameters.THIS_ENTITY, player);
         }

         lootTable.supplyInventory(this, builder.build(LootContextTypes.CHEST), this.getLootTableSeed());
      }
   }
}
