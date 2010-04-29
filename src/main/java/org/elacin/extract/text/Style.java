package org.elacin.extract.text;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 18, 2010
 * Time: 2:32:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class Style implements Serializable {
    // ------------------------------ FIELDS ------------------------------

    public final float xSize, ySize, widthOfSpace;
    public final String font;
    public int numCharsWithThisStyle;

    // --------------------------- CONSTRUCTORS ---------------------------

    public Style(final String font, final float xSize, final float ySize, final float widthOfSpace) {
        this.font = font;
        this.xSize = xSize;
        this.ySize = ySize;
        this.widthOfSpace = widthOfSpace;
    }

    // ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Style style = (Style) o;

        if (Float.compare(style.widthOfSpace, widthOfSpace) != 0) return false;
        if (Float.compare(style.xSize, xSize) != 0) return false;
        if (Float.compare(style.ySize, ySize) != 0) return false;
        if (font == null && style.font == null)
            return true;
        
        if (font != null ? !font.equals(style.font) : style.font != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (xSize != +0.0f ? Float.floatToIntBits(xSize) : 0);
        result = 31 * result + (ySize != +0.0f ? Float.floatToIntBits(ySize) : 0);
        result = 31 * result + (widthOfSpace != +0.0f ? Float.floatToIntBits(widthOfSpace) : 0);
        result = 31 * result + (font != null ? font.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(font);
        sb.append(":").append(xSize);
        sb.append('}');
        return sb.toString();
    }
}
