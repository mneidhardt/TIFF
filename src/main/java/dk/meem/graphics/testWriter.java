package dk.meem.graphics;

/*
 * @(#)testWriter.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that tests TiffWriter.
 *
 */

import java.io.IOException;

public class testWriter {

   public static void main(String[] args) throws IOException {
      String syntax = "Syntax: java -jar testWriter.jar filename\n" +
            "hvor filename altsaa er den fil der skal konverteres.";

      if (args.length == 0) {
         System.err.println(syntax);
         System.exit(1);
      }

      TiffConverter converter = new TiffConverter();
      converter.tiff2ptiff(args[0]);
   }
}
