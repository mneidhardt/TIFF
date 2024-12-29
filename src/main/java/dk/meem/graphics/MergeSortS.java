package dk.meem.graphics;

/*
 * @(#)MergeSort.java   Based on Mergesorter by H.W.Lang
 * FH Flensburg (lang@fh-flensburg.de)
 */

/**
 * This version handles arrays of shorts. It is primitive
 * but I had no time to implement a sorter that can handle
 * all types, so I made a hard coded version.
 *
 * A merge sort that handles sorting a simple array as
 * well as a (n x m) matrix.
 * I.e. the rows are being sorted, possibly on more
 * than one column.
 * Since arguments in Java are references, the sort()-methods
 * dont return anything. The array is sorted as it goes along.
 *
 * @author Michael Neidhardt
 * @version    0.1, 27. April 2007
 */


public class MergeSortS {
   private short arr[][];
   private short sequence[]=null;


   /** If you want to sort array on only one column, this is used.
   * @param a The array to be sorted (on one column in this case).
   * @param sortcolumn The column in array a to sort on.
   */
   public void sort(short a[][], int sortcolumn) {
      int sortcolumns[] = new int[1];
      sortcolumns[0] = sortcolumn;

      sort(a, sortcolumns);
   }


   /**
   * The main method for sorting.
   * @param a The array to be sorted (on 2 or more columns).
   * @param sortcolumns The list of columns to sort on.
   * @return The sorted array.
   */
   public void sort(short a[][], int sortcolumns[]) {
      arr = a;
      int n = arr.length;

      sequence = new short[n];

      // sequence contains the original position of each row.
      for (short i=0; i<sequence.length; i++) {
         sequence[i] = i;
      }

      mergesort(0, n-1, sortcolumns[0]);

      short val=arr[0][sortcolumns[0]];
      int lo=0, hi=0;


      /**
      * If we sort on more than one column, it's done here.
      */
      if (sortcolumns.length > 1) {
         for (int row=1; row<arr.length; row++) {
            if (arr[row][sortcolumns[0]] == val && row<arr.length-1) {
               ++hi;
            }
            else if (arr[row][sortcolumns[0]] == val && row==arr.length-1) {
               ++hi;
               mergesort(lo, hi, sortcolumns[1]);
            }
            else {
               mergesort(lo, hi, sortcolumns[1]);

               val=arr[row][sortcolumns[0]];
               lo = row;
               hi = row;
            }
         }
      }
   }


    private void mergesort(int lo, int hi, int sortcolumn)  {
        if (lo<hi) {
            int m=(lo+hi)/2;

            mergesort(lo, m, sortcolumn);
            mergesort(m+1, hi, sortcolumn);
            merge(lo, m, hi, sortcolumn);
        }
    }


   private void merge(int lo, int m, int hi, int sortcolumn) {
        int i, j, k;
        short b[][] = new short[m-lo+1][arr[0].length];
        short b2[] = new short[m-lo+1];

        i=0; j=lo;


        while (j<=m) {  // Copy first half of array a to auxiliary arrays.
           b2[i] = sequence[j];

           for (int col=0; col<arr[0].length; col++) {
              b[i][col] = arr[j][col];
           }

           ++i;
           ++j;
        }

        i=0; k=lo;

        while (k<j && j<=hi) {   // Copy back next-greatest element at each time

           if (b[i][sortcolumn] <= arr[j][sortcolumn]) {
              sequence[k] = b2[i];

              for (int col=0; col<arr[0].length; col++) {
                 arr[k][col] = b[i][col];
              }

              ++k;
              ++i;
           }
           else {
              sequence[k] = sequence[j];

              for (int col=0; col<arr[0].length; col++) {
                 arr[k][col] = arr[j][col];
              }

              ++k;
              ++j;
           }

        }

        while (k<j) {   // Copy back remaining elements of first half (if any)
           sequence[k] = b2[i];

           for (int col=0; col<arr[0].length; col++) {
              arr[k][col] = b[i][col];
           }

           ++k;
           ++i;
        }
   }


   /**
   * Returns the indices of the unsorted array in sorted order.
   * If the unsorted array is [40 10 11 30 20], the indices in
   * sorted order are [1 2 4 3 0].
   */
   public short[] getSequence() {
      return sequence;
   }

}
