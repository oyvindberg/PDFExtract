package org.elacin.extract.builder;

import org.elacin.extract.text.Style;

import java.math.BigDecimal;
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
        BigDecimal bd = new BigDecimal(num, mc);
        return bd.floatValue();
    }
}
