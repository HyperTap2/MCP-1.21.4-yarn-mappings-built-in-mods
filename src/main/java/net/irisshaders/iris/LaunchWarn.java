package net.irisshaders.iris;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class LaunchWarn {
   public static void main(String[] args) {
      String message = "This file is the mod version of Iris, meant to be installed as a mod. Would you like to get the Iris Installer instead?";
      String fallback = "This file is the mod version of Iris, meant to be installed as a mod. Please download the Iris Installer from https://irisshaders.dev.";
      if (GraphicsEnvironment.isHeadless()) {
         System.err.println(fallback);
      } else {
         try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (ReflectiveOperationException | UnsupportedLookAndFeelException var6) {
         }

         if (Desktop.getDesktop().isSupported(Action.BROWSE)) {
            int option = JOptionPane.showOptionDialog(null, message, "Iris Installer", 0, 1, null, null, null);
            if (option == 0) {
               try {
                  Desktop.getDesktop().browse(URI.create("https://irisshaders.dev"));
               } catch (IOException e) {
                  System.out.println("Welp; we're screwed.");
                  e.printStackTrace();
               }
            }
         } else {
            JOptionPane.showMessageDialog(null, fallback);
         }
      }

      System.exit(0);
   }
}
