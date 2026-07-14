package net.minecraft.client.gui.tab;

import java.util.function.Consumer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public interface Tab {
   Text getTitle();

   void forEachChild(Consumer<ClickableWidget> consumer);

   void refreshGrid(ScreenRect tabArea);
}
