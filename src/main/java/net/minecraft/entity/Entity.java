package net.minecraft.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import dev.tr7zw.entityculling.Cullable;
import dev.tr7zw.entityculling.CullingState;
import com.viaversion.viafabricplus.features.entity.riding_offset.EntityRidingOffsetsPre1_20_2;
import com.viaversion.viafabricplus.injection.access.world.always_tick_entities.IEntity;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.HoneyBlock;
import net.minecraft.block.Portal;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.caffeinemc.mods.lithium.common.block.BlockCountingSection;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlags;
import net.caffeinemc.mods.lithium.common.block.TrackedBlockStatePredicate;
import net.caffeinemc.mods.lithium.common.entity.pushable.FeetBlockCachingEntity;
import net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import net.caffeinemc.mods.lithium.common.util.collections.LazyList;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.data.DataTracked;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.NetherPortal;
import net.minecraft.world.dimension.PortalManager;
import net.minecraft.world.entity.EntityChangeListener;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Entity implements DataTracked, Nameable, EntityLike, ScoreHolder, IEntity, Cullable, FeetBlockCachingEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String ID_KEY = "id";
   public static final String PASSENGERS_KEY = "Passengers";
   private static final AtomicInteger CURRENT_ID = new AtomicInteger();
   public static final int field_49791 = 0;
   public static final int MAX_RIDING_COOLDOWN = 60;
   public static final int DEFAULT_PORTAL_COOLDOWN = 300;
   public static final int MAX_COMMAND_TAGS = 1024;
   public static final float field_44870 = 0.2F;
   public static final double field_44871 = 0.500001;
   public static final double field_44872 = 0.999999;
   public static final int DEFAULT_MIN_FREEZE_DAMAGE_TICKS = 140;
   public static final int FREEZING_DAMAGE_INTERVAL = 40;
   public static final int field_49073 = 3;
   private static final Box NULL_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   private static final double SPEED_IN_WATER = 0.014;
   private static final double SPEED_IN_LAVA_IN_NETHER = 0.007;
   private static final double SPEED_IN_LAVA = 0.0023333333333333335;
   public static final String UUID_KEY = "UUID";
   private static double renderDistanceMultiplier = 1.0;
   private final EntityType<?> type;
   private final CullingState entityCulling$state = new CullingState();
   private int id = CURRENT_ID.incrementAndGet();
   private boolean viaFabricPlus$isInLoadedChunkAndShouldTick;
   public boolean intersectionChecked;
   private ImmutableList<Entity> passengerList = ImmutableList.of();
   public int ridingCooldown;
   @Nullable
   private Entity vehicle;
   private World world;
   public double prevX;
   public double prevY;
   public double prevZ;
   private Vec3d pos;
   private BlockPos blockPos;
   private ChunkPos chunkPos;
   private Vec3d velocity = Vec3d.ZERO;
   private float yaw;
   private float pitch;
   public float prevYaw;
   public float prevPitch;
   private Box boundingBox = NULL_BOX;
   public boolean onGround;
   public boolean horizontalCollision;
   public boolean verticalCollision;
   public boolean groundCollision;
   public boolean collidedSoftly;
   public boolean velocityModified;
   public Vec3d movementMultiplier = Vec3d.ZERO;
   @Nullable
   private Entity.RemovalReason removalReason;
   public static final float DEFAULT_FRICTION = 0.6F;
   public static final float MIN_RISING_BUBBLE_COLUMN_SPEED = 1.8F;
   public float distanceTraveled;
   public float speed;
   public float fallDistance;
   private float nextStepSoundDistance = 1.0F;
   public double lastRenderX;
   public double lastRenderY;
   public double lastRenderZ;
   public boolean noClip;
   private boolean onFire;
   protected final Random random = Random.create();
   public int age;
   public int fireTicks = -this.getBurningDuration();
   protected boolean touchingWater;
   protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap(2);
   protected boolean submergedInWater;
   private final Set<TagKey<Fluid>> submergedFluidTag = new ReferenceArraySet<>();
   public int timeUntilRegen;
   protected boolean firstUpdate = true;
   protected final DataTracker dataTracker;
   protected static final TrackedData<Byte> FLAGS = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BYTE);
   protected static final int ON_FIRE_FLAG_INDEX = 0;
   private static final int SNEAKING_FLAG_INDEX = 1;
   private static final int SPRINTING_FLAG_INDEX = 3;
   private static final int SWIMMING_FLAG_INDEX = 4;
   private static final int INVISIBLE_FLAG_INDEX = 5;
   protected static final int GLOWING_FLAG_INDEX = 6;
   protected static final int GLIDING_FLAG_INDEX = 7;
   private static final TrackedData<Integer> AIR = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final TrackedData<Optional<Text>> CUSTOM_NAME = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.OPTIONAL_TEXT_COMPONENT);
   private static final TrackedData<Boolean> NAME_VISIBLE = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
   private static final TrackedData<Boolean> SILENT = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
   private static final TrackedData<Boolean> NO_GRAVITY = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
   protected static final TrackedData<EntityPose> POSE = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.ENTITY_POSE);
   private static final TrackedData<Integer> FROZEN_TICKS = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);
   private EntityChangeListener changeListener = EntityChangeListener.NONE;
   private final TrackedPosition trackedPosition = new TrackedPosition();
   public boolean velocityDirty;
   @Nullable
   public PortalManager portalManager;
   private int portalCooldown;
   private boolean invulnerable;
   protected UUID uuid = MathHelper.randomUuid(this.random);
   protected String uuidString = this.uuid.toString();
   public boolean glowing;
   private final Set<String> commandTags = Sets.newHashSet();
   private final double[] pistonMovementDelta = new double[]{0.0, 0.0, 0.0};
   private long pistonMovementTick;
   private EntityDimensions dimensions;
   private float standingEyeHeight;
   public boolean inPowderSnow;
   public boolean wasInPowderSnow;
   public Optional<BlockPos> supportingBlockPos = Optional.empty();
   private boolean forceUpdateSupportingBlockPos = false;
   private float lastChimeIntensity;
   private int lastChimeAge;
   private boolean hasVisualFire;
   @Nullable
   private BlockState stateAtPos = null;
   private final List<Entity.QueuedCollisionCheck> queuedCollisionChecks = new ArrayList<>();
   private final Set<BlockState> collidedBlocks = new ReferenceArraySet();
   private final LongSet collidedBlockPositions = new LongOpenHashSet();

   public Entity(EntityType<?> type, World world) {
      this.type = type;
      this.world = world;
      this.dimensions = type.getDimensions();
      this.pos = Vec3d.ZERO;
      this.blockPos = BlockPos.ORIGIN;
      this.chunkPos = ChunkPos.ORIGIN;
      DataTracker.Builder builder = new DataTracker.Builder(this);
      builder.add(FLAGS, (byte)0);
      builder.add(AIR, this.getMaxAir());
      builder.add(NAME_VISIBLE, false);
      builder.add(CUSTOM_NAME, Optional.empty());
      builder.add(SILENT, false);
      builder.add(NO_GRAVITY, false);
      builder.add(POSE, EntityPose.STANDING);
      builder.add(FROZEN_TICKS, 0);
      this.initDataTracker(builder);
      this.dataTracker = builder.build();
      this.setPosition(0.0, 0.0, 0.0);
      this.standingEyeHeight = this.dimensions.eyeHeight();
   }

   public boolean collidesWithStateAtPos(BlockPos pos, BlockState state) {
      VoxelShape voxelShape = state.getCollisionShape(this.getWorld(), pos, ShapeContext.of(this));
      VoxelShape voxelShape2 = voxelShape.offset(pos.getX(), pos.getY(), pos.getZ());
      return VoxelShapes.matchesAnywhere(voxelShape2, VoxelShapes.cuboid(this.getBoundingBox()), BooleanBiFunction.AND);
   }

   public int getTeamColorValue() {
      AbstractTeam abstractTeam = this.getScoreboardTeam();
      return abstractTeam != null && abstractTeam.getColor().getColorValue() != null ? abstractTeam.getColor().getColorValue() : 16777215;
   }

   public boolean isSpectator() {
      return false;
   }

   public final void detach() {
      if (this.hasPassengers()) {
         this.removeAllPassengers();
      }

      if (this.hasVehicle()) {
         this.stopRiding();
      }
   }

   public void updateTrackedPosition(double x, double y, double z) {
      this.trackedPosition.setPos(new Vec3d(x, y, z));
   }

   public TrackedPosition getTrackedPosition() {
      return this.trackedPosition;
   }

   public EntityType<?> getType() {
      return this.type;
   }

   @Override
   public int getId() {
      return this.id;
   }

   @Override
   public boolean viaFabricPlus$isInLoadedChunkAndShouldTick() {
      return this.viaFabricPlus$isInLoadedChunkAndShouldTick || DebugSettings.INSTANCE.alwaysTickClientPlayer.isEnabled();
   }

   @Override
   public void viaFabricPlus$setInLoadedChunkAndShouldTick(boolean inLoadedChunkAndShouldTick) {
      this.viaFabricPlus$isInLoadedChunkAndShouldTick = inLoadedChunkAndShouldTick;
   }

   public void setId(int id) {
      this.id = id;
   }

   public Set<String> getCommandTags() {
      return this.commandTags;
   }

   public boolean addCommandTag(String tag) {
      return this.commandTags.size() >= 1024 ? false : this.commandTags.add(tag);
   }

   public boolean removeCommandTag(String tag) {
      return this.commandTags.remove(tag);
   }

   public void kill(ServerWorld world) {
      this.remove(Entity.RemovalReason.KILLED);
      this.emitGameEvent(GameEvent.ENTITY_DIE);
   }

   public final void discard() {
      this.remove(Entity.RemovalReason.DISCARDED);
   }

   protected abstract void initDataTracker(DataTracker.Builder builder);

   public DataTracker getDataTracker() {
      return this.dataTracker;
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Entity ? ((Entity)o).id == this.id : false;
   }

   @Override
   public int hashCode() {
      return this.id;
   }

   public void remove(Entity.RemovalReason reason) {
      this.setRemoved(reason);
   }

   public void onRemoved() {
   }

   public void onRemove(Entity.RemovalReason reason) {
   }

   public void setPose(EntityPose pose) {
      this.dataTracker.set(POSE, pose);
   }

   public EntityPose getPose() {
      return this.dataTracker.get(POSE);
   }

   public boolean isInPose(EntityPose pose) {
      return this.getPose() == pose;
   }

   public boolean isInRange(Entity entity, double radius) {
      return this.getPos().isInRange(entity.getPos(), radius);
   }

   public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
      double d = entity.getX() - this.getX();
      double e = entity.getY() - this.getY();
      double f = entity.getZ() - this.getZ();
      return MathHelper.squaredHypot(d, f) < MathHelper.square(horizontalRadius) && MathHelper.square(e) < MathHelper.square(verticalRadius);
   }

   public void setRotation(float yaw, float pitch) {
      this.setYaw(yaw % 360.0F);
      this.setPitch(pitch % 360.0F);
   }

   public final void setPosition(Vec3d pos) {
      this.setPosition(pos.getX(), pos.getY(), pos.getZ());
   }

   public void setPosition(double x, double y, double z) {
      this.setPos(x, y, z);
      this.setBoundingBox(this.calculateBoundingBox());
   }

   protected final Box calculateBoundingBox() {
      return this.calculateDefaultBoundingBox(this.pos);
   }

   protected Box calculateDefaultBoundingBox(Vec3d pos) {
      return this.dimensions.getBoxAt(pos);
   }

   protected void refreshPosition() {
      this.setPosition(this.pos.x, this.pos.y, this.pos.z);
   }

   public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
      float f = (float)cursorDeltaY * 0.15F;
      float g = (float)cursorDeltaX * 0.15F;
      this.setPitch(this.getPitch() + f);
      this.setYaw(this.getYaw() + g);
      this.setPitch(MathHelper.clamp(this.getPitch(), -90.0F, 90.0F));
      this.prevPitch += f;
      this.prevYaw += g;
      this.prevPitch = MathHelper.clamp(this.prevPitch, -90.0F, 90.0F);
      if (this.vehicle != null) {
         this.vehicle.onPassengerLookAround(this);
      }
   }

   public void tick() {
      this.baseTick();
   }

   public void baseTick() {
      Profiler profiler = Profilers.get();
      profiler.push("entityBaseTick");
      this.lithium$onFeetBlockCacheDeleted();
      this.stateAtPos = null;
      if (this.hasVehicle() && this.getVehicle().isRemoved()) {
         this.stopRiding();
      }

      if (this.ridingCooldown > 0) {
         this.ridingCooldown--;
      }

      this.tickPortalTeleportation();
      if (this.getWorld().isClient && this.shouldSpawnSprintingParticles()) {
         this.spawnSprintingParticles();
      }

      this.wasInPowderSnow = this.inPowderSnow;
      this.inPowderSnow = false;
      this.updateWaterState();
      this.updateSubmergedInWaterState();
      this.updateSwimming();
      if (this.getWorld() instanceof ServerWorld serverWorld) {
         if (this.fireTicks > 0) {
            if (this.isFireImmune()) {
               this.setFireTicks(this.fireTicks - 4);
               if (this.fireTicks < 0) {
                  this.extinguish();
               }
            } else {
               if (this.fireTicks % 20 == 0 && !this.isInLava()) {
                  this.damage(serverWorld, this.getDamageSources().onFire(), 1.0F);
               }

               this.setFireTicks(this.fireTicks - 1);
            }

            if (this.getFrozenTicks() > 0) {
               this.setFrozenTicks(0);
               this.getWorld().syncWorldEvent(null, 1009, this.blockPos, 1);
            }
         }
      } else {
         this.extinguish();
      }

      if (this.isInLava()) {
         this.setOnFireFromLava();
         this.fallDistance *= 0.5F;
      }

      this.attemptTickInVoid();
      if (!this.getWorld().isClient) {
         this.setOnFire(this.fireTicks > 0);
      }

      this.firstUpdate = false;
      if (this.getWorld() instanceof ServerWorld serverWorld && this instanceof Leashable) {
         Leashable.tickLeash(serverWorld, (Entity & Leashable)this);
      }

      profiler.pop();
   }

   public void setOnFire(boolean onFire) {
      this.setFlag(0, onFire || this.hasVisualFire);
   }

   public void attemptTickInVoid() {
      if (this.getY() < this.getWorld().getBottomY() - 64) {
         this.tickInVoid();
      }
   }

   public void resetPortalCooldown() {
      this.portalCooldown = this.getDefaultPortalCooldown();
   }

   public void setPortalCooldown(int portalCooldown) {
      this.portalCooldown = portalCooldown;
   }

   public int getPortalCooldown() {
      return this.portalCooldown;
   }

   public boolean hasPortalCooldown() {
      return this.portalCooldown > 0;
   }

   protected void tickPortalCooldown() {
      if (this.hasPortalCooldown()) {
         this.portalCooldown--;
      }
   }

   public void setOnFireFromLava() {
      if (!this.isFireImmune()) {
         this.setOnFireFor(15.0F);
         if (this.getWorld() instanceof ServerWorld serverWorld
            && this.damage(serverWorld, this.getDamageSources().lava(), 4.0F)
            && this.shouldPlayBurnSoundInLava()
            && !this.isSilent()) {
            serverWorld.playSound(
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               SoundEvents.ENTITY_GENERIC_BURN,
               this.getSoundCategory(),
               0.4F,
               2.0F + this.random.nextFloat() * 0.4F
            );
         }
      }
   }

   protected boolean shouldPlayBurnSoundInLava() {
      return true;
   }

   public final void setOnFireFor(float seconds) {
      this.setOnFireForTicks(MathHelper.floor(seconds * 20.0F));
   }

   public void setOnFireForTicks(int ticks) {
      if (this.fireTicks < ticks) {
         this.setFireTicks(ticks);
      }
   }

   public void setFireTicks(int fireTicks) {
      this.fireTicks = fireTicks;
   }

   public int getFireTicks() {
      return this.fireTicks;
   }

   public void extinguish() {
      this.setFireTicks(0);
   }

   protected void tickInVoid() {
      this.discard();
   }

   public boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
      return this.doesNotCollide(this.getBoundingBox().offset(offsetX, offsetY, offsetZ));
   }

   private boolean doesNotCollide(Box box) {
      return this.getWorld().isSpaceEmpty(this, box) && !this.getWorld().containsFluid(box);
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
      this.updateSupportingBlockPos(onGround, null);
   }

   public void setMovement(boolean onGround, Vec3d movement) {
      this.setMovement(onGround, this.horizontalCollision, movement);
   }

   public void setMovement(boolean onGround, boolean horizontalCollision, Vec3d movement) {
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      this.updateSupportingBlockPos(onGround, movement);
   }

   public boolean isSupportedBy(BlockPos pos) {
      return this.supportingBlockPos.isPresent() && this.supportingBlockPos.get().equals(pos);
   }

   protected void updateSupportingBlockPos(boolean onGround, @Nullable Vec3d movement) {
      if (onGround) {
         Box box = this.getBoundingBox();
         Box box2 = new Box(box.minX, box.minY - 1.0E-6, box.minZ, box.maxX, box.minY, box.maxZ);
         Optional<BlockPos> optional = this.world.findSupportingBlockPos(this, box2);
         if (optional.isPresent() || this.forceUpdateSupportingBlockPos) {
            this.supportingBlockPos = optional;
         } else if (movement != null) {
            Box box3 = box2.offset(-movement.x, 0.0, -movement.z);
            optional = this.world.findSupportingBlockPos(this, box3);
            this.supportingBlockPos = optional;
         }

         this.forceUpdateSupportingBlockPos = optional.isEmpty();
      } else {
         this.forceUpdateSupportingBlockPos = false;
         if (this.supportingBlockPos.isPresent()) {
            this.supportingBlockPos = Optional.empty();
         }
      }
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public void move(MovementType type, Vec3d movement) {
      if (this.noClip) {
         this.setPosition(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
      } else {
         this.onFire = this.isOnFire();
         if (type == MovementType.PISTON) {
            movement = this.adjustMovementForPiston(movement);
            if (movement.equals(Vec3d.ZERO)) {
               return;
            }
         }

         Profiler profiler = Profilers.get();
         profiler.push("move");
         if (this.movementMultiplier.lengthSquared() > 1.0E-7) {
            movement = movement.multiply(this.movementMultiplier);
            this.movementMultiplier = Vec3d.ZERO;
            this.setVelocity(Vec3d.ZERO);
         }

         movement = this.adjustMovementForSneaking(movement, type);
         Vec3d vec3d = this.adjustMovementForCollisions(movement);
         double d = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21)
            ? Double.MAX_VALUE
            : vec3d.lengthSquared();
         if (d > 1.0E-7 || movement.lengthSquared() - d < 1.0E-7) {
            if (this.fallDistance != 0.0F && d >= 1.0) {
               BlockHitResult blockHitResult = this.getWorld()
                  .raycast(
                     new RaycastContext(
                        this.getPos(), this.getPos().add(vec3d), RaycastContext.ShapeType.FALLDAMAGE_RESETTING, RaycastContext.FluidHandling.WATER, this
                     )
                  );
               if (blockHitResult.getType() != HitResult.Type.MISS) {
                  this.onLanding();
               }
            }

            this.setPosition(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z);
         }

         profiler.pop();
         profiler.push("rest");
         boolean bl = !MathHelper.approximatelyEquals(movement.x, vec3d.x);
         boolean bl2 = !MathHelper.approximatelyEquals(movement.z, vec3d.z);
         this.horizontalCollision = bl || bl2;
         if (Math.abs(movement.y) > 0.0 || this.isLocalPlayerOrLogicalSideForUpdatingMovement()) {
            this.verticalCollision = movement.y != vec3d.y;
            this.groundCollision = this.verticalCollision && movement.y < 0.0;
            this.setMovement(this.groundCollision, this.horizontalCollision, vec3d);
         }

         if (this.horizontalCollision) {
            this.collidedSoftly = this.hasCollidedSoftly(vec3d);
         } else {
            this.collidedSoftly = false;
         }

         BlockPos blockPos = this.getLandingPos();
         BlockState blockState = this.getWorld().getBlockState(blockPos);
         if ((!this.getWorld().isClient() || this.isLogicalSideForUpdatingMovement()) && !this.isControlledByPlayer()) {
            this.fall(vec3d.y, this.isOnGround(), blockState, blockPos);
         }

         if (this.isRemoved()) {
            profiler.pop();
         } else {
            if (this.horizontalCollision) {
               Vec3d vec3d2 = this.getVelocity();
               this.setVelocity(bl ? 0.0 : vec3d2.x, vec3d2.y, bl2 ? 0.0 : vec3d2.z);
            }

            if (this.isLogicalSideForUpdatingMovement()) {
               Block block = blockState.getBlock();
               if (movement.y != vec3d.y) {
                  block.onEntityLand(this.getWorld(), this);
               }
            }

            if (!this.getWorld().isClient() || this.isLogicalSideForUpdatingMovement()) {
               Entity.MoveEffect moveEffect = this.getMoveEffect();
               if (moveEffect.hasAny() && !this.hasVehicle()) {
                  this.applyMoveEffect(moveEffect, vec3d, blockPos, blockState);
               }
            }

            float f = this.getVelocityMultiplier();
            this.setVelocity(this.getVelocity().multiply(f, 1.0, f));
            profiler.pop();
         }
      }
   }

   private void applyMoveEffect(Entity.MoveEffect moveEffect, Vec3d movement, BlockPos landingPos, BlockState landingState) {
      float f = 0.6F;
      float g = (float)(movement.length() * 0.6F);
      float h = (float)(movement.horizontalLength() * 0.6F);
      BlockPos blockPos = this.getSteppingPos();
      BlockState blockState = this.getWorld().getBlockState(blockPos);
      boolean bl = this.canClimb(blockState);
      this.distanceTraveled += bl ? g : h;
      this.speed += g;
      if (this.distanceTraveled > this.nextStepSoundDistance && !blockState.isAir()) {
         boolean bl2 = blockPos.equals(landingPos);
         boolean bl3 = this.stepOnBlock(landingPos, landingState, moveEffect.playsSounds(), bl2, movement);
         if (!bl2) {
            bl3 |= this.stepOnBlock(blockPos, blockState, false, moveEffect.emitsGameEvents(), movement);
         }

         if (bl3) {
            this.nextStepSoundDistance = this.calculateNextStepSoundDistance();
         } else if (this.isTouchingWater()) {
            this.nextStepSoundDistance = this.calculateNextStepSoundDistance();
            if (moveEffect.playsSounds()) {
               this.playSwimSound();
            }

            if (moveEffect.emitsGameEvents()) {
               this.emitGameEvent(GameEvent.SWIM);
            }
         }
      } else if (blockState.isAir()) {
         this.addAirTravelEffects();
      }
   }

   public void tickBlockCollision() {
      this.tickBlockCollision(this.getLastRenderPos(), this.pos);
   }

   public void tickBlockCollision(Vec3d lastRenderPos, Vec3d pos) {
      if (this.shouldTickBlockCollision()) {
         if (this.isOnGround()) {
            BlockPos blockPos = this.getLandingPos();
            BlockState blockState = this.getWorld().getBlockState(blockPos);
            blockState.getBlock().onSteppedOn(this.getWorld(), blockPos, blockState, this);
         }

         this.queuedCollisionChecks.add(new Entity.QueuedCollisionCheck(lastRenderPos, pos));
         List<Entity.QueuedCollisionCheck> list = List.copyOf(this.queuedCollisionChecks);
         this.queuedCollisionChecks.clear();
         this.checkBlockCollision(list, this.collidedBlocks);
         boolean bl = Iterables.any(this.collidedBlocks, state -> state.isIn(BlockTags.FIRE) || state.isOf(Blocks.LAVA));
         this.collidedBlocks.clear();
         if (!bl && this.isAlive()) {
            if (this.fireTicks <= 0) {
               this.setFireTicks(-this.getBurningDuration());
            }

            if (this.onFire && (this.inPowderSnow || this.isWet())) {
               this.playExtinguishSound();
            }
         }

         if (this.isOnFire() && (this.inPowderSnow || this.isWet())) {
            this.setFireTicks(-this.getBurningDuration());
         }
      }
   }

   protected boolean shouldTickBlockCollision() {
      return !this.isRemoved() && !this.noClip;
   }

   private boolean canClimb(BlockState state) {
      return state.isIn(BlockTags.CLIMBABLE) || state.isOf(Blocks.POWDER_SNOW);
   }

   private boolean stepOnBlock(BlockPos pos, BlockState state, boolean playSound, boolean emitEvent, Vec3d movement) {
      if (state.isAir()) {
         return false;
      }

      boolean bl = this.canClimb(state);
      if ((this.isOnGround() || bl || this.isInSneakingPose() && movement.y == 0.0 || this.isOnRail()) && !this.isSwimming()) {
         if (playSound) {
            this.playStepSounds(pos, state);
         }

         if (emitEvent) {
            this.getWorld().emitGameEvent(GameEvent.STEP, this.getPos(), GameEvent.Emitter.of(this, state));
         }

         return true;
      } else {
         return false;
      }
   }

   protected boolean hasCollidedSoftly(Vec3d adjustedMovement) {
      return false;
   }

   protected void playExtinguishSound() {
      this.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
   }

   public void extinguishWithSound() {
      if (!this.getWorld().isClient && this.onFire) {
         this.playExtinguishSound();
      }

      this.extinguish();
   }

   protected void addAirTravelEffects() {
      if (this.isFlappingWings()) {
         this.addFlapEffects();
         if (this.getMoveEffect().emitsGameEvents()) {
            this.emitGameEvent(GameEvent.FLAP);
         }
      }
   }

   @Deprecated
   public BlockPos getLandingPos() {
      return this.getPosWithYOffset(0.2F);
   }

   public BlockPos getVelocityAffectingPos() {
      ProtocolVersion targetVersion = ProtocolTranslator.getTargetVersion();
      if (targetVersion.olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
         return BlockPos.ofFloored(
            this.pos.x,
            this.getBoundingBox().minY - (targetVersion.olderThanOrEqualTo(ProtocolVersion.v1_14_4) ? 1.0 : 0.5000001),
            this.pos.z
         );
      }

      return this.getPosWithYOffset(0.500001F);
   }

   public BlockPos getSteppingPos() {
      return this.getPosWithYOffset(1.0E-5F);
   }

   protected BlockPos getPosWithYOffset(float offset) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
         int i = MathHelper.floor(this.pos.x);
         float effectiveOffset = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18_2) && offset == 1.0E-5F
            ? 0.2F
            : offset;
         int j = MathHelper.floor(this.pos.y - effectiveOffset);
         int k = MathHelper.floor(this.pos.z);
         BlockPos blockPos = new BlockPos(i, j, k);
         if (this.world.getBlockState(blockPos).isAir()) {
            BlockPos downPos = blockPos.down();
            BlockState blockState = this.world.getBlockState(downPos);
            if (blockState.isIn(BlockTags.FENCES) || blockState.isIn(BlockTags.WALLS) || blockState.getBlock() instanceof FenceGateBlock) {
               return downPos;
            }
         }

         return blockPos;
      }

      if (this.supportingBlockPos.isPresent()) {
         BlockPos blockPos = this.supportingBlockPos.get();
         if (!(offset > 1.0E-5F)) {
            return blockPos;
         }

         BlockState blockState = this.getWorld().getBlockState(blockPos);
         return (!(offset <= 0.5) || !blockState.isIn(BlockTags.FENCES))
               && !blockState.isIn(BlockTags.WALLS)
               && !(blockState.getBlock() instanceof FenceGateBlock)
            ? blockPos.withY(MathHelper.floor(this.pos.y - offset))
            : blockPos;
      } else {
         int i = MathHelper.floor(this.pos.x);
         int j = MathHelper.floor(this.pos.y - offset);
         int k = MathHelper.floor(this.pos.z);
         return new BlockPos(i, j, k);
      }
   }

   protected float getJumpVelocityMultiplier() {
      float f = this.getWorld().getBlockState(this.getBlockPos()).getBlock().getJumpVelocityMultiplier();
      float g = this.getWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getJumpVelocityMultiplier();
      return f == 1.0 ? g : f;
   }

   protected float getVelocityMultiplier() {
      BlockState blockState = this.getWorld().getBlockState(this.getBlockPos());
      float f = blockState.getBlock().getVelocityMultiplier();
      if (!blockState.isOf(Blocks.WATER) && !blockState.isOf(Blocks.BUBBLE_COLUMN)) {
         return f == 1.0 ? this.getWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getVelocityMultiplier() : f;
      } else {
         return f;
      }
   }

   protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
      return movement;
   }

   protected Vec3d adjustMovementForPiston(Vec3d movement) {
      if (movement.lengthSquared() <= 1.0E-7) {
         return movement;
      }

      long l = this.getWorld().getTime();
      if (l != this.pistonMovementTick) {
         Arrays.fill(this.pistonMovementDelta, 0.0);
         this.pistonMovementTick = l;
      }

      if (movement.x != 0.0) {
         double d = this.calculatePistonMovementFactor(Direction.Axis.X, movement.x);
         return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(d, 0.0, 0.0);
      } else if (movement.y != 0.0) {
         double d = this.calculatePistonMovementFactor(Direction.Axis.Y, movement.y);
         return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, d, 0.0);
      } else if (movement.z != 0.0) {
         double d = this.calculatePistonMovementFactor(Direction.Axis.Z, movement.z);
         return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, 0.0, d);
      } else {
         return Vec3d.ZERO;
      }
   }

   private double calculatePistonMovementFactor(Direction.Axis axis, double offsetFactor) {
      int i = axis.ordinal();
      double d = MathHelper.clamp(offsetFactor + this.pistonMovementDelta[i], -0.51, 0.51);
      offsetFactor = d - this.pistonMovementDelta[i];
      this.pistonMovementDelta[i] = d;
      return offsetFactor;
   }

   private Vec3d adjustMovementForCollisions(Vec3d movement) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20_5)) {
         Box box = this.getBoundingBox();
         List<VoxelShape> collisions = this.getWorld().getEntityCollisions(this, box.stretch(movement));
         Vec3d adjustedMovement = movement.lengthSquared() == 0.0
            ? movement
            : adjustMovementForCollisions(this, movement, box, this.getWorld(), collisions);
         boolean changedX = movement.x != adjustedMovement.x;
         boolean changedY = movement.y != adjustedMovement.y;
         boolean changedZ = movement.z != adjustedMovement.z;
         boolean mayTouchGround = this.isOnGround() || changedY && movement.y < 0.0;
         float stepHeight = this.getStepHeight();
         if (stepHeight > 0.0F && mayTouchGround && (changedX || changedZ)) {
            Vec3d stepMovement = adjustMovementForCollisions(
               this, new Vec3d(movement.x, stepHeight, movement.z), box, this.getWorld(), collisions
            );
            Vec3d verticalStep = adjustMovementForCollisions(
               this, new Vec3d(0.0, stepHeight, 0.0), box.stretch(movement.x, 0.0, movement.z), this.getWorld(), collisions
            );
            if (verticalStep.y < stepHeight) {
               Vec3d horizontalAfterStep = adjustMovementForCollisions(
                     this, new Vec3d(movement.x, 0.0, movement.z), box.offset(verticalStep), this.getWorld(), collisions
                  )
                  .add(verticalStep);
               if (horizontalAfterStep.horizontalLengthSquared() > stepMovement.horizontalLengthSquared()) {
                  stepMovement = horizontalAfterStep;
               }
            }

            if (stepMovement.horizontalLengthSquared() > adjustedMovement.horizontalLengthSquared()) {
               double originalVerticalMovement = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2) ? 0.0 : movement.y;
               adjustedMovement = stepMovement.add(
                  adjustMovementForCollisions(
                     this,
                     new Vec3d(0.0, -stepMovement.y + originalVerticalMovement, 0.0),
                     box.offset(stepMovement),
                     this.getWorld(),
                     collisions
                  )
               );
            }
         }

         return adjustedMovement;
      }

      Box box = this.getBoundingBox();
      List<VoxelShape> list = new ArrayList<>();
      boolean[] entityCollisionsAdded = new boolean[1];
      Vec3d vec3d = movement.lengthSquared() == 0.0
         ? movement
         : lithium$adjustMovementForCollisions(this, movement, box, this.getWorld(), list, entityCollisionsAdded);
      boolean bl = movement.x != vec3d.x;
      boolean bl2 = movement.y != vec3d.y;
      boolean bl3 = movement.z != vec3d.z;
      boolean bl4 = bl2 && movement.y < 0.0;
      if (this.getStepHeight() > 0.0F && (bl4 || this.isOnGround()) && (bl || bl3)) {
         Box box2 = bl4 ? box.offset(0.0, vec3d.y, 0.0) : box;
         Box box3 = box2.stretch(movement.x, this.getStepHeight(), movement.z);
         if (!bl4) {
            box3 = box3.stretch(0.0, -1.0E-5F, 0.0);
         }

         if (!entityCollisionsAdded[0]) {
            LithiumEntityCollisions.appendEntityCollisions(list, this.world, this, box3);
            entityCollisionsAdded[0] = true;
         }
         List<VoxelShape> list2 = findCollisionsForMovement(this, this.world, list, box3);
         float f = (float)vec3d.y;
         float[] fs = collectStepHeights(box2, list2, this.getStepHeight(), f);

         for (float g : fs) {
            Vec3d vec3d2 = adjustMovementForCollisions(new Vec3d(movement.x, g, movement.z), box2, list2);
            if (vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
               double d = box.minY - box2.minY;
               return vec3d2.add(0.0, -d, 0.0);
            }
         }
      }

      return vec3d;
   }

   private static float[] collectStepHeights(Box collisionBox, List<VoxelShape> collisions, float f, float stepHeight) {
      FloatSet floatSet = new FloatArraySet(4);

      for (VoxelShape voxelShape : collisions) {
         DoubleList doubleList = voxelShape.getPointPositions(Direction.Axis.Y);
         DoubleListIterator var8 = doubleList.iterator();

         while (var8.hasNext()) {
            double d = (Double)var8.next();
            float g = (float)(d - collisionBox.minY);
            if (!(g < 0.0F) && g != stepHeight) {
               if (g > f) {
                  break;
               }

               floatSet.add(g);
            }
         }
      }

      float[] fs = floatSet.toFloatArray();
      FloatArrays.unstableSort(fs);
      return fs;
   }

   public static Vec3d adjustMovementForCollisions(@Nullable Entity entity, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> collisions) {
      return lithium$adjustMovementForCollisions(entity, movement, entityBoundingBox, world, collisions, null);
   }

   private static Vec3d lithium$adjustMovementForCollisions(
      @Nullable Entity entity,
      Vec3d movement,
      Box entityBoundingBox,
      World world,
      List<VoxelShape> entityCollisions,
      @Nullable boolean[] delayedEntityCollisions
   ) {
      double movementX = movement.x;
      double movementY = movement.y;
      double movementZ = movement.z;
      boolean singleAxis = (movementX == 0.0 ? 0 : 1) + (movementY == 0.0 ? 0 : 1) + (movementZ == 0.0 ? 0 : 1) == 1;
      if (movementY < 0.0) {
         VoxelShape supportingShape = LithiumEntityCollisions.getSupportingCollisionForEntity(world, entity, entityBoundingBox);
         if (supportingShape != null && supportingShape.calculateMaxDistance(Direction.Axis.Y, entityBoundingBox, movementY) == 0.0) {
            if (singleAxis) {
               return Vec3d.ZERO;
            }
            movementY = 0.0;
            singleAxis = (movementX == 0.0 ? 0 : 1) + (movementZ == 0.0 ? 0 : 1) == 1;
         }
      }

      Box movementSpace = singleAxis
         ? LithiumEntityCollisions.getSmallerBoxForSingleAxisMovement(movement, entityBoundingBox, movementY, movementX, movementZ)
         : entityBoundingBox.stretch(movement);
      boolean shouldAddEntities = delayedEntityCollisions != null && !delayedEntityCollisions[0];
      boolean shouldAddWorldBorder = true;
      boolean shouldAddLastBlock = true;
      ChunkAwareBlockCollisionSweeper blockSweeper = new ChunkAwareBlockCollisionSweeper(world, entity, movementSpace, true);
      LazyList<VoxelShape> blockCollisions = new LazyList<>(new ArrayList<>(), blockSweeper);
      ArrayList<VoxelShape> borderAndLastBlock = new ArrayList<>(2);

      if (movementY != 0.0) {
         movementY = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, blockCollisions, movementY);
         if (movementY != 0.0) {
            if (shouldAddEntities) {
               LithiumEntityCollisions.appendEntityCollisions(entityCollisions, world, entity, movementSpace);
               delayedEntityCollisions[0] = true;
               shouldAddEntities = false;
            }
            shouldAddWorldBorder = LithiumEntityCollisions.addWorldBorderCollisionIfRequired(
               shouldAddWorldBorder, entity, borderAndLastBlock, movementSpace
            );
            shouldAddLastBlock = LithiumEntityCollisions.addLastBlockCollisionIfRequired(shouldAddLastBlock, blockSweeper, borderAndLastBlock);
            if (!entityCollisions.isEmpty()) {
               movementY = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, entityCollisions, movementY);
            }
            if (!borderAndLastBlock.isEmpty()) {
               movementY = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, borderAndLastBlock, movementY);
            }
            if (movementY != 0.0) {
               entityBoundingBox = entityBoundingBox.offset(0.0, movementY, 0.0);
            }
         }
      }

      boolean zFirst = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)
         ? false
         : Math.abs(movementX) < Math.abs(movementZ);
      if (zFirst && movementZ != 0.0) {
         movementZ = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, blockCollisions, movementZ);
         if (movementZ != 0.0) {
            if (shouldAddEntities) {
               LithiumEntityCollisions.appendEntityCollisions(entityCollisions, world, entity, movementSpace);
               delayedEntityCollisions[0] = true;
               shouldAddEntities = false;
            }
            shouldAddWorldBorder = LithiumEntityCollisions.addWorldBorderCollisionIfRequired(
               shouldAddWorldBorder, entity, borderAndLastBlock, movementSpace
            );
            shouldAddLastBlock = LithiumEntityCollisions.addLastBlockCollisionIfRequired(shouldAddLastBlock, blockSweeper, borderAndLastBlock);
            if (!entityCollisions.isEmpty()) {
               movementZ = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, entityCollisions, movementZ);
            }
            if (!borderAndLastBlock.isEmpty()) {
               movementZ = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, borderAndLastBlock, movementZ);
            }
            if (movementZ != 0.0) {
               entityBoundingBox = entityBoundingBox.offset(0.0, 0.0, movementZ);
            }
         }
      }

      if (movementX != 0.0) {
         movementX = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, blockCollisions, movementX);
         if (movementX != 0.0) {
            if (shouldAddEntities) {
               LithiumEntityCollisions.appendEntityCollisions(entityCollisions, world, entity, movementSpace);
               delayedEntityCollisions[0] = true;
               shouldAddEntities = false;
            }
            shouldAddWorldBorder = LithiumEntityCollisions.addWorldBorderCollisionIfRequired(
               shouldAddWorldBorder, entity, borderAndLastBlock, movementSpace
            );
            shouldAddLastBlock = LithiumEntityCollisions.addLastBlockCollisionIfRequired(shouldAddLastBlock, blockSweeper, borderAndLastBlock);
            if (!entityCollisions.isEmpty()) {
               movementX = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, entityCollisions, movementX);
            }
            if (!borderAndLastBlock.isEmpty()) {
               movementX = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, borderAndLastBlock, movementX);
            }
            if (movementX != 0.0) {
               entityBoundingBox = entityBoundingBox.offset(movementX, 0.0, 0.0);
            }
         }
      }

      if (!zFirst && movementZ != 0.0) {
         movementZ = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, blockCollisions, movementZ);
         if (movementZ != 0.0) {
            if (shouldAddEntities) {
               LithiumEntityCollisions.appendEntityCollisions(entityCollisions, world, entity, movementSpace);
               delayedEntityCollisions[0] = true;
            }
            LithiumEntityCollisions.addWorldBorderCollisionIfRequired(shouldAddWorldBorder, entity, borderAndLastBlock, movementSpace);
            LithiumEntityCollisions.addLastBlockCollisionIfRequired(shouldAddLastBlock, blockSweeper, borderAndLastBlock);
            if (!entityCollisions.isEmpty()) {
               movementZ = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, entityCollisions, movementZ);
            }
            if (!borderAndLastBlock.isEmpty()) {
               movementZ = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, borderAndLastBlock, movementZ);
            }
         }
      }

      return new Vec3d(movementX, movementY, movementZ);
   }

   private static List<VoxelShape> findCollisionsForMovement(
      @Nullable Entity entity, World world, List<VoxelShape> regularCollisions, Box movingEntityBoundingBox
   ) {
      Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(regularCollisions.size() + 1);
      if (!regularCollisions.isEmpty()) {
         builder.addAll(regularCollisions);
      }

      WorldBorder worldBorder = world.getWorldBorder();
      boolean bl = entity != null && worldBorder.canCollide(entity, movingEntityBoundingBox);
      if (bl) {
         builder.add(worldBorder.asVoxelShape());
      }

      builder.addAll(world.getBlockCollisions(entity, movingEntityBoundingBox));
      return builder.build();
   }

   private static Vec3d adjustMovementForCollisions(Vec3d movement, Box entityBoundingBox, List<VoxelShape> collisions) {
      if (collisions.isEmpty()) {
         return movement;
      }

      double d = movement.x;
      double e = movement.y;
      double f = movement.z;
      if (e != 0.0) {
         e = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, collisions, e);
         if (e != 0.0) {
            entityBoundingBox = entityBoundingBox.offset(0.0, e, 0.0);
         }
      }

      boolean bl = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2) ? false : Math.abs(d) < Math.abs(f);
      if (bl && f != 0.0) {
         f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
         if (f != 0.0) {
            entityBoundingBox = entityBoundingBox.offset(0.0, 0.0, f);
         }
      }

      if (d != 0.0) {
         d = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, collisions, d);
         if (!bl && d != 0.0) {
            entityBoundingBox = entityBoundingBox.offset(d, 0.0, 0.0);
         }
      }

      if (!bl && f != 0.0) {
         f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
      }

      return new Vec3d(d, e, f);
   }

   protected float calculateNextStepSoundDistance() {
      return (int)this.distanceTraveled + 1;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.ENTITY_GENERIC_SWIM;
   }

   protected SoundEvent getSplashSound() {
      return SoundEvents.ENTITY_GENERIC_SPLASH;
   }

   protected SoundEvent getHighSpeedSplashSound() {
      return SoundEvents.ENTITY_GENERIC_SPLASH;
   }

   public void queueBlockCollisionCheck(Vec3d oldPos, Vec3d newPos) {
      this.queuedCollisionChecks.add(new Entity.QueuedCollisionCheck(oldPos, newPos));
   }

   private void checkBlockCollision(List<Entity.QueuedCollisionCheck> queuedCollisionChecks, Set<BlockState> collidedBlocks) {
      if (this.shouldTickBlockCollision()) {
         LongSet longSet = this.collidedBlockPositions;

         for (Entity.QueuedCollisionCheck queuedCollisionCheck : queuedCollisionChecks) {
            Vec3d vec3d = queuedCollisionCheck.from();
            Vec3d vec3d2 = queuedCollisionCheck.to();
            double collisionMargin;
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_1)) {
               collisionMargin = 0.001;
            } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21)) {
               collisionMargin = 1.0E-7;
            } else {
               collisionMargin = 1.0E-5F;
            }

            Box box = this.calculateDefaultBoundingBox(vec3d2).contract(collisionMargin);

            for (BlockPos blockPos : BlockView.collectCollisionsBetween(vec3d, vec3d2, box)) {
               if (!this.isAlive()) {
                  return;
               }

               BlockState blockState = this.getWorld().getBlockState(blockPos);
               if (!blockState.isAir() && longSet.add(blockPos.asLong())) {
                  try {
                     VoxelShape voxelShape = blockState.getInsideCollisionShape(this.getWorld(), blockPos);
                     if (voxelShape != VoxelShapes.fullCube() && !this.collides(vec3d, vec3d2, blockPos, voxelShape)) {
                        continue;
                     }

                     blockState.onEntityCollision(this.getWorld(), blockPos, this);
                     this.onBlockCollision(blockState);
                  } catch (Throwable throwable) {
                     CrashReport crashReport = CrashReport.create(throwable, "Colliding entity with block");
                     CrashReportSection crashReportSection = crashReport.addElement("Block being collided with");
                     CrashReportSection.addBlockInfo(crashReportSection, this.getWorld(), blockPos, blockState);
                     CrashReportSection crashReportSection2 = crashReport.addElement("Entity being checked for collision");
                     this.populateCrashReport(crashReportSection2);
                     throw new CrashException(crashReport);
                  }

                  collidedBlocks.add(blockState);
               }
            }
         }

         longSet.clear();
      }
   }

   private boolean collides(Vec3d oldPos, Vec3d newPos, BlockPos blockPos, VoxelShape shape) {
      Box box = this.calculateDefaultBoundingBox(oldPos);
      Vec3d vec3d = newPos.subtract(oldPos);
      return box.collides(vec3d, shape.offset(new Vec3d(blockPos)).getBoundingBoxes());
   }

   protected void onBlockCollision(BlockState state) {
   }

   public BlockPos getWorldSpawnPos(ServerWorld world, BlockPos basePos) {
      BlockPos blockPos = world.getSpawnPos();
      Vec3d vec3d = blockPos.toCenterPos();
      int i = world.getWorldChunk(blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, blockPos.getX(), blockPos.getZ()) + 1;
      return BlockPos.ofFloored(vec3d.x, i, vec3d.z);
   }

   public void emitGameEvent(RegistryEntry<GameEvent> event, @Nullable Entity entity) {
      this.getWorld().emitGameEvent(entity, event, this.pos);
   }

   public void emitGameEvent(RegistryEntry<GameEvent> event) {
      this.emitGameEvent(event, this);
   }

   private void playStepSounds(BlockPos pos, BlockState state) {
      this.playStepSound(pos, state);
      if (this.shouldPlayAmethystChimeSound(state)) {
         this.playAmethystChimeSound();
      }
   }

   protected void playSwimSound() {
      Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
      float f = entity == this ? 0.35F : 0.4F;
      Vec3d vec3d = entity.getVelocity();
      float g = Math.min(1.0F, (float)Math.sqrt(vec3d.x * vec3d.x * 0.2F + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.2F) * f);
      this.playSwimSound(g);
   }

   protected BlockPos getStepSoundPos(BlockPos pos) {
      BlockPos blockPos = pos.up();
      BlockState blockState = this.getWorld().getBlockState(blockPos);
      return !blockState.isIn(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !blockState.isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? pos : blockPos;
   }

   protected void playCombinationStepSounds(BlockState primaryState, BlockState secondaryState) {
      BlockSoundGroup blockSoundGroup = primaryState.getSoundGroup();
      this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
      this.playSecondaryStepSound(secondaryState);
   }

   protected void playSecondaryStepSound(BlockState state) {
      BlockSoundGroup blockSoundGroup = state.getSoundGroup();
      this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.05F, blockSoundGroup.getPitch() * 0.8F);
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      BlockSoundGroup blockSoundGroup = state.getSoundGroup();
      this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
   }

   private boolean shouldPlayAmethystChimeSound(BlockState state) {
      return state.isIn(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.age >= this.lastChimeAge + 20;
   }

   private void playAmethystChimeSound() {
      this.lastChimeIntensity = this.lastChimeIntensity * (float)Math.pow(0.997, this.age - this.lastChimeAge);
      this.lastChimeIntensity = Math.min(1.0F, this.lastChimeIntensity + 0.07F);
      float f = 0.5F + this.lastChimeIntensity * this.random.nextFloat() * 1.2F;
      float g = 0.1F + this.lastChimeIntensity * 1.2F;
      this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, g, f);
      this.lastChimeAge = this.age;
   }

   protected void playSwimSound(float volume) {
      this.playSound(this.getSwimSound(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
   }

   protected void addFlapEffects() {
   }

   protected boolean isFlappingWings() {
      return false;
   }

   public void playSound(SoundEvent sound, float volume, float pitch) {
      if (!this.isSilent()) {
         this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
      }
   }

   public void playSoundIfNotSilent(SoundEvent event) {
      if (!this.isSilent()) {
         this.playSound(event, 1.0F, 1.0F);
      }
   }

   public boolean isSilent() {
      return this.dataTracker.get(SILENT);
   }

   public void setSilent(boolean silent) {
      this.dataTracker.set(SILENT, silent);
   }

   public boolean hasNoGravity() {
      return this.dataTracker.get(NO_GRAVITY);
   }

   public void setNoGravity(boolean noGravity) {
      this.dataTracker.set(NO_GRAVITY, noGravity);
   }

   protected double getGravity() {
      return 0.0;
   }

   public final double getFinalGravity() {
      return this.hasNoGravity() ? 0.0 : this.getGravity();
   }

   protected void applyGravity() {
      double d = this.getFinalGravity();
      if (d != 0.0) {
         this.setVelocity(this.getVelocity().add(0.0, -d, 0.0));
      }
   }

   protected Entity.MoveEffect getMoveEffect() {
      return Entity.MoveEffect.ALL;
   }

   public boolean occludeVibrationSignals() {
      return false;
   }

   public final void handleFall(double xDifference, double yDifference, double zDifference, boolean onGround) {
      if (!this.isRegionUnloaded()) {
         this.updateSupportingBlockPos(onGround, new Vec3d(xDifference, yDifference, zDifference));
         BlockPos blockPos = this.getLandingPos();
         BlockState blockState = this.getWorld().getBlockState(blockPos);
         this.fall(yDifference, onGround, blockState, blockPos);
      }
   }

   protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
      if (onGround) {
         if (this.fallDistance > 0.0F) {
            state.getBlock().onLandedUpon(this.getWorld(), state, landedPosition, this, this.fallDistance);
            this.getWorld()
               .emitGameEvent(
                  GameEvent.HIT_GROUND,
                  this.pos,
                  GameEvent.Emitter.of(this, this.supportingBlockPos.<BlockState>map(blockPos -> this.getWorld().getBlockState(blockPos)).orElse(state))
               );
         }

         this.onLanding();
      } else if (heightDifference < 0.0) {
         this.fallDistance -= (float)heightDifference;
      }
   }

   public boolean isFireImmune() {
      return this.getType().isFireImmune();
   }

   public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
      if (this.type.isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return false;
      }

      if (this.hasPassengers()) {
         for (Entity entity : this.getPassengerList()) {
            entity.handleFallDamage(fallDistance, damageMultiplier, damageSource);
         }
      }

      return false;
   }

   public boolean isTouchingWater() {
      return this.touchingWater;
   }

   private boolean isBeingRainedOn() {
      BlockPos blockPos = this.getBlockPos();
      return this.getWorld().hasRain(blockPos) || this.getWorld().hasRain(BlockPos.ofFloored(blockPos.getX(), this.getBoundingBox().maxY, blockPos.getZ()));
   }

   private boolean isInsideBubbleColumn() {
      return this.getBlockStateAtPos().isOf(Blocks.BUBBLE_COLUMN);
   }

   public boolean isTouchingWaterOrRain() {
      return this.isTouchingWater() || this.isBeingRainedOn();
   }

   public boolean isWet() {
      return this.isTouchingWater() || this.isBeingRainedOn() || this.isInsideBubbleColumn();
   }

   public boolean isInsideWaterOrBubbleColumn() {
      return this.isTouchingWater() || this.isInsideBubbleColumn();
   }

   public boolean isInFluid() {
      return this.isInsideWaterOrBubbleColumn() || this.isInLava();
   }

   public boolean isSubmergedInWater() {
      return this.submergedInWater && this.isTouchingWater();
   }

   public void updateSwimming() {
      if (this.isSwimming()) {
         this.setSwimming(this.isSprinting() && this.isTouchingWater() && !this.hasVehicle());
      } else {
         this.setSwimming(
            this.isSprinting() && this.isSubmergedInWater() && !this.hasVehicle() && this.getWorld().getFluidState(this.blockPos).isIn(FluidTags.WATER)
         );
      }
   }

   protected boolean updateWaterState() {
      this.fluidHeight.clear();
      this.checkWaterState();
      double d = this.getWorld().getDimension().ultrawarm() ? 0.007 : 0.0023333333333333335;
      boolean bl = this.updateMovementInFluid(FluidTags.LAVA, d);
      return this.isTouchingWater() || bl;
   }

   void checkWaterState() {
      if (this.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity && !abstractBoatEntity.isSubmergedInWater()) {
         this.touchingWater = false;
      } else if (this.updateMovementInFluid(FluidTags.WATER, 0.014)) {
         if (!this.touchingWater && !this.firstUpdate) {
            this.onSwimmingStart();
         }

         this.onLanding();
         this.touchingWater = true;
         this.extinguish();
      } else {
         this.touchingWater = false;
      }
   }

   private void updateSubmergedInWaterState() {
      this.submergedInWater = this.isSubmergedIn(FluidTags.WATER);
      if (!this.submergedFluidTag.isEmpty()) {
         this.submergedFluidTag.clear();
      }
      double d = this.getEyeY();
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20_3)) {
         d -= 0.11111111F;
      }
      if (!(
         this.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity
            && !abstractBoatEntity.isSubmergedInWater()
            && abstractBoatEntity.getBoundingBox().maxY >= d
            && abstractBoatEntity.getBoundingBox().minY <= d
      )) {
         BlockPos blockPos = BlockPos.ofFloored(this.getX(), d, this.getZ());
         FluidState fluidState = this.getWorld().getFluidState(blockPos);
         double e = blockPos.getY() + fluidState.getHeight(this.getWorld(), blockPos);
         if (e > d) {
            fluidState.streamTags().forEach(this.submergedFluidTag::add);
         }
      }
   }

   protected void onSwimmingStart() {
      Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
      float f = entity == this ? 0.2F : 0.9F;
      Vec3d vec3d = entity.getVelocity();
      float g = Math.min(1.0F, (float)Math.sqrt(vec3d.x * vec3d.x * 0.2F + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.2F) * f);
      if (g < 0.25F) {
         this.playSound(this.getSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
      } else {
         this.playSound(this.getHighSpeedSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
      }

      float h = MathHelper.floor(this.getY());

      for (int i = 0; i < 1.0F + this.dimensions.width() * 20.0F; i++) {
         double d = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
         double e = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
         this.getWorld()
            .addParticle(ParticleTypes.BUBBLE, this.getX() + d, h + 1.0F, this.getZ() + e, vec3d.x, vec3d.y - this.random.nextDouble() * 0.2F, vec3d.z);
      }

      for (int i = 0; i < 1.0F + this.dimensions.width() * 20.0F; i++) {
         double d = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
         double e = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
         this.getWorld().addParticle(ParticleTypes.SPLASH, this.getX() + d, h + 1.0F, this.getZ() + e, vec3d.x, vec3d.y, vec3d.z);
      }

      this.emitGameEvent(GameEvent.SPLASH);
   }

   @Deprecated
   protected BlockState getLandingBlockState() {
      return this.getWorld().getBlockState(this.getLandingPos());
   }

   public BlockState getSteppingBlockState() {
      return this.getWorld().getBlockState(this.getSteppingPos());
   }

   public boolean shouldSpawnSprintingParticles() {
      return this.isSprinting() && !this.isTouchingWater() && !this.isSpectator() && !this.isInSneakingPose() && !this.isInLava() && this.isAlive();
   }

   protected void spawnSprintingParticles() {
      BlockPos blockPos = this.getLandingPos();
      BlockState blockState = this.getWorld().getBlockState(blockPos);
      if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
         Vec3d vec3d = this.getVelocity();
         BlockPos blockPos2 = this.getBlockPos();
         double d = this.getX() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
         double e = this.getZ() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
         if (blockPos2.getX() != blockPos.getX()) {
            d = MathHelper.clamp(d, blockPos.getX(), blockPos.getX() + 1.0);
         }

         if (blockPos2.getZ() != blockPos.getZ()) {
            e = MathHelper.clamp(e, blockPos.getZ(), blockPos.getZ() + 1.0);
         }

         this.getWorld()
            .addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), d, this.getY() + 0.1, e, vec3d.x * -4.0, 1.5, vec3d.z * -4.0);
      }
   }

   public boolean isSubmergedIn(TagKey<Fluid> fluidTag) {
      return this.submergedFluidTag.contains(fluidTag);
   }

   public boolean isInLava() {
      return !this.firstUpdate && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
   }

   public void updateVelocity(float speed, Vec3d movementInput) {
      Vec3d vec3d = movementInputToVelocity(movementInput, speed, this.getYaw());
      this.setVelocity(this.getVelocity().add(vec3d));
   }

   protected static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
      double d = movementInput.lengthSquared();
      double velocityEpsilon = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2) ? 1.0E-4 : 1.0E-7;
      if (d < velocityEpsilon) {
         return Vec3d.ZERO;
      }

      Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
      float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
      float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
      return new Vec3d(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
   }

   @Deprecated
   public float getBrightnessAtEyes() {
      return this.getWorld().isPosLoaded(this.getBlockX(), this.getBlockZ())
         ? this.getWorld().getBrightness(BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ()))
         : 0.0F;
   }

   public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch) {
      this.updatePosition(x, y, z);
      this.setAngles(yaw, pitch);
   }

   public void setAngles(float yaw, float pitch) {
      this.setYaw(yaw % 360.0F);
      this.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F) % 360.0F);
      this.prevYaw = this.getYaw();
      this.prevPitch = this.getPitch();
   }

   public void updatePosition(double x, double y, double z) {
      double d = MathHelper.clamp(x, -3.0E7, 3.0E7);
      double e = MathHelper.clamp(z, -3.0E7, 3.0E7);
      this.prevX = d;
      this.prevY = y;
      this.prevZ = e;
      this.setPosition(d, y, e);
   }

   public void refreshPositionAfterTeleport(Vec3d pos) {
      this.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
   }

   public void refreshPositionAfterTeleport(double x, double y, double z) {
      this.refreshPositionAndAngles(x, y, z, this.getYaw(), this.getPitch());
   }

   public void refreshPositionAndAngles(BlockPos pos, float yaw, float pitch) {
      this.refreshPositionAndAngles(pos.toBottomCenterPos(), yaw, pitch);
   }

   public void refreshPositionAndAngles(Vec3d pos, float yaw, float pitch) {
      this.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
   }

   public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
      this.setPos(x, y, z);
      this.setYaw(yaw);
      this.setPitch(pitch);
      this.resetPosition();
      this.refreshPosition();
   }

   public final void resetPosition() {
      this.updatePrevPosition();
      this.updatePrevAngles();
   }

   public final void setPrevPositionAndAngles(Vec3d pos, float yaw, float pitch) {
      this.setPrevPosition(pos);
      this.setPrevAngles(yaw, pitch);
   }

   protected void updatePrevPosition() {
      this.setPrevPosition(this.pos);
   }

   public void updatePrevAngles() {
      this.setPrevAngles(this.getYaw(), this.getPitch());
   }

   private void setPrevPosition(Vec3d pos) {
      this.prevX = this.lastRenderX = pos.x;
      this.prevY = this.lastRenderY = pos.y;
      this.prevZ = this.lastRenderZ = pos.z;
   }

   private void setPrevAngles(float prevYaw, float prevPitch) {
      this.prevYaw = prevYaw;
      this.prevPitch = prevPitch;
   }

   public final Vec3d getLastRenderPos() {
      return new Vec3d(this.lastRenderX, this.lastRenderY, this.lastRenderZ);
   }

   public float distanceTo(Entity entity) {
      float f = (float)(this.getX() - entity.getX());
      float g = (float)(this.getY() - entity.getY());
      float h = (float)(this.getZ() - entity.getZ());
      return MathHelper.sqrt(f * f + g * g + h * h);
   }

   public double squaredDistanceTo(double x, double y, double z) {
      double d = this.getX() - x;
      double e = this.getY() - y;
      double f = this.getZ() - z;
      return d * d + e * e + f * f;
   }

   public double squaredDistanceTo(Entity entity) {
      return this.squaredDistanceTo(entity.getPos());
   }

   public double squaredDistanceTo(Vec3d vector) {
      double d = this.getX() - vector.x;
      double e = this.getY() - vector.y;
      double f = this.getZ() - vector.z;
      return d * d + e * e + f * f;
   }

   public void onPlayerCollision(PlayerEntity player) {
   }

   public void pushAwayFrom(Entity entity) {
      if (!this.isConnectedThroughVehicle(entity)) {
         if (!entity.noClip && !this.noClip) {
            double d = entity.getX() - this.getX();
            double e = entity.getZ() - this.getZ();
            double f = MathHelper.absMax(d, e);
            if (f >= 0.01F) {
               f = Math.sqrt(f);
               d /= f;
               e /= f;
               double g = 1.0 / f;
               if (g > 1.0) {
                  g = 1.0;
               }

               d *= g;
               e *= g;
               d *= 0.05F;
               e *= 0.05F;
               if (!this.hasPassengers() && this.isPushable()) {
                  this.addVelocity(-d, 0.0, -e);
               }

               if (!entity.hasPassengers() && entity.isPushable()) {
                  entity.addVelocity(d, 0.0, e);
               }
            }
         }
      }
   }

   public void addVelocity(Vec3d velocity) {
      this.addVelocity(velocity.x, velocity.y, velocity.z);
   }

   public void addVelocity(double deltaX, double deltaY, double deltaZ) {
      this.setVelocity(this.getVelocity().add(deltaX, deltaY, deltaZ));
      this.velocityDirty = true;
   }

   protected void scheduleVelocityUpdate() {
      this.velocityModified = true;
   }

   @Deprecated
   public final void serverDamage(DamageSource source, float amount) {
      if (this.world instanceof ServerWorld serverWorld) {
         this.damage(serverWorld, source, amount);
      }
   }

   @Deprecated
   public final boolean sidedDamage(DamageSource source, float amount) {
      return this.world instanceof ServerWorld serverWorld ? this.damage(serverWorld, source, amount) : this.clientDamage(source);
   }

   public abstract boolean damage(ServerWorld world, DamageSource source, float amount);

   public boolean clientDamage(DamageSource source) {
      return false;
   }

   public final Vec3d getRotationVec(float tickDelta) {
      return this.getRotationVector(this.getPitch(tickDelta), this.getYaw(tickDelta));
   }

   public Direction getFacing() {
      return Direction.getFacing(this.getRotationVec(1.0F));
   }

   public float getPitch(float tickDelta) {
      return this.getLerpedPitch(tickDelta);
   }

   public float getYaw(float tickDelta) {
      return this.getLerpedYaw(tickDelta);
   }

   public float getLerpedPitch(float tickDelta) {
      return tickDelta == 1.0F ? this.getPitch() : MathHelper.lerp(tickDelta, this.prevPitch, this.getPitch());
   }

   public float getLerpedYaw(float tickDelta) {
      return tickDelta == 1.0F ? this.getYaw() : MathHelper.lerpAngleDegrees(tickDelta, this.prevYaw, this.getYaw());
   }

   public final Vec3d getRotationVector(float pitch, float yaw) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return Vec3d.fromPolar(pitch, yaw);
      }

      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public final Vec3d getOppositeRotationVector(float tickDelta) {
      return this.getOppositeRotationVector(this.getPitch(tickDelta), this.getYaw(tickDelta));
   }

   protected final Vec3d getOppositeRotationVector(float pitch, float yaw) {
      return this.getRotationVector(pitch - 90.0F, yaw);
   }

   public final Vec3d getEyePos() {
      return new Vec3d(this.getX(), this.getEyeY(), this.getZ());
   }

   public final Vec3d getCameraPosVec(float tickDelta) {
      double d = MathHelper.lerp(tickDelta, this.prevX, this.getX());
      double e = MathHelper.lerp(tickDelta, this.prevY, this.getY()) + this.getStandingEyeHeight();
      double f = MathHelper.lerp(tickDelta, this.prevZ, this.getZ());
      return new Vec3d(d, e, f);
   }

   public Vec3d getClientCameraPosVec(float tickDelta) {
      return this.getCameraPosVec(tickDelta);
   }

   public final Vec3d getLerpedPos(float delta) {
      double d = MathHelper.lerp(delta, this.prevX, this.getX());
      double e = MathHelper.lerp(delta, this.prevY, this.getY());
      double f = MathHelper.lerp(delta, this.prevZ, this.getZ());
      return new Vec3d(d, e, f);
   }

   public HitResult raycast(double maxDistance, float tickDelta, boolean includeFluids) {
      Vec3d vec3d = this.getCameraPosVec(tickDelta);
      Vec3d vec3d2 = this.getRotationVec(tickDelta);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
      return this.getWorld()
         .raycast(
            new RaycastContext(
               vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, this
            )
         );
   }

   public boolean canBeHitByProjectile() {
      return this.isAlive() && this.canHit();
   }

   public boolean canHit() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   public void updateKilledAdvancementCriterion(Entity entityKilled, DamageSource damageSource) {
      if (entityKilled instanceof ServerPlayerEntity) {
         Criteria.ENTITY_KILLED_PLAYER.trigger((ServerPlayerEntity)entityKilled, this, damageSource);
      }
   }

   public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
      double d = this.getX() - cameraX;
      double e = this.getY() - cameraY;
      double f = this.getZ() - cameraZ;
      double g = d * d + e * e + f * f;
      return this.shouldRender(g);
   }

   public boolean shouldRender(double distance) {
      double d = this.getBoundingBox().getAverageSideLength();
      if (Double.isNaN(d)) {
         d = 1.0;
      }

      d *= 64.0 * renderDistanceMultiplier;
      return distance < d * d;
   }

   public boolean saveSelfNbt(NbtCompound nbt) {
      if (this.removalReason != null && !this.removalReason.shouldSave()) {
         return false;
      }

      String string = this.getSavedEntityId();
      if (string == null) {
         return false;
      }

      nbt.putString("id", string);
      this.writeNbt(nbt);
      return true;
   }

   public boolean saveNbt(NbtCompound nbt) {
      return this.hasVehicle() ? false : this.saveSelfNbt(nbt);
   }

   public NbtCompound writeNbt(NbtCompound nbt) {
      try {
         if (this.vehicle != null) {
            nbt.put("Pos", this.toNbtList(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
         } else {
            nbt.put("Pos", this.toNbtList(this.getX(), this.getY(), this.getZ()));
         }

         Vec3d vec3d = this.getVelocity();
         nbt.put("Motion", this.toNbtList(vec3d.x, vec3d.y, vec3d.z));
         nbt.put("Rotation", this.toNbtList(this.getYaw(), this.getPitch()));
         nbt.putFloat("FallDistance", this.fallDistance);
         nbt.putShort("Fire", (short)this.fireTicks);
         nbt.putShort("Air", (short)this.getAir());
         nbt.putBoolean("OnGround", this.isOnGround());
         nbt.putBoolean("Invulnerable", this.invulnerable);
         nbt.putInt("PortalCooldown", this.portalCooldown);
         nbt.putUuid("UUID", this.getUuid());
         Text text = this.getCustomName();
         if (text != null) {
            nbt.putString("CustomName", Text.Serialization.toJsonString(text, this.getRegistryManager()));
         }

         if (this.isCustomNameVisible()) {
            nbt.putBoolean("CustomNameVisible", this.isCustomNameVisible());
         }

         if (this.isSilent()) {
            nbt.putBoolean("Silent", this.isSilent());
         }

         if (this.hasNoGravity()) {
            nbt.putBoolean("NoGravity", this.hasNoGravity());
         }

         if (this.glowing) {
            nbt.putBoolean("Glowing", true);
         }

         int i = this.getFrozenTicks();
         if (i > 0) {
            nbt.putInt("TicksFrozen", this.getFrozenTicks());
         }

         if (this.hasVisualFire) {
            nbt.putBoolean("HasVisualFire", this.hasVisualFire);
         }

         if (!this.commandTags.isEmpty()) {
            NbtList nbtList = new NbtList();

            for (String string : this.commandTags) {
               nbtList.add(NbtString.of(string));
            }

            nbt.put("Tags", nbtList);
         }

         this.writeCustomDataToNbt(nbt);
         if (this.hasPassengers()) {
            NbtList nbtList = new NbtList();

            for (Entity entity : this.getPassengerList()) {
               NbtCompound nbtCompound = new NbtCompound();
               if (entity.saveSelfNbt(nbtCompound)) {
                  nbtList.add(nbtCompound);
               }
            }

            if (!nbtList.isEmpty()) {
               nbt.put("Passengers", nbtList);
            }
         }

         return nbt;
      } catch (Throwable throwable) {
         CrashReport crashReport = CrashReport.create(throwable, "Saving entity NBT");
         CrashReportSection crashReportSection = crashReport.addElement("Entity being saved");
         this.populateCrashReport(crashReportSection);
         throw new CrashException(crashReport);
      }
   }

   public void readNbt(NbtCompound nbt) {
      try {
         NbtList nbtList = nbt.getList("Pos", 6);
         NbtList nbtList2 = nbt.getList("Motion", 6);
         NbtList nbtList3 = nbt.getList("Rotation", 5);
         double d = nbtList2.getDouble(0);
         double e = nbtList2.getDouble(1);
         double f = nbtList2.getDouble(2);
         this.setVelocity(Math.abs(d) > 10.0 ? 0.0 : d, Math.abs(e) > 10.0 ? 0.0 : e, Math.abs(f) > 10.0 ? 0.0 : f);
         this.velocityDirty = true;
         double g = 3.0000512E7;
         this.setPos(
            MathHelper.clamp(nbtList.getDouble(0), -3.0000512E7, 3.0000512E7),
            MathHelper.clamp(nbtList.getDouble(1), -2.0E7, 2.0E7),
            MathHelper.clamp(nbtList.getDouble(2), -3.0000512E7, 3.0000512E7)
         );
         this.setYaw(nbtList3.getFloat(0));
         this.setPitch(nbtList3.getFloat(1));
         this.resetPosition();
         this.setHeadYaw(this.getYaw());
         this.setBodyYaw(this.getYaw());
         this.fallDistance = nbt.getFloat("FallDistance");
         this.fireTicks = nbt.getShort("Fire");
         if (nbt.contains("Air")) {
            this.setAir(nbt.getShort("Air"));
         }

         this.onGround = nbt.getBoolean("OnGround");
         this.invulnerable = nbt.getBoolean("Invulnerable");
         this.portalCooldown = nbt.getInt("PortalCooldown");
         if (nbt.containsUuid("UUID")) {
            this.uuid = nbt.getUuid("UUID");
            this.uuidString = this.uuid.toString();
            if (this instanceof ItemEntity) {
               this.changeListener.updateEntityPosition();
            }
         }

         if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
            throw new IllegalStateException("Entity has invalid position");
         }

         if (Double.isFinite(this.getYaw()) && Double.isFinite(this.getPitch())) {
            this.refreshPosition();
            this.setRotation(this.getYaw(), this.getPitch());
            if (nbt.contains("CustomName", 8)) {
               String string = nbt.getString("CustomName");

               try {
                  this.setCustomName(Text.Serialization.fromJson(string, this.getRegistryManager()));
               } catch (Exception exception) {
                  LOGGER.warn("Failed to parse entity custom name {}", string, exception);
               }
            } else {
               this.dataTracker.set(CUSTOM_NAME, Optional.empty());
            }

            this.setCustomNameVisible(nbt.getBoolean("CustomNameVisible"));
            this.setSilent(nbt.getBoolean("Silent"));
            this.setNoGravity(nbt.getBoolean("NoGravity"));
            this.setGlowing(nbt.getBoolean("Glowing"));
            this.setFrozenTicks(nbt.getInt("TicksFrozen"));
            this.hasVisualFire = nbt.getBoolean("HasVisualFire");
            if (nbt.contains("Tags", 9)) {
               this.commandTags.clear();
               NbtList nbtList4 = nbt.getList("Tags", 8);
               int i = Math.min(nbtList4.size(), 1024);

               for (int j = 0; j < i; j++) {
                  this.commandTags.add(nbtList4.getString(j));
               }
            }

            this.readCustomDataFromNbt(nbt);
            if (this.shouldSetPositionOnLoad()) {
               this.refreshPosition();
            }
         } else {
            throw new IllegalStateException("Entity has invalid rotation");
         }
      } catch (Throwable throwable) {
         CrashReport crashReport = CrashReport.create(throwable, "Loading entity NBT");
         CrashReportSection crashReportSection = crashReport.addElement("Entity being loaded");
         this.populateCrashReport(crashReportSection);
         throw new CrashException(crashReport);
      }
   }

   protected boolean shouldSetPositionOnLoad() {
      return true;
   }

   @Nullable
   protected final String getSavedEntityId() {
      EntityType<?> entityType = this.getType();
      Identifier identifier = EntityType.getId(entityType);
      return entityType.isSaveable() && identifier != null ? identifier.toString() : null;
   }

   protected abstract void readCustomDataFromNbt(NbtCompound nbt);

   protected abstract void writeCustomDataToNbt(NbtCompound nbt);

   protected NbtList toNbtList(double... values) {
      NbtList nbtList = new NbtList();

      for (double d : values) {
         nbtList.add(NbtDouble.of(d));
      }

      return nbtList;
   }

   protected NbtList toNbtList(float... values) {
      NbtList nbtList = new NbtList();

      for (float f : values) {
         nbtList.add(NbtFloat.of(f));
      }

      return nbtList;
   }

   @Nullable
   public ItemEntity dropItem(ServerWorld world, ItemConvertible item) {
      return this.dropItem(world, item, 0);
   }

   @Nullable
   public ItemEntity dropItem(ServerWorld world, ItemConvertible item, int offsetY) {
      return this.dropStack(world, new ItemStack(item), offsetY);
   }

   @Nullable
   public ItemEntity dropStack(ServerWorld world, ItemStack stack) {
      return this.dropStack(world, stack, 0.0F);
   }

   @Nullable
   public ItemEntity dropStack(ServerWorld world, ItemStack stack, float yOffset) {
      if (stack.isEmpty()) {
         return null;
      }

      ItemEntity itemEntity = new ItemEntity(world, this.getX(), this.getY() + yOffset, this.getZ(), stack);
      itemEntity.setToDefaultPickupDelay();
      world.spawnEntity(itemEntity);
      return itemEntity;
   }

   public boolean isAlive() {
      return !this.isRemoved();
   }

   public boolean isInsideWall() {
      if (this.noClip) {
         return false;
      }

      float f = this.dimensions.width() * 0.8F;
      Box box = Box.of(this.getEyePos(), f, 1.0E-6, f);
      return BlockPos.stream(box)
         .anyMatch(
            pos -> {
               BlockState blockState = this.getWorld().getBlockState(pos);
               return !blockState.isAir()
                  && blockState.shouldSuffocate(this.getWorld(), pos)
                  && VoxelShapes.matchesAnywhere(
                     blockState.getCollisionShape(this.getWorld(), pos).offset(pos.getX(), pos.getY(), pos.getZ()),
                     VoxelShapes.cuboid(box),
                     BooleanBiFunction.AND
                  );
            }
         );
   }

   public ActionResult interact(PlayerEntity player, Hand hand) {
      if (this.isAlive() && this instanceof Leashable leashable) {
         if (leashable.getLeashHolder() == player) {
            if (!this.getWorld().isClient()) {
               if (player.isInCreativeMode()) {
                  leashable.detachLeashWithoutDrop();
               } else {
                  leashable.detachLeash();
               }

               this.emitGameEvent(GameEvent.ENTITY_INTERACT, player);
            }

            return ActionResult.SUCCESS.noIncrementStat();
         }

         ItemStack itemStack = player.getStackInHand(hand);
         if (itemStack.isOf(Items.LEAD) && leashable.canLeashAttachTo()) {
            if (!this.getWorld().isClient()) {
               leashable.attachLeash(player, true);
            }

            itemStack.decrement(1);
            return ActionResult.SUCCESS;
         }
      }

      return ActionResult.PASS;
   }

   public boolean collidesWith(Entity other) {
      return other.isCollidable() && !this.isConnectedThroughVehicle(other);
   }

   public boolean isCollidable() {
      return false;
   }

   public void tickRiding() {
      this.setVelocity(Vec3d.ZERO);
      this.tick();
      if (this.hasVehicle()) {
         this.getVehicle().updatePassengerPosition(this);
      }
   }

   public final void updatePassengerPosition(Entity passenger) {
      if (this.hasPassenger(passenger)) {
         this.updatePassengerPosition(passenger, Entity::setPosition);
      }
   }

   protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
      Vec3d vec3d = this.getPassengerRidingPos(passenger);
      Vec3d vec3d2 = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)
         ? new Vec3d(0.0, -EntityRidingOffsetsPre1_20_2.getHeightOffset(passenger), 0.0)
         : passenger.getVehicleAttachmentPos(this);
      positionUpdater.accept(passenger, vec3d.x - vec3d2.x, vec3d.y - vec3d2.y, vec3d.z - vec3d2.z);
   }

   public void onPassengerLookAround(Entity passenger) {
   }

   public Vec3d getVehicleAttachmentPos(Entity vehicle) {
      return this.getAttachments().getPoint(EntityAttachmentType.VEHICLE, 0, this.yaw);
   }

   public Vec3d getPassengerRidingPos(Entity passenger) {
      Vec3d attachmentPos = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)
         ? EntityRidingOffsetsPre1_20_2.getMountedHeightOffset(this, passenger).rotateY(-this.getYaw() * (float) (Math.PI / 180.0))
         : this.getPassengerAttachmentPos(passenger, this.dimensions, 1.0F);
      return this.getPos().add(attachmentPos);
   }

   protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
      return getPassengerAttachmentPos(this, passenger, dimensions.attachments());
   }

   protected static Vec3d getPassengerAttachmentPos(Entity vehicle, Entity passenger, EntityAttachments attachments) {
      int i = vehicle.getPassengerList().indexOf(passenger);
      return attachments.getPointOrDefault(EntityAttachmentType.PASSENGER, i, vehicle.yaw);
   }

   public boolean startRiding(Entity entity) {
      return this.startRiding(entity, false);
   }

   public boolean isLiving() {
      return this instanceof LivingEntity;
   }

   public boolean startRiding(Entity entity, boolean force) {
      if (entity == this.vehicle) {
         return false;
      }

      if (!entity.couldAcceptPassenger()) {
         return false;
      }

      if (!this.getWorld().isClient() && !entity.type.isSaveable()) {
         return false;
      }

      for (Entity entity2 = entity; entity2.vehicle != null; entity2 = entity2.vehicle) {
         if (entity2.vehicle == this) {
            return false;
         }
      }

      if (force || this.canStartRiding(entity) && entity.canAddPassenger(this)) {
         if (this.hasVehicle()) {
            this.stopRiding();
         }

         this.setPose(EntityPose.STANDING);
         this.vehicle = entity;
         this.vehicle.addPassenger(this);
         entity.streamIntoPassengers()
            .filter(passenger -> passenger instanceof ServerPlayerEntity)
            .forEach(player -> Criteria.STARTED_RIDING.trigger((ServerPlayerEntity)player));
         return true;
      } else {
         return false;
      }
   }

   protected boolean canStartRiding(Entity entity) {
      return !this.isSneaking() && this.ridingCooldown <= 0;
   }

   public void removeAllPassengers() {
      for (int i = this.passengerList.size() - 1; i >= 0; i--) {
         ((Entity)this.passengerList.get(i)).stopRiding();
      }
   }

   public void dismountVehicle() {
      if (this.vehicle != null) {
         Entity entity = this.vehicle;
         this.vehicle = null;
         entity.removePassenger(this);
      }
   }

   public void stopRiding() {
      this.dismountVehicle();
   }

   protected void addPassenger(Entity passenger) {
      if (passenger.getVehicle() != this) {
         throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
      }

      if (this.passengerList.isEmpty()) {
         this.passengerList = ImmutableList.of(passenger);
      } else {
         List<Entity> list = Lists.newArrayList(this.passengerList);
         if (!this.getWorld().isClient && passenger instanceof PlayerEntity && !(this.getFirstPassenger() instanceof PlayerEntity)) {
            list.add(0, passenger);
         } else {
            list.add(passenger);
         }

         this.passengerList = ImmutableList.copyOf(list);
      }

      this.emitGameEvent(GameEvent.ENTITY_MOUNT, passenger);
   }

   protected void removePassenger(Entity passenger) {
      if (passenger.getVehicle() == this) {
         throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
      }

      if (this.passengerList.size() == 1 && this.passengerList.get(0) == passenger) {
         this.passengerList = ImmutableList.of();
      } else {
         this.passengerList = this.passengerList.stream().filter(entity -> entity != passenger).collect(ImmutableList.toImmutableList());
      }

      passenger.ridingCooldown = 60;
      this.emitGameEvent(GameEvent.ENTITY_DISMOUNT, passenger);
   }

   protected boolean canAddPassenger(Entity passenger) {
      return this.passengerList.isEmpty();
   }

   protected boolean couldAcceptPassenger() {
      return true;
   }

   public void resetLerp() {
   }

   public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
      this.setPosition(x, y, z);
      this.setRotation(yaw, pitch);
   }

   public double getLerpTargetX() {
      return this.getX();
   }

   public double getLerpTargetY() {
      return this.getY();
   }

   public double getLerpTargetZ() {
      return this.getZ();
   }

   public float getLerpTargetPitch() {
      return this.getPitch();
   }

   public float getLerpTargetYaw() {
      return this.getYaw();
   }

   public void updateTrackedHeadRotation(float yaw, int interpolationSteps) {
      this.setHeadYaw(yaw);
   }

   public float getTargetingMargin() {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) ? 0.1F : 0.0F;
   }

   public Vec3d getRotationVector() {
      return this.getRotationVector(this.getPitch(), this.getYaw());
   }

   public Vec3d getHandPosOffset(Item item) {
      if (!(this instanceof PlayerEntity playerEntity)) {
         return Vec3d.ZERO;
      } else {
         boolean bl = playerEntity.getOffHandStack().isOf(item) && !playerEntity.getMainHandStack().isOf(item);
         Arm arm = bl ? playerEntity.getMainArm().getOpposite() : playerEntity.getMainArm();
         return this.getRotationVector(0.0F, this.getYaw() + (arm == Arm.RIGHT ? 80 : -80)).multiply(0.5);
      }
   }

   public Vec2f getRotationClient() {
      return new Vec2f(this.getPitch(), this.getYaw());
   }

   public Vec3d getRotationVecClient() {
      return Vec3d.fromPolar(this.getRotationClient());
   }

   public void tryUsePortal(Portal portal, BlockPos pos) {
      if (this.hasPortalCooldown()) {
         this.resetPortalCooldown();
      } else {
         if (this.portalManager == null || !this.portalManager.portalMatches(portal)) {
            this.portalManager = new PortalManager(portal, pos.toImmutable());
         } else if (!this.portalManager.isInPortal()) {
            this.portalManager.setPortalPos(pos.toImmutable());
            this.portalManager.setInPortal(true);
         }
      }
   }

   protected void tickPortalTeleportation() {
      if (this.getWorld() instanceof ServerWorld serverWorld) {
         this.tickPortalCooldown();
         if (this.portalManager != null) {
            if (this.portalManager.tick(serverWorld, this, this.canUsePortals(false))) {
               Profiler profiler = Profilers.get();
               profiler.push("portal");
               this.resetPortalCooldown();
               TeleportTarget teleportTarget = this.portalManager.createTeleportTarget(serverWorld, this);
               if (teleportTarget != null) {
                  ServerWorld serverWorld2 = teleportTarget.world();
                  if (serverWorld.getServer().isWorldAllowed(serverWorld2)
                     && (serverWorld2.getRegistryKey() == serverWorld.getRegistryKey() || this.canTeleportBetween(serverWorld, serverWorld2))) {
                     this.teleportTo(teleportTarget);
                  }
               }

               profiler.pop();
            } else if (this.portalManager.hasExpired()) {
               this.portalManager = null;
            }
         }
      }
   }

   public int getDefaultPortalCooldown() {
      Entity entity = this.getFirstPassenger();
      return entity instanceof ServerPlayerEntity ? entity.getDefaultPortalCooldown() : 300;
   }

   public void setVelocityClient(double x, double y, double z) {
      this.setVelocity(x, y, z);
   }

   public void onDamaged(DamageSource damageSource) {
   }

   public void handleStatus(byte status) {
      switch (status) {
         case 53:
            HoneyBlock.addRegularParticles(this);
      }
   }

   public void animateDamage(float yaw) {
   }

   public boolean isOnFire() {
      boolean bl = this.getWorld() != null && this.getWorld().isClient;
      return !this.isFireImmune() && (this.fireTicks > 0 || bl && this.getFlag(0));
   }

   public boolean hasVehicle() {
      return this.getVehicle() != null;
   }

   public boolean hasPassengers() {
      return !this.passengerList.isEmpty();
   }

   public boolean shouldDismountUnderwater() {
      return this.getType().isIn(EntityTypeTags.DISMOUNTS_UNDERWATER);
   }

   public boolean shouldControlVehicles() {
      return !this.getType().isIn(EntityTypeTags.NON_CONTROLLING_RIDER);
   }

   public void setSneaking(boolean sneaking) {
      this.setFlag(1, sneaking);
   }

   public boolean isSneaking() {
      return this.getFlag(1);
   }

   public boolean bypassesSteppingEffects() {
      return this.isSneaking();
   }

   public boolean bypassesLandingEffects() {
      return this.isSneaking();
   }

   public boolean isSneaky() {
      return this.isSneaking();
   }

   public boolean isDescending() {
      return this.isSneaking();
   }

   public boolean isInSneakingPose() {
      return this.isInPose(EntityPose.CROUCHING);
   }

   public boolean isSprinting() {
      return this.getFlag(3);
   }

   public void setSprinting(boolean sprinting) {
      this.setFlag(3, sprinting);
   }

   public boolean isSwimming() {
      return this.getFlag(4);
   }

   public boolean isInSwimmingPose() {
      return this.isInPose(EntityPose.SWIMMING);
   }

   public boolean isCrawling() {
      return this.isInSwimmingPose() && !this.isTouchingWater();
   }

   public void setSwimming(boolean swimming) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) && swimming) {
         return;
      }

      this.setFlag(4, swimming);
   }

   public final boolean isGlowingLocal() {
      return this.glowing;
   }

   public final void setGlowing(boolean glowing) {
      this.glowing = glowing;
      this.setFlag(6, this.isGlowing());
   }

   public boolean isGlowing() {
      return this.getWorld().isClient() ? this.getFlag(6) : this.glowing;
   }

   public boolean isInvisible() {
      return this.getFlag(5);
   }

   public boolean isInvisibleTo(PlayerEntity player) {
      if (player.isSpectator()) {
         return false;
      }

      AbstractTeam abstractTeam = this.getScoreboardTeam();
      return abstractTeam != null && player != null && player.getScoreboardTeam() == abstractTeam && abstractTeam.shouldShowFriendlyInvisibles()
         ? false
         : this.isInvisible();
   }

   public boolean isOnRail() {
      return false;
   }

   public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
   }

   @Nullable
   public Team getScoreboardTeam() {
      return this.getWorld().getScoreboard().getScoreHolderTeam(this.getNameForScoreboard());
   }

   public final boolean isTeammate(@Nullable Entity other) {
      return other == null ? false : this == other || this.isInSameTeam(other) || other.isInSameTeam(this);
   }

   public boolean isInSameTeam(Entity other) {
      return this.isTeamPlayer(other.getScoreboardTeam());
   }

   public boolean isTeamPlayer(@Nullable AbstractTeam team) {
      return this.getScoreboardTeam() != null ? this.getScoreboardTeam().isEqual(team) : false;
   }

   public void setInvisible(boolean invisible) {
      this.setFlag(5, invisible);
   }

   protected boolean getFlag(int index) {
      return (this.dataTracker.get(FLAGS) & 1 << index) != 0;
   }

   public void setFlag(int index, boolean value) {
      byte b = this.dataTracker.get(FLAGS);
      if (value) {
         this.dataTracker.set(FLAGS, (byte)(b | 1 << index));
      } else {
         this.dataTracker.set(FLAGS, (byte)(b & ~(1 << index)));
      }
   }

   public int getMaxAir() {
      return 300;
   }

   public int getAir() {
      return this.dataTracker.get(AIR);
   }

   public void setAir(int air) {
      this.dataTracker.set(AIR, air);
   }

   public int getFrozenTicks() {
      return this.dataTracker.get(FROZEN_TICKS);
   }

   public void setFrozenTicks(int frozenTicks) {
      this.dataTracker.set(FROZEN_TICKS, frozenTicks);
   }

   public float getFreezingScale() {
      int i = this.getMinFreezeDamageTicks();
      return (float)Math.min(this.getFrozenTicks(), i) / i;
   }

   public boolean isFrozen() {
      return this.getFrozenTicks() >= this.getMinFreezeDamageTicks();
   }

   public int getMinFreezeDamageTicks() {
      return 140;
   }

   public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
      this.setFireTicks(this.fireTicks + 1);
      if (this.fireTicks == 0) {
         this.setOnFireFor(8.0F);
      }

      this.damage(world, this.getDamageSources().lightningBolt(), 5.0F);
   }

   public void onBubbleColumnSurfaceCollision(boolean drag) {
      Vec3d vec3d = this.getVelocity();
      double d;
      if (drag) {
         d = Math.max(-0.9, vec3d.y - 0.03);
      } else {
         d = Math.min(1.8, vec3d.y + 0.1);
      }

      this.setVelocity(vec3d.x, d, vec3d.z);
   }

   public void onBubbleColumnCollision(boolean drag) {
      Vec3d vec3d = this.getVelocity();
      double d;
      if (drag) {
         d = Math.max(-0.3, vec3d.y - 0.03);
      } else {
         d = Math.min(0.7, vec3d.y + 0.06);
      }

      this.setVelocity(vec3d.x, d, vec3d.z);
      this.onLanding();
   }

   public boolean onKilledOther(ServerWorld world, LivingEntity other) {
      return true;
   }

   public void limitFallDistance() {
      if (this.getVelocity().getY() > -0.5 && this.fallDistance > 1.0F) {
         this.fallDistance = 1.0F;
      }
   }

   public void onLanding() {
      this.fallDistance = 0.0F;
   }

   protected void pushOutOfBlocks(double x, double y, double z) {
      BlockPos blockPos = BlockPos.ofFloored(x, y, z);
      Vec3d vec3d = new Vec3d(x - blockPos.getX(), y - blockPos.getY(), z - blockPos.getZ());
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      Direction direction = Direction.UP;
      double d = Double.MAX_VALUE;

      for (Direction direction2 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
         mutable.set(blockPos, direction2);
         if (!this.getWorld().getBlockState(mutable).isFullCube(this.getWorld(), mutable)) {
            double e = vec3d.getComponentAlongAxis(direction2.getAxis());
            double f = direction2.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - e : e;
            if (f < d) {
               d = f;
               direction = direction2;
            }
         }
      }

      float g = this.random.nextFloat() * 0.2F + 0.1F;
      float h = direction.getDirection().offset();
      Vec3d vec3d2 = this.getVelocity().multiply(0.75);
      if (direction.getAxis() == Direction.Axis.X) {
         this.setVelocity(h * g, vec3d2.y, vec3d2.z);
      } else if (direction.getAxis() == Direction.Axis.Y) {
         this.setVelocity(vec3d2.x, h * g, vec3d2.z);
      } else if (direction.getAxis() == Direction.Axis.Z) {
         this.setVelocity(vec3d2.x, vec3d2.y, h * g);
      }
   }

   public void slowMovement(BlockState state, Vec3d multiplier) {
      this.onLanding();
      this.movementMultiplier = multiplier;
   }

   private static Text removeClickEvents(Text textComponent) {
      MutableText mutableText = textComponent.copyContentOnly().setStyle(textComponent.getStyle().withClickEvent(null));

      for (Text text : textComponent.getSiblings()) {
         mutableText.append(removeClickEvents(text));
      }

      return mutableText;
   }

   @Override
   public Text getName() {
      Text text = this.getCustomName();
      return text != null ? removeClickEvents(text) : this.getDefaultName();
   }

   protected Text getDefaultName() {
      return this.type.getName();
   }

   public boolean isPartOf(Entity entity) {
      return this == entity;
   }

   public float getHeadYaw() {
      return 0.0F;
   }

   public void setHeadYaw(float headYaw) {
   }

   public void setBodyYaw(float bodyYaw) {
   }

   public boolean isAttackable() {
      return true;
   }

   public boolean handleAttack(Entity attacker) {
      return false;
   }

   @Override
   public String toString() {
      String string = this.getWorld() == null ? "~NULL~" : this.getWorld().toString();
      return this.removalReason != null
         ? String.format(
            Locale.ROOT,
            "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
            this.getClass().getSimpleName(),
            this.getName().getString(),
            this.id,
            string,
            this.getX(),
            this.getY(),
            this.getZ(),
            this.removalReason
         )
         : String.format(
            Locale.ROOT,
            "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
            this.getClass().getSimpleName(),
            this.getName().getString(),
            this.id,
            string,
            this.getX(),
            this.getY(),
            this.getZ()
         );
   }

   protected final boolean isAlwaysInvulnerableTo(DamageSource damageSource) {
      return this.isRemoved()
         || this.invulnerable && !damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !damageSource.isSourceCreativePlayer()
         || damageSource.isIn(DamageTypeTags.IS_FIRE) && this.isFireImmune()
         || damageSource.isIn(DamageTypeTags.IS_FALL) && this.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE);
   }

   public boolean isInvulnerable() {
      return this.invulnerable;
   }

   public void setInvulnerable(boolean invulnerable) {
      this.invulnerable = invulnerable;
   }

   public void copyPositionAndRotation(Entity entity) {
      this.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
   }

   public void copyFrom(Entity original) {
      NbtCompound nbtCompound = original.writeNbt(new NbtCompound());
      nbtCompound.remove("Dimension");
      this.readNbt(nbtCompound);
      this.portalCooldown = original.portalCooldown;
      this.portalManager = original.portalManager;
   }

   @Nullable
   public Entity teleportTo(TeleportTarget teleportTarget) {
      if (this.getWorld() instanceof ServerWorld serverWorld && !this.isRemoved()) {
         ServerWorld serverWorld2 = teleportTarget.world();
         boolean bl = serverWorld2.getRegistryKey() != serverWorld.getRegistryKey();
         if (!teleportTarget.asPassenger()) {
            this.stopRiding();
         }

         return bl ? this.teleportCrossDimension(serverWorld2, teleportTarget) : this.teleportSameDimension(serverWorld, teleportTarget);
      } else {
         return null;
      }
   }

   private Entity teleportSameDimension(ServerWorld world, TeleportTarget teleportTarget) {
      for (Entity entity : this.getPassengerList()) {
         entity.teleportTo(this.getPassengerTeleportTarget(teleportTarget, entity));
      }

      Profiler profiler = Profilers.get();
      profiler.push("teleportSameDimension");
      this.setPosition(PlayerPosition.fromTeleportTarget(teleportTarget), teleportTarget.relatives());
      if (!teleportTarget.asPassenger()) {
         this.sendTeleportPacket(teleportTarget);
      }

      teleportTarget.postTeleportTransition().onTransition(this);
      profiler.pop();
      return this;
   }

   private Entity teleportCrossDimension(ServerWorld world, TeleportTarget teleportTarget) {
      List<Entity> list = this.getPassengerList();
      List<Entity> list2 = new ArrayList<>(list.size());
      this.removeAllPassengers();

      for (Entity entity : list) {
         Entity entity2 = entity.teleportTo(this.getPassengerTeleportTarget(teleportTarget, entity));
         if (entity2 != null) {
            list2.add(entity2);
         }
      }

      Profiler profiler = Profilers.get();
      profiler.push("teleportCrossDimension");
      Entity entity = this.getType().create(world, SpawnReason.DIMENSION_TRAVEL);
      if (entity == null) {
         profiler.pop();
         return null;
      }

      entity.copyFrom(this);
      this.removeFromDimension();
      entity.setPosition(PlayerPosition.fromTeleportTarget(teleportTarget), teleportTarget.relatives());
      world.onDimensionChanged(entity);

      for (Entity entity3 : list2) {
         entity3.startRiding(entity, true);
      }

      world.resetIdleTimeout();
      teleportTarget.postTeleportTransition().onTransition(entity);
      profiler.pop();
      return entity;
   }

   private TeleportTarget getPassengerTeleportTarget(TeleportTarget teleportTarget, Entity passenger) {
      float f = teleportTarget.yaw() + (teleportTarget.relatives().contains(PositionFlag.Y_ROT) ? 0.0F : passenger.getYaw() - this.getYaw());
      float g = teleportTarget.pitch() + (teleportTarget.relatives().contains(PositionFlag.X_ROT) ? 0.0F : passenger.getPitch() - this.getPitch());
      Vec3d vec3d = passenger.getPos().subtract(this.getPos());
      Vec3d vec3d2 = teleportTarget.position()
         .add(
            teleportTarget.relatives().contains(PositionFlag.X) ? 0.0 : vec3d.getX(),
            teleportTarget.relatives().contains(PositionFlag.Y) ? 0.0 : vec3d.getY(),
            teleportTarget.relatives().contains(PositionFlag.Z) ? 0.0 : vec3d.getZ()
         );
      return teleportTarget.withPosition(vec3d2).withRotation(f, g).asPassengerTeleportTarget();
   }

   private void sendTeleportPacket(TeleportTarget teleportTarget) {
      Entity entity = this.getControllingPassenger();

      for (Entity entity2 : this.getPassengersDeep()) {
         if (entity2 instanceof ServerPlayerEntity serverPlayerEntity) {
            if (entity != null && serverPlayerEntity.getId() == entity.getId()) {
               serverPlayerEntity.networkHandler
                  .sendPacket(
                     EntityPositionS2CPacket.create(this.getId(), PlayerPosition.fromTeleportTarget(teleportTarget), teleportTarget.relatives(), this.onGround)
                  );
            } else {
               serverPlayerEntity.networkHandler
                  .sendPacket(EntityPositionS2CPacket.create(this.getId(), PlayerPosition.fromEntity(this), Set.of(), this.onGround));
            }
         }
      }
   }

   public void setPosition(PlayerPosition pos, Set<PositionFlag> flags) {
      PlayerPosition playerPosition = PlayerPosition.fromEntity(this);
      PlayerPosition playerPosition2 = PlayerPosition.apply(playerPosition, pos, flags);
      this.setPos(playerPosition2.position().x, playerPosition2.position().y, playerPosition2.position().z);
      this.setYaw(playerPosition2.yaw());
      this.setHeadYaw(playerPosition2.yaw());
      this.setPitch(playerPosition2.pitch());
      this.refreshPosition();
      this.resetPosition();
      this.setVelocity(playerPosition2.deltaMovement());
      this.queuedCollisionChecks.clear();
   }

   public void rotate(float yaw, float pitch) {
      this.setYaw(yaw);
      this.setHeadYaw(yaw);
      this.setPitch(pitch);
      this.updatePrevAngles();
   }

   public void addPortalChunkTicketAt(BlockPos pos) {
      if (this.getWorld() instanceof ServerWorld serverWorld) {
         serverWorld.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(pos), 3, pos);
      }
   }

   protected void removeFromDimension() {
      this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
      if (this instanceof Leashable leashable) {
         leashable.detachLeashWithoutDrop();
      }
   }

   public Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
      return NetherPortal.entityPosInPortal(portalRect, portalAxis, this.getPos(), this.getDimensions(this.getPose()));
   }

   public boolean canUsePortals(boolean allowVehicles) {
      return (allowVehicles || !this.hasVehicle()) && this.isAlive();
   }

   public boolean canTeleportBetween(World from, World to) {
      if (from.getRegistryKey() == World.END && to.getRegistryKey() == World.OVERWORLD) {
         for (Entity entity : this.getPassengerList()) {
            if (entity instanceof ServerPlayerEntity serverPlayerEntity && !serverPlayerEntity.seenCredits) {
               return false;
            }
         }
      }

      return true;
   }

   public float getEffectiveExplosionResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState, float max) {
      return max;
   }

   public boolean canExplosionDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float explosionPower) {
      return true;
   }

   public int getSafeFallDistance() {
      return 3;
   }

   public boolean canAvoidTraps() {
      return false;
   }

   public void populateCrashReport(CrashReportSection section) {
      section.add("Entity Type", () -> EntityType.getId(this.getType()) + " (" + this.getClass().getCanonicalName() + ")");
      section.add("Entity ID", this.id);
      section.add("Entity Name", () -> this.getName().getString());
      section.add("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
      section.add(
         "Entity's Block location",
         CrashReportSection.createPositionString(this.getWorld(), MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()))
      );
      Vec3d vec3d = this.getVelocity();
      section.add("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3d.x, vec3d.y, vec3d.z));
      section.add("Entity's Passengers", () -> this.getPassengerList().toString());
      section.add("Entity's Vehicle", () -> String.valueOf(this.getVehicle()));
   }

   public boolean doesRenderOnFire() {
      return this.isOnFire() && !this.isSpectator();
   }

   public void setUuid(UUID uuid) {
      this.uuid = uuid;
      this.uuidString = this.uuid.toString();
   }

   @Override
   public UUID getUuid() {
      return this.uuid;
   }

   public String getUuidAsString() {
      return this.uuidString;
   }

   @Override
   public String getNameForScoreboard() {
      return this.uuidString;
   }

   public boolean isPushedByFluids() {
      return true;
   }

   public static double getRenderDistanceMultiplier() {
      return renderDistanceMultiplier;
   }

   public static void setRenderDistanceMultiplier(double value) {
      renderDistanceMultiplier = value;
   }

   @Override
   public Text getDisplayName() {
      return Team.decorateName(this.getScoreboardTeam(), this.getName())
         .styled(style -> style.withHoverEvent(this.getHoverEvent()).withInsertion(this.getUuidAsString()));
   }

   public void setCustomName(@Nullable Text name) {
      this.dataTracker.set(CUSTOM_NAME, Optional.ofNullable(name));
   }

   @Nullable
   @Override
   public Text getCustomName() {
      return this.dataTracker.get(CUSTOM_NAME).orElse(null);
   }

   @Override
   public boolean hasCustomName() {
      return this.dataTracker.get(CUSTOM_NAME).isPresent();
   }

   public void setCustomNameVisible(boolean visible) {
      this.dataTracker.set(NAME_VISIBLE, visible);
   }

   public boolean isCustomNameVisible() {
      return this.dataTracker.get(NAME_VISIBLE);
   }

   public boolean teleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, boolean resetCamera) {
      float f = MathHelper.clamp(pitch, -90.0F, 90.0F);
      Entity entity = this.teleportTo(new TeleportTarget(world, new Vec3d(destX, destY, destZ), Vec3d.ZERO, yaw, f, flags, TeleportTarget.NO_OP));
      return entity != null;
   }

   public void requestTeleportAndDismount(double destX, double destY, double destZ) {
      this.requestTeleport(destX, destY, destZ);
   }

   public void requestTeleport(double destX, double destY, double destZ) {
      if (this.getWorld() instanceof ServerWorld) {
         this.refreshPositionAndAngles(destX, destY, destZ, this.getYaw(), this.getPitch());
         this.teleportPassengers();
      }
   }

   private void teleportPassengers() {
      this.streamSelfAndPassengers().forEach(entity -> {
         UnmodifiableIterator var1 = entity.passengerList.iterator();

         while (var1.hasNext()) {
            Entity entity2 = (Entity)var1.next();
            entity.updatePassengerPosition(entity2, Entity::refreshPositionAfterTeleport);
         }
      });
   }

   public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ) {
      this.requestTeleport(this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
   }

   public boolean shouldRenderName() {
      return this.isCustomNameVisible();
   }

   @Override
   public void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> entries) {
   }

   @Override
   public void onTrackedDataSet(TrackedData<?> data) {
      if (POSE.equals(data)) {
         this.calculateDimensions();
      }
   }

   @Deprecated
   protected void reinitDimensions() {
      EntityPose entityPose = this.getPose();
      EntityDimensions entityDimensions = this.getDimensions(entityPose);
      this.dimensions = entityDimensions;
      this.standingEyeHeight = entityDimensions.eyeHeight();
   }

   public void calculateDimensions() {
      EntityDimensions entityDimensions = this.dimensions;
      EntityPose entityPose = this.getPose();
      EntityDimensions entityDimensions2 = this.getDimensions(entityPose);
      this.dimensions = entityDimensions2;
      this.standingEyeHeight = entityDimensions2.eyeHeight();
      this.refreshPosition();
      boolean bl = entityDimensions2.width() <= 4.0F && entityDimensions2.height() <= 4.0F;
      if (!this.world.isClient
         && !this.firstUpdate
         && !this.noClip
         && bl
         && (entityDimensions2.width() > entityDimensions.width() || entityDimensions2.height() > entityDimensions.height())
         && !(this instanceof PlayerEntity)) {
         this.recalculateDimensions(entityDimensions);
      }
   }

   public boolean recalculateDimensions(EntityDimensions previous) {
      EntityDimensions entityDimensions = this.getDimensions(this.getPose());
      Vec3d vec3d = this.getPos().add(0.0, previous.height() / 2.0, 0.0);
      double d = Math.max(0.0F, entityDimensions.width() - previous.width()) + 1.0E-6;
      double e = Math.max(0.0F, entityDimensions.height() - previous.height()) + 1.0E-6;
      VoxelShape voxelShape = VoxelShapes.cuboid(Box.of(vec3d, d, e, d));
      Optional<Vec3d> optional = this.world
         .findClosestCollision(this, voxelShape, vec3d, entityDimensions.width(), entityDimensions.height(), entityDimensions.width());
      if (optional.isPresent()) {
         this.setPosition(optional.get().add(0.0, -entityDimensions.height() / 2.0, 0.0));
         return true;
      }

      if (entityDimensions.width() > previous.width() && entityDimensions.height() > previous.height()) {
         VoxelShape voxelShape2 = VoxelShapes.cuboid(Box.of(vec3d, d, 1.0E-6, d));
         Optional<Vec3d> optional2 = this.world
            .findClosestCollision(this, voxelShape2, vec3d, entityDimensions.width(), previous.height(), entityDimensions.width());
         if (optional2.isPresent()) {
            this.setPosition(optional2.get().add(0.0, -previous.height() / 2.0 + 1.0E-6, 0.0));
            return true;
         }
      }

      return false;
   }

   public Direction getHorizontalFacing() {
      return Direction.fromHorizontalDegrees(this.getYaw());
   }

   public Direction getMovementDirection() {
      return this.getHorizontalFacing();
   }

   protected HoverEvent getHoverEvent() {
      return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(this.getType(), this.getUuid(), this.getName()));
   }

   public boolean canBeSpectated(ServerPlayerEntity spectator) {
      return true;
   }

   @Override
   public final Box getBoundingBox() {
      return this.boundingBox;
   }

   public final void setBoundingBox(Box boundingBox) {
      this.boundingBox = boundingBox;
   }

   public final float getEyeHeight(EntityPose pose) {
      return this.getDimensions(pose).eyeHeight();
   }

   public final float getStandingEyeHeight() {
      return this.standingEyeHeight;
   }

   public Vec3d getLeashOffset(float tickDelta) {
      return this.getLeashOffset();
   }

   protected Vec3d getLeashOffset() {
      return new Vec3d(0.0, this.getStandingEyeHeight(), this.getWidth() * 0.4F);
   }

   public StackReference getStackReference(int mappedIndex) {
      return StackReference.EMPTY;
   }

   public World getEntityWorld() {
      return this.getWorld();
   }

   @Nullable
   public MinecraftServer getServer() {
      return this.getWorld().getServer();
   }

   public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
      return ActionResult.PASS;
   }

   public boolean isImmuneToExplosion(Explosion explosion) {
      return false;
   }

   public void onStartedTrackingBy(ServerPlayerEntity player) {
   }

   public void onStoppedTrackingBy(ServerPlayerEntity player) {
   }

   public float applyRotation(BlockRotation rotation) {
      float f = MathHelper.wrapDegrees(this.getYaw());
      switch (rotation) {
         case CLOCKWISE_180:
            return f + 180.0F;
         case COUNTERCLOCKWISE_90:
            return f + 270.0F;
         case CLOCKWISE_90:
            return f + 90.0F;
         default:
            return f;
      }
   }

   public float applyMirror(BlockMirror mirror) {
      float f = MathHelper.wrapDegrees(this.getYaw());
      switch (mirror) {
         case FRONT_BACK:
            return -f;
         case LEFT_RIGHT:
            return 180.0F - f;
         default:
            return f;
      }
   }

   public ProjectileDeflection getProjectileDeflection(ProjectileEntity projectile) {
      return this.getType().isIn(EntityTypeTags.DEFLECTS_PROJECTILES) ? ProjectileDeflection.SIMPLE : ProjectileDeflection.NONE;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      return null;
   }

   public final boolean hasControllingPassenger() {
      return this.getControllingPassenger() != null;
   }

   public final List<Entity> getPassengerList() {
      return this.passengerList;
   }

   @Nullable
   public Entity getFirstPassenger() {
      return this.passengerList.isEmpty() ? null : (Entity)this.passengerList.get(0);
   }

   public boolean hasPassenger(Entity passenger) {
      return this.passengerList.contains(passenger);
   }

   public boolean hasPassenger(Predicate<Entity> predicate) {
      UnmodifiableIterator var2 = this.passengerList.iterator();

      while (var2.hasNext()) {
         Entity entity = (Entity)var2.next();
         if (predicate.test(entity)) {
            return true;
         }
      }

      return false;
   }

   private Stream<Entity> streamIntoPassengers() {
      if (this.passengerList.isEmpty()) {
         return Stream.empty();
      }
      ArrayList<Entity> passengers = new ArrayList<>();
      this.lithium$addPassengersDeep(passengers);
      return passengers.stream();
   }

   @Override
   public Stream<Entity> streamSelfAndPassengers() {
      if (this.passengerList.isEmpty()) {
         return Stream.of(this);
      }
      ArrayList<Entity> passengers = new ArrayList<>();
      passengers.add(this);
      this.lithium$addPassengersDeep(passengers);
      return passengers.stream();
   }

   @Override
   public Stream<Entity> streamPassengersAndSelf() {
      if (this.passengerList.isEmpty()) {
         return Stream.of(this);
      }
      ArrayList<Entity> passengers = new ArrayList<>();
      this.lithium$addPassengersDeepFirst(passengers);
      passengers.add(this);
      return passengers.stream();
   }

   public Iterable<Entity> getPassengersDeep() {
      if (this.passengerList.isEmpty()) {
         return Collections.emptyList();
      }
      ArrayList<Entity> passengers = new ArrayList<>();
      this.lithium$addPassengersDeep(passengers);
      return passengers;
   }

   private void lithium$addPassengersDeep(ArrayList<Entity> passengers) {
      for (int i = 0; i < this.passengerList.size(); i++) {
         Entity passenger = this.passengerList.get(i);
         passengers.add(passenger);
         passenger.lithium$addPassengersDeep(passengers);
      }
   }

   private void lithium$addPassengersDeepFirst(ArrayList<Entity> passengers) {
      for (int i = 0; i < this.passengerList.size(); i++) {
         Entity passenger = this.passengerList.get(i);
         passenger.lithium$addPassengersDeepFirst(passengers);
         passengers.add(passenger);
      }
   }

   public int getPlayerPassengers() {
      return (int)this.streamIntoPassengers().filter(passenger -> passenger instanceof PlayerEntity).count();
   }

   public boolean hasPlayerRider() {
      return this.getPlayerPassengers() == 1;
   }

   public Entity getRootVehicle() {
      Entity entity = this;

      while (entity.hasVehicle()) {
         entity = entity.getVehicle();
      }

      return entity;
   }

   public boolean isConnectedThroughVehicle(Entity entity) {
      return this.getRootVehicle() == entity.getRootVehicle();
   }

   public boolean hasPassengerDeep(Entity passenger) {
      if (!passenger.hasVehicle()) {
         return false;
      }

      Entity entity = passenger.getVehicle();
      return entity == this ? true : this.hasPassengerDeep(entity);
   }

   public boolean isLocalPlayerOrLogicalSideForUpdatingMovement() {
      return this instanceof PlayerEntity playerEntity ? playerEntity.isMainPlayer() : this.isLogicalSideForUpdatingMovement();
   }

   public boolean isLogicalSideForUpdatingMovement() {
      return this.getControllingPassenger() instanceof PlayerEntity playerEntity ? playerEntity.isMainPlayer() : this.canMoveVoluntarily();
   }

   public boolean isControlledByPlayer() {
      LivingEntity livingEntity = this.getControllingPassenger();
      return livingEntity != null && livingEntity.isControlledByPlayer();
   }

   public boolean canMoveVoluntarily() {
      return !this.getWorld().isClient;
   }

   protected static Vec3d getPassengerDismountOffset(double vehicleWidth, double passengerWidth, float passengerYaw) {
      double d = (vehicleWidth + passengerWidth + 1.0E-5F) / 2.0;
      float f = -MathHelper.sin(passengerYaw * (float) (Math.PI / 180.0));
      float g = MathHelper.cos(passengerYaw * (float) (Math.PI / 180.0));
      float h = Math.max(Math.abs(f), Math.abs(g));
      return new Vec3d(f * d / h, 0.0, g * d / h);
   }

   public Vec3d updatePassengerForDismount(LivingEntity passenger) {
      return new Vec3d(this.getX(), this.getBoundingBox().maxY, this.getZ());
   }

   @Nullable
   public Entity getVehicle() {
      return this.vehicle;
   }

   @Nullable
   public Entity getControllingVehicle() {
      return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
   }

   public PistonBehavior getPistonBehavior() {
      return PistonBehavior.NORMAL;
   }

   public SoundCategory getSoundCategory() {
      return SoundCategory.NEUTRAL;
   }

   protected int getBurningDuration() {
      return 1;
   }

   public ServerCommandSource getCommandSource(ServerWorld world) {
      return new ServerCommandSource(
         CommandOutput.DUMMY, this.getPos(), this.getRotationClient(), world, 0, this.getName().getString(), this.getDisplayName(), world.getServer(), this
      );
   }

   public void lookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
      Vec3d vec3d = anchorPoint.positionAt(this);
      double d = target.x - vec3d.x;
      double e = target.y - vec3d.y;
      double f = target.z - vec3d.z;
      double g = Math.sqrt(d * d + f * f);
      this.setPitch(MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 180.0F / (float)Math.PI))));
      this.setYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F));
      this.setHeadYaw(this.getYaw());
      this.prevPitch = this.getPitch();
      this.prevYaw = this.getYaw();
   }

   public float lerpYaw(float delta) {
      return MathHelper.lerp(delta, this.prevYaw, this.yaw);
   }

   public boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         Box legacyBox = this.getBoundingBox().expand(0.0, -0.4, 0.0).contract(0.001);
         int minX = MathHelper.floor(legacyBox.minX);
         int maxX = MathHelper.ceil(legacyBox.maxX);
         int minY = MathHelper.floor(legacyBox.minY);
         int maxY = MathHelper.ceil(legacyBox.maxY);
         int minZ = MathHelper.floor(legacyBox.minZ);
         int maxZ = MathHelper.ceil(legacyBox.maxZ);
         if (!this.world.isRegionLoaded(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
         }

         double fluidHeight = 0.0;
         boolean foundFluid = false;
         Vec3d push = Vec3d.ZERO;
         BlockPos.Mutable mutablePos = new BlockPos.Mutable();
         for (int x = minX; x < maxX; x++) {
            for (int y = minY - 1; y < maxY; y++) {
               for (int z = minZ; z < maxZ; z++) {
                  mutablePos.set(x, y, z);
                  FluidState state = this.world.getFluidState(mutablePos);
                  if (state.isIn(tag)) {
                     double surfaceHeight = y + state.getHeight(this.world, mutablePos);
                     if (surfaceHeight >= legacyBox.minY - 0.4) {
                        fluidHeight = Math.max(surfaceHeight - legacyBox.minY + 0.4, fluidHeight);
                     }

                     if (y >= minY && maxY >= surfaceHeight) {
                        foundFluid = true;
                        push = push.add(state.getVelocity(this.world, mutablePos));
                     }
                  }
               }
            }
         }

         if (push.length() > 0.0) {
            push = push.normalize().multiply(0.014);
            this.setVelocity(this.getVelocity().add(push));
         }

         this.fluidHeight.put(tag, fluidHeight);
         return foundFluid;
      }

      if (this.isRegionUnloaded()) {
         return false;
      }

      Box box = this.getBoundingBox().contract(0.001);
      int i = MathHelper.floor(box.minX);
      int j = MathHelper.ceil(box.maxX);
      int k = MathHelper.floor(box.minY);
      int l = MathHelper.ceil(box.maxY);
      int m = MathHelper.floor(box.minZ);
      int n = MathHelper.ceil(box.maxZ);
      if (this.lithium$canSkipFluidScan(tag, i, j, k, l, m, n)) {
         this.fluidHeight.put(tag, 0.0);
         return false;
      }
      double d = 0.0;
      boolean bl = this.isPushedByFluids();
      boolean bl2 = false;
      Vec3d vec3d = Vec3d.ZERO;
      int o = 0;
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for (int p = i; p < j; p++) {
         for (int q = k; q < l; q++) {
            for (int r = m; r < n; r++) {
               mutable.set(p, q, r);
               FluidState fluidState = this.getWorld().getFluidState(mutable);
               if (fluidState.isIn(tag)) {
                  double e = q + fluidState.getHeight(this.getWorld(), mutable);
                  if (e >= box.minY) {
                     bl2 = true;
                     d = Math.max(e - box.minY, d);
                     if (bl) {
                        Vec3d vec3d2 = fluidState.getVelocity(this.getWorld(), mutable);
                        if (d < 0.4) {
                           vec3d2 = vec3d2.multiply(d);
                        }

                        vec3d = vec3d.add(vec3d2);
                        o++;
                     }
                  }
               }
            }
         }
      }

      if (vec3d.length() > 0.0) {
         if (o > 0) {
            vec3d = vec3d.multiply(1.0 / o);
         }

         if (!(this instanceof PlayerEntity)) {
            vec3d = vec3d.normalize();
         }

         Vec3d vec3d3 = this.getVelocity();
         vec3d = vec3d.multiply(speed);
         double f = 0.003;
         if (Math.abs(vec3d3.x) < 0.003 && Math.abs(vec3d3.z) < 0.003 && vec3d.length() < 0.0045000000000000005) {
            vec3d = vec3d.normalize().multiply(0.0045000000000000005);
         }

         this.setVelocity(this.getVelocity().add(vec3d));
      }

      this.fluidHeight.put(tag, d);
      return bl2;
   }

   private boolean lithium$canSkipFluidScan(TagKey<Fluid> tag, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
      if (!BlockStateFlags.ENABLED) {
         return false;
      }

      TrackedBlockStatePredicate fluidFlag;
      if (tag == FluidTags.WATER) {
         fluidFlag = BlockStateFlags.WATER;
      } else if (tag == FluidTags.LAVA) {
         fluidFlag = BlockStateFlags.LAVA;
      } else {
         return false;
      }

      int minChunkX = ChunkSectionPos.getSectionCoord(minX);
      int maxChunkX = ChunkSectionPos.getSectionCoord(maxX - 1);
      int minChunkZ = ChunkSectionPos.getSectionCoord(minZ);
      int maxChunkZ = ChunkSectionPos.getSectionCoord(maxZ - 1);
      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            Chunk chunk = this.world.getChunk(chunkX, chunkZ);
            ChunkSection[] sections = chunk.getSectionArray();
            int minSection = Math.max(0, chunk.getSectionIndex(minY));
            int maxSection = Math.min(sections.length - 1, chunk.getSectionIndex(maxY - 1));
            for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
               ChunkSection section = sections[sectionIndex];
               if (section != null && ((BlockCountingSection)section).lithium$mayContainAny(fluidFlag)) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   public boolean isRegionUnloaded() {
      Box box = this.getBoundingBox().expand(1.0);
      int i = MathHelper.floor(box.minX);
      int j = MathHelper.ceil(box.maxX);
      int k = MathHelper.floor(box.minZ);
      int l = MathHelper.ceil(box.maxZ);
      return !this.getWorld().isRegionLoaded(i, k, j, l);
   }

   public double getFluidHeight(TagKey<Fluid> fluid) {
      return this.fluidHeight.getDouble(fluid);
   }

   public double getSwimHeight() {
      return this.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   public final float getWidth() {
      return this.dimensions.width();
   }

   public final float getHeight() {
      return this.dimensions.height();
   }

   public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
      return new EntitySpawnS2CPacket(this, entityTrackerEntry);
   }

   public EntityDimensions getDimensions(EntityPose pose) {
      return this.type.getDimensions();
   }

   public final EntityAttachments getAttachments() {
      return this.dimensions.attachments();
   }

   public Vec3d getPos() {
      return this.pos;
   }

   public Vec3d getSyncedPos() {
      return this.getPos();
   }

   @Override
   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public BlockState getBlockStateAtPos() {
      if (this.stateAtPos == null) {
         this.stateAtPos = this.getWorld().getBlockState(this.getBlockPos());
         this.lithium$onFeetBlockCacheSet(this.stateAtPos);
      }

      return this.stateAtPos;
   }

   @Override
   @Nullable
   public BlockState lithium$getCachedFeetBlockState() {
      return this.stateAtPos;
   }

   public ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   public Vec3d getVelocity() {
      return this.velocity;
   }

   public void setVelocity(Vec3d velocity) {
      this.velocity = velocity;
   }

   public void addVelocityInternal(Vec3d velocity) {
      this.setVelocity(this.getVelocity().add(velocity));
   }

   public void setVelocity(double x, double y, double z) {
      this.setVelocity(new Vec3d(x, y, z));
   }

   public final int getBlockX() {
      return this.blockPos.getX();
   }

   public final double getX() {
      return this.pos.x;
   }

   public double getBodyX(double widthScale) {
      return this.pos.x + this.getWidth() * widthScale;
   }

   public double getParticleX(double widthScale) {
      return this.getBodyX((2.0 * this.random.nextDouble() - 1.0) * widthScale);
   }

   public final int getBlockY() {
      return this.blockPos.getY();
   }

   public final double getY() {
      return this.pos.y;
   }

   public double getBodyY(double heightScale) {
      return this.pos.y + this.getHeight() * heightScale;
   }

   public double getRandomBodyY() {
      return this.getBodyY(this.random.nextDouble());
   }

   public double getEyeY() {
      return this.pos.y + this.standingEyeHeight;
   }

   public final int getBlockZ() {
      return this.blockPos.getZ();
   }

   public final double getZ() {
      return this.pos.z;
   }

   public double getBodyZ(double widthScale) {
      return this.pos.z + this.getWidth() * widthScale;
   }

   public double getParticleZ(double widthScale) {
      return this.getBodyZ((2.0 * this.random.nextDouble() - 1.0) * widthScale);
   }

   public final void setPos(double x, double y, double z) {
      if (this.pos.x != x || this.pos.y != y || this.pos.z != z) {
         this.pos = new Vec3d(x, y, z);
         int i = MathHelper.floor(x);
         int j = MathHelper.floor(y);
         int k = MathHelper.floor(z);
         if (i != this.blockPos.getX() || j != this.blockPos.getY() || k != this.blockPos.getZ()) {
            this.lithium$onFeetBlockCacheDeleted();
            this.blockPos = new BlockPos(i, j, k);
            this.stateAtPos = null;
            if (ChunkSectionPos.getSectionCoord(i) != this.chunkPos.x || ChunkSectionPos.getSectionCoord(k) != this.chunkPos.z) {
               this.chunkPos = new ChunkPos(this.blockPos);
            }
         }

         this.changeListener.updateEntityPosition();
      }
   }

   public void checkDespawn() {
   }

   public Vec3d getLeashPos(float delta) {
      return this.getLerpedPos(delta).add(0.0, this.standingEyeHeight * 0.7, 0.0);
   }

   public void onSpawnPacket(EntitySpawnS2CPacket packet) {
      int i = packet.getEntityId();
      double d = packet.getX();
      double e = packet.getY();
      double f = packet.getZ();
      this.updateTrackedPosition(d, e, f);
      this.refreshPositionAndAngles(d, e, f, packet.getYaw(), packet.getPitch());
      this.setId(i);
      this.setUuid(packet.getUuid());
   }

   @Nullable
   public ItemStack getPickBlockStack() {
      return null;
   }

   public void setInPowderSnow(boolean inPowderSnow) {
      this.inPowderSnow = inPowderSnow;
   }

   public boolean canFreeze() {
      return !this.getType().isIn(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
   }

   public boolean shouldEscapePowderSnow() {
      return (this.inPowderSnow || this.wasInPowderSnow) && this.canFreeze();
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getBodyYaw() {
      return this.getYaw();
   }

   public void setYaw(float yaw) {
      boolean allowNonFinite = this instanceof net.minecraft.client.network.ClientPlayerEntity
         && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4);
      if (!Float.isFinite(yaw) && !allowNonFinite) {
         Util.logErrorOrPause("Invalid entity rotation: " + yaw + ", discarding.");
      } else {
         this.yaw = yaw;
      }
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      boolean allowNonFinite = this instanceof net.minecraft.client.network.ClientPlayerEntity
         && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4);
      if (!Float.isFinite(pitch) && !allowNonFinite) {
         Util.logErrorOrPause("Invalid entity rotation: " + pitch + ", discarding.");
      } else {
         float wrappedPitch = pitch % 360.0F;
         this.pitch = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21)
            ? wrappedPitch
            : Math.clamp(wrappedPitch, -90.0F, 90.0F);
      }
   }

   public boolean canSprintAsVehicle() {
      return false;
   }

   public float getStepHeight() {
      return 0.0F;
   }

   public void onExplodedBy(@Nullable Entity entity) {
   }

   public final boolean isRemoved() {
      return this.removalReason != null;
   }

   @Nullable
   public Entity.RemovalReason getRemovalReason() {
      return this.removalReason;
   }

   @Override
   public final void setRemoved(Entity.RemovalReason reason) {
      if (this.removalReason == null) {
         this.removalReason = reason;
      }

      if (this.removalReason.shouldDestroy()) {
         this.stopRiding();
      }

      this.getPassengerList().forEach(Entity::stopRiding);
      this.changeListener.remove(reason);
      this.onRemove(reason);
   }

   protected void unsetRemoved() {
      this.removalReason = null;
   }

   @Override
   public void setChangeListener(EntityChangeListener changeListener) {
      this.changeListener = changeListener;
   }

   public EntityChangeListener lithium$getChangeListener() {
      return this.changeListener;
   }

   @Override
   public boolean shouldSave() {
      if (this.removalReason != null && !this.removalReason.shouldSave()) {
         return false;
      } else {
         return this.hasVehicle() ? false : !this.hasPassengers() || !this.hasPlayerRider();
      }
   }

   @Override
   public boolean isPlayer() {
      return false;
   }

   public boolean canModifyAt(ServerWorld world, BlockPos pos) {
      return true;
   }

   public World getWorld() {
      return this.world;
   }

   protected void setWorld(World world) {
      this.world = world;
   }

   public DamageSources getDamageSources() {
      return this.getWorld().getDamageSources();
   }

   public DynamicRegistryManager getRegistryManager() {
      return this.getWorld().getRegistryManager();
   }

   public void lerpPosAndRotation(int step, double x, double y, double z, double yaw, double pitch) {
      double d = 1.0 / step;
      double e = MathHelper.lerp(d, this.getX(), x);
      double f = MathHelper.lerp(d, this.getY(), y);
      double g = MathHelper.lerp(d, this.getZ(), z);
      float h = (float)MathHelper.lerpAngleDegrees(d, this.getYaw(), yaw);
      float i = (float)MathHelper.lerp(d, this.getPitch(), pitch);
      this.setPosition(e, f, g);
      this.setRotation(h, i);
   }

   public Random getRandom() {
      return this.random;
   }

   @Override
   public CullingState entityCulling$getState() {
      return this.entityCulling$state;
   }

   public Vec3d getMovement() {
      return this.getControllingPassenger() instanceof PlayerEntity playerEntity && this.isAlive() ? playerEntity.getMovement() : this.getVelocity();
   }

   @Nullable
   public ItemStack getWeaponStack() {
      return null;
   }

   public Optional<RegistryKey<LootTable>> getLootTableKey() {
      return this.type.getLootTableKey();
   }

   public enum MoveEffect {
      NONE(false, false),
      SOUNDS(true, false),
      EVENTS(false, true),
      ALL(true, true);

      final boolean sounds;
      final boolean events;

      MoveEffect(final boolean sounds, final boolean events) {
         this.sounds = sounds;
         this.events = events;
      }

      public boolean hasAny() {
         return this.events || this.sounds;
      }

      public boolean emitsGameEvents() {
         return this.events;
      }

      public boolean playsSounds() {
         return this.sounds;
      }
   }

   @FunctionalInterface
   public interface PositionUpdater {
      void accept(Entity entity, double x, double y, double z);
   }

   record QueuedCollisionCheck(Vec3d from, Vec3d to) {
   }

   public enum RemovalReason {
      KILLED(true, false),
      DISCARDED(true, false),
      UNLOADED_TO_CHUNK(false, true),
      UNLOADED_WITH_PLAYER(false, false),
      CHANGED_DIMENSION(false, false);

      private final boolean destroy;
      private final boolean save;

      RemovalReason(final boolean destroy, final boolean save) {
         this.destroy = destroy;
         this.save = save;
      }

      public boolean shouldDestroy() {
         return this.destroy;
      }

      public boolean shouldSave() {
         return this.save;
      }
   }
}
