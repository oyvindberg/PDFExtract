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

package org.elacin.pdfextract.integration.pdfbox;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:39 PM <p/> <p/> This
 * class
 * is meant to belong to a document, so that all the styles used in a document will be available
 * here. There is some optimization to avoid excessive object creation.
 */
public class Fonts implements Serializable {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(Fonts.class);

private static List<String> mathFonts = new ArrayList<String>() {{
    add("CMSY");
    add("CMEX");
    add("CMMI");
}};


final Map<String, Style> styles = new HashMap<String, Style>();

private class FontInfo {
    String  font;
    boolean mathFont, bold, italic;
}

final Map<PDFont, FontInfo> fontInfoCache = new HashMap<PDFont, FontInfo>();


// --------------------- GETTER / SETTER METHODS ---------------------

public Map<String, Style> getStyles() {
    return styles;
}

// -------------------------- PUBLIC METHODS --------------------------

public Style getStyleForTextPosition(@NotNull TextPosition tp) {
    int xSize = (int) (tp.getFontSize() * tp.getXScale());
    int ySize = (int) (tp.getFontSize() * tp.getYScale());

    final FontInfo fi = getFontInfo(tp.getFont());

    StringBuffer idBuffer = new StringBuffer(fi.font);
    idBuffer.append("-");
    idBuffer.append(xSize);
    if (fi.italic) {
        idBuffer.append("I");
    }
    if (fi.bold) {
        idBuffer.append("B");
    }
    if (fi.mathFont) {
        idBuffer.append("M");
    }
    final String id = idBuffer.toString();

    if (!styles.containsKey(id)) {
        final Style style = new Style(fi.font, xSize, ySize, id, fi.italic, fi.bold, fi.mathFont);
        styles.put(id, style);
        if (log.isInfoEnabled()) {
            log.info("LOG00800:New style:" + style);
        }
    }

    return styles.get(id);
}

private FontInfo getFontInfo(PDFont pdFont) {

    if (fontInfoCache.containsKey(pdFont)) {
        return fontInfoCache.get(pdFont);
    }

    /* find the appropriate font name to use  - baseFont might sometimes be null*/
    String font;
    if (pdFont.getBaseFont() == null) {
        font = pdFont.getSubType();
    } else {
        font = pdFont.getBaseFont();
    }

    /* a lot of embedded fonts have names like XCSFS+Times, so remove everything before and
     including '+' */
    final int plusIndex = font.indexOf('+');
    if (plusIndex != -1) {
        font = font.substring(plusIndex + 1, font.length());
    }

    boolean mathFont = font.length() > 4 && mathFonts.contains(font.substring(0, 4));
    boolean bold = font.toLowerCase().contains("bold");
    boolean italic = font.toLowerCase().contains("italic");


    /**
     * Find the plain font name, without any other information
     * Three typical font names can be:
     * - LPPMinionUnicode-Italic
     * - LPPMyriadCondLightUnicode (as apposed to for example LPPMyriadCondUnicode and
     * LPPMyriadLightUnicode-Bold)
     * - Times-Bold (Type1)
     *
     * I want to separate the LPPMinion part for example from the first, so i look for the
     *  index of the first capital letter after a small one.
     *  Also stop if we reach an '-' or whitespace, as that is a normal separators
     * */

    boolean foundLowercase = false;
    int index = -1;
    for (int i = 0; i < font.length(); i++) {
        final char ch = font.charAt(i);

        if (!foundLowercase && isLowerCase(ch)) {
            foundLowercase = true;
        } else if (foundLowercase && isUpperCase(ch)) {
            index = i;
            break;
        } else if ('-' == ch || isWhitespace(ch)) {
            index = i;
            break;
        }
    }

    if (index != -1) {
        font = font.substring(0, index);
    }


    /* this is latex specific */
    if (font.contains("CMBX")) {
        font = font.replace("CMBX", "CMR");
        bold = true;
        italic = false;
    } else if (font.contains("CMTI")) {
        font = font.replace("CMTI", "CMR");
        bold = false;
        italic = true;
    }


    final FontInfo fi = new FontInfo();
    fi.font = font;
    fi.bold = bold;
    fi.italic = italic;
    fi.mathFont = mathFont;

    fontInfoCache.put(pdFont, fi);
    return fi;
}
}
