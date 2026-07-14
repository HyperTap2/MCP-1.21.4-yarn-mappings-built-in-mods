package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public class GameModeSelectionScreen extends Screen {
   static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("gamemode_switcher/slot");
   static final Identifier SELECTION_TEXTURE = Identifier.ofVanilla("gamemode_switcher/selection");
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/gamemode_switcher.png");
   private static final int TEXTURE_WIDTH = 128;
   private static final int TEXTURE_HEIGHT = 128;
   private static final int BUTTON_SIZE = 26;
   private static final int ICON_OFFSET = 5;
   private static final int field_32314 = 31;
   private static final int field_32315 = 5;
   private static final Text SELECT_NEXT_TEXT = Text.translatable(
      "debug.gamemodes.select_next", new Object[]{Text.translatable("debug.gamemodes.press_f4").formatted(Formatting.AQUA)}
   );
   private final GameModeSelectionScreen.GameModeSelection currentGameMode;
   private GameModeSelectionScreen.GameModeSelection gameMode;
   private int lastMouseX;
   private int lastMouseY;
   private boolean mouseUsedForSelection;
   private final List<GameModeSelectionScreen.ButtonWidget> gameModeButtons = Lists.newArrayList();
   private final GameModeSelectionScreen.GameModeSelection[] availableGameModes;
   private final int uiWidth;

   public GameModeSelectionScreen() {
      super(NarratorManager.EMPTY);
      this.currentGameMode = GameModeSelectionScreen.GameModeSelection.of(this.getPreviousGameMode());
      this.gameMode = this.currentGameMode;
      if (ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
         List<GameModeSelectionScreen.GameModeSelection> selections = new ArrayList<>(Arrays.asList(GameModeSelectionScreen.GameModeSelection.values()));
         selections.remove(GameModeSelectionScreen.GameModeSelection.SPECTATOR);
         if (ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)) {
            selections.remove(GameModeSelectionScreen.GameModeSelection.ADVENTURE);
         }

         this.availableGameModes = selections.toArray(GameModeSelectionScreen.GameModeSelection[]::new);
      } else {
         this.availableGameModes = GameModeSelectionScreen.GameModeSelection.VALUES;
      }

      this.uiWidth = this.availableGameModes.length * 31 - 5;
   }

   private GameMode getPreviousGameMode() {
      ClientPlayerInteractionManager clientPlayerInteractionManager = MinecraftClient.getInstance().interactionManager;
      GameMode gameMode = clientPlayerInteractionManager.getPreviousGameMode();
      if (gameMode != null) {
         return gameMode;
      } else {
         return clientPlayerInteractionManager.getCurrentGameMode() == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE;
      }
   }

   @Override
   protected void init() {
      if (ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
         this.close();
         return;
      }

      super.init();
      this.gameMode = this.currentGameMode;

      for (int i = 0; i < this.availableGameModes.length; i++) {
         GameModeSelectionScreen.GameModeSelection gameModeSelection = this.availableGameModes[i];
         this.gameModeButtons
            .add(new GameModeSelectionScreen.ButtonWidget(gameModeSelection, this.width / 2 - this.uiWidth / 2 + i * 31, this.height / 2 - 31));
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.checkForClose()) {
         context.getMatrices().push();
         int i = this.width / 2 - 62;
         int j = this.height / 2 - 31 - 27;
         context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, i, j, 0.0F, 0.0F, 125, 75, 128, 128);
         context.getMatrices().pop();
         super.render(context, mouseX, mouseY, delta);
         context.drawCenteredTextWithShadow(this.textRenderer, this.gameMode.getText(), this.width / 2, this.height / 2 - 31 - 20, -1);
         context.drawCenteredTextWithShadow(this.textRenderer, SELECT_NEXT_TEXT, this.width / 2, this.height / 2 + 5, 16777215);
         if (!this.mouseUsedForSelection) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.mouseUsedForSelection = true;
         }

         boolean bl = this.lastMouseX == mouseX && this.lastMouseY == mouseY;

         for (GameModeSelectionScreen.ButtonWidget buttonWidget : this.gameModeButtons) {
            buttonWidget.render(context, mouseX, mouseY, delta);
            buttonWidget.setSelected(this.gameMode == buttonWidget.gameMode);
            if (!bl && buttonWidget.isSelected()) {
               this.gameMode = buttonWidget.gameMode;
            }
         }
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   private void apply() {
      apply(this.client, this.gameMode);
   }

   private static void apply(MinecraftClient client, GameModeSelectionScreen.GameModeSelection gameModeSelection) {
      if (client.interactionManager != null && client.player != null) {
         GameModeSelectionScreen.GameModeSelection gameModeSelection2 = GameModeSelectionScreen.GameModeSelection.of(
            client.interactionManager.getCurrentGameMode()
         );
         GameModeSelectionScreen.GameModeSelection gameModeSelection3 = gameModeSelection;
         if (client.player.hasPermissionLevel(2) && gameModeSelection3 != gameModeSelection2) {
            client.player.networkHandler.sendCommand(gameModeSelection3.getCommand());
         }
      }
   }

   private boolean checkForClose() {
      if (!InputUtil.isKeyPressed(this.client.getWindow().getHandle(), 292)) {
         this.apply();
         this.client.setScreen(null);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 293) {
         this.mouseUsedForSelection = false;
         this.gameMode = this.gameMode.next();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public static class ButtonWidget extends ClickableWidget {
      final GameModeSelectionScreen.GameModeSelection gameMode;
      private boolean selected;

      public ButtonWidget(GameModeSelectionScreen.GameModeSelection gameMode, int x, int y) {
         super(x, y, 26, 26, gameMode.getText());
         this.gameMode = gameMode;
      }

      @Override
      public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
         this.drawBackground(context);
         this.gameMode.renderIcon(context, this.getX() + 5, this.getY() + 5);
         if (this.selected) {
            this.drawSelectionBox(context);
         }
      }

      @Override
      public void appendClickableNarrations(NarrationMessageBuilder builder) {
         this.appendDefaultNarrations(builder);
      }

      @Override
      public boolean isSelected() {
         return super.isSelected() || this.selected;
      }

      public void setSelected(boolean selected) {
         this.selected = selected;
      }

      private void drawBackground(DrawContext context) {
         context.drawGuiTexture(RenderLayer::getGuiTextured, GameModeSelectionScreen.SLOT_TEXTURE, this.getX(), this.getY(), 26, 26);
      }

      private void drawSelectionBox(DrawContext context) {
         context.drawGuiTexture(RenderLayer::getGuiTextured, GameModeSelectionScreen.SELECTION_TEXTURE, this.getX(), this.getY(), 26, 26);
      }
   }

   enum GameModeSelection {
      CREATIVE(Text.translatable("gameMode.creative"), "gamemode creative", new ItemStack(Blocks.GRASS_BLOCK)),
      SURVIVAL(Text.translatable("gameMode.survival"), "gamemode survival", new ItemStack(Items.IRON_SWORD)),
      ADVENTURE(Text.translatable("gameMode.adventure"), "gamemode adventure", new ItemStack(Items.MAP)),
      SPECTATOR(Text.translatable("gameMode.spectator"), "gamemode spectator", new ItemStack(Items.ENDER_EYE));

      protected static final GameModeSelectionScreen.GameModeSelection[] VALUES = values();
      private static final int field_32317 = 16;
      protected static final int field_32316 = 5;
      final Text text;
      final String command;
      final ItemStack icon;

      GameModeSelection(final Text text, final String command, final ItemStack icon) {
         this.text = text;
         this.command = command;
         this.icon = icon;
      }

      void renderIcon(DrawContext context, int x, int y) {
         context.drawItem(this.icon, x, y);
      }

      Text getText() {
         return this.text;
      }

      String getCommand() {
         if (ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)) {
            boolean creative = this == CREATIVE || this == SPECTATOR;
            return "gamemode " + MinecraftClient.getInstance().getSession().getUsername() + " " + creative;
         }

         return this.command;
      }

      GameModeSelectionScreen.GameModeSelection next() {
         if (ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            return switch (this) {
               case CREATIVE -> SURVIVAL;
               case SURVIVAL -> ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)
                  ? CREATIVE
                  : ADVENTURE;
               case ADVENTURE, SPECTATOR -> CREATIVE;
            };
         }

         return switch (this) {
            case CREATIVE -> SURVIVAL;
            case SURVIVAL -> ADVENTURE;
            case ADVENTURE -> SPECTATOR;
            case SPECTATOR -> CREATIVE;
         };
      }

      static GameModeSelectionScreen.GameModeSelection of(GameMode gameMode) {
         return switch (gameMode) {
            case SPECTATOR -> SPECTATOR;
            case SURVIVAL -> SURVIVAL;
            case CREATIVE -> CREATIVE;
            case ADVENTURE -> ADVENTURE;
            default -> throw new MatchException(null, null);
         };
      }
   }
}
