package dk.meem.graphics;

/*
 * @(#)TiffIFDClone.java  0.01 25/september/2007
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds the IFD directory made when cloning
 * an IFD or creating a new one when downsizing an image.
 */

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import dk.meem.basics.BigEndian;
import dk.meem.basics.Endian;
import dk.meem.basics.LittleEndian;
import dk.meem.basics.MergeSortS;

public class TiffIFDClone extends TiffIFD {
   private long IFDOffset;
   private long currentoffset;
   private int  tilelength;
   private int  tilewidth;
   private int  numOfTiles;
   private long tilebytecount;
   private int  numOfSUBIFDS;
   private long SUBIFDS[];
   // The field software contains just this text for now:
   private byte software[];


   /** "Copy Constructor". Used when creating a new Tiff  based on a real
   * file-based IFD, as you would do when converting a Tiff to a pTiff.
   * Creates the new primary IFD, which is basically a copy
   * of the original one. We might here, however, remove fields we know
   * that cannot be handled in a pTiff or are incompatible in some way.
   * @param ifd The IFD for the original Tiff-file.
   */
   TiffIFDClone(TiffIFDFile originalIFD,
                long IFDOffset,
                boolean isLittleEndian) throws IOException {

      this.IFDOffset = IFDOffset;      // Is this used??????
      this.bitman = (isLittleEndian ? new LittleEndian() : new BigEndian());

      byte softwareText[] = {77,73,67,72,65,69,76,83}; // Should be an argument or something...
      fixSoftwareText(softwareText);

      ArrayList<Integer> chosenfields = new ArrayList<Integer>();

      for (int i=0; i<originalIFD.getIFDDircount(); i++) {
         int fieldno = originalIFD.getTag(i);
         long vo[] = originalIFD.getValueOffset(fieldno);

         // We really need to check if this field is OK or not
         // for inclusion in a new IFD, but for now I just accept
         // all fields for the primary new IFD except these 3:
         // Stripoffsets, Stripbytecounts, Rowsperstrip.
         if (fieldno != TiffNames.STRIPOFFSETS &&
             fieldno != TiffNames.STRIPBYTECOUNTS &&
             fieldno != TiffNames.ROWSPERSTRIP)      {

            chosenfields.add(fieldno);
         }
      }


      long imglength[]  = originalIFD.getValueOffset(TiffNames.IMAGELENGTH);
      long imgwidth[]   = originalIFD.getValueOffset(TiffNames.IMAGEWIDTH);

      // Should these be args to the constructor?
      tilelength=64;
      tilewidth=64;
      numOfTiles = ((int)Math.ceil((double)imglength[3]/(double)tilelength)) *
                   ((int)Math.ceil((double)imgwidth[3]/(double)tilewidth));

      // Calculate bytes used by each tile:
      tilebytecount = (long)(tilelength*getBytesPerScanline(
                              tilewidth,originalIFD.getImageType()
                             ));

      numOfSUBIFDS = 2;
      SUBIFDS = new long[numOfSUBIFDS];

System.out.println("Image dims: " + imglength[3] + "x" + imgwidth[3] + " => Tiles ialt= " + numOfTiles);

      // The new fields and their types that need to be added.
      // This presupposes that the original tiff is in strips.
      // NB ----: SHOULD THIS BE ARGUMENT TO THIS CONSTRUCTOR???????????
      //
      // columns are:        FieldID, type, count. value/offset
      // --- although if it is an offset, it will only be inserted
      // when we run through emtries later... see below.
      long newfields[][] = {
                           { TiffNames.TILELENGTH,     4, 1, tilelength},
                           { TiffNames.TILEWIDTH,      4, 1, tilewidth},
                           { TiffNames.TILEOFFSETS,    4, numOfTiles, 0},
                           { TiffNames.TILEBYTECOUNTS, 4, numOfTiles, 0}
                           ,{ TiffNames.SUBIFDS,       13, numOfSUBIFDS, 0}
                          };

      chosenfields.trimToSize();
      this.entries = new short[chosenfields.size() + newfields.length][];

      this.IFDDircount = entries.length;
      this.nextIFD = 0;

      // Add the existing fields and their content:
      for (int i=0; i<chosenfields.size(); i++) {
         int fieldno = (Integer)chosenfields.get(i);

         entries[i] = originalIFD.getFieldAsShorts(fieldno);
      }

      // Add the new fieldnumbers:
      int rowid = chosenfields.size();      // Row-index in entries.

      for (int i=0; i<newfields.length; i++) {
         if (originalIFD.getValueOffset((int)newfields[i][1]) == null) {

            entries[rowid] = fielddata2Bytes(newfields[i]);
            ++rowid;
         }
         else { // If we end here, it means that one of the new fields
                // was there already.
            throw new RuntimeException(TiffNames.num2Name((int)newfields[i][0])
               + " already exists. I expect a 1-image, strip-based Tiff.");
         }
      }


      // Sort the new IFD on fieldnumber, as per the spec.
      MergeSortS sorter = new MergeSortS();
      int sortcolumns[] = new int[2];


      // All values in the IFD are expressed as unsigned bytes (i.e. as
      // Java shorts), and since the fieldnumber is 2 bytes, I sort
      // on 2 columns:
      if (isLittleEndian) {
         sortcolumns[0] = 1;
         sortcolumns[1] = 0;
      }
      else {
         sortcolumns[0] = 0;
         sortcolumns[1] = 1;
      }
      sorter.sort(entries, sortcolumns);


      // SUBIFDS[0] is the offset of the first downsized image IFD,
      // and SUBIFDS[1] is the offset of the second downsized image, and so on.
      SUBIFDS[0] = getBytesUsedForImage() + TiffNames.getHeaderSize();
      SUBIFDS[1] = SUBIFDS[0] + precalculateBytesUsedForImage(
                                          (int)imglength[3]/2,
                                          (int)imgwidth[3]/2,
                                          originalIFD.getImageType(),
                                          tilelength/2,
                                          tilewidth/2);


      System.out.println("Offset for first  downsized IFD:" + SUBIFDS[0]);
      System.out.println("Offset for second downsized IFD:" + SUBIFDS[1]);


      // Now add offsets to the relevant fields in new IFD.
      // The very first offset is right after the IFD, i.e. after the 4 bytes
      // containing the Next IFD:
      currentoffset = TiffNames.getHeaderSize() +
                           entries.length*TiffNames.getEntrySize() +
                           TiffNames.getEntryCountSize() +
                           TiffNames.getNextIFDOffsetSize();

      // currentoffset cannot be odd now (all even numbers), but if it could,
      // we would need to perform the following check:
      // >>>> if ( ! (currentoffset%2 == 0)) { ++currentoffset; }


      for (int i=0; i<entries.length; i++) {
         long totalbytecount = (getCount(i)*typesize[getType(i)]);

         if (totalbytecount > 4) {
            byte offsetAsBytes[] = bitman.long2Bytes(currentoffset);

            // Ugly, but it works...
            entries[i][8]  = (short)(offsetAsBytes[0] & 0xff);
            entries[i][9]  = (short)(offsetAsBytes[1] & 0xff);
            entries[i][10] = (short)(offsetAsBytes[2] & 0xff);
            entries[i][11] = (short)(offsetAsBytes[3] & 0xff);

            currentoffset += totalbytecount;

            if ( ! (totalbytecount%2 == 0)) { ++currentoffset; }
            // If we increment currentoffset here, it is because
            // totalbytecount for a field is odd. This will
            // be detected in writeNewIFD and there we will
            // write an extra byte to the file.
         }
      }

   }


   /** Constructor for new IFD. Used when creating a downsized version
   * of an image, as when you create a pTiff from a Tiff.
   * @param imagelength Length in pixels of this image.
   * @param imagewidth Width in pixels of this image.
   * @param imagetype Type of image.
   * @param tilelength Length in pixels of each tile.
   * @param tilewidth Width in pixels of each tile.
   * @param currentoffset The next free position in file.
   * @param Endian Either Big or Little, depending on the original file.
   */
   TiffIFDClone(int imagelength,
                int imagewidth,
                int imagetype,
                int tilelength,
                int tilewidth,
                long currentoffset,
                Endian bitman) {

      this.tilelength = tilelength;
      this.tilewidth = tilewidth;
      this.currentoffset = currentoffset;
      this.bitman = bitman;


      numOfTiles = ((int)Math.ceil((double)imagelength/(double)tilelength)) *
                   ((int)Math.ceil((double)imagewidth/(double)tilewidth));

      tilebytecount = (long)(tilelength*getBytesPerScanline(tilewidth, imagetype));

      byte softwareText[] = {77,73,67,72,65,69,76,83}; // Should be an argument or something...
      fixSoftwareText(softwareText);

      System.out.println("SUBIFD: ImageDims: " + imagelength + "x" + imagewidth +
            " Tiles:" + tilelength + "x" + tilewidth + " tilebytecount=" + tilebytecount + " Tiles ialt=" + numOfTiles);

      entries = createNewIFD(imagelength,
                             imagewidth,
                             imagetype,
                             numOfTiles
                             );

      /* TESTTTTTTTTT
      for (int ix=0; ix<entries.length; ix++) {
         System.out.print("ENTRIES: ");
         for (int jx=0; jx<entries[ix].length; jx++) {
            System.out.print(entries[ix][jx] + " ");
         }
         System.out.println();
      }
      System.out.println("\n");
      */
   }


   /** Creates a new basic IFD with the minimum required fields.
   * This is used when creating downsized (or upsized?) versions
   * of an image for inclusion in eg a pTiff.
   */
   public short[][] createNewIFD(int imagelength,
                                 int imagewidth,
                                 int imagetype,
                                 int numOfTiles) {

      long fields[][] = getBasefields(imagelength,
                                      imagewidth,
                                      imagetype,
                                      numOfTiles,
                                      software);

      entries = new short[fields.length][];

System.out.print("createNewIFD: CurrOffset=" + currentoffset);

      currentoffset += entries.length*TiffNames.getEntrySize() +
                       TiffNames.getEntryCountSize() +
                       TiffNames.getNextIFDOffsetSize();

System.out.println("CreateNewIFD: First offset efter ny IFD=" + currentoffset);

      for (int i=0; i<fields.length; i++) {
         entries[i] = fielddata2Bytes(fields[i]);


         // TEST:
         if (fields[i][0] == TiffNames.BITSPERSAMPLE ||
             fields[i][0] == TiffNames.COMPRESSION ||
             fields[i][0] == TiffNames.PHOTOMETRICINTERPRETATION) {
            System.out.println("NEWFIELD= " + fields[i][1] + " " + fields[i][2] + " " + fields[i][3]);
            System.out.println("ENTRIES = " + entries[i][0] + " " + entries[i][1] + " " + entries[i][2] + " "
                              + entries[i][3] + " " + entries[i][4] + " " + entries[i][5] + " "
                              + entries[i][6] + " " + entries[i][7] + " " + entries[i][8] + " "
                              + entries[i][9] + " " + entries[i][10] + " " + entries[i][11] + " ");
         }

         long totalbytecount = (typesize[(int)fields[i][1]]*fields[i][2]);

         if (totalbytecount > 4) {
            byte offsetAsBytes[] = bitman.long2Bytes(currentoffset);

            // Ugly, but it works...
            entries[i][8]  = (short)(offsetAsBytes[0] & 0xff);
            entries[i][9]  = (short)(offsetAsBytes[1] & 0xff);
            entries[i][10] = (short)(offsetAsBytes[2] & 0xff);
            entries[i][11] = (short)(offsetAsBytes[3] & 0xff);

            currentoffset += totalbytecount;

            if ( ! (totalbytecount%2 == 0)) { ++currentoffset; }
            // If we increment currentoffset here, it is because
            // totalbytecount for a field is odd. This will
            // be detected in writeDownsizedIFD and there we will
            // write an extra byte to the file, in case.
         }
      }

      this.IFDDircount = entries.length;

      this.nextIFD = currentoffset + tilebytecount*numOfTiles;

// Sort entries here??????????

      return entries;
   }


   /** This takes an int-vector and returns it expressed as bytes,
   * fit for a Tiff-field, with endianness corresponding to current image.
   * @param vector A long[] with 4 fields - fieldno,type,count,value/offset.
   * @return The input expressed as unsigned bytes (ie Java shorts).
   */
   public short[] fielddata2Bytes(long fielddata[]) {
      short result[] = new short[TiffNames.getEntrySize()];

      byte fn[] = bitman.short2Bytes((short)fielddata[0]);
      result[0] = (short)(fn[0] & 0xff);
      result[1] = (short)(fn[1] & 0xff);

      byte type[] = bitman.short2Bytes((short)fielddata[1]);
      result[2] = (short)(type[0] & 0xff);
      result[3] = (short)(type[1] & 0xff);

      byte count[] = bitman.long2Bytes((long)fielddata[2]);
      result[4] = (short)(count[0] & 0xff);
      result[5] = (short)(count[1] & 0xff);
      result[6] = (short)(count[2] & 0xff);
      result[7] = (short)(count[3] & 0xff);

      if (typesize[(int)fielddata[1]]*fielddata[2] < 5) {

         // If it is a SHORT we must "left justify" the values
         // in the value field.
         // See Adobe's TIFF Revision 6.0 spec, p. 15 under Value/Offset
         if (typesize[(int)fielddata[1]] == 4) {
            byte value[] = bitman.long2Bytes(fielddata[3]);

            result[8]  = (short)(value[0] & 0xff);
            result[9]  = (short)(value[1] & 0xff);
            result[10] = (short)(value[2] & 0xff);
            result[11] = (short)(value[3] & 0xff);
         }
         else if (typesize[(int)fielddata[1]] == 2) {
            byte value[] = bitman.short2Bytes((short)fielddata[3]);
            result[8]  = (short)(value[0] & 0xff);
            result[9]  = (short)(value[1] & 0xff);
            result[10]  = 0;
            result[11]  = 0;
         }
         else {
            throw new RuntimeException("Dont know what to do here: "+
               "TiffIFDClone/fielddata2Bytes.");
         }
      }

      return result;
   }


   /** Returns long-array with the base fields for either Gray/8
   * or STD RGB images.
   * @param imagelength Length in pixels of image.
   * @param imagewidth Width in pixels of image.
   * @param imagetype The type of this image.
   * @return long[][] array with these data per row (if possible):
   *         [fieldno, type, count, value/offset]
   */
   public long[][] getBasefields(int imagelength,
                                 int imagewidth,
                                 int imagetype,
                                 int numOfTiles,
                                 byte software[]) {

      if (imagetype == TiffNames.GRAY8_IMG) {
         long fields[][] = {
                         {TiffNames.IMAGEWIDTH, 4, 1, imagewidth},
                         {TiffNames.IMAGELENGTH, 4, 1, imagelength},
                         {TiffNames.BITSPERSAMPLE, 3, 1, 8},
                         {TiffNames.COMPRESSION, 3, 1, 1},
                         {TiffNames.PHOTOMETRICINTERPRETATION, 3, 1, 0},
                         {TiffNames.XRESOLUTION, 5, 1, 0},
                         {TiffNames.YRESOLUTION, 5, 1, 0},
                         {TiffNames.RESOLUTIONUNIT, 3, 1, 0},
                         {TiffNames.SOFTWARE, 2, software.length, 0},
                         {TiffNames.TILEWIDTH, 4, 1, tilewidth},
                         {TiffNames.TILELENGTH, 4, 1, tilelength},
                         {TiffNames.TILEOFFSETS, 4, numOfTiles, 0},
                         {TiffNames.TILEBYTECOUNTS, 4, numOfTiles, 0}
                        };
         return fields;
      }
      else if (imagetype == TiffNames.STDRGB_IMG) {
         long fields[][] = {
                         {TiffNames.IMAGEWIDTH, 4, 1, (long)imagewidth},
                         {TiffNames.IMAGELENGTH, 4, 1, (long)imagelength},
                         {TiffNames.BITSPERSAMPLE, 3, 3, 0},
                         {TiffNames.COMPRESSION, 3, 1, 1},
                         {TiffNames.PHOTOMETRICINTERPRETATION, 3, 1, 2},
                         {TiffNames.SAMPLESPERPIXEL, 3, 1, 3},
                         {TiffNames.XRESOLUTION, 5, 1, 0},
                         {TiffNames.YRESOLUTION, 5, 1, 0},
                         {TiffNames.RESOLUTIONUNIT, 3, 1, 0},
                         {TiffNames.SOFTWARE, 2, software.length, 0},
                         {TiffNames.TILEWIDTH, 4, 1, tilewidth},
                         {TiffNames.TILELENGTH, 4, 1, tilelength},
                         {TiffNames.TILEOFFSETS, 4, numOfTiles, 0},
                         {TiffNames.TILEBYTECOUNTS, 4, numOfTiles, 0}
                        };
         return fields;
      }
      else {
         throw new
            RuntimeException("Can only handle Gray/8 or STD RGB images.");
      }
   }


   /** Creates and writes header and primary IFD. Used in conjunction
   * with the 'copy constructor', when making a pTiff from a Tiff.
   * @param headerdata The header for this IFD.
   * @param originalIFD The original IFD we get the offset data from.
   * @param originalInfile The TiffFile object holding the original file.
   * @param outfilename Name of the new file we are creating.
   */
   public void writePrimaryIFD(byte headerdata[],
                           TiffIFDFile originalIFD,
                           TiffFile originalInfile,
                           String outfilename) throws IOException {

      long tileoffsets[]=null;

      // These will be passed on to a downsized IFD, if we make one:
      byte bitspersample[]=null, xres[]=null, yres[]=null;


      System.out.println("Creating file " + outfilename);
      FileChannel outfc = createFile(outfilename);
      outfc.write(ByteBuffer.wrap(headerdata));
      outfc.write(ByteBuffer.wrap(getAsBytes()));

      // Now write the values pointed to by the offsets in the new IFD:
      for (int i=0; i<entries.length; i++) {
         int fieldno = getTag(i);
         long valoff[] = originalIFD.getValueOffset(fieldno);

         // If its in the orig. IFD, we get it from there:
         if (valoff != null) {
            if (valoff[2] > 4) {
               byte offsetdata[] = originalInfile.getBytes(valoff[3], (int)valoff[2]);

               if (fieldno == TiffNames.BITSPERSAMPLE) {
                  bitspersample = offsetdata;
               }
               else if (fieldno == TiffNames.XRESOLUTION) {
                  xres = offsetdata;
               }
               else if (fieldno == TiffNames.YRESOLUTION) {
                  yres = offsetdata;
               }

               // TEST: The file position must match the offset stated in the IFD:
               //long newVO[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + getTag(i) + " FP=" + outfc.position() + " Offset in new IFD=" + newVO[3]);

               outfc.write(ByteBuffer.wrap(offsetdata));

               // Append a byte if the number of bytes is odd:
               if (offsetdata.length%2 != 0) {
                  outfc.write(ByteBuffer.wrap(new byte[1]));
               }
            }
         }
         else {
            if (getCount(i)*typesize[getType(i)] > 4) {

               if (fieldno == TiffNames.TILEOFFSETS) {

                  // TEST The file position must match the offset stated in the IFD:
                  //long newVO2[] = getValueOffset(fieldno);
                  //System.out.println("Writing offsetdata for Felt: " + getTag(i) + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3]);

                  tileoffsets = new long[numOfTiles];

                  for (int j=0; j<numOfTiles; j++) {
                     outfc.write(ByteBuffer.wrap(bitman.long2Bytes(currentoffset)));
                     tileoffsets[j] = currentoffset;

                     // I am adding new data (well, pointers to new data),
                     // so in effect I am reserving space, and therefore
                     // I must increment the currentoffset:
                     currentoffset += tilebytecount;
                  }
               }
               else if (fieldno == TiffNames.TILEBYTECOUNTS) {

                  // TEST: The file position must match the offset stated in the IFD:
                  //long newVO2[] = getValueOffset(fieldno);
                  //System.out.println("Writing offsetdata for Felt: " + getTag(i) + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3]);

                  for (int j=0; j<numOfTiles; j++) {
                     outfc.write(ByteBuffer.wrap(bitman.long2Bytes(tilebytecount)));
                  }
               }
               else if (fieldno == TiffNames.SUBIFDS) {

                  // TEST: The file position must match the offset stated in the IFD:
                  //long newVO2[] = getValueOffset(fieldno);
                  //System.out.println("Writing offsetdata for Felt: " + getTag(i) + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3]);

                  for (int j=0; j<numOfSUBIFDS; j++) {
                     outfc.write(ByteBuffer.wrap(bitman.long2Bytes(SUBIFDS[j])));
                  }
               }

            }
         }
      }

      System.out.println("Finished primary IFD and metadata. CurrentOffset=" + currentoffset + ". FilePosition=: " + outfc.position() + "\nTAGS for Primary IFD:");
      showTags();

      byte imagedata[][] = originalIFD.getImageData();
      writeTileData2(imagedata,
                    originalIFD.getImageType(),
                    tilelength,
                    tilewidth,
                    tilebytecount,
                    outfc);


      // TEST: Trying to create two downsized versions of image.
      System.out.println("\nCurrent Offset=" + getCurrentoffset() + "\n");
      System.out.println("tilelength/width: " + tilelength + "/" + tilewidth);

      int imagelength = (int)(originalIFD.getValueOffset(TiffNames.IMAGELENGTH))[3];
      int imagewidth  = (int)(originalIFD.getValueOffset(TiffNames.IMAGEWIDTH))[3];
      int imagetype   = originalIFD.getImageType();


      // --------------- Make the first downsized image:
      TiffTransformations transformer = new TiffTransformations(imagedata,imagelength,imagewidth,imagetype);
      byte downsizedImagedata[][] = transformer.halfsizeImage();
      imagelength = transformer.getNewImagelength();
      imagewidth  = transformer.getNewImagewidth();
      tilelength = tilelength/2;
      tilewidth = tilewidth/2;

      TiffIFDClone downsizedIFD = new TiffIFDClone(imagelength,imagewidth,imagetype,tilelength,tilewidth,outfc.position(),bitman);
      downsizedIFD.writeDownsizedIFD(outfc,downsizedImagedata,imagetype,bitspersample,xres,yres);

      System.out.println("TESTDISPLAY OF TAGS:\nPrimaryIFD:");
      System.out.println("\nSecondaryIFD uses " + downsizedIFD.getBytesUsedForImage() + " bytes total.");
      downsizedIFD.showTags();

      // ----------------- Make the second downsized image:
      TiffTransformations transformer2 = new TiffTransformations(downsizedImagedata,imagelength,imagewidth,imagetype);
      byte downsizedImagedata2[][] = transformer2.halfsizeImage();
      imagelength = transformer2.getNewImagelength();
      imagewidth  = transformer2.getNewImagewidth();
      tilelength = tilelength/2;
      tilewidth = tilewidth/2;

      System.out.println("Transformer: NewImageDims: " + imagelength + "x" + imagewidth);

      TiffIFDClone downsizedIFD2 = new TiffIFDClone(imagelength,imagewidth,imagetype,tilelength,tilewidth,outfc.position(),bitman);

      downsizedIFD2.writeDownsizedIFD(outfc,downsizedImagedata2,imagetype,bitspersample,xres,yres);

      System.out.println("\nTertiaryIFD uses " + downsizedIFD2.getBytesUsedForImage() + " bytes total.");
      downsizedIFD2.showTags();

      outfc.close();

   }


   /** Creates and writes a downsized IFD. Called from writePrimaryIFD.
   * @param outfc The file we are writing to.
   * @param imagedata The data for this image.
   * @param imagetype Type of this image.
   * @param bitspersample What to write in case its a RGB image.
   * @param xresolution The original data passed on for this field.
   * @param yresolution The original data passed on for this field.
   */
   public void writeDownsizedIFD(FileChannel outfc,
                                 byte imagedata[][],
                                 int imagetype,
                                 byte bitspersample[],
                                 byte xresolution[],
                                 byte yresolution[]) throws IOException {

      System.out.println("writeDownsizedIFD: FP=" + outfc.position());

      outfc.write(ByteBuffer.wrap(getAsBytes()));

      // Now write the values pointed to by the offsets in the new IFD:
      for (int i=0; i<entries.length; i++) {
         int fieldno = getTag(i);

         // If its an offset:
         if (getCount(i)*typesize[getType(i)] > 4) {
            if (fieldno == TiffNames.BITSPERSAMPLE) {

               // TEST The file position must match the offset stated in the IFD:
               //long newVO2[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + fieldno + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3] + " Bits/Sample.length=" + bitspersample.length);

               outfc.write(ByteBuffer.wrap(bitspersample));
            }
            else if (fieldno == TiffNames.XRESOLUTION) {

               // TEST: The file position must match the offset stated in the IFD:
               //long newVO2[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + fieldno + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3] + " Xres.length=" + xresolution.length);

               outfc.write(ByteBuffer.wrap(xresolution));
            }
            else if (fieldno == TiffNames.YRESOLUTION) {

               // TEST: The file position must match the offset stated in the IFD:
               //long newVO2[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + fieldno + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3] + " Yres.length=" + yresolution.length);

               outfc.write(ByteBuffer.wrap(yresolution));
            }
            else if (fieldno == TiffNames.SOFTWARE) {

               // TEST: The file position must match the offset stated in the IFD:
               //long newVO2[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + fieldno + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3] + " software.length=" + software.length);

               outfc.write(ByteBuffer.wrap(software));
            }
            else if (fieldno == TiffNames.TILEOFFSETS) {

               // TEST: The file position must match the offset stated in the IFD:
               //long newVO2[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + fieldno + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3] + " NumOfTiles=" + numOfTiles);

               for (int j=0; j<numOfTiles; j++) {
                  outfc.write(ByteBuffer.wrap(bitman.long2Bytes(currentoffset)));

                  // I am adding new data (well, pointers to new data),
                  // so in effect I am reserving space, and therefore
                  // I must increment the currentoffset:
                  currentoffset += tilebytecount;
               }
            }
            else if (fieldno == TiffNames.TILEBYTECOUNTS) {

               // TEST The file position must match the offset stated in the IFD:
               //long newVO2[] = getValueOffset(fieldno);
               //System.out.println("Writing offsetdata for Felt: " + fieldno + " FP=" + outfc.position() + " Offset in new IFD=" + newVO2[3]);

               for (int j=0; j<numOfTiles; j++) {
                  outfc.write(ByteBuffer.wrap(bitman.long2Bytes(tilebytecount)));
               }
            }
         }
      }

      writeTileData2(imagedata, imagetype,
                    tilelength, tilewidth,
                    tilebytecount, outfc);
   }




   /** This writes out the image data, in the form of tiles, to file.
   * @param imagedata The image data for this image.
   * @param imagetype The type of this image.
   * @param tilelength Length in pixels of each tile.
   * @param tilewidth Width in pixels of each tile.
   * @param tilebytecount number of bytes used by each tile.
   * @param outfc The file (channel) we are writing to.
   */
   private void writeTileData(byte imagedata[][],
                              int imagetype,
                              int tilelength,
                              int tilewidth,
                              long tilebytecount,
                              FileChannel outfc) throws IOException {

      long imglength[] = getValueOffset(TiffNames.IMAGELENGTH);
      long imgwidth[]  = getValueOffset(TiffNames.IMAGEWIDTH);

      // The number of rows of tiles:
      int n = (int)Math.ceil((double)imglength[3]/(double)tilelength);
      // The number of columns of tiles:
      int m = (int)Math.ceil((double)imgwidth[3]/(double)tilewidth);

      int factor=0;

      if (imagetype == TiffNames.GRAY8_IMG)       { factor = 1; }
      else if (imagetype == TiffNames.STDRGB_IMG) { factor = 3; }
      else {
         throw new
            RuntimeException("Can only handle Gray/8 or STD RGB images.");
      }

      System.out.println("ImgLen/Wid: " + imglength[3] + "/" + imgwidth[3] + " and " +
         n + " by " + m + " tiles in grid. Tilebytecount=" + tilebytecount);

      int rowoffset=0;

      for (int i=0; i<n; i++) {
         int columnoffset=0;

         for (int j=0; j<m; j++) {
            byte tiledata[] = new byte[(int)tilebytecount];
            int counter=0;

            for (int y=rowoffset; y<rowoffset+tilelength; y++) {
               for (int x=columnoffset; x<columnoffset+tilewidth*factor; x++) {
                  tiledata[counter] = imagedata[y][x];
                  ++counter;

                  if (x == imagedata[y].length-1) {
                     counter += tilewidth-(counter%tilewidth);
                     break;
                  }
               }

               if (y == imagedata.length-1) {
                  break;
               }
            }

            outfc.write(ByteBuffer.wrap(tiledata));
            //System.out.println("TILEWRITER: " + tiledata.length);

            columnoffset += tilewidth*factor;
         }
         rowoffset += tilelength;
      }

      System.out.println("Done tile data - filePosition=: " + outfc.position());

   }

   /** This writes out the image data, in the form of tiles, to file.
   * @param imagedata The image data for this image.
   * @param imagetype The type of this image.
   * @param tilelength Length in pixels of each tile.
   * @param tilewidth Width in pixels of each tile.
   * @param tilebytecount number of bytes used by each tile.
   * @param outfc The file (channel) we are writing to.
   */
   private void writeTileData2(byte imagedata[][],
                              int imagetype,
                              int tilelength,
                              int tilewidth,
                              long tilebytecount,
                              FileChannel outfc) throws IOException {

      long imglength[] = getValueOffset(TiffNames.IMAGELENGTH);
      long imgwidth[]  = getValueOffset(TiffNames.IMAGEWIDTH);

      // The number of tiles down:
      int n = (int)Math.ceil((double)imglength[3]/(double)tilelength);
      // The number of tiles across:
      int m = (int)Math.ceil((double)imgwidth[3]/(double)tilewidth);

      int factor=0;

      if (imagetype == TiffNames.GRAY8_IMG)       { factor = 1; }
      else if (imagetype == TiffNames.STDRGB_IMG) { factor = 3; }
      else {
         throw new
            RuntimeException("Can only handle Gray/8 or STD RGB images.");
      }

      int padwidth = (m*factor*tilewidth)-imagedata[0].length;
      int rowoffset=0;

      System.out.println("ImgLen/Wid: " + imglength[3] + "/" + imgwidth[3] + " and " +
         n + " by " + m + " tiles. Tilebytecount=" + tilebytecount + " Padwidth=" + padwidth +
         "\nImagedata DIMS: " + imagedata.length + "x" + imagedata[0].length);

      for (int i=0; i<n; i++) {        // n tiles down
         int columnoffset=0;

         for (int j=0; j<m; j++) {     // m tiles across
            byte tiledata[] = new byte[(int)tilebytecount];
            int tileIDX=0;

            for (int y=0; y<tilelength; y++) {
               for (int x=0; x<tilewidth*factor; x++) {
                  //System.out.println("y=" + y + " rowoffset=" + rowoffset + " x=" + x + " columnoffset=" + columnoffset);
                  tiledata[tileIDX] = imagedata[y+rowoffset][x+columnoffset];

                  if ((x+columnoffset) == imagedata[y+rowoffset].length-1) {
                     tileIDX += tilewidth-padwidth;   //(counter%tilewidth);
                     break;
                  }
                  else {
                     ++tileIDX;
                  }
               }

               if ((y+rowoffset) == imagedata.length-1) {
                  break;
               }
            }

            outfc.write(ByteBuffer.wrap(tiledata));
            //System.out.println("TILEWRITER: " + tiledata.length);

            columnoffset += tilewidth*factor;
         }
         rowoffset += tilelength;
      }

      System.out.println("Done tile data - filePosition=: " + outfc.position());

   }


   /** Calculate the number of bytes used by an image.
   * @return The number of bytes used by this image, ie by the IFD,
   * the meta data and the image data.
   */
   public long getBytesUsedForImage() {
      long count = entries.length*TiffNames.getEntrySize() +
                   TiffNames.getEntryCountSize() +
                   TiffNames.getNextIFDOffsetSize();

      for (int i=0; i<entries.length; i++) {
         long fieldcount = (getCount(i)*typesize[getType(i)]);
         if (fieldcount > 4) {
            count += fieldcount;
         }
      }

      count += tilebytecount*numOfTiles;

      return count;
   }


   /** Calculate the number of bytes that will be used by a downsized
   * image - this is called before the image or the IFD is created,
   * so is a little different from the similar method above. It means
   * that this can be called from the primary IFD, where we need it when
   * writing the SUBIFDs offsets.
   * @param imagelength Length in pixels in downsized image.
   * @param imagewidth Width in pixels in downsized image.
   * @param imagetype Type of image.
   * @param tilelength Length of each tile in pixels.
   * @param tilewidth Width of each tile in pixels.
   * @return The number of bytes used by this image, ie by the IFD,
   * the meta data and the image data.
   */
   public long precalculateBytesUsedForImage(int imagelength,
                                             int imagewidth,
                                             int imagetype,
                                             int tilelength,
                                             int tilewidth) {


      int numoftiles =
         ((int)Math.ceil((double)imagelength/(double)tilelength)) *
         ((int)Math.ceil((double)imagewidth/(double)tilewidth));

      long fields[][] = getBasefields(imagelength,
                                      imagewidth,
                                      imagetype,
                                      numoftiles,
                                      software);

      long count = fields.length*TiffNames.getEntrySize() +
                   TiffNames.getEntryCountSize() +
                   TiffNames.getNextIFDOffsetSize();

      for (int i=0; i<fields.length; i++) {
         long fieldcount = typesize[(int)fields[i][1]]*fields[i][2];
         if (fieldcount > 4) {
            count += fieldcount;
         }
      }

      count += tilelength*getBytesPerScanline(tilewidth, imagetype)*numOfTiles;

      return count;
   }


   /** This sets the attribute software so that is has even length and
   * ends in a zero-byte.
   * @param argument The ASCII characters (as bytes) the user gave us.
   */
   private void fixSoftwareText(byte argument[]) {
      byte appendix[];

      if (argument.length % 2 == 0) {
         this.software = new byte[argument.length+2];
         appendix = new byte[2];
         appendix[0] = 32;    // A space so that length is even.
         appendix[1] = 0;
      }
      else {
         this.software = new byte[argument.length+1];
         appendix = new byte[1];
         appendix[0] = 0;
      }

      for (int i=0; i<argument.length; i++) {
         this.software[i] = argument[i];
      }
      for (int i=0; i<appendix.length; i++) {
         this.software[i+argument.length] = argument[i];
      }
   }

   /** Returns the current offset, ie the next free position.
   * @return The current offset (a long)
   */
   public long getCurrentoffset() {
      return currentoffset;
   }


   /** Method creates (opens) a file that we will later write to.
   * @param filename The name of the file to create.
   */
   private FileChannel createFile(String filename) throws FileNotFoundException {
      return new FileOutputStream(filename).getChannel();
   }


   /** Closes the file created in createFile().
   */
   //private void closeFile() throws IOException {
   //   outfc.close();
   //}


   /** Method that writes an array of bytes to the outfilechannel.
   * @param values The array of bytes to write to file.
   */
   //private void writeBytes(byte values[]) throws IOException {
   //   outfc.write(ByteBuffer.wrap(values));
   //}
}
