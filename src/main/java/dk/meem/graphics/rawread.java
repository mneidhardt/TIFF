package dk.meem.graphics;

/*
 * @(#)rawread.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 * Class that reads and displays a byte stream from a file..
 *
 */

import java.io.IOException;
import java.io.FileInputStream;

public class rawread {

   public static void main(String[] args) throws IOException {
      FileInputStream fhandle=null;
      int offset=0, bytecount=200, bytesperline=0;

      System.out.println("Syntax: " +
            "java rawread filename offset bytecount [bytesperline]\n"+
            "Will read bytecount bytes from filename.\n" +
            "If 'bytesperline' is present, inserts a newline every time\n" +
            "it has printed 'bytesperline' bytes.\n\n");

      try {
         fhandle = new FileInputStream(args[0]);
         offset = Integer.parseInt(args[1]);
         bytecount = Integer.parseInt(args[2]);
         bytesperline = Integer.parseInt(args[3]);

         for (int i=0; i<offset; i++) {
            short b = (short)fhandle.read();

            // Print the first 8 bytes (the TIFF header + IFD offset).
            if (i < 10) { System.out.printf("%4d", b); }
         }

         System.out.println();

         for (int i=0; i<bytecount; i++) {
            if (bytesperline>0 && i>0 && i%bytesperline==0) {
               System.out.println();
            }

            short data = (short)fhandle.read();

            if (data == -1) { break; }

            System.out.printf("%4d", data);

         }

         fhandle.close();

      }
      catch (IOException ie) {
         System.err.println("Cannot open file.");
      }
      catch (NumberFormatException e) {
         System.err.println("Bytecount set to 200. I.e. reading 200 bytes after header.");
      }
      catch (ArrayIndexOutOfBoundsException e) {
      }
   }
}
