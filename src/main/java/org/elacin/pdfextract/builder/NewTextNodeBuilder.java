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
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.TextNode;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 12, 2010
 * Time: 3:34:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class NewTextNodeBuilder {
    // ------------------------------ FIELDS ------------------------------

    final DocumentNode root;

    char[] chars = new char[512];
    float maxHeight = 0.0f, width = 0.0f, y = -1, x = -1;
    int len = 0, textPositionIdx;
    private int pageNum;

    // --------------------------- CONSTRUCTORS ---------------------------

    public NewTextNodeBuilder(final DocumentNode root) {
        this.root = root;
    }

    // -------------------------- STATIC METHODS --------------------------

    private static boolean hasDifferentStyle(final Style oldStyle, final Style currentStyle) {
        return !oldStyle.equals(currentStyle);
    }

    private static boolean isOnDifferentLine(final TextPosition old, final TextPosition current) {
        return old.getYDirAdj() != current.getYDirAdj();
    }

    private static boolean isTooMuchSpaceBetween(final TextPosition old, final TextPosition current) {
        float oldTextXEnd = old.getXDirAdj() + old.getWidthDirAdj();
        float oldTextXEndPlusOneSpace = oldTextXEnd + 0.5f * old.getWidthOfSpace();

        //                if (current.getXDirAdj() < oldTextXEnd) return true;
        //TODO:
        if (current.getXDirAdj() > oldTextXEndPlusOneSpace) return true;

        return false;
    }

    // -------------------------- PUBLIC METHODS --------------------------

    public void fillPage(final int pageNum, List<TextPosition> textPositions) {
        processList(textPositions);
        this.pageNum = pageNum;
    }

    // -------------------------- OTHER METHODS --------------------------

    void advanceOneGlyph(TextPosition current, Style currentStyle) {
        char c = current.getCharacter().charAt(textPositionIdx);

        chars[len] = c;
        width += current.getIndividualWidths()[textPositionIdx];
        maxHeight = Math.max(maxHeight, current.getHeightDir());
        len++;
        textPositionIdx++;

        /* split on chars for now */
        if (Character.isSpaceChar(c)) {
            createTextNodeAndAddToTree(current, currentStyle);
        }
    }

    void createTextNodeAndAddToTree(TextPosition current, Style currentStyle) {
        String text = new String(chars, 0, len);
        root.addTextNode(new TextNode(new Rectangle2D.Float(x, y, width, maxHeight), pageNum, currentStyle, text));

        /* then reset state */
        len = 0;
        maxHeight = 0.0f;
        width = 0.0f;

        x = current.getXDirAdj();
        y = current.getYDirAdj();
    }

    private void reset() {
        for (int i = 0; i < len; i++) {
            chars[i] = '\0';
        }
        maxHeight = 0.0f;
        width = 0.0f;
        y += maxHeight;
        x += width;
        len = 0;
    }

    void processList(List<TextPosition> textPositions) {
        TextPosition old = null;
        Style oldStyle = null, currentStyle = null;

        reset();

        for (TextPosition current : textPositions) {
            currentStyle = root.getStyles().getStyleForTextPosition(current);
            //            System.out.println(TextNodeBuilder.getTextPositionString(current));
            /* see if we should finish the current TextNode */
            if (oldStyle == null || old == null) {
                x = current.getXDirAdj();
                y = current.getYDirAdj();
            } else if (isOnDifferentLine(old, current) || hasDifferentStyle(oldStyle, currentStyle) || isTooMuchSpaceBetween(old, current)) {
                createTextNodeAndAddToTree(current, currentStyle);
            } else {
                width = current.getXDirAdj() - x;
                maxHeight = Math.max(maxHeight, current.getHeightDir());
            }

            textPositionIdx = 0;

            while (textPositionIdx < current.getIndividualWidths().length) {
                advanceOneGlyph(current, currentStyle);
            }

            old = current;
            oldStyle = currentStyle;
        }

        createTextNodeAndAddToTree(old, currentStyle);
    }
}
