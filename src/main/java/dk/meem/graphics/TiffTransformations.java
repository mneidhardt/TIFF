package dk.meem.graphics;

/*
 * @(#)TiffHeader.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds methods for transforming a Tiff image.
 *
 */
import java.io.IOException;

public class TiffTransformations {
   private int imagelength, newlength;
   private int imagewidth, newwidth;
   private int imagetype=-1;
   private byte[][] imagedata;


   /**
   * C'tor with just filename.
   */
   TiffTransformations(String filename) throws IOException {
      TiffFile infile = new TiffFile(filename);
      infile.open();

      TiffHeader header = new TiffHeader(infile.getBytes(8));

      if (header == null) {
         throw new RuntimeException("TiffTransformations: "+
                                    "Cant open file.");
      }

      TiffIFDFile ifd = new TiffIFDFile(infile, header.getIFDOffset(), header.isLittleEndian());

      long imagelength[] = ifd.getValueOffset(TiffNames.IMAGELENGTH);
      long imagewidth[]  = ifd.getValueOffset(TiffNames.IMAGEWIDTH);
      long compression[] = ifd.getValueOffset(TiffNames.COMPRESSION);
      this.imagelength = (int)imagelength[3];
      this.imagewidth = (int)imagewidth[3];

      if (compression[3] != 1) {
         throw new RuntimeException("TiffTransformation: " +
                           "Cannot handle compressed images");
      }

      this.imagetype = ifd.getImageType();
      this.imagedata = ifd.getImageData();
   }



   /**
   * C'tor with dimensions and type of image.
   * @param imgdata The image data for the image to be trsnaformed.s
   * @param imagelength The length (ie height) of the image in pixels.
   * @param imagewidth The width of the image in pixels.
   * @param imagetype The type of image, as described in
   *                  TiffIFD.getImageType().
   */
   TiffTransformations(byte imgdata[][], int imagelength, int imagewidth, int imagetype) {
      this.imagedata = imgdata;
      this.imagelength = imagelength;
      this.imagewidth = imagewidth;
      this.imagetype = imagetype;
   }


   public byte[][] halfsizeImage() {
      if (imagetype == 2) {
         return halfsizeGray8();
      }
      else if (imagetype == 3) {
         return halfsizeStdRGB();
      }
      else {
         return null;
      }
   }


   /**
   * This will create a new image, half the size of the original, which
   * is contained in imagedata.
   * @return An array containing the new image.
   */
   private byte[][] halfsizeGray8() {
      newlength = imagelength/2;
      newwidth  = imagewidth/2;

      byte newimagedata[][] = new byte[newlength][newwidth];

      //System.out.println("Transformer: HalfSizeGray8: " + imagelength + "," + imagewidth + " => " + newlength + "," + newwidth);

      int MAXX, MAXY;
      if (imagedata.length%2 == 0)    { MAXY = imagedata.length-2; }
      else                            { MAXY = imagedata.length-1; }
      if (imagedata[0].length%2 == 0) { MAXX = imagedata[0].length-2; }
      else                            { MAXX = imagedata[0].length-1; }

      int x2=0, y2=0;
      int max[] = {0,0};      // Just for testing...


      for (int y=1; y<MAXY; y+=2) {
         for (int x=1; x<MAXX; x+=2) {
            float sum = (float)
                        ((imagedata[y-1][x-1] & 0xff) +
                           (imagedata[y-1][x] & 0xff) +
                                 (imagedata[y-1][x+1] & 0xff) +

                         (imagedata[y][x-1] & 0xff) +
                            (imagedata[y][x] & 0xff) +
                              (imagedata[y][x+1] & 0xff)  +

                         (imagedata[y+1][x-1] & 0xff) +
                           (imagedata[y+1][x] & 0xff) +
                              (imagedata[y+1][x+1] & 0xff));


            newimagedata[y2][x2] = (byte)(sum/9f);
            ++x2;

            max[1] = x;
         }
         x2 = 0;
         ++y2;

         max[0] = y;
      }

      System.out.println("MaxXY=" + max[0] + "," + max[1]);


      // If number of rows is even, we need to calculate a new last row.
      // Its done a little differently since we dont have 8 neighbours,
      // so we do it here - saves an if-stmt in the above.
      if (imagedata.length%2 == 0) {
         int lastrow = imagedata.length-1;
         int lastnewrow = newimagedata.length-1;
         x2=0;

         for (int x=1; x<imagedata[0].length-1; x+=2) {
            float sum = (float)
              ((imagedata[lastrow-1][x-1] & 0xff) +
                 (imagedata[lastrow-1][x] & 0xff) +
                    (imagedata[lastrow-1][x+1] & 0xff) +

               (imagedata[lastrow][x-1] & 0xff) +
                  (imagedata[lastrow][x] & 0xff) +
                     (imagedata[lastrow][x+1] & 0xff));

            newimagedata[lastnewrow][x2] = (byte)(sum/6f);
            ++x2;
            max[1] = x;
         }
         System.out.println("MaxXY=" + lastrow + "," + max[1] + " LastnewRow=" + lastnewrow);
      }

      // If number of columns is even, we need to calculate a new last column.
      // Its done a little differently since we dont have 8 neighbours,
      // so we do it here - saves an if-stmt in the above.
      if (imagedata[0].length%2 == 0) {
         int lastcol = imagedata[0].length-1;
         int lastnewcol = newimagedata[0].length-1;
         y2=0;

         for (int y=1; y<imagedata.length-1; y+=2) {
            float sum = (float)
              ((imagedata[y-1][lastcol-1] & 0xff) +
                  (imagedata[y-1][lastcol] & 0xff) +
               (imagedata[y][lastcol-1] & 0xff) +
                  (imagedata[y][lastcol] & 0xff) +
               (imagedata[y+1][lastcol-1] & 0xff) +
                  (imagedata[y+1][lastcol] & 0xff));

            newimagedata[y2][lastnewcol] = (byte)(sum/6f);
            ++y2;
            max[0] = y;
         }
         System.out.println("MaxXY=" + max[0] + "," + lastcol  + " LastnewCol=" + lastnewcol);
      }

      return newimagedata;
   }


   /**
   * This will create a new image, half the size of the original, which
   * is contained in imagedata. RGB images only here.
   * @return An array containing the new image.
   */
   private byte[][] halfsizeStdRGB() {
      newlength = imagelength/2;
      newwidth  = imagewidth/2;

      byte newimagedata[][] = new byte[newlength][newwidth*3];

      //System.out.print("Transformer: HalfSizeRGB:" + imagelength + "," + imagewidth + " => " + newlength + "x" + newwidth);
      //System.out.println("New bytearray:" + newlength + "x" + (newwidth*3));

      int MAXX, MAXY;
      if (imagedata.length%2 == 0)    { MAXY = imagedata.length-2; }
      else                            { MAXY = imagedata.length-1; }
      if (imagedata[0].length%2 == 0) { MAXX = imagedata[0].length-6; }
      else                            { MAXX = imagedata[0].length-3; }

      int y2=0;

      for (int y=1; y<MAXY; y+=2) {
         int x=3;
         int x2=0;

         while (x<MAXX) {
            float sumR = (float)
                        ((imagedata[y-1][x-3] & 0xff) + (imagedata[y-1][x] & 0xff) + (imagedata[y-1][x+3] & 0xff) +
                         (imagedata[y][x-3] & 0xff)   + (imagedata[y][x] & 0xff)   + (imagedata[y][x+3] & 0xff)   +
                         (imagedata[y+1][x-3] & 0xff) + (imagedata[y+1][x] & 0xff) + (imagedata[y+1][x+3] & 0xff));

            ++x;
            float sumG = (float)
                        ((imagedata[y-1][x-3] & 0xff) + (imagedata[y-1][x] & 0xff) + (imagedata[y-1][x+3] & 0xff) +
                         (imagedata[y][x-3] & 0xff)   + (imagedata[y][x] & 0xff)   + (imagedata[y][x+3] & 0xff)   +
                         (imagedata[y+1][x-3] & 0xff) + (imagedata[y+1][x] & 0xff) + (imagedata[y+1][x+3] & 0xff));

            ++x;
            float sumB = (float)
                        ((imagedata[y-1][x-3] & 0xff) + (imagedata[y-1][x] & 0xff) + (imagedata[y-1][x+3] & 0xff) +
                         (imagedata[y][x-3] & 0xff)   + (imagedata[y][x] & 0xff)   + (imagedata[y][x+3] & 0xff)   +
                         (imagedata[y+1][x-3] & 0xff) + (imagedata[y+1][x] & 0xff) + (imagedata[y+1][x+3] & 0xff));

            newimagedata[y2][x2] = (byte)(sumR/9f);
            ++x2;
            newimagedata[y2][x2] = (byte)(sumG/9f);
            ++x2;
            newimagedata[y2][x2] = (byte)(sumB/9f);
            ++x2;

            x += 4;
         }
         ++y2;
      }

      // HER SKAL DER OGS� LIGE CHECKES FOR OM SIDSTE R�KKE/S�JLE SKAL UDREGNES!!!!

      return newimagedata;

   }


   /** Returns the new width of the transformed image.
   */
   public int getNewImagewidth() {
      return newwidth;
   }


   /** Returns the new length of the transformed image.
   */
   public int getNewImagelength() {
      return newlength;
   }


   /** Returns the type of this image.
   */
   public int getImageType() {
      return imagetype;
   }
}
