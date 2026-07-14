package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandSource.RelativePosition;
import net.minecraft.command.CommandSource.SuggestedIdType;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatSuggestionsS2CPacket.Action;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.jetbrains.annotations.Nullable;

public class ClientCommandSource implements CommandSource {
   private final ClientPlayNetworkHandler networkHandler;
   private final MinecraftClient client;
   private int completionId = -1;
   @Nullable
   private CompletableFuture<Suggestions> pendingCommandCompletion;
   private final Set<String> chatSuggestions = new HashSet<>();

   public ClientCommandSource(ClientPlayNetworkHandler networkHandler, MinecraftClient client) {
      this.networkHandler = networkHandler;
      this.client = client;
   }

   public Collection<String> getPlayerNames() {
      if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         return this.chatSuggestions;
      }

      List<String> list = Lists.newArrayList();

      for (PlayerListEntry playerListEntry : this.networkHandler.getPlayerList()) {
         list.add(playerListEntry.getProfile().getName());
      }

      return list;
   }

   public Collection<String> getChatSuggestions() {
      if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         return this.chatSuggestions;
      }

      if (this.chatSuggestions.isEmpty()) {
         return this.getPlayerNames();
      }

      Set<String> set = new HashSet<>(this.getPlayerNames());
      set.addAll(this.chatSuggestions);
      return set;
   }

   public Collection<String> getEntitySuggestions() {
      return this.client.crosshairTarget != null && this.client.crosshairTarget.getType() == Type.ENTITY
         ? Collections.singleton(((EntityHitResult)this.client.crosshairTarget).getEntity().getUuidAsString())
         : Collections.emptyList();
   }

   public Collection<String> getTeamNames() {
      return this.networkHandler.getScoreboard().getTeamNames();
   }

   public Stream<Identifier> getSoundIds() {
      return this.client.getSoundManager().getKeys().stream();
   }

   public boolean hasPermissionLevel(int level) {
      ClientPlayerEntity clientPlayerEntity = this.client.player;
      return clientPlayerEntity != null ? clientPlayerEntity.hasPermissionLevel(level) : level == 0;
   }

   public CompletableFuture<Suggestions> listIdSuggestions(
      RegistryKey<? extends Registry<?>> registryRef, SuggestedIdType suggestedIdType, SuggestionsBuilder builder, CommandContext<?> context
   ) {
      return this.getRegistryManager().getOptional(registryRef).map(registry -> {
         this.suggestIdentifiers(registry, suggestedIdType, builder);
         return builder.buildFuture();
      }).orElseGet(() -> this.getCompletions(context));
   }

   public CompletableFuture<Suggestions> getCompletions(CommandContext<?> context) {
      if (this.pendingCommandCompletion != null) {
         this.pendingCommandCompletion.cancel(false);
      }

      this.pendingCommandCompletion = new CompletableFuture<>();
      int i = ++this.completionId;
      this.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(i, context.getInput()));
      return this.pendingCommandCompletion;
   }

   private static String format(double d) {
      return String.format(Locale.ROOT, "%.2f", d);
   }

   private static String format(int i) {
      return Integer.toString(i);
   }

   public Collection<RelativePosition> getBlockPositionSuggestions() {
      HitResult hitResult = this.client.crosshairTarget;
      if (hitResult != null && hitResult.getType() == Type.BLOCK) {
         BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
         return Collections.singleton(new RelativePosition(format(blockPos.getX()), format(blockPos.getY()), format(blockPos.getZ())));
      } else {
         return CommandSource.super.getBlockPositionSuggestions();
      }
   }

   public Collection<RelativePosition> getPositionSuggestions() {
      HitResult hitResult = this.client.crosshairTarget;
      if (hitResult != null && hitResult.getType() == Type.BLOCK) {
         Vec3d vec3d = hitResult.getPos();
         return Collections.singleton(new RelativePosition(format(vec3d.x), format(vec3d.y), format(vec3d.z)));
      } else {
         return CommandSource.super.getPositionSuggestions();
      }
   }

   public Set<RegistryKey<World>> getWorldKeys() {
      return this.networkHandler.getWorldKeys();
   }

   public DynamicRegistryManager getRegistryManager() {
      return this.networkHandler.getRegistryManager();
   }

   public FeatureSet getEnabledFeatures() {
      return this.networkHandler.getEnabledFeatures();
   }

   public void onCommandSuggestions(int completionId, Suggestions suggestions) {
      if (completionId == this.completionId) {
         this.pendingCommandCompletion.complete(suggestions);
         this.pendingCommandCompletion = null;
         this.completionId = -1;
      }
   }

   public void onChatSuggestions(Action action, List<String> suggestions) {
      switch (action) {
         case ADD:
            this.chatSuggestions.addAll(suggestions);
            break;
         case REMOVE:
            suggestions.forEach(this.chatSuggestions::remove);
            break;
         case SET:
            this.chatSuggestions.clear();
            this.chatSuggestions.addAll(suggestions);
      }
   }
}
