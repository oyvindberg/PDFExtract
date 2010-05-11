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

import org.apache.pdfbox.util.ICU4JImpl;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.TextNode;
import org.elacin.pdfextract.util.MathUtils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextNodeBuilder {
    // --------------------------- CONSTRUCTORS ---------------------------

    private TextNodeBuilder() {
    }

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    public static void fillPage(final DocumentNode root, final int pageNum, final List<TextPosition> textPositions, StyleFactory sf) throws IOException {
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
                root.addTextNode(combineIntoFragment(toBeCombined, sf, pageNum));
                toBeCombined.clear();
                toBeCombined.add(newText);
            }
        }

        /* the loop above is stupidly written and may leave some text not 
        *   combined. finish the previous text fragment here */
        if (!toBeCombined.isEmpty()) {
            root.addTextNode(combineIntoFragment(toBeCombined, sf, pageNum));
        }
    }

    public static String getTextPositionString(final TextPosition position) {
        StringBuilder sb = new StringBuilder("pos{");
        sb.append("c=\"").append(position.getCharacter()).append("\"");
        //        sb.append(", X=").append(position.getX());
        //        sb.append(", XScale=").append(position.getXScale());
        //        sb.append(", Y=").append(position.getY());
        //        sb.append(", Dir=").append(position.getDir());
        sb.append(", XDirAdj=").append(position.getXDirAdj());
        sb.append(", YDirAdj=").append(position.getYDirAdj());
        //        sb.append(", XScale=").append(position.getXScale());
        //        sb.append(", YScale=").append(position.getYScale());
        //        sb.append(", Height=").append(position.getHeight());
        //        sb.append(", Width=").append(position.getWidth());
        sb.append(", endY=").append(position.getYDirAdj() + position.getHeightDir());
        sb.append(", endX=").append(position.getXDirAdj() + position.getWidthDirAdj());

        sb.append(", HeightDir=").append(position.getHeightDir());
        sb.append(", WidthDirAdj=").append(position.getWidthDirAdj());

        sb.append(", WidthOfSpace=").append(position.getWidthOfSpace());
        sb.append(", WordSpacing()=").append(position.getWordSpacing());
        sb.append(", FontSize=").append(position.getFontSize());
        //        sb.append(", FontSizeInPt=").append(position.getFontSizeInPt());
        sb.append(", getIndividualWidths=").append(Arrays.toString(position.getIndividualWidths()));
        sb.append(", getFont().getBaseFont()=").append(position.getFont().getBaseFont());

        sb.append("}");
        return sb.toString();
    }

    public static boolean textContinuesEarlierText(final TextPosition newText, final TextPosition oldText) {
        final float yFontSize = newText.getFontSize() * newText.getYScale();

        /**
         * A few observations:
         *
         *  TextPosition.wordSpacing generally seems to be very close to zero, we want to be a bit more flexible.
         *      one tenth of a space would for a ten pt font be around 0.5pts
         */

        final double dist = Point2D.distance(oldText.getXDirAdj() + oldText.getWidthDirAdj(), oldText.getYDirAdj(), newText.getXDirAdj(), newText.getYDirAdj());


        /* check if the new text is at most one character after the end of the previous one */
        //        if (!MathUtils.withinNum(oldText.getWidthOfSpace() * 0.1, newText.getXDirAdj(), oldText.getXDirAdj() + oldText.getWidthDirAdj())) {
        if (dist > oldText.getWidthOfSpace() * 0.7f) {
            return false;
        }

        if (!MathUtils.withinNum(yFontSize, newText.getYDirAdj(), oldText.getYDirAdj())) {
            return false;
        }

        return textIsSameFont(newText, oldText);
    }

    public static boolean textIsSameFont(final TextPosition newText, final TextPosition oldText) {
        if (!newText.getFont().equals(oldText.getFont())) {
            return false;
        }

        final float xFontSize = newText.getFontSize() * newText.getXScale();
        if (!MathUtils.withinPercent(1, xFontSize, oldText.getFontSize() * oldText.getXScale())) {
            return false;
        }
        final float yFontSize = newText.getFontSize() * newText.getYScale();
        if (!MathUtils.withinPercent(1, yFontSize, oldText.getFontSize() * oldText.getYScale())) {
            return false;
        }

        if (newText.getDir() != oldText.getDir()) {
            return false;
        }
        return true;
    }

    // -------------------------- STATIC METHODS --------------------------

    static TextNode combineIntoFragment(final List<TextPosition> toBeCombined, StyleFactory sf, final int pageNum) throws IOException {
        StringBuilder contentBuffer = new StringBuilder();

        /* holds the upper left point */
        float maxy = 0.0f, minx = Float.MAX_VALUE;
        float width = 0.0f, maxheight = 0.0f;

        /* compute upper left point, and offsets in X and Y direction */
        Loggers.getTextNodeBuilderLog().info("These textpositions will form one TextNode:");
        for (TextPosition position : toBeCombined) {
            if (Loggers.getTextNodeBuilderLog().isInfoEnabled()) {
                Loggers.getTextNodeBuilderLog().info(getTextPositionString(position));
            }

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
        Style style = sf.getStyleForTextPosition(toBeCombined.get(0));
        if (Loggers.getTextNodeBuilderLog().isInfoEnabled()) {
            Loggers.getTextNodeBuilderLog().info("style=" + style.toString());
        }
        float realFontSizeY = (toBeCombined.get(0).getFontSize() * toBeCombined.get(0).getYScale());

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

        /* create the new TextNode */
        return new TextNode(position, pageNum, style, content);
    }
}