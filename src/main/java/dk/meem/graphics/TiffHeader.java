package dk.meem.graphics;

/*
 * @(#)TiffHeader.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds the header of a TIFF-file.
 *
 */

import java.nio.ByteBuffer;

import dk.meem.basics.BigEndian;
import dk.meem.basics.Endian;
import dk.meem.basics.LittleEndian;

public class TiffHeader {
   private int headersize=8;
   private boolean validTiff  = false;
   private byte header[]=null;
   private long firstIFDOffset;
   private boolean isLittleEndian;
   private Endian bitman;


   /** Constructor that sets the 3 values: order, 42 and endianness.
   */
   TiffHeader(byte headerbytes[]) {
      if (headerbytes.length != 8) {
         throw new RuntimeException("Header is not right size.");
      }
      else {
         this.header = headerbytes;

         if (isValidTiff(header)) {
            validTiff = true;
            isLittleEndian = isThisLittleEndian(header);

            bitman = (isLittleEndian ? new LittleEndian() : new BigEndian());
            firstIFDOffset = bitman.short2Long((short)(header[4]&0xff),
                                               (short)(header[5]&0xff),
                                               (short)(header[6]&0xff),
                                               (short)(header[7]&0xff));
         }
      }
   }


   /** Checks if the header contains what it is supposed to contain.
   * @param header A byte array containing the first 8 bytes of the file.
   * @return True if file is valid TIFF, and false otherwise.
   */
   public boolean isValidTiff(byte header[]) {
      boolean retval=false;

      if (header[0] == header[1]) {
         if (header[0] == 73) {                 // Little endian.
            if (header[2] == 42 && header[3] == 0) {
               retval=true;
            }
         }
         else if (header[0] == 77) {
            if (header[2] == 0 && header[3] == 42) { // Big endian.
               retval=true;
            }
         }
         else {
            throw new RuntimeException("Not a valid TIFF file. "+
                                       "Header incorrect (endianness).");
         }
      }
      else {
         throw new RuntimeException("Not a valid TIFF file. " +
                                    "Header incorrect (endianness).");
      }

      return retval;
   }


   /** Returns true if this TIFF is little endian, and
   * false if not.
   */
   private boolean isThisLittleEndian(byte header[]) {
      if (header[0] == 73 && header[1] == 73) {
         return true;
      }
      else if (header[0] == 77 && header[1] == 77) {
         return false;
      }
      else {
         throw new
            RuntimeException("Not a valid TIFF file. Header incorrect");
      }
   }


   /** Returns true if this TIFF is little endian, and
   * false if not. For use by the public.
   */
   public boolean isLittleEndian() {
      return isLittleEndian;
   }

   public long getIFDOffset() {
      return firstIFDOffset;
   }


   public byte[] getAsBytes() {
      return header;
   }


   public void showHeader() {
      System.out.println("Order  : " + header[0] + " " + header[1] +
                       (isLittleEndian ? " LittleEndian " : " BigEndian ") +
                       " Magic  : " + header[2] + " " + header[3] +
                       " IFD Offset: " + firstIFDOffset);
   }

}
