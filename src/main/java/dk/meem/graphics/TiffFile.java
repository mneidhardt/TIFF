package dk.meem.graphics;

/*
 * @(#)TiffFile.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds a TIFF-file-handle and
 * can read bytes from it. This one using New I/O (java.nio).
 *
 */

import java.nio.*;
import java.nio.channels.*;

import dk.meem.basics.Endian;

import java.nio.ShortBuffer.*;
import java.io.*;


public class TiffFile {
   private File file;
   private int fpointer;
   private FileChannel fc;


   public TiffFile(String filename) {
      this.file = new File(filename);
      this.fc = null;
   }


   /** Opens the file.
   */
   public void open() throws FileNotFoundException {    //throws Exception {
      fpointer=0;    // The position in the file,
                     // i.e. the offset as defined in TIFF-spec.

      fc = new FileInputStream(file).getChannel();

      //ByteBuffer header = ByteBuffer.allocate(8);
      //fc.read(header);
   }


   /** Closes the file.
   * @return True if OK, else false.
   */
   public void close() throws IOException {
      if (fc != null) {
         fc.close();
      }
   }



   /**
   * The basic read - reads counter bytes from file.
   * @return bytebuffer.
   */
   public ByteBuffer readBytes(int counter) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);

      return buffer;
   }



   /**
   * The basic read - reads counter bytes from file, starting at offset.
   * @return bytebuffer.
   */
   public ByteBuffer readBytes(long offset, int counter) throws IOException {
      fc.position(offset);
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);

      return buffer;
   }



   /* The getBytes() and getShorts() methods are here partly because I previously
   *  read the bytes as shorts (ie I converted them while reading) when I used
   *  the old style IO. I have made my own Endian-converter, and that expects
   *  shorts, so these are made to make life a little easier.
   *  It is probably not the ideal way, e.g. perhaps ReverseBytes etc. might
   *  be useful also, but there's no time for that now.
   */



   /**
   * This method reads bytes and returns them in a byte array.
   * @return bytearray.
   */
   public byte[] getBytes(int counter) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);
      buffer.flip();

      byte result[] = new byte[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = buffer.get();
         ++i;
      }

      return result;
   }


   /**
   * This method reads bytes from a given offset and returns
   * them in a byte array.
   * @return bytearray.
   */
   public byte[] getBytes(long offset, int counter) throws IOException {
      fc.position(offset);
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);
      buffer.flip();

      byte result[] = new byte[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = buffer.get();
         ++i;
      }

      return result;
   }



   /**
   * This method reads bytes and returns them in a short array,
   * ie each byte is cast to a short.
   * @return shortarray.
   */
   public short[] getShorts(int counter) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);
      buffer.flip();

      short result[] = new short[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = (short)(buffer.get() & 0xff);
         ++i;
      }

      return result;
   }


   /**
   * This method reads bytes from a given offset and returns
   * them in a byte array.
   * @return shortarray.
   */
   public short[] getShorts(long offset, int counter) throws IOException {
      fc.position(offset);
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);
      buffer.flip();

      short result[] = new short[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = (short)(buffer.get() & 0xff);
         ++i;
      }

      return result;
   }


   /**
   * This method reads counter TIFF BYTES from a given offset and stores
   * each byte as a LONG.
   * I return LONGs because it gives me a uniform interface.
   * @return long array.
   */
   public long[] getTiffBytes(long offset, int counter, Endian bitman) throws IOException {
      fc.position(offset);
      ByteBuffer buffer = ByteBuffer.allocate(counter);
      fc.read(buffer);
      buffer.flip();

      long result[] = new long[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = (long)(buffer.get() & 0xff);
         ++i;
      }

      return result;
   }

   /**
   * This method reads counter TIFF SHORTS from a given offset and stores
   * each block of 2 bytes as a LONG.
   * I return LONGs because it gives me a uniform interface.
   * @return long array.
   */
   public long[] getTiffShorts(long offset, int counter, Endian bitman) throws IOException {
      fc.position(offset);
      ByteBuffer buffer = ByteBuffer.allocate(counter*2);
      fc.read(buffer);
      buffer.flip();

      long result[] = new long[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = (long)bitman.short2Int((short)(buffer.get() & 0xff),
                                            (short)(buffer.get() & 0xff)
                                           );
         ++i;
      }

      return result;
   }


   /**
   * This method reads counter TIFF LONGS from a given offset and stores
   * each block of 4 bytes as a LONG.
   * I return LONGs because it gives me a uniform interface.
   * @return long array.
   */
   public long[] getTiffLongs(long offset, int counter, Endian bitman) throws IOException {
      fc.position(offset);
      ByteBuffer buffer = ByteBuffer.allocate(counter*4);
      fc.read(buffer);
      buffer.flip();

      long result[] = new long[counter];
      int i=0;

      while (buffer.hasRemaining()) {
         result[i] = (long)bitman.short2Long((short)(buffer.get() & 0xff),
                                            (short)(buffer.get() & 0xff),
                                            (short)(buffer.get() & 0xff),
                                            (short)(buffer.get() & 0xff)
                                           );
         ++i;
      }

      return result;
   }


   /** Method that reads data for a given TIFF field.
   * @param valoff 4-element array: type, count, total_bytes, value/offset.
   * @param bitman The endian converter.
   * @return An array of count longs.
   */
   public long[] getData(long valoff[], Endian bitman) throws IOException {
      long data[]=null;

      if (valoff[2] > 4) {
         if (valoff[0] == 3) {       // TIFF Shorts
            data = getTiffShorts(valoff[3], (int)valoff[1], bitman);
         }
         else if (valoff[0] == 4 || valoff[0] == 13) {  // TIFF Longs
            data = getTiffLongs(valoff[3], (int)valoff[1], bitman);
         }
         else {
            System.out.println("\nNB: TiffFile/getData(): " +
                  " DONT HANDLE THIS TYPE: " + valoff[0]);
         }
         // ------- I SHOULD HANDLE ALL TYPES HERE ??? -----------
      }
      else {                     // It is a value.
         data = new long[1];
         data[0] = valoff[3];
      }


      return data;
   }




   /* Reads image data into an NxM array of bytes - for images stored
   * in strips.
   * N is the number of scanlines in the image and M is the number of
   * uncompressed bytes needed to describe one row of pixels of the
   * image.
   *
   * @param numOfRows Rows in image data array (ie scanlines in image).
   * @param numOfColumns Number of bytes needed for describing 1
   *                     scanline of the image.
   * @param offsets Array with offsets for all strips.
   * @param bytecounts Array with bytecount for each strip.
   * @return An array containing all image data.
   */
   public byte[][] getStripImageData(int numOfRows,
                                     int numOfColumns,
                                     long offsets[],
                                     long bytecounts[])
                                                 throws IOException {

      //System.out.println("rows/cols=" + numOfRows + "/" + numOfColumns);
      byte imgdata[][] = new byte[numOfRows][numOfColumns];
      int rowcount=0, columncount=0;

      for (int i=0; i<offsets.length; i++) {
         fc.position(offsets[i]);
         ByteBuffer buffer = ByteBuffer.allocate((int)bytecounts[i]);
         fc.read(buffer);
         buffer.flip();

         // Now this buffer has to be filled into the imgdata byte array!
         // Possibly not the best solution.
         while (buffer.hasRemaining()) {
            imgdata[rowcount][columncount] = buffer.get();

            ++columncount;

            if (columncount == imgdata[rowcount].length) {
               ++rowcount;
               columncount=0;
            }
         }
      }

      return imgdata;
   }


   /* Reads image data into an NxM array of bytes - for images stored
   * in tiles.
   * N is the number of scanlines in the image and M is the number of
   * uncompressed bytes needed to describe one row of pixels of the
   * image.
   * @param numOfRows Rows in image data array (ie scanlines in image).
   * @param numOfColumns Number of bytes needed for describing 1
   *                     scanline of the image.
   * @param offsets Array with offsets for all tiles.
   * @param bytecounts Array with bytecount for each tile.
   * @param imagetype Type of this image.
   * @param tilelength Length in pixels of each tile.
   * @param tilewidth Width in pixels of each tile.
   * @param tilesdown Number of tiles from top to bottom.
   * @param tilesacross Number of tiles from left to right.
   * @return An array containing all image data.
   */
   public byte[][] getTileImageData(int numOfRows,
                                    int numOfColumns,
                                    long offsets[],
                                    long bytecounts[],
                                    int imagetype,
                                    long tilelength,
                                    long tilewidth,
                                    int tilesdown,
                                    int tilesacross) throws IOException {

      byte imgdata[][] = new byte[numOfRows][numOfColumns];
      int rowoffset=0, offsetcounter=0;
      int factor=0;

      if (imagetype == TiffNames.GRAY8_IMG)       { factor = 1; }
      else if (imagetype == TiffNames.STDRGB_IMG) { factor = 3; }
      else {
         throw new RuntimeException("Can only handle GRAY/8 or STDRGB.");
      }


      for (int i=0; i<tilesdown; i++) {
         int columnoffset = 0;

         for (int j=0; j<tilesacross; j++) {
            int x=columnoffset;
            int y=rowoffset;

            fc.position(offsets[offsetcounter]);
            ByteBuffer buffer = ByteBuffer.allocate((int)bytecounts[offsetcounter]);
            fc.read(buffer);
            buffer.flip();

            int bytecounter=0;

            // Now this buffer has to be filled into the imgdata byte array!
            // Possibly not the best solution.
            while (buffer.hasRemaining()) {
               imgdata[y][x] = buffer.get();
               ++bytecounter;

               ++x;

               if (x == imgdata[y].length) { // Skip past padding bytes in this tile:
                  for (int k=0; k<factor*(tilewidth-bytecounter); k++) { buffer.get(); }
                  x = columnoffset;
                  ++y;
                  if (y == imgdata.length) { break; }
               }
               else if (x%(factor*tilewidth) == 0) {
                  x = columnoffset;
                  ++y;
                  if (y == imgdata.length) { break; }
               }
            }
            ++offsetcounter;
            columnoffset += tilewidth;
         }
         rowoffset += tilelength;
      }

      return imgdata;
   }


   /** Returns the file pointer for the file, ie where we are in it.
   */
   public long getFilePointer() throws IOException {
      return fc.position();
   }

   /** Sets the file pointer for the file.
   */
   public void setFilePointer(long newposition) throws IOException {
      fc.position(newposition);

      System.out.println("New filepos=" + fc.position());
   }


   /** Returns the name of the file.
   * @return String filename.
   */
   public String getFilename() {
      return file.getName();
   }


   /** Returns the absolute pathname of the file.
   * @return String absolute pathfilename.
   */
   public String getAbsPath() {
      return file.getAbsolutePath();
   }

}
