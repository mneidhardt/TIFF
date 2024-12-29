package dk.meem.graphics;

/*
 * @(#)TiffReader.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that reads a TIFF-file and displays various info about it.
 *
 */

//import java.io.FileInputStream;
import java.io.IOException;
//import dk.meem.basics.Search;
//import dk.meem.basics.MergeSort;

public class TiffReader {

   public static void main(String[] args) throws IOException {
      String syntax = "Syntax: TiffReader filename showtags|showtags2|subifds|printfieldNNN";
      syntax += "\nArgumenter er altså filnavn og et af de viste.";

      if (args.length == 0) {
         System.err.println(syntax);
         System.exit(1);
      }

      TiffFile infile = new TiffFile(args[0]);
      infile.open();

      TiffHeader header = new TiffHeader(infile.getBytes(8));
      header.showHeader();
      Endian bitman = (header.isLittleEndian() ?
                     new LittleEndian() : new BigEndian());

      TiffIFDFile ifd = new TiffIFDFile(infile, header.getIFDOffset(), header.isLittleEndian());

      System.out.println("LittleEndian? " + header.isLittleEndian());

      /* -------- These are mostly for testing ------------ */
      if (args.length == 1) {
         //System.out.println("FP=" + file.getFilePointer());
      }
      else if (args.length > 1 && args[1].equals("showtags")) {
         ifd.showTags();
      }
      else if (args.length > 1 && args[1].equals("showtags2")) {
         ifd.showTags2();
      }
      else if (args.length > 1 && args[1].equals("subifds")) {
         System.out.println("Primary IFD:");
         ifd.showTags();
         System.out.println("\nSubIFDS:");

         long fielddata[] = ifd.getValueOffset(TiffNames.SUBIFDS);

         if (fielddata == null) {
            System.err.println("No SUBIFDs in this file.");
            System.exit(1);
         }
         else if (fielddata[2] > 4) {
            displayFieldContents(ifd, TiffNames.SUBIFDS, infile, bitman);

            long subifds[] = infile.getData(fielddata, bitman);

            for (int s=0; s<subifds.length; s++) {
               System.out.println("LittleEndian? " + header.isLittleEndian());

               TiffIFDFile subifd =
                     new TiffIFDFile(infile,
                                     subifds[s],
                                     header.isLittleEndian());

               System.out.println("\nSubIFD " + s + " is here:");
               subifd.showTags2();
               System.out.println();
               /*displayFieldContents(subifd, TiffNames.BITSPERSAMPLE, infile, bitman);
               displayFieldContents(subifd, TiffNames.XRESOLUTION, infile, bitman);
               displayFieldContents(subifd, TiffNames.YRESOLUTION, infile, bitman);
               displayFieldContents(subifd, TiffNames.SOFTWARE, infile, bitman);
               displayFieldContents(subifd, TiffNames.TILEOFFSETS, infile, bitman);
               displayFieldContents(subifd, TiffNames.TILEBYTECOUNTS, infile, bitman);
               */
               System.out.println();
            }
         }
      }
      else if (args.length > 1 && args[1].startsWith("printfield")) {
         try {
            int fieldno = Integer.parseInt(args[1].substring(10));
            long fielddata[] = ifd.getValueOffset(fieldno);

            String valoffTxt = ((fielddata[2] < 5) ? "Value" : "Offset");
            System.out.print("Field " + fieldno +
                  " contains (type, count, totalbytes, " + valoffTxt + "): ");
            for (int i=0; i<fielddata.length; i++) { System.out.print(fielddata[i] + " "); }
            System.out.println();


            if (fielddata[2] > 4) {    // NB: Hvis der er mere data end der kan v�re i en
                                       // Java-int, s� er der et muligt problem, idet
                                       // en array skal oprettes med ints som dims...!?!?!?!?

               short data2[] = infile.getShorts(fielddata[3], (int)fielddata[2]);
               System.out.println("\nExpressed as bytes:");
               for (int i=0; i<data2.length; i++) {
                  // If its ASCII, prints chars.
                  if (fielddata[0] == 2) { System.out.print((char)data2[i]); }
                  else                   { System.out.print(data2[i] + " "); }
               }
            }
            // Alternativ til infile.getShorts(n,m) - n�r type er 3, 4, eller 13:
            long data3[] = infile.getData(fielddata, bitman);
            if (data3 != null) {
               System.out.println("\n\nExpressed as decimal numbers:");
               for (int i=0; i<data3.length; i++) {
                  System.out.print(data3[i] + " ");
               }
            }
         }
         catch (NumberFormatException ne) {
            System.out.println("Syntax is: printfieldNNN where nnn is the field ID.");
         }
      }
      else if (args.length > 1 && args[1].startsWith("imgmeta")) {      // Gets image meta data:
         //TiffFieldReader treader = new TiffFieldReader(header.isLittleEndian());

         long vo1[] = ifd.getValueOffset(TiffNames.STRIPOFFSETS);
         if (vo1 != null) {
            System.out.println("StripOffsets: " + vo1[0] + "," + vo1[1] + "," + vo1[2] + "," + vo1[3] + "\n");
            long offsets[] = infile.getData(vo1, bitman);

            long vo2[] = ifd.getValueOffset(TiffNames.STRIPBYTECOUNTS);
            System.out.println("StripByteCounts: " + vo2[0] + "," + vo2[1] + "," + vo2[2] + "," + vo2[3] + "\n");
            long bytecounts[] = infile.getData(vo2, bitman);

            System.out.println("ImgMeta: " + offsets.length + " offsets og " + bytecounts.length + " bytecounts.");

            for (int i=0; i<offsets.length; i++) {
               System.out.println("Bytes: " + bytecounts[i] + " @ " + offsets[i]);
            }
            System.out.println("\n");  //ImgMeta: " + offsets.length + " offsets.");
         }
         else {
            long vo1t[] = ifd.getValueOffset(TiffNames.TILEOFFSETS);
            System.out.println("TileOffsets: " + vo1t[0] + "," + vo1t[1] + "," + vo1t[2] + "," + vo1t[3] + "\n");
            long offsets[] = infile.getData(vo1t, bitman);

            long vo2t[] = ifd.getValueOffset(TiffNames.TILEBYTECOUNTS);
            System.out.println("TileByteCounts: " + vo2t[0] + "," + vo2t[1] + "," + vo2t[2] + "," + vo2t[3] + "\n");
            long bytecounts[] = infile.getData(vo2t, bitman);

            System.out.println("ImgMeta: " + offsets.length + " offsets og " + bytecounts.length + " bytecounts.");

            for (int i=0; i<offsets.length; i++) {
               System.out.println("Bytes: " + bytecounts[i] + " @ " + offsets[i]);
            }
            System.out.println("\n");  //ImgMeta: " + offsets.length + " offsets.");
         }
      }
      else if (args.length > 1 && args[1].startsWith("imgdata")) {      // Gets image data:
         long timer = System.currentTimeMillis();          // TESTTTTTTTTTTTTTTT

         byte imgdata[][] = ifd.getImageData();

         System.out.println("ImgData: " + imgdata.length + "x" + imgdata[0].length +
                  " Time: " + (System.currentTimeMillis()-timer) + " msek.");
      }

   }



   public static void displayFieldContents(TiffIFDFile ifd,
                                           int fieldnumber,
                                           TiffFile infile,
                                           Endian bitman)
                                           throws IOException {


      long fielddata[] = ifd.getValueOffset(fieldnumber);
      System.out.print("Field " + fieldnumber + " contains (type, count, totalbytes, v/o): ");
      for (int i=0; i<fielddata.length; i++) { System.out.print(fielddata[i] + " "); }
      System.out.println();

      if (fielddata[2] > 4) {
         short data2[] = infile.getShorts(fielddata[3], (int)fielddata[2]);
         System.out.println("As bytes:");
         for (int i=0; i<data2.length; i++) {
            // If its ASCII, prints chars.
            if (fielddata[0] == 2) { System.out.print((char)data2[i]); }
            else                   { System.out.print(data2[i] + " "); }
         }
         System.out.println();
      }


      long data[] = infile.getData(fielddata, bitman);
      if (data != null) {
         System.out.println("As decimal numbers:");
         for (int i=0; i<data.length; i++) {
            System.out.print(data[i] + " ");
         }
      }

      System.out.println("\n");

   }
}
