package dk.meem.graphics;

/**
 * Interface for Endian-conversions.
 *
 * @author Michael Neidhardt
 * @version    0.1, 29. september 2007
 */

public interface Endian {

   int  short2Int(short s1, short s2);
   long short2Long(short s1, short s2, short s3);
   long short2Long(short s1, short s2, short s3, short s4);
   int  short2Int(short values[]);
   long short2Long(short values[]);

   byte[] short2Bytes(short s);
   byte[] long2Bytes(long s);
}
