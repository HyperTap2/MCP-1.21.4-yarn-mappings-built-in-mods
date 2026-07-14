package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.features.interaction.r1_18_2_block_ack_emulation.ClientPlayerInteractionManager1_18_2;
import com.viaversion.viafabricplus.features.interaction.replace_block_placement_logic.ActionResultException1_12_2;
import com.viaversion.viafabricplus.injection.access.base.IClientConnection;
import com.viaversion.viafabricplus.injection.access.interaction.container_clicking.IScreenHandler;
import com.viaversion.viafabricplus.injection.access.interaction.r1_18_2_block_ack_emulation.IClientPlayerInteractionManager;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion.ViaFabricPlusHandItemProvider;
import com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator;
import com.viaversion.viafabricplus.visuals.settings.VisualSettings;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OperatorBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.SlotChangedStateC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.PassToDefaultBlockAction;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ClientPlayerInteractionManager implements IClientPlayerInteractionManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftClient client;
   private final ClientPlayNetworkHandler networkHandler;
   private BlockPos currentBreakingPos = new BlockPos(-1, -1, -1);
   private ItemStack selectedStack = ItemStack.EMPTY;
   private float currentBreakingProgress;
   private float blockBreakingSoundCooldown;
   private int blockBreakingCooldown;
   private boolean breakingBlock;
   private GameMode gameMode = GameMode.DEFAULT;
   @Nullable
   private GameMode previousGameMode;
   private int lastSelectedSlot;
   private final ClientPlayerInteractionManager1_18_2 viaFabricPlus$1_18_2InteractionManager = new ClientPlayerInteractionManager1_18_2();

   public ClientPlayerInteractionManager(MinecraftClient client, ClientPlayNetworkHandler networkHandler) {
      this.client = client;
      this.networkHandler = networkHandler;
   }

   public void copyAbilities(PlayerEntity player) {
      this.gameMode.setAbilities(player.getAbilities());
   }

   public void setGameModes(GameMode gameMode, @Nullable GameMode previousGameMode) {
      this.gameMode = gameMode;
      this.previousGameMode = previousGameMode;
      this.gameMode.setAbilities(this.client.player.getAbilities());
   }

   public void setGameMode(GameMode gameMode) {
      if (gameMode != this.gameMode) {
         this.previousGameMode = this.gameMode;
      }

      this.gameMode = gameMode;
      this.gameMode.setAbilities(this.client.player.getAbilities());
   }

   public boolean hasStatusBars() {
      return this.gameMode.isSurvivalLike();
   }

   public boolean breakBlock(BlockPos pos) {
      if (this.client.player.isBlockBreakingRestricted(this.client.world, pos, this.gameMode)) {
         return false;
      }

      World world = this.client.world;
      BlockState blockState = world.getBlockState(pos);
      if (!this.client.player.getMainHandStack().getItem().canMine(blockState, world, pos, this.client.player)) {
         return false;
      }

      Block block = blockState.getBlock();
      if (block instanceof OperatorBlock && !this.client.player.isCreativeLevelTwoOp()) {
         return false;
      }

      if (blockState.isAir()) {
         return false;
      }

      block.onBreak(world, pos, blockState, this.client.player);
      FluidState fluidState = world.getFluidState(pos);
      boolean bl = world.setBlockState(pos, fluidState.getBlockState(), 11);
      if (bl) {
         block.onBroken(world, pos, blockState);
      }

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_3)) {
         this.currentBreakingPos = new BlockPos(this.currentBreakingPos.getX(), -1, this.currentBreakingPos.getZ());
      }

      return bl;
   }

   public boolean attackBlock(BlockPos pos, Direction direction) {
      if (this.client.player.isBlockBreakingRestricted(this.client.world, pos, this.gameMode)) {
         return false;
      }

      if (!this.client.world.getWorldBorder().contains(pos)) {
         return false;
      }

      if (this.gameMode.isCreative()) {
         BlockState blockState = this.client.world.getBlockState(pos);
         this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 1.0F);
         this.sendSequencedPacket(this.client.world, sequence -> {
            this.breakBlock(pos);
            return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
         });
         this.blockBreakingCooldown = 5;
      } else if (!this.breakingBlock || !this.isCurrentlyBreaking(pos)) {
         if (this.breakingBlock) {
            this.networkHandler.sendPacket(this.viaFabricPlus$createPlayerAction(Action.ABORT_DESTROY_BLOCK, this.currentBreakingPos, direction));
         }

         BlockState blockState = this.client.world.getBlockState(pos);
         this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 0.0F);
         this.sendSequencedPacket(this.client.world, sequence -> {
            boolean bl = !blockState.isAir();
            if (bl && this.currentBreakingProgress == 0.0F) {
               blockState.onBlockBreakStart(this.client.world, pos, this.client.player);
            }

            if (bl && blockState.calcBlockBreakingDelta(this.client.player, this.client.player.getWorld(), pos) >= 1.0F) {
               this.breakBlock(pos);
            } else {
               this.breakingBlock = true;
               this.currentBreakingPos = pos;
               this.selectedStack = this.client.player.getMainHandStack();
               this.currentBreakingProgress = 0.0F;
               this.blockBreakingSoundCooldown = 0.0F;
               this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, this.getBlockBreakingProgress());
            }

            return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
         });
      }

      return true;
   }

   public void cancelBlockBreaking() {
      if (this.breakingBlock) {
         BlockState blockState = this.client.world.getBlockState(this.currentBreakingPos);
         this.client.getTutorialManager().onBlockBreaking(this.client.world, this.currentBreakingPos, blockState, -1.0F);
         this.networkHandler.sendPacket(this.viaFabricPlus$createPlayerAction(Action.ABORT_DESTROY_BLOCK, this.currentBreakingPos, Direction.DOWN));
         this.breakingBlock = false;
         this.currentBreakingProgress = 0.0F;
         this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, -1);
         this.client.player.resetLastAttackedTicks();
      }
   }

   public boolean updateBlockBreakingProgress(BlockPos pos, Direction direction) {
      this.syncSelectedSlot();
      if (this.blockBreakingCooldown > 0) {
         this.blockBreakingCooldown--;
         return true;
      }

      if (this.gameMode.isCreative() && this.client.world.getWorldBorder().contains(pos)) {
         this.blockBreakingCooldown = 5;
         BlockState blockState = this.client.world.getBlockState(pos);
         this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 1.0F);
         this.sendSequencedPacket(this.client.world, sequence -> {
            this.viaFabricPlus$breakBlock(pos, direction);
            return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
         });
         return true;
      }

      if (this.isCurrentlyBreaking(pos)) {
         BlockState blockState = this.client.world.getBlockState(pos);
         if (blockState.isAir()) {
            this.breakingBlock = false;
            return false;
         }

         this.currentBreakingProgress = this.currentBreakingProgress
            + blockState.calcBlockBreakingDelta(this.client.player, this.client.player.getWorld(), pos);
         if (this.blockBreakingSoundCooldown % 4.0F == 0.0F) {
            BlockSoundGroup blockSoundGroup = blockState.getSoundGroup();
            this.client
               .getSoundManager()
               .play(
                  new PositionedSoundInstance(
                     blockSoundGroup.getHitSound(),
                     SoundCategory.BLOCKS,
                     (blockSoundGroup.getVolume() + 1.0F) / 8.0F,
                     blockSoundGroup.getPitch() * 0.5F,
                     SoundInstance.createRandom(),
                     pos
                  )
               );
         }

         this.blockBreakingSoundCooldown++;
         this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, MathHelper.clamp(this.currentBreakingProgress, 0.0F, 1.0F));
         if (this.currentBreakingProgress >= 1.0F) {
            this.breakingBlock = false;
            this.sendSequencedPacket(this.client.world, sequence -> {
               this.viaFabricPlus$breakBlock(pos, direction);
               return new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
            });
            this.currentBreakingProgress = 0.0F;
            this.blockBreakingSoundCooldown = 0.0F;
            this.blockBreakingCooldown = 5;
         }

         this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, this.getBlockBreakingProgress());
         return true;
      } else {
         return this.attackBlock(pos, direction);
      }
   }

   public final void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator) {
      try (PendingUpdateManager pendingUpdateManager = world.getPendingUpdateManager().incrementSequence()) {
         int i = pendingUpdateManager.getSequence();
         Packet<ServerPlayPacketListener> packet = packetCreator.predict(i);
         if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)
            && packet instanceof PlayerActionC2SPacket playerActionPacket) {
            this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(playerActionPacket.getAction(), playerActionPacket.getPos());
         }

         this.networkHandler.sendPacket(packet);
      }
   }

   public void tick() {
      this.syncSelectedSlot();
      if (this.networkHandler.getConnection().isOpen()) {
         this.networkHandler.getConnection().tick();
      } else {
         this.networkHandler.getConnection().handleDisconnection();
      }
   }

   private boolean isCurrentlyBreaking(BlockPos pos) {
      ItemStack itemStack = this.client.player.getMainHandStack();
      return pos.equals(this.currentBreakingPos) && ItemStack.areItemsAndComponentsEqual(itemStack, this.selectedStack);
   }

   public final void syncSelectedSlot() {
      int i = this.client.player.getInventory().selectedSlot;
      if (i != this.lastSelectedSlot) {
         this.lastSelectedSlot = i;
         this.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.lastSelectedSlot));
      }
   }

   public ActionResult interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && hand != Hand.MAIN_HAND) {
         return ActionResult.PASS;
      }

      this.syncSelectedSlot();
      if (!this.client.world.getWorldBorder().contains(hitResult.getBlockPos())) {
         return ActionResult.FAIL;
      }

      MutableObject<ActionResult> mutableObject = new MutableObject();
      try {
         this.sendSequencedPacket(this.client.world, sequence -> {
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
               ViaFabricPlusHandItemProvider.lastUsedItem = player.getStackInHand(hand).copy();
            }

            try {
               mutableObject.setValue(this.interactBlockInternal(player, hand, hitResult));
               return new PlayerInteractBlockC2SPacket(hand, hitResult, sequence);
            } catch (ActionResultException1_12_2 exception) {
               mutableObject.setValue(exception.getActionResult());
               throw exception;
            }
         });
      } catch (ActionResultException1_12_2 ignored) {
      }

      return (ActionResult)mutableObject.getValue();
   }

   public final ActionResult interactBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
      BlockPos blockPos = hitResult.getBlockPos();
      ItemStack itemStack = player.getStackInHand(hand);
      if (this.gameMode == GameMode.SPECTATOR) {
         return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21) ? ActionResult.SUCCESS : ActionResult.CONSUME;
      }

      boolean bl = !player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty();
      boolean bl2 = player.shouldCancelInteraction() && bl;
      if (!bl2) {
         BlockState blockState = this.client.world.getBlockState(blockPos);
         if (!this.networkHandler.hasFeature(blockState.getBlock().getRequiredFeatures())) {
            return ActionResult.FAIL;
         }

         ActionResult actionResult = blockState.onUseWithItem(player.getStackInHand(hand), this.client.world, player, hand, hitResult);
         if (actionResult.isAccepted()) {
            return actionResult;
         }

         if (actionResult instanceof PassToDefaultBlockAction && hand == Hand.MAIN_HAND) {
            ActionResult actionResult2 = blockState.onUse(this.client.world, player, hitResult);
            if (actionResult2.isAccepted()) {
               return actionResult2;
            }
         }
      }

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         this.viaFabricPlus$interactBlockLegacy(player, hand, hitResult, itemStack);
      }

      if (!itemStack.isEmpty() && !player.getItemCooldownManager().isCoolingDown(itemStack)) {
         ItemUsageContext itemUsageContext = new ItemUsageContext(player, hand, hitResult);
         ActionResult actionResult3;
         if (this.gameMode.isCreative()) {
            int i = itemStack.getCount();
            actionResult3 = itemStack.useOnBlock(itemUsageContext);
            itemStack.setCount(i);
         } else {
            actionResult3 = itemStack.useOnBlock(itemUsageContext);
         }

         return actionResult3;
      } else {
         return ActionResult.PASS;
      }
   }

   public ActionResult interactItem(PlayerEntity player, Hand hand) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && hand != Hand.MAIN_HAND) {
         return ActionResult.PASS;
      }

      if (this.gameMode == GameMode.SPECTATOR) {
         return ActionResult.PASS;
      }

      this.syncSelectedSlot();
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_17, ProtocolVersion.v1_20_5)) {
         this.networkHandler
            .sendPacket(new Full(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.isOnGround(), player.horizontalCollision));
      }

      MutableObject<ActionResult> mutableObject = new MutableObject();
      this.sendSequencedPacket(this.client.world, sequence -> {
         PlayerInteractItemC2SPacket playerInteractItemC2SPacket = new PlayerInteractItemC2SPacket(hand, sequence, player.getYaw(), player.getPitch());
         ItemStack itemStack = player.getStackInHand(hand);
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            ViaFabricPlusHandItemProvider.lastUsedItem = itemStack.copy();
         }

         if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
            mutableObject.setValue(ActionResult.PASS);
            return playerInteractItemC2SPacket;
         }

         int oldCount = itemStack.getCount();
         ActionResult actionResult = itemStack.use(this.client.world, player, hand);
         ItemStack itemStack2;
         if (actionResult instanceof Success success) {
            itemStack2 = Objects.requireNonNullElseGet(success.getNewHandStack(), () -> player.getStackInHand(hand));
         } else {
            itemStack2 = player.getStackInHand(hand);
         }

         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            boolean accepted = !itemStack2.isEmpty() && (itemStack2 != itemStack || itemStack2.getCount() != oldCount);
            if (actionResult.isAccepted() != accepted) {
               actionResult = accepted ? ActionResult.SUCCESS.withNewHandStack(itemStack2) : ActionResult.PASS;
            }
         }

         if (itemStack2 != itemStack) {
            player.setStackInHand(hand, itemStack2);
         }

         mutableObject.setValue(actionResult);
         return playerInteractItemC2SPacket;
      });
      return (ActionResult)mutableObject.getValue();
   }

   public ClientPlayerEntity createPlayer(ClientWorld world, StatHandler statHandler, ClientRecipeBook recipeBook) {
      return this.createPlayer(world, statHandler, recipeBook, false, false);
   }

   public ClientPlayerEntity createPlayer(ClientWorld world, StatHandler statHandler, ClientRecipeBook recipeBook, boolean lastSneaking, boolean lastSprinting) {
      return new ClientPlayerEntity(this.client, world, this.networkHandler, statHandler, recipeBook, lastSneaking, lastSprinting);
   }

   public void attackEntity(PlayerEntity player, Entity target) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
      if (this.gameMode != GameMode.SPECTATOR) {
         player.attack(target);
         player.resetLastAttackedTicks();
      }
   }

   public ActionResult interactEntity(PlayerEntity player, Entity entity, Hand hand) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, player.isSneaking(), hand));
      return (ActionResult)(this.gameMode == GameMode.SPECTATOR ? ActionResult.PASS : player.interact(entity, hand));
   }

   public ActionResult interactEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand) {
      this.syncSelectedSlot();
      Vec3d vec3d = hitResult.getPos().subtract(entity.getX(), entity.getY(), entity.getZ());
      this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, player.isSneaking(), hand, vec3d));
      return (ActionResult)(this.gameMode == GameMode.SPECTATOR ? ActionResult.PASS : entity.interactAt(player, vec3d, hand));
   }

   public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_5tob1_5_2) && actionType != SlotActionType.PICKUP) {
         return;
      }

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)
         && actionType != SlotActionType.PICKUP
         && actionType != SlotActionType.QUICK_MOVE
         && actionType != SlotActionType.SWAP
         && actionType != SlotActionType.CLONE) {
         return;
      }

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)
         && actionType == SlotActionType.SWAP
         && button == 40) {
         return;
      }

      ScreenHandler screenHandler = player.currentScreenHandler;
      if (syncId != screenHandler.syncId) {
         LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", syncId, screenHandler.syncId);
      } else {
         DefaultedList<Slot> defaultedList = screenHandler.slots;
         int i = defaultedList.size();
         List<ItemStack> list = Lists.newArrayListWithCapacity(i);
         ItemStack oldCursorStack = screenHandler.getCursorStack().copy();

         for (Slot slot : defaultedList) {
            list.add(slot.getStack().copy());
         }

         screenHandler.onSlotClick(slotId, button, actionType, player);
         Int2ObjectMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = ((Slot)defaultedList.get(j)).getStack();
            if (!ItemStack.areEqual(itemStack, itemStack2)) {
               int2ObjectMap.put(j, itemStack2.copy());
            }
         }

         ClickSlotC2SPacket clickPacket = new ClickSlotC2SPacket(
            syncId, screenHandler.getRevision(), slotId, button, actionType, screenHandler.getCursorStack().copy(), int2ObjectMap
         );
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            ItemStack slotItemBeforeModification;
            if (this.viaFabricPlus$shouldClickStackBeEmpty(actionType, slotId)) {
               slotItemBeforeModification = ItemStack.EMPTY;
            } else if (slotId >= 0 && slotId < list.size()) {
               slotItemBeforeModification = list.get(slotId);
            } else {
               slotItemBeforeModification = oldCursorStack;
            }

            PacketWrapper containerClick = PacketWrapper.create(
               ServerboundPackets1_16_2.CONTAINER_CLICK, ((IClientConnection)this.networkHandler.getConnection()).viaFabricPlus$getUserConnection()
            );
            containerClick.write(Types.BYTE, (byte)syncId);
            containerClick.write(Types.SHORT, (short)slotId);
            containerClick.write(Types.BYTE, (byte)button);
            containerClick.write(Types.SHORT, ((IScreenHandler)screenHandler).viaFabricPlus$incrementAndGetActionId());
            containerClick.write(Types.VAR_INT, actionType.ordinal());
            containerClick.write(Types.ITEM1_13_2, ItemTranslator.mcToVia(slotItemBeforeModification, ProtocolVersion.v1_16_4));
            containerClick.scheduleSendToServer(Protocol1_16_4To1_17.class);
         } else {
            this.networkHandler.sendPacket(clickPacket);
         }
      }
   }

   private boolean viaFabricPlus$shouldClickStackBeEmpty(SlotActionType type, int slot) {
      if (type == SlotActionType.QUICK_CRAFT || type == SlotActionType.THROW) {
         return true;
      }

      return type == SlotActionType.QUICK_MOVE && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_11_1)
         || type == SlotActionType.PICKUP && slot == ScreenHandler.EMPTY_SPACE_SLOT_INDEX;
   }

   public void clickRecipe(int syncId, NetworkRecipeId recipeId, boolean craftAll) {
      this.networkHandler.sendPacket(new CraftRequestC2SPacket(syncId, recipeId, craftAll));
   }

   public void clickButton(int syncId, int buttonId) {
      this.networkHandler.sendPacket(new ButtonClickC2SPacket(syncId, buttonId));
   }

   public void clickCreativeStack(ItemStack stack, int slotId) {
      if (this.gameMode.isCreative() && this.networkHandler.hasFeature(stack.getItem().getRequiredFeatures())) {
         this.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slotId, stack));
      }
   }

   public void dropCreativeStack(ItemStack stack) {
      boolean bl = this.client.currentScreen instanceof HandledScreen && !(this.client.currentScreen instanceof CreativeInventoryScreen);
      if (this.gameMode.isCreative() && !bl && !stack.isEmpty() && this.networkHandler.hasFeature(stack.getItem().getRequiredFeatures())) {
         this.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(-1, stack));
         this.client.player.getItemDropCooldown().increment();
      }
   }

   public void stopUsingItem(PlayerEntity player) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
      player.stopUsingItem();
   }

   public boolean hasExperienceBar() {
      return !VisualSettings.INSTANCE.hideModernHUDElements.isEnabled() && this.gameMode.isSurvivalLike();
   }

   public boolean hasLimitedAttackSpeed() {
      return !this.gameMode.isCreative();
   }

   public boolean hasCreativeInventory() {
      return this.gameMode.isCreative();
   }

   public boolean hasRidingInventory() {
      return this.client.player.hasVehicle() && this.client.player.getVehicle() instanceof RideableInventory;
   }

   public boolean isFlyingLocked() {
      return this.gameMode == GameMode.SPECTATOR;
   }

   @Nullable
   public GameMode getPreviousGameMode() {
      return this.previousGameMode;
   }

   public GameMode getCurrentGameMode() {
      return this.gameMode;
   }

   public boolean isBreakingBlock() {
      return this.breakingBlock;
   }

   public int getBlockBreakingProgress() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
         return (int)(this.currentBreakingProgress * 10.0F) - 1;
      }

      return this.currentBreakingProgress > 0.0F ? (int)(this.currentBreakingProgress * 10.0F) : -1;
   }

   private PlayerActionC2SPacket viaFabricPlus$createPlayerAction(Action action, BlockPos pos, Direction direction) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)) {
         this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(action, pos);
      }

      return new PlayerActionC2SPacket(action, pos, direction);
   }

   private boolean viaFabricPlus$breakBlock(BlockPos pos, Direction direction) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)
         && this.viaFabricPlus$extinguishFire(pos, direction)) {
         return false;
      }

      return this.breakBlock(pos);
   }

   private boolean viaFabricPlus$extinguishFire(BlockPos pos, Direction direction) {
      BlockPos firePos = pos.offset(direction);
      if (!this.client.world.getBlockState(firePos).isOf(Blocks.FIRE)) {
         return false;
      }

      this.client.world.syncWorldEvent(this.client.player, 1009, firePos, 0);
      this.client.world.removeBlock(firePos, false);
      return true;
   }

   private void viaFabricPlus$interactBlockLegacy(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, ItemStack itemStack) {
      BlockHitResult placementHitResult = hitResult;
      if (itemStack.getItem() instanceof BlockItem) {
         BlockState clickedBlock = this.client.world.getBlockState(hitResult.getBlockPos());
         if (clickedBlock.isOf(Blocks.SNOW) && clickedBlock.get(SnowBlock.LAYERS) == 1) {
            placementHitResult = hitResult.withSide(Direction.UP);
         }

         ItemPlacementContext placementContext = new ItemPlacementContext(new ItemUsageContext(player, hand, placementHitResult));
         if (!placementContext.canPlace() || ((BlockItem)placementContext.getStack().getItem()).getPlacementState(placementContext) == null) {
            throw new ActionResultException1_12_2(ActionResult.PASS);
         }
      }

      this.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
      if (itemStack.isEmpty()) {
         throw new ActionResultException1_12_2(ActionResult.PASS);
      }

      ItemUsageContext usageContext = new ItemUsageContext(player, hand, placementHitResult);
      ActionResult actionResult;
      if (this.gameMode.isCreative()) {
         int count = itemStack.getCount();
         actionResult = itemStack.useOnBlock(usageContext);
         itemStack.setCount(count);
      } else {
         actionResult = itemStack.useOnBlock(usageContext);
      }

      if (!actionResult.isAccepted()) {
         actionResult = ActionResult.PASS;
      }

      throw new ActionResultException1_12_2(actionResult);
   }

   @Override
   public ClientPlayerInteractionManager1_18_2 viaFabricPlus$get1_18_2InteractionManager() {
      return this.viaFabricPlus$1_18_2InteractionManager;
   }

   public void pickItemFromBlock(BlockPos pos, boolean includeData) {
      this.networkHandler.sendPacket(new PickItemFromBlockC2SPacket(pos, includeData));
   }

   public void pickItemFromEntity(Entity entity, boolean includeData) {
      this.networkHandler.sendPacket(new PickItemFromEntityC2SPacket(entity.getId(), includeData));
   }

   public void slotChangedState(int slot, int screenHandlerId, boolean newState) {
      this.networkHandler.sendPacket(new SlotChangedStateC2SPacket(slot, screenHandlerId, newState));
   }
}
