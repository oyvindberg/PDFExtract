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

package org.elacin.pdfextract.style;


import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:20 PM <p/> A style is a
 * collection of a font (described by a string), its X and Y sizes, and an estimate of the widt of a
 * space in this font. All these properties are immutable.
 */
public class Style implements Serializable {
// ------------------------------ FIELDS ------------------------------

public static final Style GRAPHIC_IMAGE     = new Style("Graphical container", -1, -1, "[IMG]",
        false, false, false);
public static final Style GRAPHIC_VSEP      = new Style("Graphical vertical separator", -1, -1,
        "[VSEP]", false, false, false);
public static final Style GRAPHIC_HSEP      = new Style("Graphical horizontal separator", -1, -1,
        "[HSEP]", false, false, false);
public static final Style GRAPHIC_MATH_BAR  = new Style("Graphical math bar", -1, -1, "[BAR]",
        false, false, false);
public static final Style GRAPHIC_CHARACTER = new Style("Graphical character", -1, -1, "[?]", false,
        false, false);
public static final Style GRAPHIC_CONTAINER = new Style("Graphical container", -1, -1,
        "[CONTAINER]", false, false, false);

public static final Style FORMULA  = new Style("Formula", -2, -2, "FORMULA", false, false, true);
public static       Style NO_STYLE = new Style("No style", -3, -3, "[NOSTYLE]", false, false,
        false);


public final int xSize, ySize;
public final String  fontName;
public final String  id;
final        boolean italic;
final        boolean bold;
final        boolean mathFont;

private transient boolean toStringCreated;
private transient String  toStringCache;

// --------------------------- CONSTRUCTORS ---------------------------

public Style(final String fontName,
             final int xSize,
             final int ySize,
             final String id,
             final boolean italic,
             final boolean bold,
             final boolean mathFont) {
    this.fontName = fontName;
    this.xSize = xSize;
    this.ySize = ySize;
    this.id = id;
    this.italic = italic;
    this.bold = bold;
    this.mathFont = mathFont;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public boolean equals(final Object o) {
    if (this == o) {
        return true;
    }
    if (o == null || getClass() != o.getClass()) {
        return false;
    }

    final Style style = (Style) o;

    return id.equals(style.id);
}

@Override
public int hashCode() {
    return id.hashCode();
}

@Override
public String toString() {
    if (!toStringCreated) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(fontName);
        sb.append(", size=").append(xSize);
        if (italic) {
            sb.append(" (italic)");
        }
        if (bold) {
            sb.append(" (bold)");
        }
        if (mathFont) {
            sb.append(" (math)");
        }
        sb.append('}');
        toStringCache = sb.toString();
        toStringCreated = true;
    }

    return toStringCache;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isBold() {
    return bold;
}

public boolean isItalic() {
    return italic;
}

public boolean isMathFont() {
    return mathFont;
}
}
