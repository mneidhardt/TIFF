package dk.meem.graphics;

/*
 * @(#)TiffFieldnames.java  0.01 25/september/2007
 *
 * Copyright (c) 2007 Michael Neidhardt.
 *
 * Class that holds names of TIFF-fields and
 * their corresponding numbers.
 * The tag info is based on the TIFF specification,
 * http://www.awaresystems.be/imaging/tiff/tifftags/ and
 * http://remotesensing.org/libtiff/support.html.
 * All these sources were used around september/october 2007.
 *
 */

public class TiffNames {

   /**  --- My image types ---
   * BW_IMG is bilevel, black/white image and BitsPerSample absent or 1.
   * GRAY4_IMG is grayscale with BitsPerSample=4.
   * GRAY8_IMG is grayscale with BitsPerSample=8.
   * STDRGB_IMG is RGB with BitsPerSample=8,8,8 and ExtraSamples absent.
   * PhotometricInterpretation is 0 or 1 for BW_IMG, GRAY4_IMG and GRAY8_IMG.
   * PhotometricInterpretation is 2 for RGB (all types).
   */
   public static final int BW_IMG = 0;
   public static final int GRAY4_IMG = 1;
   public static final int GRAY8_IMG = 2;
   public static final int STDRGB_IMG = 3;
   public static final String[] imagetypes = {"Black/White",
                                              "Gray/4",
                                              "Gray/8",
                                              "STDRGB"};


   /** For each legal type of Tiff field, this tells the
   * number of bytes it occupies. The SBYTE, SSHORT, SLONG and
   * SRATIONAL are signed bytes, whereas the others are unsigned.
   * The first entry is for TYPE=1, the second for TYPE=2 etc.
   * Note that the names used here are TIFF spec. names, not
   * Java names.
   * For instance, a LONG here occupies exactly 4 bytes, but in
   * Java it must be stored in a 'long', not in an 'int'.
   */
   private static final int typesize[] =
                        {
                           0,       // 0  = N/A
                           1,       // 1  = BYTE
                           1,       // 2  = ASCII
                           2,       // 3  = SHORT
                           4,       // 4  = LONG
                           8,       // 5  = RATIONAL
                           1,       // 6  = SBYTE
                           1,       // 7  = UNDEFINED (but 1 byte)
                           2,       // 8  = SSHORT
                           4,       // 9  = SLONG
                           8,       // 10 = SRATIONAL
                           4,       // 11 = FLOAT
                           8,       // 12 = DOUBLE
                           4        // 13 = "IFD"

                        };





   public static final int NEWSUBFILETYPE             = 254;
   public static final int SUBFILETYPE                = 255;
   public static final int IMAGEWIDTH                 = 256;
   public static final int IMAGELENGTH                = 257;
   public static final int BITSPERSAMPLE              = 258;
   public static final int COMPRESSION                = 259;
   public static final int PHOTOMETRICINTERPRETATION  = 262;
   public static final int THRESHHOLDING              = 263;
   public static final int FILLORDER                  = 266;
   public static final int DOCUMENTNAME               = 269;
   public static final int IMAGEDESCRIPTION           = 270;
   public static final int MAKE                       = 271;
   public static final int MODEL                      = 272;
   public static final int STRIPOFFSETS               = 273;
   public static final int ORIENTATION                = 274;
   public static final int SAMPLESPERPIXEL            = 277;
   public static final int ROWSPERSTRIP               = 278;
   public static final int STRIPBYTECOUNTS            = 279;
   public static final int MINSAMPLEVALUE             = 280;
   public static final int MAXSAMPLEVALUE             = 281;
   public static final int XRESOLUTION                = 282;
   public static final int YRESOLUTION                = 283;
   public static final int PLANARCONFIGURATION        = 284;
   public static final int T4OPTIONS                  = 292;
   public static final int RESOLUTIONUNIT             = 296;
   public static final int PAGENUMBER                 = 297;
   public static final int SOFTWARE                   = 305;
   public static final int DATETIME                   = 306;
   public static final int PREDICTOR                  = 317;
   public static final int COLORMAP                   = 320;
   public static final int HALFTONEHINTS              = 321;
   public static final int TILEWIDTH                  = 322;
   public static final int TILELENGTH                 = 323;
   public static final int TILEOFFSETS                = 324;
   public static final int TILEBYTECOUNTS             = 325;
   public static final int BADFAXLINES                = 326;
   public static final int CLEANFAXDATA               = 327;
   public static final int CONSECUTIVEBADFAXLINES     = 328;
   public static final int SUBIFDS                    = 330;
   public static final int EXTRASAMPLES               = 338;
   public static final int SAMPLESFORMAT              = 339;
   public static final int JPEGPROC                   = 512;   // Invalid by Technote2
   public static final int JPEGQTABLES                = 519;   // Invalid by Technote2
   public static final int JPEGDCTABLES               = 520;   // Invalid by Technote2
   public static final int JPEGACTABLES               = 521;   // Invalid by Technote2
   public static final int YCBCRCOEFFICIENTS          = 529;
   public static final int YCBCRSUBSAMPLING           = 530;
   public static final int YCBCRPOSITIONING           = 531;
   public static final int REFERENCEBLACKWHITE        = 532;
   public static final int DATATYPE                   = 32996; // Obsoleted by SampleFormat.
   public static final int IMAGEDEPTH                 = 32997; // Tile/strip calculations.
   public static final int TILEDEPTH                  = 32998; // Tile/strip calculations.
   public static final int COPYRIGHT                  = 33432;
   public static final int RICHTIFFIPTC               = 33723;
   public static final int PHOTOSHOP                  = 34377;
   public static final int EXIFIFD                    = 34665;
   public static final int ICCPROFILE                 = 34675;
   public static final int STONITS                    = 37439; // Possibly SGI-specific.
   public static final int GDAL_METADATA              = 42112;
   public static final int PRINTIMAGEMATCHINGINFO     = 50341; // ???



   public static String num2Name(int fieldnumber) {

      switch (fieldnumber) {
         case 254:   return "NEWSUBFILETYPE";
         case 255:   return "SUBFILETYPE";
         case 256:   return "IMAGEWIDTH";
         case 257:   return "IMAGELENGTH";
         case 258:   return "BITSPERSAMPLE";
         case 259:   return "COMPRESSION";
         case 262:   return "PHOTOMETRINTRP";
         case 263:   return "THRESHHOLDING";
         case 266:   return "FILLORDER";
         case 269:   return "DOCUMENTNAME";
         case 270:   return "IMAGEDESCRIPTION";
         case 271:   return "MAKE";
         case 272:   return "MODEL";
         case 273:   return "STRIPOFFSETS";
         case 274:   return "ORIENTATION";
         case 277:   return "SAMPLESPERPIXEL";
         case 278:   return "ROWSPERSTRIP";
         case 279:   return "STRIPBYTECOUNTS";
         case 280:   return "MINSAMPLEVALUE";
         case 281:   return "MAXSAMPLEVALUE";
         case 282:   return "XRESOLUTION";
         case 283:   return "YRESOLUTION";
         case 284:   return "PLANARCONFIG";
         case 292:   return "T4OPTIONS";
         case 296:   return "RESOLUTIONUNIT";
         case 297:   return "PAGENUMBER";
         case 305:   return "SOFTWARE";
         case 306:   return "DATETIME";
         case 317:   return "PREDICTOR";
         case 320:   return "COLORMAP";
         case 321:   return "HALFTONEHINTS";
         case 322:   return "TILEWIDTH";
         case 323:   return "TILELENGTH";
         case 324:   return "TILEOFFSETS";
         case 325:   return "TILEBYTECOUNTS";
         case 326:   return "BADFAXLINES";
         case 327:   return "CLEANFAXDATA";
         case 328:   return "CONSECUTIVEBADFAXLINES";
         case 330:   return "SUBIFDS";
         case 338:   return "EXTRASAMPLES";
         case 339:   return "SAMPLEFORMAT";
         case 512:   return "JPEGPROC";
         case 519:   return "JPEGQTABLES";
         case 520:   return "JPEGDCTABLES";
         case 521:   return "JPEGACTABLES";
         case 529:   return "YCBCRCOEFFICIENTS";
         case 530:   return "YCBCRSUBSAMPLING";
         case 531:   return "YCBCRPOSITIONING";
         case 532:   return "REFERENCEBLACKWHITE";
         case 32996: return "DATATYPE";
         case 32997: return "IMAGEDEPTH";
         case 32998: return "TILEDEPTH";
         case 33432: return "COPYRIGHT";
         case 33723: return "RICHTIFFIPTC";
         case 34377: return "PHOTOSHOP";
         case 34665: return "EXIFIFD";
         case 34675: return "ICCPROFILE";
         case 37439: return "STONITS";
         case 42112: return "GDAL_METADATA";
         case 50341: return "PRINTIMAGEMATCHINGINFO";
         default:    return "-UnknownField-";
      }

   }


   /**
   * Returns the typesizes in int-array.
   * @return The sizes of each type in an int-array.
   */
   public static int[] getTypeSizes() {
      return typesize;
   }


   /**
   * Returns the size of an entry in the IFD.
   * @return Size in bytes of an entry in the IFD.
   */
   public static short getEntrySize() {
      return 12;
   }

   /**
   * Returns the size of the Tiff header.
   * @return The size in bytes of the Tiff header.
   */
   public static short getHeaderSize() {
      return 8;
   }

   /**
   * Returns the size of the entry count field.
   * @return The size in bytes of the field in the IFD that tells
   * the number of directory entries (ie the start of the IFD).
   */
   public static short getEntryCountSize() {
      return 2;
   }

   /**
   * Returns the size of the next IFD field.
   * @return The size in bytes of the field that tells
   *         the next ifd offset.
   */
   public static short getNextIFDOffsetSize() {
      return 4;
   }

}
