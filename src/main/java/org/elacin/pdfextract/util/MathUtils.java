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
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 7, 2010
 * Time: 5:54:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class MathUtils {
    /**
     * Returns true if num2 is within num ± i
     *
     * @param i
     * @param num1
     * @param num2
     * @return
     */
    public static boolean withinNum(final double i, final double num1, final double num2) {
        if (num1 == num2) return true;

        return (num1 - i) <= num2 && (num1 + i) >= num2;
    }

    /**
     * Returns true if num2 is within i% of num1
     *
     * @param i
     * @param num1
     * @param num2
     * @return
     */
    public static boolean withinPercent(final double i, final double num1, final double num2) {
        if (num1 == num2) return true;

        return (num1 - (num1 / 100.0 * i)) <= num2 && (num1 + (num1 / 100.0 * i) > num2);
    }
}
