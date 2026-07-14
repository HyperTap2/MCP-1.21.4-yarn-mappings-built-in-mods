package net.minecraft.client.gui.navigation;


public interface GuiNavigation {
   NavigationDirection getDirection();

   record Arrow(NavigationDirection direction) implements GuiNavigation {
      @Override
      public NavigationDirection getDirection() {
         return this.direction.getAxis() == NavigationAxis.VERTICAL ? this.direction : NavigationDirection.DOWN;
      }
   }

   class Down implements GuiNavigation {
      @Override
      public NavigationDirection getDirection() {
         return NavigationDirection.DOWN;
      }
   }

   record Tab(boolean forward) implements GuiNavigation {
      @Override
      public NavigationDirection getDirection() {
         return this.forward ? NavigationDirection.DOWN : NavigationDirection.UP;
      }
   }
}
