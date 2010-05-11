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

package org.elacin.pdfextract.builder;

import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.text.Style;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 18, 2010
 * Time: 2:32:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class StyleFactory {
    // ------------------------------ FIELDS ------------------------------

    Map<Style, Style> styles = new HashMap<Style, Style>();
    private final MathContext mc;

    public StyleFactory() {
        mc = new MathContext(2, RoundingMode.HALF_UP);
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public Map<Style, Style> getStyles() {
        return styles;
    }

    // -------------------------- STATIC METHODS --------------------------


    // -------------------------- PUBLIC METHODS --------------------------

    public Style getStyleForTextPosition(TextPosition position) {
        float realFontSizeX = (position.getFontSize() * position.getXScale());
        float realFontSizeY = (position.getFontSize() * position.getYScale());

        /* build a string with fontname / type */
        final String baseFontName = position.getFont().getBaseFont() == null ? "null" : position.getFont().getBaseFont();
        final String fontname = baseFontName + " (" + position.getFont().getSubType() + ")";

        return getStyle(realFontSizeX, realFontSizeY, position.getWidthOfSpace(), fontname);
    }

    public Style getStyle(float xSize, float ySize, final float widthOfSpace, String font) {
        Style style = new Style(font, round(xSize), round(ySize), round(widthOfSpace));
        Style existing = styles.get(style);

        if (existing != null) {
            return existing;
        }

        styles.put(style, style);
        return style;
    }

    // -------------------------- OTHER METHODS --------------------------

    private float round(float num) {
        //        //TODO: this was slower than i thought
        //        BigDecimal bd = new BigDecimal(num, mc);
        //        return bd.floatValue();
        return num;
    }
}
