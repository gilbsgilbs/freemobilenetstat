/*
 * Copyright (C) 2012 Pixmob (http://github.com/pixmob)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pixmob.freemobile.netstat.util;

/**
 * Search utilities.
 * @author gilbsgilbs
 */
public final class SearchUtils {

    private SearchUtils() {}

    /**
     * Search a value in an array of intervals. Intervals are closed.
     * If some intervals are overlapping and are containing the searched value, this function will
     * possibly not return the first one that match.
     *
     * @param value the searched value
     * @param intervals the sorted array of closed intervals in \{inf, sup\} format.
     * @return the index of the interval containing the value or -1.
     */
    public static int binarySearchInIntervals(int value, final int[][] intervals) {
        if (intervals.length == 0)
            return -1;

        int left = 0,
            right = intervals.length - 1;

        do {
            final int middle = (right + left) / 2;
            if (value < intervals[middle][0])
                right = middle - 1;
            else if (value > intervals[middle][1])
                left = middle + 1;
            else
                return middle;
        } while (left <= right);

        return -1;
    }
}
