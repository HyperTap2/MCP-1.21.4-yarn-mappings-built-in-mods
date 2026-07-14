package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.minecraft.text.Text;

public abstract class CommentedElementWidget<T extends OptionMenuElement> extends AbstractElementWidget<T> {
   public CommentedElementWidget(T element) {
      super(element);
   }

   public abstract Optional<Text> getCommentTitle();

   public abstract Optional<Text> getCommentBody();
}
