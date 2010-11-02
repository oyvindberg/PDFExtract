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
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.segmentation.PhysicalText;
import org.elacin.pdfextract.segmentation.WordSegmentator;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentStyles;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.StringUtils;

import java.util.*;

import static org.elacin.pdfextract.util.MathUtils.isWithinVariance;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 12, 2010 Time: 3:34:09 AM
 * <p/>
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * PhyscalTexts, as used by this application. The difference between the two classes are technical,
 * but also semantic, in that they are defined to be at most a whole word (the normal case: Word
 * fragments will only occur when a word is split over two lines, or if the word is formatted with
 * two different styles) instead of arbitrary length.
 * <p/>
 * This makes it easier to reason about the information we have, and also reconstructs some notion
 * of word and character spacing, which will be an important property for feature recognition.
 * <p/>
 */
public class WordSegmentatorImpl implements WordSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(WordSegmentatorImpl.class);

private final DocumentStyles styles;

// --------------------------- CONSTRUCTORS ---------------------------

public WordSegmentatorImpl(final DocumentStyles styles) {
    this.styles = styles;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface WordSegmentator ---------------------

/**
 * This method will convert the text into WordNodes.
 * <p/>
 * To do this, the text is split on whitespaces, character and word distances are approximated, and
 * words are created based on those
 */
@Override
public List<PhysicalText> segmentWords(final List<ETextPosition> text) {
    long t0 = System.currentTimeMillis();

    List<PhysicalText> ret = new ArrayList<PhysicalText>(text.size());

    /* iterate through all incoming TextPositions, and process them
       in a line by line fashion. We do this to be able to calculate
       char and word distances for each line
    */
    List<TextPosition> line = new ArrayList<TextPosition>();

    Collections.sort(text, new TextPositionComparator());

    float minY = 0.0f;
    float maxY = 0.0f;
    float maxX = 0.0f;

    for (ETextPosition tp : text) {
        /* if this is the first text in a line */
        if (line.isEmpty()) {
            minY = tp.getY();
            maxY = minY + tp.getHeightDir();
            maxX = tp.getPos().getEndX();
        }

        if (isOnAnotherLine(minY, tp) || isTooFarAwayHorizontally(maxX, tp)) {
            if (!line.isEmpty()) {
                ret.addAll(createWordsInLine(line));
                line.clear();
            }
            minY = Float.MAX_VALUE;
        }

        /* then add the current text to start next line */
        line.add(tp);
        minY = Math.min(tp.getYDirAdj(), minY);
        maxY = Math.max(minY + tp.getHeightDir(), maxY);
        maxX = tp.getPos().getEndX();
    }

    if (!line.isEmpty()) {
        ret.addAll(createWordsInLine(line));
        line.clear();
    }

    if (log.isDebugEnabled()) {
        log.debug("LOG00010:word segmentation took " + (System.currentTimeMillis() - t0) + " ms");
    }
    ;
    return ret;
}

// -------------------------- STATIC METHODS --------------------------

/**
 * it happens that some of the TextPositions are unreasonably tall. This destroys my algorithms, so
 * for those we will use a lower value
 *
 * @param tp
 * @return
 */
private static float getAdjustedHeight(final TextPosition tp) {
    final float adjustedHeight;
    //    final float maxHeight = tp.getFontSize() * tp.getYScale() * 1.2f;
    //    if (tp.getHeightDir() > maxHeight) {
    //        adjustedHeight = maxHeight;
    //    } else {
    adjustedHeight = tp.getHeightDir();
    //    }
    return adjustedHeight;
}

private static boolean isOnAnotherLine(final float y, final ETextPosition tp) {
    return !isWithinVariance(y, tp.getYDirAdj(), 3.0f);
}

private static boolean isTooFarAwayHorizontally(final float endX, final ETextPosition tp) {
    return !isWithinVariance(endX, tp.getPos().getX(), tp.getFontSizeInPt());
}

// -------------------------- OTHER METHODS --------------------------

/**
 * This creates
 *
 * @param texts
 * @return
 */
@SuppressWarnings({"ObjectAllocationInLoop"})
List<PhysicalText> splitTextPositionsOnSpace(final List<TextPosition> texts) {
    final List<PhysicalText> ret = new ArrayList<PhysicalText>(texts.size() * 2);

    final FloatPoint lastWordBoundary = new FloatPoint(0.0F, 0.0F);
    final StringBuilder contents = new StringBuilder();

    Collections.sort(texts, new TextPositionComparator());

    float width = 0.0f;
    boolean firstInLine = true;
    for (TextPosition tp : texts) {
        if (log.isDebugEnabled()) {
            log.debug("LOG00000:in:" + StringUtils.getTextPositionString(tp));
        }
        float x = tp.getXDirAdj();
        float y = tp.getYDirAdj();
        final String s = tp.getCharacter();
        final Style style = styles.getStyleForTextPosition(tp);

        /** iterate through all the characters in textposition. Notice that the loop is a bit evil
         * in that it iterates one extra time as apposed to what you would expect, in order make sure
         * that the last piece of the textposition is created into a text.
         */
        for (int i = 0; i <= s.length(); i++) {
            /* if this is either the last character or a space then just output a new text */
            if (i == s.length() || Character.isSpaceChar(s.charAt(i))) {
                if (contents.length() > 0) {
                    /* calculate distance to the former word in the line if there was one */
                    final float distance;
                    if (firstInLine) {
                        distance = Float.MIN_VALUE;
                        firstInLine = false;
                    } else {
                        distance = x - lastWordBoundary.getX();
                    }

                    /* this is a workaround for when pdfbox sometimes miscalculates the coordinates
                        of certain mathematical symbols */
                    final float adjustedY;
                    if (contents.toString().matches("[∑√∏⊗]")) {
                        adjustedY = y;
                    } else {
                        adjustedY = y - tp.getHeightDir();
                    }

                    /* output this word */
                    ret.add(new PhysicalText(contents.toString(), style, x, adjustedY, width,
                                             getAdjustedHeight(tp), distance));

                    /* change X coordinate to include this text */
                    x += width;

                    /* mark where this word ended so we can calculate distance between it and
                        the subsequent one */
                    lastWordBoundary.setPosition(x, y);

                    /* rest the rest of the state for the next word */
                    width = 0.0F;
                    contents.setLength(0);
                }

                if (i != s.length()) {
                    x += tp.getIndividualWidths()[i];
                }
            } else {
                /* include this character in the word we are building */
                width += tp.getIndividualWidths()[i];
                contents.append(s.charAt(i));
            }
        }
    }

    return ret;
}

/**
 * This method will process one line worth of TextPositions , and split and/or combine them as to
 * output words.
 *
 * @param line
 */
private List<PhysicalText> createWordsInLine(final List<TextPosition> line) {
    final Comparator<PhysicalText> sortByLowerX = new Comparator<PhysicalText>() {
        public int compare(final PhysicalText o1, final PhysicalText o2) {
            return Float.compare(o1.getPosition().getX(), o2.getPosition().getX());
        }
    };
    PriorityQueue<PhysicalText> queue = new PriorityQueue<PhysicalText>(line.size(), sortByLowerX);

    /* first convert into text elements */
    queue.addAll(splitTextPositionsOnSpace(line));

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
}
