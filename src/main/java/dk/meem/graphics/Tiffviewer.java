package dk.meem.graphics;

import javax.swing.*;

class Tiffviewer {

   public static void main(String args[]) {

      // Set cross-platform Java L&F (also called "Metal")
      //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

      String syntax = "Syntax: java -jar Tiffviewer.jar filename\n";

      if (args.length == 0) {
         System.err.println(syntax);
         System.exit(1);
      }

      JFrame frame = new Mainframe(args);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }
}