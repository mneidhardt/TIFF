package dk.meem.graphics;

/*
 * @(#)TiffIFD.java  0.01 25/september/2007
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds the base IFD entities.
*/

import java.io.IOException;

public class TiffIFD {
   protected int IFDDircount;
   protected short entries[][];
   protected long nextIFD=0;     // Actually the offset for the nex IFD.
   protected Endian bitman;
   protected int typesize[]=TiffNames.getTypeSizes();

   /**
   * Constructor.
   */
   // Dont know if this is necessary...
   //TiffIFD() {
   //}


   /** Returns a row in 'entries' as shorts (ie as bytes but
   * stored in java shorts) - I use it for creating a new IFD.
   * @param fieldno The number of the field, ie Tiff field number.
   * @return A short array containing the bytes of this field.
   */
   public short[] getFieldAsShorts(int fieldno) {
      short result[]=null;

      for (int i=0; i<entries.length; i++) {
         if (getTag(i) == fieldno) {
            result = new short[entries[i].length];

            // This must be a deep copy:
            for (int j=0; j<result.length; j++) {
               result[j] = entries[i][j];
            }
            break;
         }
      }

      return result;
   }

   /** Returns the decimal value of this field (ie tag).
   * @param rowid The index for the row in 'entries' you want.
   * @return Decimal number of the field (eg 256 for ImageWidth).
   */
   public int getTag(int rowid) {
      //System.out.println(entries.length + " " + rowid + " : " + entries[rowid].length);
      return bitman.short2Int(entries[rowid][0],entries[rowid][1]);
   }


   /** Returns the type of a field.
   * @param rowid The index for the row in 'entries' you want.
   * @return Type of the field.
   */
   public int getType(int rowid) {
      return bitman.short2Int(entries[rowid][2],entries[rowid][3]);
   }


   /** Returns the count of the field.
   * @param rowid The index for the row in 'entries' you want.
   * @return The count of the field, ie number of values of the given type,
   *         NOT the bytecount).
   */
   public long getCount(int rowid) {
      return bitman.short2Long(entries[rowid][4],entries[rowid][5],
                               entries[rowid][6],entries[rowid][7]);
   }



   /** Returns the value or offset for a given entry in the IFD.
   * @param rowid The row of the IFD we're looking at.
   * @param type The type of the value/offset
   * @param count The count for the value/offset.
   */
   private long getValueOffset(int rowid, int type, long count)
                                       throws RuntimeException {
      long valoff=-42;
      long totalbytes;

      if (type < typesize.length) {
         totalbytes = count*typesize[type];
      }
      else {
         totalbytes = -1;
      }

      if (type == 1 || type == 2 || type == 6 || type == 7) {
         if (totalbytes < 5) {
            valoff = entries[rowid][8];
         }
         else {
            valoff = bitman.short2Long(entries[rowid][8],
                                       entries[rowid][9],
                                       entries[rowid][10],
                                       entries[rowid][11]);
         }
      }
      // Is this correct? In theory you could have 2 SHORT values
      // in 4 bytes, which means you should return 2 Java ints.
      else if ((type == 3 || type == 8) && totalbytes < 5) {
         valoff = bitman.short2Int(entries[rowid][8],
                                   entries[rowid][9]);
      }
      else if (type == 3 || type == 8) {
         valoff = bitman.short2Long(entries[rowid][8],
                                    entries[rowid][9],
                                    entries[rowid][10],
                                    entries[rowid][11]);
      }
      else if (type == 4  || type == 5  || type == 9  ||
               type == 10 || type == 11 || type == 12 ||type == 13) {

         valoff = bitman.short2Long(entries[rowid][8],
                                    entries[rowid][9],
                                    entries[rowid][10],
                                    entries[rowid][11]);
      }
      else {
         throw new RuntimeException("ERROR: Not all types " +
                                    "are implemented. Type=" + type);
      }

      return valoff;
   }

   /** Returns the value/offset for a given TIFF field.
   * I guess this will be used from external parts???
   * @param fieldno The number of the TIFF field in decimal,
   *                eg 273 for the StripOffsets.
   * @return A long[4] containing the type, count, bytes used, value/offset.
   */
   public long[] getValueOffset(int fieldno) {
      long result[] = null;

      for (int i=0; i<entries.length; i++) {
         if (getTag(i) == fieldno) {
            result = new long[4];
            result[0] = getType(i);
            result[1] = getCount(i);
            result[2] = getCount(i)*typesize[getType(i)];
            result[3] = getValueOffset(i, getType(i), getCount(i));
            break;
         }
      }

      return result;
   }



   /** Returns the number of bytes required per scanline, eg if a
   * standard RGB image is 100 pixels wide, you need 3*100 bytes
   * to describe each scanline.
   * @param imagetype A type I have defined and which can be had from
   *                  the method getImageType(bitspersample).
   * @return Number of bytes needed per scanline.
   */
   public int getBytesPerScanline(long imagewidth, int imagetype) throws RuntimeException {
      if (imagetype == TiffNames.BW_IMG) {
         return (int)Math.ceil((float)imagewidth/(float)8);
      }
      else if (imagetype == TiffNames.GRAY4_IMG) {
         return integerDiv(imagewidth,2);
      }
      else if (imagetype == TiffNames.GRAY8_IMG) {
         return (int)imagewidth;
      }
      else if (imagetype == TiffNames.STDRGB_IMG) {
         return (int)(imagewidth*3);
      }
      else {
         throw new RuntimeException("TiffIFD: Dont recognise imagetype: "
                                     + imagetype + ".");
      }
   }


   /** Divides two integers, making sure that denominator is a
   * factor in numerator.
   * @param numerator The numerator in the division.
   * @param denominator The denominator in the division.
   * @return The result of the division.
   */
   public int integerDiv(long numerator, long denominator) throws RuntimeException {
      if (numerator%denominator != 0) {
         throw new RuntimeException(numerator + "/" + denominator + " has decimals!");
      }
      else {
         return (int)((double)numerator/(double)denominator);
      }
   }


   /** Method that shows the TIFF tags (ie fields) that
   * are in the IFD.
   */
   public void showTags() throws IOException {
      int tagid, type;
      long count, valoffset=0, totalbytes;

      for (int i=0; i<entries.length; i++) {
         tagid = getTag(i);
         type  = getType(i);
         count = getCount(i);
         valoffset = getValueOffset(i, type, count);

         if (type < typesize.length) {
            totalbytes = count*typesize[type];
         }
         else {
            totalbytes = -1;
         }

         String vo = ((totalbytes < 5) ? " " : ">");     // ">" means Offset.

         int fnamelength=17;
         String fieldname = TiffNames.num2Name(tagid);
         if (fieldname.length() > fnamelength) {
            fieldname = fieldname.substring(0, fnamelength-2) + "..";
         }
         System.out.printf("#%2d Tag=%5d %-"+fnamelength+"s Type=%2d (%1d) Count=%6d  V/O=%10d  %s\n",
              i, tagid, fieldname, type, typesize[type], count, valoffset, vo);

      }

      System.out.println("\nNextIFDOffset= " + nextIFD);
   }


   /** Method that shows the TIFF tags (ie fields) that
   * are in the IFD - but this method shows the raw byte values.
   */
   public void showTags2() {
      for (int i=0; i<entries.length; i++) {
         for (int j=0; j<entries[i].length; j++) {
            System.out.print(entries[i][j] + " ");
         }
         System.out.println();
      }

      System.out.println("NextIFDOffset= " + nextIFD);
   }


   /** Returns the number if IFD entries.
   */
   public int getIFDDircount() {
      return IFDDircount;
   }


   /** Returns the full IFD table as a vector of bytes,
   * ie the bytes telling the number of entries, the entries
   * themselves and lastly the offset for the next IFD.
   * This is necessary because of the decision to store the IFD in
   * a 2-dim short array. It might have been smarter to use a byte[].
   * @return Byte array containing the full IFD.
   */
   public byte[] getAsBytes() {
      int bytecount = TiffNames.getEntryCountSize() +
                      TiffNames.getEntrySize()*IFDDircount +
                      TiffNames.getNextIFDOffsetSize();

      byte result[] = new byte[bytecount];

      byte ifdsize[] = bitman.short2Bytes((short)IFDDircount);
      byte nextifdOffsetAsBytes[] = bitman.long2Bytes(nextIFD);

      System.out.println(IFDDircount + " entries. Bytecount for IFD: " + bytecount);


      // First write the number of IFD entries:
      int bcount=0;
      for (bcount=0; bcount<ifdsize.length; bcount++) {
         result[bcount] = ifdsize[bcount];
      }

      // Then write all the entries:
      for (int i=0; i<entries.length; i++) {
         for (int j=0; j<entries[i].length; j++) {
            result[bcount] = (byte)(entries[i][j] & 0xff);
            ++bcount;
         }
      }


      // Last write the offset of next IFD:
      for (int i=0; i<nextifdOffsetAsBytes.length; i++) {
         //result[bcount] = (byte)(nextifdOffsetAsBytes[i] & 0xff);
         result[bcount] = nextifdOffsetAsBytes[i];
         ++bcount;
      }

      return result;
   }



   /** Returns the raw entries.
   */
   public short[][] getFullIFD() {
      return entries;
   }

}
