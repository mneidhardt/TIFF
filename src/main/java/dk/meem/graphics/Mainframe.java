package dk.meem.graphics;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.text.*;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;


class Mainframe extends JFrame implements MouseListener, MouseMotionListener, ActionListener {
   private DrawPanel drawarea;
   private JFileChooser fc;
   int maxX=600, maxY=600;    // Default values?!
   String basefile="";          // Which file are we working with.


   Mainframe(String args[]) {
      super("Tiffviewercvxcv");

      setPreferredSize(new Dimension(maxX, maxY));

      addMouseListener(this);
      addMouseMotionListener(this);

      setupMenu();

      drawarea = new DrawPanel(maxX, maxY, this);

      Container pane = getContentPane();
      JScrollPane scrollpane = new JScrollPane(drawarea);
      scrollpane.setPreferredSize(new Dimension(maxX, maxY));
      pane.add(scrollpane, BorderLayout.CENTER);
      setVisible(true);
      pack();

      try {
         if (args.length == 1) {
            System.out.println("Opening file " + args[0]);
            setTitle("Tiffviewer: " + args[0]);
            drawarea.drawTiffImage(args[0]);
         }
         else if (args.length > 1 && args[1].equals("-z")) {         // -z for zoom
            System.out.println("Zooming image " + args[0]);
            setTitle(args[0] + " (50%)");
            drawarea.zoomTiffImage(args[0]);
         }
      }
      catch (IOException ioe) {
         System.err.println("IO Error in openFile: " + ioe.toString());
      }
   }



   public void actionPerformed(ActionEvent e) {
      System.out.println("Action performed: " + e.getActionCommand());

      if(e.getActionCommand() == "Open file") {
         openFile();
      }
      else if(e.getActionCommand() == "Exit") {
         System.exit(0);
      }
   }


   private void openFile() {
      File file=null;

      int returnVal = fc.showOpenDialog(Mainframe.this);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         file = fc.getSelectedFile();
      }

      if (file == null) {
         return;
      }
      else {
         if ((file.toString()).endsWith(".tif")) {
            try {
               System.out.println("Opening file " + file.toString());
               setTitle(file.toString());
               drawarea.drawTiffImage(file.toString());
            }
            catch (IOException ioe) {
               System.err.println("IO Error in openFile.");
            }
         }
      }
   }



   private void setupMenu() {
      JMenuBar bar = new JMenuBar();
      setJMenuBar(bar);

      JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic(KeyEvent.VK_F);
      //bar.add(fileMenu);


      JMenuItem fileMenuItems[] = new JMenuItem[2];
      fileMenuItems[0] = new JMenuItem("Open file");
      fileMenuItems[0].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
      fileMenuItems[0].addActionListener(this);
      fileMenu.add(fileMenuItems[0]);

      fileMenuItems[1]  = new JMenuItem("Exit");
      fileMenuItems[1].addActionListener(this);
      fileMenu.add(fileMenuItems[1]);

      fc = new JFileChooser();
   }


   //public void setTitle(String newtitle) {
   //   super.setTitle(newtitle);
   //}

    public void mouseDragged(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}


    private void showMsg(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }
}