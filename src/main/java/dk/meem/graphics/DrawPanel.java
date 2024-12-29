package dk.meem.graphics;

import javax.swing.*;

import java.awt.*;
import java.util.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;


class DrawPanel extends JPanel implements MouseListener, MouseMotionListener {

   // ------ First some global vars ------------

   private boolean showgrid=false, drawtiff=false, subifdsPresent=false;
   private int maxX, maxY;
   private Color backgroundColor = new Color(255,255,255);
   private Color textColor = new Color(0,0,100);

   private String infotxt="";

   //private short imgdata[][];
   private byte imgdata[][];

   private byte imgdata0[][];
   private byte imgdata1[][];
   private byte imgdata2[][];

   private long dims[] = {0L, 0L};
   private int imgtype=-1;
   private int imagewidth=-1, imagelength=-1;
   private int whichImage = 0;

   private Mainframe parent;

   private TiffIFDFile ifd[] = new TiffIFDFile[3];
   private Endian bitman;

   // ------ End of global vars ----------


   // Constructor:
   DrawPanel(int x, int y, Mainframe parent)  {
      maxX=x; maxY=y;
      setPreferredSize(new Dimension(maxX, maxY));
      addMouseListener(this);
      addMouseMotionListener(this);

      this.parent = parent;

      infotxt = "Your TIFF could be here.";

      RepaintManager.currentManager(this).setDoubleBufferingEnabled(false);

   }



   public void paintComponent(Graphics g) {
      int height = this.getHeight();
      int width = this.getWidth();

      g.setColor(backgroundColor);
      g.fillRect(0,0, width, height);

      g.setColor(textColor);

      if (showgrid) {
         drawGrid(g, width, height, 10);
         g.setColor(textColor);
      }

      if (drawtiff) {
         if (imgtype == 0) {
            drawBWTiffImage(g);
         }
         else if (imgtype == 1) {
            drawGrayTiffImage(g);
         }
         else if (imgtype == 2) {
            drawGrayTiffImage(g);
         }
         else if (imgtype == 3) {
            drawRGBTiffImage(g);
         }
      }
      else {
         g.drawString(infotxt, (width/2), (height/2));
      }
   }



   /**
   * This draws a TIFF black/white bitmap image.
   * @param g The Graphics object needed for drawing.
   */
   public void drawBWTiffImage(Graphics g) {
      int bitmasks[] = {128, 64, 32, 16, 8, 4, 2, 1};
      int xcoord=0;

      for (int y=0; y<imgdata.length; y++) {
         for (int x=0; x<imgdata[y].length; x++) {
            for (int k=0; k<8; k++) {     // Run through each bit of each byte.
               int tmp = imgdata[y][x] & bitmasks[k];

               if (tmp == 0) { g.setColor(Color.white); }
               else          { g.setColor(Color.black); }
               g.drawLine(xcoord, y, xcoord, y);
               ++xcoord;

               if (xcoord == imagewidth) {
                  xcoord = 0;
                  break;      // No more bits from current byte should be used.
               }

            }
         }
      }
   }


   /**
   * This draws a TIFF gray scale image.
   * Currently it assumes that BitsPerSample is 8.
   * I am not sure yet how to deal with a value of 4 here.
   * @param g The Graphics object needed for drawing.
   */
   public void drawGrayTiffImage(Graphics g) {
      //int startx=10, x=startx, y=10;

      for (int y=0; y<imgdata.length; y++) {
         for (int x=0; x<imgdata[y].length; x++) {
            g.setColor(new Color((imgdata[y][x] & 0xff),
                                 (imgdata[y][x] & 0xff),
                                 (imgdata[y][x] & 0xff)));
            g.drawLine(x, y, x, y);
         }
      }
   }


   /**
   * This draws a TIFF RGB image.
   * This is a crude version. It needs to take certain
   * things into consideration, I think...!!!!!!!
   * @param g The Graphics object needed for drawing.
   */
   public void drawRGBTiffImage(Graphics g) {
      //if ((dims[0]*dims[1]*3) != imgdata2.length) {
      //   System.out.println("Cannot draw this RGB: " +
      //               " Dims: " + dims[0] + "," + dims[1] +
      //               " And I read " + imgdata2.length + " bytes.");
      //   return;
      //}

      for (int y=0; y<imgdata.length; y++) {
         int x=0, xcoord=0;

         while (x < imgdata[y].length-2) {
            g.setColor(new Color((imgdata[y][x] & 0xff),
                                 (imgdata[y][x+1] & 0xff),
                                 (imgdata[y][x+2] & 0xff)));
            g.drawLine(xcoord, y, xcoord, y);

            ++xcoord;
            x += 3;
         }
      }
   }


   public void drawTiffImage(String filename) throws IOException {

      TiffFile infile = new TiffFile(filename);
      infile.open();
      TiffHeader header = new TiffHeader(infile.getBytes(8));
      header.showHeader();

      bitman = (header.isLittleEndian() ? new LittleEndian() : new BigEndian());

      ifd[0] = new TiffIFDFile(infile, header.getIFDOffset(), header.isLittleEndian());
      long vo[] = ifd[0].getValueOffset(TiffNames.SUBIFDS);
      if (vo == null) {
         //System.out.println("No SUBIFDS!");
         //System.exit(1);
      }
      else if (vo[2] > 5) {
         subifdsPresent=true;
         long subifds[] = infile.getData(vo, bitman);
         System.out.println("SUBIFDS: " + subifds[0] + ", " + subifds[1]);
         ifd[1] = new TiffIFDFile(infile, subifds[0], header.isLittleEndian());
         ifd[2] = new TiffIFDFile(infile, subifds[1], header.isLittleEndian());
      }

      long imgwidth[] = ifd[whichImage].getValueOffset(TiffNames.IMAGEWIDTH);
      imagewidth = (int)imgwidth[3];
      long imglength[] = ifd[whichImage].getValueOffset(TiffNames.IMAGELENGTH);
      imagelength = (int)imglength[3];

      getAllImageData();
   }


   private void getAllImageData() throws IOException {
      long photometint[];
      long compression[];

      try {
         if (subifdsPresent) {
            photometint = ifd[whichImage].getValueOffset(TiffNames.PHOTOMETRICINTERPRETATION);
            compression = ifd[whichImage].getValueOffset(TiffNames.COMPRESSION);
            imgtype = ifd[whichImage].getImageType();

            // I want a mouse click to change the image. And
            // I show smallest first, then it loops round
            // whenever user clicks mouse button.
            imgdata0 = ifd[2].getImageData();
            imgdata1 = ifd[1].getImageData();
            imgdata2 = ifd[0].getImageData();

            imgdata = imgdata0;
         }
         else {
            photometint = ifd[0].getValueOffset(TiffNames.PHOTOMETRICINTERPRETATION);
            compression = ifd[0].getValueOffset(TiffNames.COMPRESSION);
            imgtype = ifd[0].getImageType();

            imgdata = ifd[0].getImageData();
         }



         String info = "Image is " + imgtype + " = " + TiffNames.imagetypes[imgtype] +
                       " Width=" + imagewidth + " Length=" + imagelength + "\n" +
                       "Datadims: " + imgdata.length + "x" + imgdata[0].length;

         info += "\nPhotometIntrp:" + photometint[3] + ". " +
               (compression[3] == 1 ? " No compression." : " Compressed: " + compression[3]);

         System.out.println(info);

         if (compression[3] != 1) {
            System.err.println("Cannot handle compressed images.");
            System.exit(1);
         }
         else if (imgtype == 0) {
            drawtiff=true;
            System.out.println("Drawing BW");
            repaint();
         }
         else if (imgtype == 1) {
         }
         else if (imgtype == 2) {
            drawtiff=true;
            System.out.println("Drawing Gray/8");
            repaint();
         }
         else if (imgtype == 3) {
            drawtiff=true;
            System.out.println("Drawing RGB");
            repaint();
         }
      }
      catch (Exception e) {
         System.err.println("Error getting image data:\n" + e.toString());
      }

   }


   /**
   * Will try to zoom a TIFF image (i.e. down to 50% of original).
   */
   public void zoomTiffImage(String filename) throws IOException {
      TiffTransformations transformer = new TiffTransformations(filename);

      imgdata = transformer.halfsizeImage();
      System.out.println("New dims: " + imgdata.length + "," + imgdata[0].length);

      imgtype = transformer.getImageType();
      drawtiff=true;
      repaint();
   }



   private void drawGrid(Graphics g, int width, int height, int spacing) {
      g.setColor(new Color(210, 210, 210));

      for (int x=spacing; x<width; x+=spacing) {
         g.drawLine(x, 0, x, height);
      }
      for (int y=spacing; y<height; y+=spacing) {
         g.drawLine(0, y, width, y);
      }

   }


   public void setGrid() {
      showgrid = !showgrid;
      repaint();
   }



   public void mousePressed(MouseEvent e) {

      if ( ! subifdsPresent) {
         return;
      }

      whichImage = (whichImage + 1)%3;
      System.out.println("Image=" + whichImage);

      if (whichImage == 0) {
         imgdata = imgdata0;
      }
      else if (whichImage == 1) {
         imgdata = imgdata1;
      }
      else {
         imgdata = imgdata2;
      }

      repaint();
   }

   public void mouseReleased(MouseEvent e) {
   }

   public void mouseDragged(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) {}
   public void mouseExited(MouseEvent e) {}
   public void mouseClicked(MouseEvent e) {}

   public void mouseMoved(MouseEvent e) {
   }
}
