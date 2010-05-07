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

package org.elacin.extract.builder;

import org.apache.pdfbox.util.ICU4JImpl;
import org.apache.pdfbox.util.TextNormalize;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.extract.Loggers;
import org.elacin.extract.text.Style;
import org.elacin.extract.tree.PageNode;
import org.elacin.extract.tree.TextNode;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextNodeBuilder {
    // ------------------------------ FIELDS ------------------------------

    static TextNormalize normalizer = new TextNormalize("UTF-8");

    // --------------------------- CONSTRUCTORS ---------------------------

    private TextNodeBuilder() {
    }

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    public static void fillPage(final PageNode pageNode, final List<TextPosition> textPositions, StyleFactory sf) throws IOException {
        //TODO
        List<TextPosition> toBeCombined = new ArrayList<TextPosition>();
        for (final TextPosition newText : textPositions) {
            /* check if the current text position is either first in a new text fragment, or
                whether it will continue the one we are combining 
             */
            if (toBeCombined.isEmpty() || textContinuesEarlierText(newText, toBeCombined.get(toBeCombined.size() - 1))) {
                toBeCombined.add(newText);
                continue;
            } else {
                /* if the new text position did not geometrically continue the former text,
                    finish the text fragment we have started building, and start a new one */
                pageNode.addTextNode(combineIntoFragment(toBeCombined, sf));
                toBeCombined.clear();
                toBeCombined.add(newText);
            }
        }

        /* the loop above is stupidly written and may leave some text not 
        *   combined. finish the previous text fragment here */
        if (!toBeCombined.isEmpty()) {
            pageNode.addTextNode(combineIntoFragment(toBeCombined, sf));
        }
    }

    public static boolean withinNum(final double i, final double num1, final double num2) {
        if (num1 == num2) return true;

        return (num1 - i) <= num2 && (num1 + i) >= num2;
    }

    // -------------------------- STATIC METHODS --------------------------

    static TextNode combineIntoFragment(final List<TextPosition> toBeCombined, StyleFactory sf) throws IOException {
        StringBuilder contentBuffer = new StringBuilder();

        /* holds the upper left point */
        float maxy = 0.0f, minx = Float.MAX_VALUE;
        float width = 0.0f, maxheight = 0.0f;

        /* compute upper left point, and offsets in X and Y direction */
        Loggers.getTextNodeBuilderLog().info("These textpositions will form one TextNode:");
        for (TextPosition position : toBeCombined) {
            Loggers.getTextNodeBuilderLog().info(getTextPositionString(position));

            contentBuffer.append(position.getCharacter());
            if (position.getYDirAdj() > maxy) {
                maxy = position.getYDirAdj();
            }
            if (position.getXDirAdj() < minx) {
                minx = position.getXDirAdj();
            }

            if (position.getXDirAdj() + position.getWidthDirAdj() > width + minx) {
                width = position.getXDirAdj() + position.getWidthDirAdj() - minx;
            }

            if (position.getHeightDir() > maxheight) {
                maxheight = position.getHeightDir();
            }
        }

        /* compute the final style and position for the fragment. */
        final TextPosition firstPos = toBeCombined.get(0);
        float realFontSizeX = (firstPos.getFontSize() * firstPos.getXScale());
        float realFontSizeY = (firstPos.getFontSize() * firstPos.getYScale());

        /* build a string with fontname / type */
        final String fontname =
                (firstPos.getFont().getBaseFont() == null ? "null" : firstPos.getFont().getBaseFont()) + " (" + firstPos.getFont().getSubType() + ")";

        Style style = sf.getStyle(realFontSizeX, realFontSizeY, firstPos.getWidthOfSpace(), fontname);


        if (maxheight == 0.0) {
            maxheight = realFontSizeY;
        }
        Rectangle2D.Float position = new Rectangle2D.Float(minx, (maxy - maxheight), width, maxheight);

        String content = contentBuffer.toString();

        /* if content looks to be base16 encoded, try to decode it.
            if this fails, we will just use the original content */
        if (Base16Converter.isBase16Encoded(content)) {
            content = Base16Converter.decodeBase16(content);
        }

        /* normalize text, that is fix ligatures and so on */
        content = new ICU4JImpl().normalizePres(content);

        /* create the new fragment */
        return new TextNode(position, style, content);
    }

    private static String getTextPositionString(final TextPosition position) {
        StringBuilder sb = new StringBuilder("pos{");
        sb.append("c=").append(position.getCharacter());
        sb.append(", X=").append(position.getX());
        sb.append(", XScale=").append(position.getXScale());
        sb.append(", Y=").append(position.getY());
        sb.append(", Dir=").append(position.getDir());
        sb.append(", XDirAdj=").append(position.getXDirAdj());
        sb.append(", YDirAdj=").append(position.getYDirAdj());
        sb.append(", XScale=").append(position.getXScale());
        sb.append(", YScale=").append(position.getYScale());
        sb.append(", Height=").append(position.getHeight());
        sb.append(", HeightDir=").append(position.getHeightDir());
        sb.append(", Width=").append(position.getWidth());
        sb.append(", WidthDirAdj=").append(position.getWidthDirAdj());
        sb.append(", WidthOfSpace=").append(position.getWidthOfSpace());
        sb.append(", WordSpacing()=").append(position.getWordSpacing());
        sb.append(", FontSize=").append(position.getFontSize());
        sb.append(", FontSizeInPt=").append(position.getFontSizeInPt());

        sb.append("}");
        return sb.toString();
    }

    static boolean textContinuesEarlierText(final TextPosition newText, final TextPosition oldText) {
        final float yFontSize = newText.getFontSize() * newText.getYScale();

        /**
         * A few observations:
         *
         *  TextPosition.wordSpacing generally seems to be very close to zero, we want to be a bit more flexible.
         *      one tenth of a space would for a ten pt font be around 0.5pts
         */

        /* check if the new text is at most one character after the end of the previous one */
        if (!withinNum(oldText.getWidthOfSpace() * 0.1, newText.getXDirAdj(), oldText.getXDirAdj() + oldText.getWidthDirAdj())) {
            return false;
        }

        if (!withinNum(yFontSize, newText.getYDirAdj(), oldText.getYDirAdj())) {
            return false;
        }

        return textIsSameFont(newText, oldText);
    }

    private static boolean textIsSameFont(final TextPosition newText, final TextPosition oldText) {
        if (!newText.getFont().equals(oldText.getFont())) {
            return false;
        }

        final float xFontSize = newText.getFontSize() * newText.getXScale();
        if (!withinPercent(1, xFontSize, oldText.getFontSize() * oldText.getXScale())) {
            return false;
        }
        final float yFontSize = newText.getFontSize() * newText.getYScale();
        if (!withinPercent(1, yFontSize, oldText.getFontSize() * oldText.getYScale())) {
            return false;
        }

        if (newText.getDir() != oldText.getDir()) {
            return false;
        }
        return true;
    }

    static boolean withinPercent(final double i, final double num1, final double num2) {
        if (num1 == num2) return true;

        return (num1 - (num1 / 100.0 * i)) <= num2 && (num1 + (num1 / 100.0 * i) > num2);
    }
}