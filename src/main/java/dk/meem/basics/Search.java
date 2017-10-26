package dk.meem.basics;

/**
 * Stuff that is useful.
 *
 * @author Michael Neidhardt
 * @version    0.1, 27. April 2007
 */

public class Search {

   /**
   * Basic binary search - from Ricardo Baeza-Yates.
   * Not used yet...
   * @param key The value to search for in r.
   * @param r The int array to search in.
   */
   public int binarySearch(int key, int array[]) {
      int high = array.length-1;
      int low  = -1;
      int mid;

      while (high-low>1) {
         mid = low + (high - low)/2;      // Avoids overflow when
                                          // both high and low are big.

         //System.out.println(low + " " + high + " : mid=" + mid );  // TESTTTTT

         if (key <= array[mid]) high = mid;
         else               low  = mid;
      }

      if (key == array[high]) {
         return high;
      }
      else {
         return -1;
      }
   }


   /**
   * Binary search in a range of an array - (from Tim Bray).
   * Used when finding the floor value or next highest.
   * If e.g. you want to find 6 or the next highest in
   * {2, 3, 5, 7}, you want to get back the index 3. This
   * does that.
   * Also handles duplicates by returning the first
   * one found, i.e. if arr={1,2,3,3,4}, and you search
   * for 3, this returns index 2.
   *
   * @param array The array we search in.
   * @param column The specific column of array to look in.
   * @param from The starting index (ie row) of array.
   * @param to The ending index (ie row) of array.
   * @param target The value we are looking for.
   * @return Index of 'target' in array[][column] or -1.
   */
   public int[] binarySearch2F(int array[][], int column, int from, int to, int target) {
      int low  = from-1;
      int high = to+1;
      int probe;
      int res[] = new int[2];

      while (high - low > 1) {
         probe = (high + low) >>> 1;

         if (array[probe][column] < target) {
            low = probe;
         }
         else {
            high = probe;
         }
      }

      if (high == to+1) {                       // Not found, and high
         res[0] = -1;                           // is above the range.
         res[1] = high;
      }
      else if (array[high][column] != target) { // Not found, and high
         res[0] = -2;                           // is within the range.
         res[1] = high;
      }
      else {                                    // Found.
         res[0] = 1;
         res[1] = high;
      }

      return res;
   }


   /**
   * Binary search in a range of an array - (from Tim Bray).
   * If, say, you want to find 6 or the next lowest in
   * {2, 3, 5, 7}, you want to get back the index 2. This
   * does that.
   * Also handles duplicates by returning the last
   * one found, i.e. if arr={1,2,3,3,4}, and you search
   * for 3, this returns index 3.
   *
   * @param array The array we search in.
   * @param column The specific column of array to look in.
   * @param from The starting index (ie row) of array.
   * @param to The ending index (ie row) of array.
   * @param target The value we are looking for.
   * @return Index of 'target' in array[][column] or -1.
   */
   public int[] binarySearch2L(int array[][], int column, int from, int to, int target) {
      int low  = from-1;
      int high = to+1;
      int probe;
      int res[] = new int[2];

      while (high - low > 1) {
         probe = (high + low) >>> 1;

         if (array[probe][column] > target) {
            high = probe;
         }
         else {
            low = probe;
         }
      }


      if (low == from-1) {                      // Not found, and low
         res[0] = -1;                           // is above the range.
         res[1] = low;
      }
      else if (array[low][column] != target) {  // Not found, and low
         res[0] = -2;                           // is within the range.
         res[1] = low;
      }
      else {                                    // Found.
         res[0] = 1;
         res[1] = low;
      }

      return res;
   }


   /**
   * Binary search for a floor and a ceiling value in a range
   * of an array. The range is specified by a start and an end row.
   * The array is an NxM array.
   * If floor is not found, returns the next higher
   * value and if ceiling is not found, returns the next lower,
   * provided these two are within the range.
   *
   * Say you want to find the position of a 6 and 9 in an array
   * A={2, 3, 8, 9}. Since 6 (the floor value) is not there,
   * 8 will do, since it is the next highest and within the range.
   * 9 is in the range, so we return indices {2, 3}.
   *
   * @param array The array we search in.
   * @param column The column of array that we search in.
   * @param rangefrom Index for the starting row.
   * @param rangeto Index for the ending row.
   * @param floorvalue The least value we search for.
   * @return An int[2] that contains the indices for the resulting
   * values, or {-1, -1} in case the search failed.
   */
   public int[] binaryRangeSearch(int array[][], int column, int rangefrom,
                              int rangeto, int floorvalue, int ceilingvalue) {

      int floorres[] = binarySearch2F(array, column, rangefrom, rangeto, floorvalue);
      int ceilres[]  = binarySearch2L(array, column, rangefrom, rangeto, ceilingvalue);

      int res[] = new int[2];

      if (floorres[0] == 1 && ceilres[0] == 1) {
         //System.out.println("OK. Both found in range.");
         res[0] = floorres[1];
         res[1] = ceilres[1];
      }
      else if (floorres[0] == -1 || ceilres[0] == -1) {
         //System.out.println("FAIL: Floor above range, ceil below range, or both.");
         res[0] = -1;
         res[1] = -1;
      }
      else if (floorres[0] == -2 && ceilres[0] == 1) {
         //System.out.println("OK: Floor not found but in range, and ceil found in range.");
         res[0] = floorres[1];
         res[1] = ceilres[1];
      }
      else if (floorres[0] == 1 && ceilres[0] == -2) {
         //System.out.println("OK: Floor found in range, and ceil not found but in range.");
         res[0] = floorres[1];
         res[1] = ceilres[1];
      }
      else if (floorres[0] == -2 && ceilres[0] == -2) {
         if (floorres[1] > ceilres[1]) {
            //System.out.println("FAIL: Floor above range, ceil below range, or both.");
            res[0] = -1;
            res[1] = -1;
         }
         else {
            //System.out.println("OK: None found, but higher floor and lower ceiling found in range.");
            res[0] = floorres[1];
            res[1] = ceilres[1];
         }
      }

      return res;

      /**
      * WHAT THE RETURN VALUES MEAN:
      * floorres[] = {status, value}, ceilres[] = {status, value}
      *
      *
      * floorres[0]     ceilres[0]     Meaning
      *-------------------------------------------------------------------------
      *      1              1          OK.   Both found.
      *      1             -1          FAIL. Ceilres < floorvalue.
      *      1             -2          OK.   Floorres found, ceil in range.
      *-------------------------------------------------------------------------
      *     -1              1          FAIL. Floorres > ceilvalue. Also
      *                                      requires ceilvalue < floorvalue!
      *     -1             -1          FAIL. Floorres > ceilvalue &
      *                                      ceilres < floorvalue.
      *     -1             -2          FAIL. Floorres > ceilvalue.
      *-------------------------------------------------------------------------
      *     -2              1          OK.   Floorres in range and ceilres found.
      *     -2             -1          FAIL. Ceilres < floorvalue.
      *     -2             -2          If floorres[1] <= ceilres[1] then OK.
      *                                Else FAIL.
      *-------------------------------------------------------------------------
      *
      * Being in range means that:
      *     floorres <= ceilvalue.
      *     ceilres  >= floorvalue
      *
      * When I write floorres and ceilres, I just mean the value actually found
      * in the search, i.e. high and low in the 2 methods that do the work.
      */
   }





   /**
   * Binary search for a range in a range of an array (from TBray.org).
   *
   * Different version of range searching.
   * This is good if you want surrounding values, i.e.
   * if you want floor value or the next lower, and the ceiling
   * value and the next higher.
   *
   * This version returns an 'inclusive'  range, i.e. if you
   * search for 2 and 6 in the array {1,2,4,6,8}, you get
   * back indices 0 and 4.
   * If you search for 3 and 7, you get back 1 and 4.
   * I.e. if the targets exist, you get their indices, and if the targets
   * dont exist, you get the index of the greatest entry that is lower
   * than targetFrom and the index of the smallest entry that is greater
   * than targetTo.
   *
   *
   * @param array The array we will search in.
   * @param column The specific column of array to search in.
   * @param from The starting index (ie row) of array.
   * @param to The ending index (ie row) of array.
   * @param target The value we are looking for.
   * @return Index of targetFrom and targetTo in array[][column] or {-1, -1}
   */
   public int[] binarySearch2Range(int array[][], int column, int from, int to, int targetFrom, int targetTo) {
      int probe, answer[] = {-1, -1};

      // Work on floor:
      int low = from-1;
      int high = to+1;

      while (high - low > 1) {
         probe = (high + low) >>> 1;
         if (array[probe][column] <= targetFrom) low = probe;
         else                                    high = probe;
      }
      answer[0] = low;

      // Work on ceiling:
      low = from-1;
      high = to+1;

      while (high - low > 1) {
         probe = (high + low) >>> 1;

         if (array[probe][column] > targetTo)  high = probe;
         else                                  low = probe;
      }
      answer[1] = high;

      return answer;
   }

}
