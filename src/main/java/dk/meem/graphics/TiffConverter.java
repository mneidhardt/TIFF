package dk.meem.graphics;

/*
 * @(#)TiffConverter.java  0.01 15/oktober/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that contains methods for converting a Tiff-file.
 * First of all, it should be able to create a pTiff from a Tiff.
 *
 */

import java.io.IOException;
//import java.io.FileNotFoundException;
//import java.nio.ByteBuffer;
//import java.nio.channels.FileChannel;
//import java.io.FileOutputStream;

import dk.meem.basics.BigEndian;
import dk.meem.basics.Endian;
import dk.meem.basics.LittleEndian;

class TiffConverter {


   /** C'tor.
   */
   public TiffConverter() {
   }


   /** Method that creates a pTiff from a Tiff.
   * @param filename Name of the Tiff-file to convert to pTiff.
   */
   public void tiff2ptiff(String infilename) throws IOException {

      TiffFile infile = new TiffFile(infilename);
      infile.open();
      TiffHeader header = new TiffHeader(infile.getBytes(8));
      TiffIFDFile originalIFD = new TiffIFDFile(infile,
                                header.getIFDOffset(),
                                header.isLittleEndian());

      int imagetype = originalIFD.getImageType();
      System.out.println("Image type is: " + imagetype +
            " (" + TiffNames.imagetypes[imagetype] + ")");

      // Create a new IFD for use in the pTiff.
      // I always put the first IFD at offset 8.
      long firstIFDOffset = 8L;
      TiffIFDClone newPrimaryIFD = new TiffIFDClone(originalIFD, firstIFDOffset,
                                   header.isLittleEndian());

      Endian bitman = (header.isLittleEndian() ? new LittleEndian() : new BigEndian());

      byte firstIFDOffsetAsbytes[] = bitman.long2Bytes(firstIFDOffset);
      byte headerbytes[] = header.getAsBytes();
      headerbytes[4] = firstIFDOffsetAsbytes[0];
      headerbytes[5] = firstIFDOffsetAsbytes[1];
      headerbytes[6] = firstIFDOffsetAsbytes[2];
      headerbytes[7] = firstIFDOffsetAsbytes[3];

      infilename = infilename.trim();
      String[] elements = infilename.split("\\\\");
      String outfilename = elements[elements.length-1] + ".mic.tif";
      newPrimaryIFD.writePrimaryIFD(headerbytes, originalIFD, infile, outfilename);
   }
}
