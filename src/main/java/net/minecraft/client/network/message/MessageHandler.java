package net.minecraft.client.network.message;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.session.report.log.ChatLog;
import net.minecraft.client.session.report.log.ReceivedMessage;
import net.minecraft.network.message.FilterMask;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class MessageHandler {
   private static final Text VALIDATION_ERROR_TEXT = Text.translatable("chat.validation_error").formatted(new Formatting[]{Formatting.RED, Formatting.ITALIC});
   private final MinecraftClient client;
   private final Deque<MessageHandler.ProcessableMessage> delayedMessages = Queues.newArrayDeque();
   private long chatDelay;
   private long lastProcessTime;

   public MessageHandler(MinecraftClient client) {
      this.client = client;
   }

   public void processDelayedMessages() {
      if (this.chatDelay != 0L) {
         if (Util.getMeasuringTimeMs() >= this.lastProcessTime + this.chatDelay) {
            MessageHandler.ProcessableMessage processableMessage = this.delayedMessages.poll();

            while (processableMessage != null && !processableMessage.accept()) {
               processableMessage = this.delayedMessages.poll();
            }
         }
      }
   }

   public void setChatDelay(double chatDelay) {
      long l = (long)(chatDelay * 1000.0);
      if (l == 0L && this.chatDelay > 0L) {
         this.delayedMessages.forEach(MessageHandler.ProcessableMessage::accept);
         this.delayedMessages.clear();
      }

      this.chatDelay = l;
   }

   public void process() {
      this.delayedMessages.remove().accept();
   }

   public long getUnprocessedMessageCount() {
      return this.delayedMessages.size();
   }

   public void processAll() {
      this.delayedMessages.forEach(MessageHandler.ProcessableMessage::accept);
      this.delayedMessages.clear();
   }

   public boolean removeDelayedMessage(MessageSignatureData signature) {
      return this.delayedMessages.removeIf(message -> signature.equals(message.signature()));
   }

   private boolean shouldDelay() {
      return this.chatDelay > 0L && Util.getMeasuringTimeMs() < this.lastProcessTime + this.chatDelay;
   }

   private void process(@Nullable MessageSignatureData signature, BooleanSupplier processor) {
      if (this.shouldDelay()) {
         this.delayedMessages.add(new MessageHandler.ProcessableMessage(signature, processor));
      } else {
         processor.getAsBoolean();
      }
   }

   public void onChatMessage(SignedMessage message, GameProfile sender, Parameters params) {
      boolean bl = this.client.options.getOnlyShowSecureChat().getValue();
      SignedMessage signedMessage = bl ? message.withoutUnsigned() : message;
      Text text = params.applyChatDecoration(signedMessage.getContent());
      Instant instant = Instant.now();
      this.process(message.signature(), () -> {
         boolean bl2 = this.processChatMessageInternal(params, message, text, sender, bl, instant);
         ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
         if (clientPlayNetworkHandler != null) {
            clientPlayNetworkHandler.acknowledge(message, bl2);
         }

         return bl2;
      });
   }

   public void onUnverifiedMessage(UUID sender, Parameters parameters) {
      this.process(null, () -> {
         if (this.client.shouldBlockMessages(sender)) {
            return false;
         }

         Text text = parameters.applyChatDecoration(VALIDATION_ERROR_TEXT);
         this.client.inGameHud.getChatHud().addMessage(text, null, MessageIndicator.chatError());
         this.lastProcessTime = Util.getMeasuringTimeMs();
         return true;
      });
   }

   public void onProfilelessMessage(Text content, Parameters params) {
      Instant instant = Instant.now();
      this.process(null, () -> {
         Text text2 = params.applyChatDecoration(content);
         this.client.inGameHud.getChatHud().addMessage(text2);
         this.narrate(params, content);
         this.addToChatLog(text2, instant);
         this.lastProcessTime = Util.getMeasuringTimeMs();
         return true;
      });
   }

   private boolean processChatMessageInternal(
      Parameters params, SignedMessage message, Text decorated, GameProfile sender, boolean onlyShowSecureChat, Instant receptionTimestamp
   ) {
      MessageTrustStatus messageTrustStatus = this.getStatus(message, decorated, receptionTimestamp);
      if (onlyShowSecureChat && messageTrustStatus.isInsecure()) {
         return false;
      }

      if (!this.client.shouldBlockMessages(message.getSender()) && !message.isFullyFiltered()) {
         MessageIndicator messageIndicator = messageTrustStatus.createIndicator(message);
         MessageSignatureData messageSignatureData = message.signature();
         FilterMask filterMask = message.filterMask();
         if (filterMask.isPassThrough()) {
            this.client.inGameHud.getChatHud().addMessage(decorated, messageSignatureData, messageIndicator);
            this.narrate(params, message.getContent());
         } else {
            Text text = filterMask.getFilteredText(message.getSignedContent());
            if (text != null) {
               this.client.inGameHud.getChatHud().addMessage(params.applyChatDecoration(text), messageSignatureData, messageIndicator);
               this.narrate(params, text);
            }
         }

         this.addToChatLog(message, params, sender, messageTrustStatus);
         this.lastProcessTime = Util.getMeasuringTimeMs();
         return true;
      } else {
         return false;
      }
   }

   private void narrate(Parameters params, Text message) {
      this.client.getNarratorManager().narrateChatMessage(params.applyNarrationDecoration(message));
   }

   private MessageTrustStatus getStatus(SignedMessage message, Text decorated, Instant receptionTimestamp) {
      return this.isAlwaysTrusted(message.getSender()) ? MessageTrustStatus.SECURE : MessageTrustStatus.getStatus(message, decorated, receptionTimestamp);
   }

   private void addToChatLog(SignedMessage message, Parameters params, GameProfile sender, MessageTrustStatus trustStatus) {
      ChatLog chatLog = this.client.getAbuseReportContext().getChatLog();
      chatLog.add(ReceivedMessage.of(sender, message, trustStatus));
   }

   private void addToChatLog(Text message, Instant timestamp) {
      ChatLog chatLog = this.client.getAbuseReportContext().getChatLog();
      chatLog.add(ReceivedMessage.of(message, timestamp));
   }

   public void onGameMessage(Text message, boolean overlay) {
      if (!this.client.options.getHideMatchedNames().getValue() || !this.client.shouldBlockMessages(this.extractSender(message))) {
         if (overlay) {
            this.client.inGameHud.setOverlayMessage(message, false);
         } else {
            this.client.inGameHud.getChatHud().addMessage(message);
            this.addToChatLog(message, Instant.now());
         }

         this.client.getNarratorManager().narrateSystemMessage(message);
      }
   }

   private UUID extractSender(Text text) {
      String string = TextVisitFactory.removeFormattingCodes(text);
      String string2 = StringUtils.substringBetween(string, "<", ">");
      return string2 == null ? Util.NIL_UUID : this.client.getSocialInteractionsManager().getUuid(string2);
   }

   private boolean isAlwaysTrusted(UUID sender) {
      if (this.client.isInSingleplayer() && this.client.player != null) {
         UUID uUID = this.client.player.getGameProfile().getId();
         return uUID.equals(sender);
      } else {
         return false;
      }
   }

   record ProcessableMessage(@Nullable MessageSignatureData signature, BooleanSupplier handler) {
      public boolean accept() {
         return this.handler.getAsBoolean();
      }
   }
}
