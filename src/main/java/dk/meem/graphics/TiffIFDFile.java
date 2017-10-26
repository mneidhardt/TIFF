package dk.meem.graphics;

/*
 * @(#)TiffIFDFile.java  0.01 25/september/2007
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds the IFD directory entries for a TIFF
 * backed by a file.
*/

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import dk.meem.basics.BigEndian;
import dk.meem.basics.LittleEndian;

//import java.io.FileOutputStream;
//import java.io.FileInputStream;
import java.io.IOException;

public class TiffIFDFile extends TiffIFD {
   //private int IFDDircount;
   //private short entries[][];
   private long IFDOffset=0;    // Always the first IFD (for now at least).
   //private long nextIFD=0;
   //private Endian bitman;
   //private int typesize[]=TiffNames.getTypeSizes();

   private TiffFile infile;

   /**
   * Constructor.
   * @param in The filehandle for the TIFF we are reading.
   * @param isLittleEndian True if little endian, otherwiose false.
   */
   TiffIFDFile(TiffFile infile, long IFDOffset, boolean isLittleEndian) throws IOException {
      this.infile = infile;
      this.IFDOffset = IFDOffset;
      this.bitman = (isLittleEndian ? new LittleEndian() : new BigEndian());

      short ifdcount[] = infile.getShorts(IFDOffset, TiffNames.getEntryCountSize());

      this.IFDDircount = bitman.short2Int(ifdcount[0], ifdcount[1]);

      //System.out.println("There are " + IFDDircount + " entries: " +
      //      ifdcount[0] + "," + ifdcount[1]);     // TESTTTTTTTT


      this.entries = new short[IFDDircount][];

      for (int i=0; i<IFDDircount; i++) {
         entries[i] = infile.getShorts(TiffNames.getEntrySize());
      }

      short[] nifd = infile.getShorts(TiffNames.getNextIFDOffsetSize());
      this.nextIFD = bitman.short2Long(nifd[0],nifd[1],nifd[2],nifd[3]);
   }



   /** Returns the image data for this Tiff IFD.
   */
   public byte[][] getImageData() throws IOException {
      if (infile == null) {
         throw new RuntimeException("Cannot get image data " +
                     " without a file.");
      }

      long imglength[] = getValueOffset(TiffNames.IMAGELENGTH);
      long imgwidth[]  = getValueOffset(TiffNames.IMAGEWIDTH);
      int bytesperScanline = getBytesPerScanline(getImageType());

      // Now get all stripoffsets and stripbytecounts:
      long voSO[] = getValueOffset(TiffNames.STRIPOFFSETS);
      long voSBC[] = getValueOffset(TiffNames.STRIPBYTECOUNTS);

      if (voSO != null && voSBC != null) {
         long allStripoffsets[] = infile.getData(voSO, bitman);
         long allStripbytecounts[] = infile.getData(voSBC, bitman);

         return infile.getStripImageData(
                                    (int)imglength[3],
                                    bytesperScanline,
                                    allStripoffsets,
                                    allStripbytecounts
                                    );
      }
      else {
         long voTL[] = getValueOffset(TiffNames.TILELENGTH);
         long voTW[] = getValueOffset(TiffNames.TILEWIDTH);
         long voTO[] = getValueOffset(TiffNames.TILEOFFSETS);
         long voTB[] = getValueOffset(TiffNames.TILEBYTECOUNTS);

         if (voTL != null && voTW != null && voTO != null && voTB != null) {
            long allTileoffsets[] = infile.getData(voTO, bitman);
            long allTilebytecounts[] = infile.getData(voTB, bitman);


            int tilesdown =   // The number of rows of tiles:
               (int)Math.ceil((double)imglength[3]/(double)voTL[3]);

            int tilesacross = // The number of columns of tiles:
               (int)Math.ceil((double)imgwidth[3]/(double)voTW[3]);

System.out.println("Tiles. Args=" + imglength[3] + " " +
          bytesperScanline + " " +
          allTileoffsets.length + " offsets. " +
          allTilebytecounts.length + " bytecounts. " +
          voTL[3] + " " + voTW[3] + " " + tilesdown + " " + tilesacross);

            return infile.getTileImageData(
                                 (int)imglength[3],
                                 bytesperScanline,
                                 allTileoffsets,
                                 allTilebytecounts,
                                 getImageType(),
                                 voTL[3],
                                 voTW[3],
                                 tilesdown,
                                 tilesacross);

         }
         else {
            throw new RuntimeException("Can only handle images that are " +
               " either strip or tile based. This is neither.");
         }
      }
   }


   /** Returns the number of bytes needed per scanline - this version is
   * used by IFD-objects that need bytes per scanline for the whole image.
   * ie the width is that of the whole image.
   * @param imagetype The type of this image.
   * @return Number of bytes per scanline.
   */
   public int getBytesPerScanline(int imagetype) throws RuntimeException {
      long imagewidth[] = getValueOffset(TiffNames.IMAGEWIDTH);

      return getBytesPerScanline(imagewidth[3], imagetype);
   }


   /** Gets the type of an image, ie if its B/W, gray/4, gray/8 or RGB.
   * It is necessary to check several pieces of information to get this.
   */
   public int getImageType() throws IOException {
      int imagetype=-1;          // 0=B/W,   1=Gray/4,   2=Gray/8,   3=RGB/3

      if (infile == null) {
         throw new RuntimeException("Cannot get image type" +
                     " without a file.");
      }

      long compression[]    = getValueOffset(TiffNames.COMPRESSION);
      long imglength[]      = getValueOffset(TiffNames.IMAGELENGTH);
      long imgwidth[]       = getValueOffset(TiffNames.IMAGEWIDTH);
      long photomIntrp[]    = getValueOffset(TiffNames.PHOTOMETRICINTERPRETATION);
      long rowsperstrip[]   = getValueOffset(TiffNames.ROWSPERSTRIP);
      long bits[]           = getValueOffset(TiffNames.BITSPERSAMPLE);
      long samplespp[]      = getValueOffset(TiffNames.SAMPLESPERPIXEL);
      long extrasamples[]   = getValueOffset(TiffNames.EXTRASAMPLES);
      long planarconfig[]   = getValueOffset(TiffNames.PLANARCONFIGURATION);
      long orientation[]    = getValueOffset(TiffNames.ORIENTATION);
      long newsubfiletype[] = getValueOffset(TiffNames.NEWSUBFILETYPE);

      if (compression != null) {
         if (compression[3] != 1) {
            throw new RuntimeException("TiffIFD: Cannot handle " +
                  " compressed images.");
         }
      }

      if (orientation != null) {
         if (orientation[3] != 1) {
            throw new RuntimeException("TiffIFD: Cannot handle " +
                  " Orientation different from 1.");
         }
      }

      if (planarconfig != null) {
         if (planarconfig[3] != 1) {
            throw new RuntimeException("TiffIFD: Cannot handle " +
                  " PlanarConfiguration different from 1.");
         }
      }

      if (newsubfiletype != null) {
         if (newsubfiletype[3] != 0) {
            throw new RuntimeException("TiffIFD: Cannot handle " +
                  " NewSubfileType different from 0.");
         }
      }

      if ((photomIntrp[3] == 0 || photomIntrp[3] == 1)) {
         // In here, I assume that bitspersample is 1 value, not offset!
         if (bits == null) {
            imagetype = 0;       // B/W
         }
         else if (bits[2] < 5 && bits[3] == 1) {
            imagetype = 0;       // B/W
         }
         else if (bits[2] < 5 && bits[3] == 4) {
            imagetype = 1;       // Gray/4
         }
         else if (bits[2] < 5 && bits[3] == 8) {
            imagetype = 2;       // Gray/8
         }
         else {
            throw new RuntimeException("TiffIFD: Cannot handle this: " +
                     " BitsPerSample = " + bits[3] +
                     " Count for this field =  " + bits[2]);
         }
      }
      else if (photomIntrp[3] == 2 && extrasamples != null) {
            throw new RuntimeException("Cannot handle ExtraSamples.");
      }
      else if (photomIntrp[3] == 2) {     // No ExtraSamples.
         int bitspersample[] = getBitsPerSample();

         //for (int i=0; i<bits.length; i++) { System.out.print(bits[i] + " + "); }   // TESTTTTTTTT

         if (samplespp[3] == 3 && (bitspersample[0] != 8 ||
                                   bitspersample[1] != 8 ||
                                   bitspersample[2] != 8)) {
            throw new RuntimeException("Cannot handle this: " +
                                       "BitsPerSample = [" +
                                        bitspersample[0] + " " +
                                        bitspersample[1] + " " +
                                        bitspersample[2] + "]");
         }
         else if (samplespp[3] == 3) {
               imagetype = 3;    // Standard RGB
         }
      }

      return imagetype;
   }


   /**
   * Finds out what the image contains in the field BitsPerSample.
   * NB: bits[] is the field BitsPerSample in the IFD.
   *     bitspersample[] are the values stored somewhere if bits[3]
   *     is an offset. It is typiclly 8,8,8 for standard RGB image.
   * @return Array of ints containing either [1], [4], [8] or [8 8 8].
   */
   public int[] getBitsPerSample() throws IOException {
      if (infile == null) {
         throw new RuntimeException("Cannot get bits/sample " +
                     " without a file. Perhaps you should call "+
                     "this on the original Tiff-file?");
      }

      long bits[] = getValueOffset(TiffNames.BITSPERSAMPLE);
      int bitspersample[];

      if (bits != null) {
         if (bits[2] > 4) {      // Its an offset.
            if (bits[2] != 6) {
               throw new RuntimeException("Cannot handle BitsPerSample " +
                           "different from [1], [4], [8] or [8,8,8]");
            }

            short tmp[] = infile.getShorts(bits[3], (int)bits[2]);
            bitspersample = new int[3];
            bitspersample[0] = bitman.short2Int(tmp[0], tmp[1]);
            bitspersample[1] = bitman.short2Int(tmp[2], tmp[3]);
            bitspersample[2] = bitman.short2Int(tmp[4], tmp[5]);
         }
         else {
            bitspersample = new int[1];
            bitspersample[0] = (int)bits[3];
         }
      }
      else {
         bitspersample = null;
      }

      return bitspersample;
   }



   /** Method that shows the TIFF tags (ie fields) that
   * are in the IFD.
   */
   public void showTags() throws IOException {
      super.showTags();

      // Only do this if we're backed by a file:
      if (infile != null) {
         int imgtype = getImageType();
         if (imgtype > -1 && imgtype < TiffNames.imagetypes.length) {
            System.out.println("\nImage type: " + getImageType() + " (" +
                               TiffNames.imagetypes[getImageType()] + ")");
         }
         else {
            System.out.println("TiffIFD: Don't know image type.");
         }
      }
   }
}
