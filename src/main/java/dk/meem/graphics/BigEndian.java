package dk.meem.graphics;

/**
 * Class that implements the Endian interface, and thus
 * converts from big endian to decimal and from decimal
 * to bytes.
 *
 * @author Michael Neidhardt
 * @version    0.1, 29. september 2007
 */

public class BigEndian implements Endian {

   public int short2Int(short s1, short s2) {
      int result = s1 << 8;
      result    += s2;

      return result;
   }

   public long short2Long(short s1, short s2, short s3) {
      int result = s1 << 16;
      result    += s2 << 8;
      result    += s3;

      return result;
   }

   public long short2Long(short s1, short s2, short s3, short s4) {
      int result = s1 << 24;
      result    += s2 << 16;
      result    += s3 << 8;
      result    += s4;

      return result;
   }

   /*
   * This takes a short-array and turns it into an integer.
   * This means that there cannot be more than 2 (?) shorts in values[]!
   */
   public int short2Int(short values[]) {
      int exponent = values.length-1;
      int result=0;

      for (int i=0; i<values.length; i++) {
         result += values[i] << exponent*8;
         --exponent;
      }

      return result;
   }

   /*
   * This takes a short-array and turns it into an integer.
   * This means that there cannot be more than 4 shorts in values[]!
   */
   public long short2Long(short values[]) {
      int exponent = values.length-1;
      long result=0;

      for (int i=0; i<values.length; i++) {
         result += (long)(values[i] << exponent*8);
         --exponent;
      }

      return result;
   }

   /** This takes a short and expresses it as 2 bytes.
   */
   public byte[] short2Bytes(short s) {
      byte result[] = {0,0};

      result[0] = (byte)(s >>> 8);
      result[1] = (byte)(s & 0xff);

      return result;
   }

   /** This takes a long and expresses it as 4 bytes. It is a
   * Tiff LONG, hence the return array �s 4 bytes long.
   * In fact, this is the reverse of short2Long above.
   */
   public byte[] long2Bytes(long s) {
      byte result[] = new byte[4];

      result[0] = (byte)(s >>> 24);
      result[1] = (byte)(s >>> 16);
      result[2] = (byte)(s >>> 8);
      result[3] = (byte)(s & 0xff);

      return result;
   }
}
