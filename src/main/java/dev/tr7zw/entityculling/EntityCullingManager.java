package dev.tr7zw.entityculling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;

public final class EntityCullingManager {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityCullingManager INSTANCE = new EntityCullingManager();
   private static final Path CONFIG_PATH = Path.of("config", "entityculling.json");
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final List<Predicate<Entity>> dynamicEntityWhitelist = new CopyOnWriteArrayList<>();
   private final List<Predicate<BlockEntity>> dynamicBlockEntityWhitelist = new CopyOnWriteArrayList<>();
   private volatile EntityCullingConfig config = new EntityCullingConfig();
   private volatile boolean running;
   private EntityCullingTask task;
   private int ticks;
   private int renderedEntities;
   private int skippedEntities;
   private int renderedBlockEntities;
   private int skippedBlockEntities;
   private int tickedEntities;
   private int skippedEntityTicks;

   private EntityCullingManager() {
   }

   public static EntityCullingManager getInstance() {
      return INSTANCE;
   }

   public synchronized void initialize(MinecraftClient client) {
      if (this.running) {
         return;
      }
      this.loadConfig();
      this.running = true;
      OcclusionCullingInstance culling = new OcclusionCullingInstance(this.config.tracingDistance, new EntityCullingProvider(client));
      this.task = new EntityCullingTask(this, culling);
      Thread worker = new Thread(this.task, "EntityCullingTask");
      worker.setDaemon(true);
      worker.setPriority(Thread.MIN_PRIORITY);
      worker.start();
      LOGGER.info("Entity Culling hard merge initialized");
   }

   public void tick(MinecraftClient client) {
      if (!this.running) {
         this.initialize(client);
      }
      if (client.world == null || client.player == null || client.gameRenderer == null) {
         this.task.clear();
         return;
      }
      if (++this.ticks % Math.max(1, this.config.captureRate) != 0) {
         return;
      }

      List<Entity> entities = new ArrayList<>();
      client.world.getEntities().forEach(entities::add);
      Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
      int radius = Math.max(0, this.config.chunkRadius);
      int centerX = client.player.getChunkPos().x;
      int centerZ = client.player.getChunkPos().z;
      for (int x = centerX - radius; x <= centerX + radius; x++) {
         for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            WorldChunk chunk = client.world.getChunk(x, z);
            blockEntities.putAll(chunk.getBlockEntities());
         }
      }
      this.task.update(List.copyOf(entities), Map.copyOf(blockEntities), client.gameRenderer.getCamera().getPos());
   }

   public boolean isRunning() {
      return this.running;
   }

   public boolean isEnabled() {
      return this.running && this.config.enabled;
   }

   public EntityCullingConfig getConfig() {
      return this.config;
   }

   public boolean shouldSkipEntity(Entity entity) {
      if (!this.isEnabled() || this.config.skipEntityCulling || !(entity instanceof Cullable cullable)) {
         return false;
      }
      if (this.isEntityAlwaysVisible(entity) || this.isEntityWhitelisted(entity) || cullable.entityCulling$isForcedVisible()) {
         this.renderedEntities++;
         cullable.entityCulling$setOutOfCamera(false);
         return false;
      }
      boolean skip = cullable.entityCulling$isCulled();
      if (skip) {
         this.skippedEntities++;
      } else {
         this.renderedEntities++;
         cullable.entityCulling$setOutOfCamera(false);
      }
      return skip;
   }

   public boolean shouldSkipBlockEntity(BlockEntity blockEntity, boolean rendersOutsideBoundingBox) {
      if (!this.isEnabled() || this.config.skipBlockEntityCulling || rendersOutsideBoundingBox || !(blockEntity instanceof Cullable cullable)) {
         return false;
      }
      boolean skip = !this.isBlockEntityWhitelisted(blockEntity)
         && !cullable.entityCulling$isForcedVisible()
         && cullable.entityCulling$isCulled();
      if (skip) {
         this.skippedBlockEntities++;
      } else {
         this.renderedBlockEntities++;
         cullable.entityCulling$setOutOfCamera(false);
      }
      return skip;
   }

   public synchronized String getDebugStringAndReset() {
      String debug = "Entity Culling E: " + this.skippedEntities + "/" + (this.renderedEntities + this.skippedEntities)
         + ", BE: " + this.skippedBlockEntities + "/" + (this.renderedBlockEntities + this.skippedBlockEntities)
         + ", T: " + this.skippedEntityTicks + "/" + (this.tickedEntities + this.skippedEntityTicks);
      this.renderedEntities = 0;
      this.skippedEntities = 0;
      this.renderedBlockEntities = 0;
      this.skippedBlockEntities = 0;
      this.tickedEntities = 0;
      this.skippedEntityTicks = 0;
      return debug;
   }

   public boolean shouldSkipEntityTick(MinecraftClient client, Entity entity) {
      if (!this.isEnabled() || !this.config.tickCulling || this.config.skipEntityCulling || !(entity instanceof Cullable cullable)) {
         this.tickedEntities++;
         return false;
      }
      if (entity == client.player || entity == client.getCameraEntity() || entity.hasPassengers() || entity.hasVehicle()
         || entity instanceof AbstractMinecartEntity || this.isEntityWhitelisted(entity)
         || this.config.tickCullingWhitelist.contains(Registries.ENTITY_TYPE.getId(entity.getType()).toString())) {
         this.tickedEntities++;
         return false;
      }
      if (cullable.entityCulling$isCulled() || cullable.entityCulling$isOutOfCamera()) {
         this.skippedEntityTicks++;
         return true;
      }
      cullable.entityCulling$setOutOfCamera(true);
      this.tickedEntities++;
      return false;
   }

   public boolean isEntityAlwaysVisible(Entity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (entity == client.player || entity == client.getCameraEntity() || client.hasOutline(entity)) {
         return true;
      }
      EntityRenderer<? super Entity, ?> renderer = (EntityRenderer<? super Entity, ?>)client.getEntityRenderDispatcher().getRenderer(entity);
      return renderer == null || !renderer.entityCulling$canBeCulled(entity);
   }

   public Box getCullingBox(Entity entity) {
      EntityRenderer<? super Entity, ?> renderer = (EntityRenderer<? super Entity, ?>)MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
      return renderer == null ? null : renderer.sodium$getCullingBox(entity);
   }

   public Box getBlockEntityCullingBox(BlockEntity blockEntity) {
      BlockPos pos = blockEntity.getPos();
      return blockEntity instanceof BannerBlockEntity
         ? new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0)
         : new Box(pos);
   }

   public boolean isEntityWhitelisted(Entity entity) {
      String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
      if (this.config.entityWhitelist.contains(id)) {
         return true;
      }
      return this.dynamicEntityWhitelist.stream().anyMatch(predicate -> predicate.test(entity));
   }

   public boolean isBlockEntityWhitelisted(BlockEntity blockEntity) {
      String id = Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()).toString();
      if (this.config.blockEntityWhitelist.contains(id)) {
         return true;
      }
      return this.dynamicBlockEntityWhitelist.stream().anyMatch(predicate -> predicate.test(blockEntity));
   }

   public void addDynamicEntityWhitelist(Predicate<Entity> predicate) {
      this.dynamicEntityWhitelist.add(predicate);
   }

   public void addDynamicBlockEntityWhitelist(Predicate<BlockEntity> predicate) {
      this.dynamicBlockEntityWhitelist.add(predicate);
   }

   private void loadConfig() {
      try {
         if (Files.isRegularFile(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
               EntityCullingConfig loaded = this.gson.fromJson(reader, EntityCullingConfig.class);
               if (loaded != null) {
                  boolean upgraded = this.upgradeConfig(loaded);
                  this.config = loaded;
                  if (upgraded) {
                     this.writeConfig();
                  }
                  return;
               }
            }
         }
         this.writeConfig();
      } catch (IOException exception) {
         LOGGER.warn("Unable to load or create {}", CONFIG_PATH, exception);
      }
   }

   private boolean upgradeConfig(EntityCullingConfig loaded) {
      boolean changed = false;
      if (loaded.blockEntityWhitelist == null) {
         loaded.blockEntityWhitelist = new HashSet<>(this.config.blockEntityWhitelist);
         changed = true;
      }
      if (loaded.entityWhitelist == null) {
         loaded.entityWhitelist = new HashSet<>(this.config.entityWhitelist);
         changed = true;
      }
      if (loaded.tickCullingWhitelist == null) {
         loaded.tickCullingWhitelist = new HashSet<>();
         changed = true;
      }
      if (loaded.configVersion < EntityCullingConfig.CURRENT_VERSION) {
         changed |= loaded.tickCullingWhitelist.addAll(EntityCullingConfig.DEFAULT_TICK_CULLING_WHITELIST);
         loaded.configVersion = EntityCullingConfig.CURRENT_VERSION;
         changed = true;
      }
      return changed;
   }

   private void writeConfig() throws IOException {
      Files.createDirectories(CONFIG_PATH.getParent());
      try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
         this.gson.toJson(this.config, writer);
      }
   }
}
