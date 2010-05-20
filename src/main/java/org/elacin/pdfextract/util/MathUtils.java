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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 7, 2010
 * Time: 5:54:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class MathUtils {
    //    /**
    //     * Returns true if num2 is within num ± i
    //     *
    //     * @param num1
    //     * @param num2
    //     * @param variance
    //     * @return
    //     */
    //    public static boolean isWithinVariance(final double num1, final double num2, final double variance) {
    //        if (num1 == num2) return true;
    //
    //        return (num1 - variance) <= num2 && (num1 + variance) >= num2;
    //    }

    /**
     * Returns true if num2 is within num ± i
     *
     * @param num1
     * @param num2
     * @param variance
     * @return
     */
    public static boolean isWithinVariance(final int num1, final int num2, final int variance) {
        if (num1 == num2) return true;

        return (num1 - variance) <= num2 && (num1 + variance) >= num2;
    }

    static MathContext mc = new MathContext(6, RoundingMode.CEILING);

    public static int round(float num) {
        //        return (int) num;
        BigDecimal a = new BigDecimal(num * 100);
        final BigDecimal bigDecimal = a.round(mc);

        //        return (int) (num * 100f) + 1;

        return bigDecimal.intValue();
    }
}
