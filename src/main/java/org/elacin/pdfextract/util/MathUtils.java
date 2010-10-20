/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.elacin.pdfextract.util;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 7, 2010 Time: 5:54:30 AM To change this template use File | Settings
 * | File Templates.
 */
public final class MathUtils {
    //    public static float INT_PRECISION = 100.0f;

    private MathUtils() {
    }
    // -------------------------- PUBLIC STATIC METHODS --------------------------

    /**
     * Returns true if num2 is within percentage percent of num1
     *
     * @param num1
     * @param num2
     * @param percentage
     * @return
     */
    public static boolean isWithinPercent(final float num1, final float num2, final float percentage) {
        if (num1 == num2) {
            return true;
        }

        return (num1 + num1 / 100.0F * percentage) >= num2 && (num1 - num1 / 100.0F * percentage) <= num2;
    }

    /**
     * Returns true if num2 is within num ± i
     *
     * @param num1
     * @param num2
     * @param variance
     * @return
     */
    public static boolean isWithinVariance(final float num1, final float num2, final float variance) {
        if (num1 == num2) {
            return true;
        }

        return (num1 - variance) <= num2 && (num1 + variance) >= num2;
    }

    //    public static int round(float num) {
    //        return (int) (INT_PRECISION * num);
    //        //        return (int) num;
    //    }

    public static boolean overlap(float y1, float height1, float y2, float height2) {
        return within(y1, y2, .1f) || (y2 <= y1 && y2 >= y1 - height1) || (y1 <= y2 && y1 >= y2 - height2);
    }

    /**
     * This will determine of two floating point numbers are within a specified variance.
     *
     * @param first    The first number to compare to.
     * @param second   The second number to compare to.
     * @param variance The allowed variance.
     */
    public static boolean within(float first, float second, float variance) {
        return second < first + variance && second > first - variance;
    }

    //    public static float deround(final int i) {
    //        return (float) i / INT_PRECISION;
    //    }
    //
    //    public static float deround(final float i) {
    //        return i / INT_PRECISION;
    //    }

    public static float log(float a) {
        return (float) StrictMath.log((double) a);
    }
}
