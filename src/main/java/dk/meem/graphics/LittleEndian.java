package dk.meem.graphics;

/**
 * Class that implements the Endian interface, and thus
 * converts from little endian to decimal.
 * The method is simply to take 2, 3 or 4 shorts, s1, s2, s3 and s4,
 * and convert them in correct order. This means here that I do this:
 * decimalvalue = s1*256^0 + s2*256^1 + s3*256^2 + s4*256^3.
 * I do this using the shift operator.
 *
 * @author Michael Neidhardt
 * @version    0.1, 29. september 2007
 */

public class LittleEndian implements Endian {

   public int short2Int(short s1, short s2) {
      int result = s1;
      result    += s2 << 8;

      return result;
   }

   public long short2Long(short s1, short s2, short s3) {
      long result = s1;
      result    += s2 << 8;
      result    += s3 << 16;

      return result;
   }

   public long short2Long(short s1, short s2, short s3, short s4) {
      int result = s1;
      result    += s2 << 8;
      result    += s3 << 16;
      result    += s4 << 24;

      return result;
   }


   /*
   * This takes a short-array and turns it into an integer.
   * This means that there cannot be more than 2 shorts in values[]!
   */
   public int short2Int(short values[]) {
      int result=0;

      for (int i=0; i<values.length; i++) {
         result += values[i] << i*8;
      }

      return result;
   }

   /*
   * This takes a short-array and turns it into an integer.
   * This means that there cannot be more than 4 shorts in values[]!
   */
   public long short2Long(short values[]) {
      long result=0;

      for (int i=0; i<values.length; i++) {
         result += (long)(values[i] << i*8);
      }

      return result;
   }


   /** This takes a short and expresses it as 2 bytes.
   */
   public byte[] short2Bytes(short s) {
      byte result[] = {0,0};

      result[0] = (byte)(s & 0xff);
      result[1] = (byte)(s >>> 8);

      return result;
   }


   /** This takes a long and expresses it as 4 bytes. It is a
   * Tiff LONG, hence the return array �s 4 bytes long.
   * In fact, this is the reverse of short2Long above.
   */
   public byte[] long2Bytes(long s) {
      byte result[] = new byte[4];

      result[0] = (byte)(s & 0xff);
      result[1] = (byte)(s >>> 8);
      result[2] = (byte)(s >>> 16);
      result[3] = (byte)(s >>> 24);

      return result;
   }

}
