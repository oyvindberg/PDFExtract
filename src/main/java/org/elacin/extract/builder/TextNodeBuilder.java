package org.elacin.extract.builder;

import org.apache.pdfbox.util.TextPosition;
import org.elacin.extract.text.Style;
import org.elacin.extract.tree.PageNode;
import org.elacin.extract.tree.TextNode;

import java.awt.*;
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

    public static void fillPage(final PageNode pageNode, final List<TextPosition> textPositions, StyleFactory sf) throws IOException {
        //TODO
        List<TextPosition> toBeCombined = new ArrayList<TextPosition>();
        for (final TextPosition newText : textPositions) {
//            System.out.println("newText = " + newText.getCharacter() + ", dir=" + newText.getDir() + ", x= " + Arrays.toString(newText.getIndividualWidths()) + ", y=" + newText.getYDirAdj() + ", width=" + newText.getWidthDirAdj() + ", height=" + newText.getHeightDir());
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
        //        Loggers.getCreateTreeLog().trace("\twithinNum: checking if " + num2 + " is within " + num1 + " +- " + i);
        if (num1 == num2) return true;

        return (num1 - i) <= num2 && (num1 + i) >= num2;
    }

    // -------------------------- STATIC METHODS --------------------------

    static TextNode combineIntoFragment(final List<TextPosition> toBeCombined, StyleFactory sf) throws IOException {
        StringBuilder text = new StringBuilder();

        /* holds the upper left point */
        float maxy = 0.0f, minx = Float.MAX_VALUE;
        float width = 0.0f, maxheight = 0.0f;

        /* compute upper left point, and offsets in X and Y direction */
        for (TextPosition position : toBeCombined) {
            text.append(position.getCharacter());
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
        float realFontSizeX =  (firstPos.getFontSize() * firstPos.getXScale());
        float realFontSizeY =  (firstPos.getFontSize() * firstPos.getYScale());
        Style style = sf.getStyle(realFontSizeX, realFontSizeY, firstPos.getWidthOfSpace(), firstPos.getFont().getBaseFont());
        Rectangle2D.Float position = new Rectangle2D.Float(minx, (maxy - maxheight), width, maxheight);

        /* create the new fragment */
        return new TextNode(position, style, text.toString());
    }

    static boolean textContinuesEarlierText(final TextPosition newText, final TextPosition oldText) {
        final float xFontSize = newText.getFontSize() * newText.getXScale();
        final float yFontSize = newText.getFontSize() * newText.getYScale();

        /* check if the new text is at most one character after the end of the previous one */
        if (!withinNum(oldText.getWidthOfSpace() * 0.5 /*xFontSize*/, newText.getXDirAdj(), oldText.getXDirAdj() + oldText.getWidthDirAdj())) {
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