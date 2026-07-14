// SPDX-License-Identifier: GPL-3.0-or-later
package com.viaversion.viafabricplus.visuals.features.classic.creative_menu;

import com.viaversion.viafabricplus.ViaFabricPlus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class GridItemSelectionScreen extends Screen {
   public static final GridItemSelectionScreen INSTANCE = new GridItemSelectionScreen();
   private static final int COLUMNS = 9;
   private static final int CELL_SIZE = 25;
   private static final int SIDE_OFFSET = 15;
   public Item[][] itemGrid;
   private ItemStack selectedItem;

   private GridItemSelectionScreen() {
      super(Text.of("Classic item selection"));
   }

   @Override
   protected void init() {
      if (this.itemGrid != null) {
         return;
      }

      List<Item> allowedItems = new ArrayList<>();
      for (Item item : Registries.ITEM) {
         if (item != Items.AIR && item.getRequiredFeatures().contains(FeatureFlags.VANILLA) && ViaFabricPlus.getImpl().itemExistsInConnection(item)) {
            allowedItems.add(item);
         }
      }

      this.itemGrid = new Item[MathHelper.ceil(allowedItems.size() / (double)COLUMNS)][COLUMNS];
      for (int index = 0; index < allowedItems.size(); index++) {
         this.itemGrid[index / COLUMNS][index % COLUMNS] = allowedItems.get(index);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.selectedItem != null) {
         this.client.interactionManager.clickCreativeStack(this.selectedItem, this.client.player.getInventory().selectedSlot + 36);
         this.client.player.getInventory().main.set(this.client.player.getInventory().selectedSlot, this.selectedItem);
         this.client.player.playerScreenHandler.sendContentUpdates();
         ClickableWidget.playClickSound(this.client.getSoundManager());
         this.close();
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
         this.close();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      int boxWidth = 255;
      int boxHeight = CELL_SIZE * this.itemGrid.length + 45;
      int renderX = this.width / 2 - boxWidth / 2;
      int renderY = this.height / 2 - boxHeight / 2;
      context.fill(renderX, renderY, renderX + boxWidth, renderY + boxHeight, Integer.MIN_VALUE);
      context.drawCenteredTextWithShadow(this.textRenderer, "Select block", renderX + boxWidth / 2, renderY + 15, -1);
      this.selectedItem = null;

      int y = 30;
      for (Item[] items : this.itemGrid) {
         int x = SIDE_OFFSET;
         for (Item item : items) {
            if (item != null) {
               if (mouseX > renderX + x && mouseY > renderY + y && mouseX < renderX + x + CELL_SIZE && mouseY < renderY + y + CELL_SIZE) {
                  context.fill(renderX + x, renderY + y, renderX + x + CELL_SIZE, renderY + y + CELL_SIZE, Integer.MAX_VALUE);
                  this.selectedItem = item.getDefaultStack();
               }
               context.drawItem(item.getDefaultStack(), renderX + x + 4, renderY + y + 4);
               x += CELL_SIZE;
            }
         }
         y += CELL_SIZE;
      }
   }
}
