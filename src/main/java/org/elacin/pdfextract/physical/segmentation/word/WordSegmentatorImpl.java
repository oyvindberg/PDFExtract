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

package org.elacin.pdfextract.physical.segmentation.word;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.physical.segmentation.WordSegmentator;
import org.elacin.pdfextract.style.DocumentStyles;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static org.elacin.pdfextract.util.MathUtils.isWithinVariance;
import static org.elacin.pdfextract.util.Sorting.sortByLowerX;
import static org.elacin.pdfextract.util.Sorting.sortTextByBaseLine;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 12, 2010 Time: 3:34:09 AM <p/>
 * <p/>
 * <p/>
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * PhyscalTexts, as used by this application. The difference between the two classes is
 * technical,
 * but also semantic, in that they are defined to be at most a whole word (the normal case: Word
 * fragments will only occur when a word is split over two lines, or if the word is formatted
 * with
 * two different styles) instead of arbitrary length.
 * <p/>
 * This makes it easier to reason about the information we have, and also reconstructs some
 * notion
 * of word and character spacing, which will be an important property for feature recognition.
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
 * This method will convert the text into WordNodes. <p/> To do this, the text is split on
 * whitespaces, character and word distances are approximated, and words are created based on
 * those
 */
@NotNull
public List<PhysicalText> segmentWords(@NotNull final List<ETextPosition> text) {
    long t0 = System.currentTimeMillis();

    List<PhysicalText> ret = new ArrayList<PhysicalText>(text.size());

    /** iterate through all incoming TextPositions, and process them
     in a line by line fashion. We do this to be able to calculate
     char and word distances for each line
     */
    List<ETextPosition> line = new ArrayList<ETextPosition>();

    Collections.sort(text, sortTextByBaseLine);

    float baseline = 0.0f;
    float maxY = Float.MIN_VALUE;
    float maxX = 0.0f;
    PDFont currentFont = null;
    for (int i = 0; i < text.size(); i++) {
        final ETextPosition tp = text.get(i);
        /* if this is the first text in a line */
        if (line.isEmpty()) {
            baseline = tp.getBaseLine();
            maxX = tp.getPos().getEndX();
            currentFont = tp.getFont();
        }

        if (isOnAnotherLine(baseline, tp, maxY) || isTooFarAwayHorizontally(maxX,
                tp) || fontDiffers(currentFont, tp)) {
            if (!line.isEmpty()) {
                ret.addAll(createWordsInLine(line));
                line.clear();
            }
            baseline = tp.getBaseLine();
            maxY = tp.getPos().getEndY();
            currentFont = tp.getFont();
        }

        /* then add the current text to start next line */
        line.add(tp);
        maxY = Math.max(maxY, tp.getPos().getEndX());
        maxX = tp.getPos().getEndX();
    }

    if (!line.isEmpty()) {
        ret.addAll(createWordsInLine(line));
        line.clear();
    }

    if (log.isDebugEnabled()) {
        log.debug("LOG00010:word segmentation took " + (System.currentTimeMillis() - t0) + " ms")
        ;
    }
    return ret;
}

private static boolean fontDiffers(final PDFont font, final ETextPosition tp) {
    return !font.equals(tp.getFont());
}

// -------------------------- STATIC METHODS --------------------------

private static boolean isOnAnotherLine(final float baseline,
                                       final ETextPosition tp,
                                       final float maxY) {
    return (baseline != tp.getBaseLine() && tp.getBaseLine() > maxY);
}

private static boolean isTooFarAwayHorizontally(final float endX, @NotNull final ETextPosition
        tp) {
    return !isWithinVariance(endX, tp.getPos().getX(), tp.getFontSizeInPt());
}

// -------------------------- OTHER METHODS --------------------------

@NotNull
@SuppressWarnings({"ObjectAllocationInLoop"})
List<PhysicalText> convertText(@NotNull final List<ETextPosition> texts) {
    final List<PhysicalText> ret = new ArrayList<PhysicalText>(texts.size() * 2);

    Collections.sort(texts, sortByLowerX);

    for (int i = 0, size = texts.size(); i < size; i++) {
        final ETextPosition text = texts.get(i);

        final float distance;
        if (i == 0) {
            distance = Float.MIN_VALUE;
        } else {
            final ETextPosition former = texts.get(i - 1);
            distance = text.getPos().getX() - former.getPos().getEndX();
        }

        ret.add(new PhysicalText(text.getCharacter(), styles.getStyleForTextPosition(text),
                text.getPos().getX(), text.getPos().getY(),
                text.getPos().getWidth(), text.getPos().getHeight(),
                distance, (int) text.getDir(), text.getSequenceNum()));

    }

    return ret;
}

/**
 * This method will process one line worth of TextPositions , and split and/or combine them as to
 * output words.
 */
@NotNull
private List<PhysicalText> createWordsInLine(@NotNull final List<ETextPosition> line) {
    PriorityQueue<PhysicalText> queue = new PriorityQueue<PhysicalText>(line.size(),
            sortByLowerX);

    /* first convert into text elements */
    queue.addAll(convertText(line));

    /* then calculate spacing */
    CharSpacingFinder.setCharSpacingForTexts(queue);

    /* finally iterate through all texts from left to right, and combine into words as we go */
    List<PhysicalText> ret = new ArrayList<PhysicalText>();
    while (!queue.isEmpty()) {
        final PhysicalText current = queue.remove();
        final PhysicalText next = queue.peek();

        if (next == null) {
            ret.add(current);
            continue;
        }
        if (!current.canBeCombinedWith(next)) {
            ret.add(current);
            continue;
        }
        if (current.isSameStyleAs(next) || current.getPos().intersectsWith(next.getPos())) {
            PhysicalText combinedWord = current.combineWith(next);
            queue.remove(next);
            queue.add(combinedWord);
            continue;
        }
        ret.add(current);
    }
    for (PhysicalText text : ret) {
        if (log.isDebugEnabled()) {
            log.debug("LOG00540: created " + text);
        }
    }

    return ret;
}
}
