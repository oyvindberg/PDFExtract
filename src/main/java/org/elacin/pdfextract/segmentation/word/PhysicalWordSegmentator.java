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

package org.elacin.pdfextract.segmentation.word;

import org.apache.log4j.Logger;
import org.apache.pdfbox.util.TextPosition;
import org.apache.pdfbox.util.TextPositionComparator;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentStyles;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.StringUtils;

import java.util.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 12, 2010 Time: 3:34:09 AM
 * <p/>
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * WordNodes, as used by this application. The difference between the two classes are technical, but
 * also semantic, in that they are defined to be at most a whole word (the normal case: Word
 * fragments will only occur when a word is split over two lines, or if the word is formatted with
 * two different styles) instead of arbitrary length.
 * <p/>
 * This makes it easier to reason about the information we have, and also reconstructs some notion
 * of word and character spacing, which will be an important property for feature recognition.
 * <p/>
 * By using createWords() all this will be done, and the words will be added to the the provided
 * document tree.
 */
public class PhysicalWordSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger LOG = Loggers.getWordBuilderLog();

// -------------------------- PUBLIC STATIC METHODS --------------------------

/**
 * This method will convert the text into WordNodes.
 * <p/>
 * To do this, the text is split on whitespaces, character and word distances are approximated, and
 * words are created based on those
 *
 * @param documentStyles The collection of styles used for the document
 * @param textToBeAdded  text which is to be added
 */
public static List<PhysicalText> createWords(final DocumentStyles documentStyles,
                                             final List<ETextPosition> textToBeAdded)
{
    long t0 = System.currentTimeMillis();

    List<PhysicalText> ret = new ArrayList<PhysicalText>(textToBeAdded.size());

    /* iterate through all incoming TextPositions, and process them
       in a line by line fashion. We do this to be able to calculate
       char and word distances for each line
    */

    List<TextPosition> line = new ArrayList<TextPosition>();

    Collections.sort(textToBeAdded, new TextPositionComparator());

    float minY = 0.0f;
    float maxY = 0.0f;
    float maxX = 0.0f;
    for (ETextPosition textPosition : textToBeAdded) {
        /* if this is the first text in a line */
        if (line.isEmpty()) {
            minY = textPosition.getY();
            maxY = minY + textPosition.getHeightDir();
            maxX = textPosition.getPos().getEndX();
        }

        if (isOnAnotherLine(minY, textPosition) || isTooFarAwayHorizontally(maxX, textPosition)) {
            if (!line.isEmpty()) {
                ret.addAll(createWordsInLine(documentStyles, line));
                line.clear();
            }
            minY = Float.MAX_VALUE;
        }

        /* then add the current text to start next line */
        line.add(textPosition);
        minY = Math.min(textPosition.getYDirAdj(), minY);
        maxY = Math.max(minY + textPosition.getHeightDir(), maxY);
        maxX = textPosition.getPos().getEndX();
    }

    if (!line.isEmpty()) {
        ret.addAll(createWordsInLine(documentStyles, line));
        line.clear();
    }

    LOG.info("PhysicalWordSegmentator.createWords took " + (System.currentTimeMillis() - t0)
            + " ms");
    return ret;
}

// -------------------------- STATIC METHODS --------------------------

/**
 * This method will process one line worth of TextPositions , and split and/or combine them as to
 * output words.
 *
 * @param documentStyles
 * @param line
 */
static List<PhysicalText> createWordsInLine(final DocumentStyles documentStyles,
                                            final List<TextPosition> line)
{
    final Comparator<PhysicalText> sortByLowerX = new Comparator<PhysicalText>() {
        public int compare(final PhysicalText o1, final PhysicalText o2) {
            return Float.compare(o1.getPosition().getX(), o2.getPosition().getX());
        }
    };
    PriorityQueue<PhysicalText> queue = new PriorityQueue<PhysicalText>(line.size(), sortByLowerX);

    /* first convert into text elements */
    queue.addAll(splitTextPositionsOnSpace(documentStyles, line));

    /* then calculate spacing */
    CharSpacingFinder.setCharSpacingForTexts(queue);

    /* finally iterate through all texts from left to right, and combine into words as we go */
    List<PhysicalText> ret = new ArrayList<PhysicalText>();
    while (!queue.isEmpty()) {
        final PhysicalText current = queue.remove();
        final PhysicalText next = queue.peek();

        if (next != null && PhysicalText.isCloseEnoughToBelongToSameWord(next)
                && current.isSameStyleAs(next)) {

            PhysicalText combinedWord = current.combineWith(next);
            queue.remove(next);
            queue.add(combinedWord);
            continue;
        }
        ret.add(current);
    }

    return ret;
}

/**
 * it happens that some of the TextPositions are unreasonably tall. This destroys my algorithms, so
 * for those we will use a lower value
 *
 * @param tp
 * @return
 */
private static float getAdjustedHeight(final TextPosition tp) {
    final float adjustedHeight;
    final float maxHeight = tp.getFontSize() * tp.getYScale() * 1.2f;
    if (tp.getHeightDir() > maxHeight) {
        adjustedHeight = maxHeight;
    } else {
        adjustedHeight = tp.getHeightDir();
    }
    return adjustedHeight;
}

private static boolean isOnAnotherLine(final float minY, final ETextPosition textPosition) {
    //noinspection FloatingPointEquality
    return minY != textPosition.getYDirAdj();
}

private static boolean isTooFarAwayHorizontally(final float maxX,
                                                final ETextPosition textPosition)
{
    return !MathUtils.isWithinVariance(maxX, textPosition.getPos().getX(),
                                       textPosition.getFontSizeInPt());
}

/**
 * This creates
 *
 * @param sf
 * @param textPositions
 * @return
 */
@SuppressWarnings({"ObjectAllocationInLoop"})
static List<PhysicalText> splitTextPositionsOnSpace(final DocumentStyles sf,
                                                    final List<TextPosition> textPositions)
{
    final List<PhysicalText> ret = new ArrayList<PhysicalText>(textPositions.size() * 2);

    final FloatPoint lastWordBoundary = new FloatPoint(0.0F, 0.0F);
    final StringBuilder contents = new StringBuilder();

    Collections.sort(textPositions, new TextPositionComparator());

    float width = 0.0f;
    boolean firstInLine = true;
    for (TextPosition tp : textPositions) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("in:" + StringUtils.getTextPositionString(tp));
        }
        float x = tp.getXDirAdj();
        float y = tp.getYDirAdj();
        final String s = tp.getCharacter();
        final Style style = sf.getStyleForTextPosition(tp);

        /** iterate through all the characters in textposition. Notice that the loop is a bit evil
         * in that it iterates one extra time as apposed to what you would expect, in order make sure
         * that the last piece of the textposition is created into a text.
         */
        for (int j = 0; j <= s.length(); j++) {
            /* if this is either the last character or a space then just output a new text */

            if (j == s.length() || Character.isSpaceChar(s.charAt(j))) {
                if (contents.length() > 0) {

                    final float distance;
                    if (firstInLine) {
                        distance = Float.MIN_VALUE;
                        firstInLine = false;
                    } else {
                        distance = x - lastWordBoundary.getX();
                    }

                    ret.add(new PhysicalText(contents.toString(), style, x, y - tp.getHeightDir(),
                                             width, getAdjustedHeight(tp), distance));

                    contents.setLength(0);
                    x += width;
                    width = 0.0F;
                    lastWordBoundary.setPosition(x, y);
                }

                if (j != s.length()) {
                    x += tp.getIndividualWidths()[j];
                }
            } else {
                /* include this character */
                width += tp.getIndividualWidths()[j];
                contents.append(s.charAt(j));
            }
        }
    }

    return ret;
}
}
