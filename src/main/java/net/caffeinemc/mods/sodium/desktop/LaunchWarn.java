package net.caffeinemc.mods.sodium.desktop;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.caffeinemc.mods.sodium.desktop.utils.browse.BrowseUrlHandler;

public class LaunchWarn {
   private static final String HELP_URL = "https://link.caffeinemc.net/guides/sodium/installation";
   private static final String RICH_MESSAGE = "<html><body><p style='width: 600px; padding: 0 0 8px 0;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. Instead, you must install Fabric Loader for Minecraft, and then place this file in your mods directory.</p><p style='width: 600px; padding: 0 0 8px 0;'>If this is your first time installing mods with Fabric Loader, then click the \"Help\" button for an installation guide.</p></body></html>";
   private static final String FALLBACK_MESSAGE = "<html><body><p style='width: 600px; padding: 0 0 8px 0;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. Instead, you must install Fabric Loader for Minecraft, and then place this file in your mods directory.</p><p style='width: 600px; padding: 0 0 8px 0;'>If this is your first time installing mods with Fabric Loader, then visit <i>https://link.caffeinemc.net/guides/sodium/installation</i> for an installation guide.</p></body></html>";
   private static final String FAILED_TO_BROWSE_MESSAGE = "<html><body><p style='width: 400px; padding: 0 0 8px 0;'>Failed to open the default browser! Your system may be misconfigured. Please open the URL <i>https://link.caffeinemc.net/guides/sodium/installation</i> manually.</p></body></html>";
   public static final String WINDOW_TITLE = "Sodium";

   public static void main(String[] args) {
      if (GraphicsEnvironment.isHeadless()) {
         showHeadlessError();
      } else {
         showGraphicalError();
      }
   }

   private static void showHeadlessError() {
      System.err
         .println(
            "<html><body><p style='width: 600px; padding: 0 0 8px 0;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. Instead, you must install Fabric Loader for Minecraft, and then place this file in your mods directory.</p><p style='width: 600px; padding: 0 0 8px 0;'>If this is your first time installing mods with Fabric Loader, then visit <i>https://link.caffeinemc.net/guides/sodium/installation</i> for an installation guide.</p></body></html>"
         );
   }

   private static void showGraphicalError() {
      trySetSystemLookAndFeel();
      trySetSystemFontPreferences();
      BrowseUrlHandler browseUrlHandler = BrowseUrlHandler.createImplementation();
      if (browseUrlHandler != null) {
         showRichGraphicalDialog(browseUrlHandler);
      } else {
         showFallbackGraphicalDialog();
      }

      System.exit(0);
   }

   private static void showRichGraphicalDialog(BrowseUrlHandler browseUrlHandler) {
      int selectedOption = showDialogBox(
         "<html><body><p style='width: 600px; padding: 0 0 8px 0;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. Instead, you must install Fabric Loader for Minecraft, and then place this file in your mods directory.</p><p style='width: 600px; padding: 0 0 8px 0;'>If this is your first time installing mods with Fabric Loader, then click the \"Help\" button for an installation guide.</p></body></html>",
         "Sodium",
         0,
         1,
         new String[]{"Help", "Close"},
         0
      );
      if (selectedOption == 0) {
         log("Opening URL: https://link.caffeinemc.net/guides/sodium/installation");

         try {
            browseUrlHandler.browseTo("https://link.caffeinemc.net/guides/sodium/installation");
         } catch (IOException var3) {
            log("Failed to open default web browser!", var3);
            showDialogBox(
               "<html><body><p style='width: 400px; padding: 0 0 8px 0;'>Failed to open the default browser! Your system may be misconfigured. Please open the URL <i>https://link.caffeinemc.net/guides/sodium/installation</i> manually.</p></body></html>",
               "Sodium",
               -1,
               2,
               null,
               -1
            );
         }
      }
   }

   private static void showFallbackGraphicalDialog() {
      showDialogBox(
         "<html><body><p style='width: 600px; padding: 0 0 8px 0;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. Instead, you must install Fabric Loader for Minecraft, and then place this file in your mods directory.</p><p style='width: 600px; padding: 0 0 8px 0;'>If this is your first time installing mods with Fabric Loader, then visit <i>https://link.caffeinemc.net/guides/sodium/installation</i> for an installation guide.</p></body></html>",
         "Sodium",
         -1,
         1,
         null,
         null
      );
   }

   private static int showDialogBox(String message, String title, int optionType, int messageType, String[] options, Object initialValue) {
      JOptionPane pane = new JOptionPane(message, messageType, optionType, null, options, initialValue);
      JDialog dialog = pane.createDialog(title);
      dialog.setVisible(true);
      Object selectedValue = pane.getValue();
      if (selectedValue == null) {
         return -1;
      } else if (options == null) {
         return selectedValue instanceof Integer ? (Integer)selectedValue : -1;
      } else {
         for (int counter = 0; counter < options.length; counter++) {
            String option = options[counter];
            if (option.equals(selectedValue)) {
               return counter;
            }
         }

         return -1;
      }
   }

   private static void trySetSystemLookAndFeel() {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (UnsupportedLookAndFeelException | ReflectiveOperationException var1) {
      }
   }

   private static void trySetSystemFontPreferences() {
      System.setProperty("awt.useSystemAAFontSettings", "on");
   }

   private static void log(String message) {
      System.err.println(message);
   }

   private static void log(String message, Throwable exception) {
      System.err.println(message);
      exception.printStackTrace(System.err);
   }
}
