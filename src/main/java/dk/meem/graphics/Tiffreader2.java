package dk.meem.graphics;

/*
 * @(#)TiffReader.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that reads a TIFF-file and displays various info about it.
 * This simplified version takes a filename and an offset as
 * argument and assumes the IFD to read starts there.
 *
 */

import java.io.IOException;

public class Tiffreader2 {

   public static void main(String[] args) throws IOException {
      // Syntax: Tiffreader2 filename IFD-offset

      TiffFile infile = new TiffFile(args[0]);
      infile.open();

      TiffHeader header = new TiffHeader(infile.getBytes(8));
      header.showHeader();
      Endian bitman = (header.isLittleEndian() ?
                     new LittleEndian() : new BigEndian());

      System.out.println("IFD offset is at: " + args[1]);
      TiffIFDFile ifd0 = new TiffIFDFile(infile, Integer.parseInt(args[1]), header.isLittleEndian());
      ifd0.showTags();
   }
}
