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
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.elacin.pdfextract.util.MathUtils.isWithinVariance;
import static org.elacin.pdfextract.util.Sorting.sortByLowerX;
import static org.elacin.pdfextract.util.Sorting.sortTextByBaseLine;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 12, 2010 Time: 3:34:09 AM <p/>
 * <p/>
 * <p/>
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * PhysicalTexts, as used by this application. The difference between the two classes is
 * technical,
 * but also semantic, in that they are defined to be at most a whole word (the normal case: Word
 * fragments will only occur when a word is split over two lines, or if the word is formatted
 * with
 * two different styles) instead of arbitrary length.
 * <p/>
 * This makes it easier to reason about the information we have, and also reconstructs some
 * notion of word and character spacing, which will be an important property for feature
 * recognition.
 */
public class WordSegmentatorImpl implements WordSegmentator {
// ------------------------------ FIELDS ------------------------------

public static boolean USE_EXISTING_WHITESPACE = true;

private static final Logger log = Logger.getLogger(WordSegmentatorImpl.class);

private final DocumentStyles styles;

// --------------------------- CONSTRUCTORS ---------------------------

public WordSegmentatorImpl(final DocumentStyles styles) {
    this.styles = styles;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface WordSegmentator ---------------------

/**
 * This method will convert the text into PhysicalTexts. <p/> To do this, the text is split on
 * whitespaces, character and word distances are approximated, and words are created based on
 * those
 */
@NotNull
public List<PhysicalText> segmentWords(@NotNull final List<ETextPosition> text) {
    final long t0 = System.currentTimeMillis();

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

    for (final ETextPosition tp : text) {

        /* if this is the first text in a line */
        if (line.isEmpty()) {
            baseline = tp.getBaseLine();
            maxX = tp.getPos().getEndX();
            currentFont = tp.getFont();
        }

        final boolean stopGrouping = isOnAnotherLine(baseline, tp, maxY)
                || isTooFarAwayHorizontally(maxX, tp)
                || fontDiffers(currentFont, tp);

        if (stopGrouping) {
            if (!line.isEmpty()) {
                ret.addAll(createWordsInLine(convertText(line)));
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
        ret.addAll(createWordsInLine(convertText(line)));
        line.clear();
    }

    if (log.isDebugEnabled()) {
        log.debug("LOG00010:word segmentation took " + (System.currentTimeMillis() - t0) + " ms");
    }
    return ret;
}

// -------------------------- STATIC METHODS --------------------------

private static boolean fontDiffers(final PDFont font, final ETextPosition tp) {
    return !font.equals(tp.getFont());
}

private static boolean isOnAnotherLine(final float baseline,
                                       final ETextPosition tp,
                                       final float maxY) {
    return (baseline != tp.getBaseLine() && tp.getBaseLine() > maxY);
}

private static boolean isTooFarAwayHorizontally(final float endX, @NotNull final ETextPosition tp) {
    final float variation = tp.getPos().getWidth();

    return !isWithinVariance(endX, tp.getPos().getX(), variation);
}

// -------------------------- OTHER METHODS --------------------------

// latex keep at 154

/**
 * The above methods are generally responsible for grouping text according to line and style;
 * this is the one which will actually do the segmentation.
 * <p/>
 * There are two cases to consider for this process:
 * - whitespace already existing: combine characters into words in the obvious way
 * - whitespace must be found:
 * <p/>
 * First approximate the applied intra-word character spacing.
 * <p/>
 * Then, iterate through the characters in the line from left to right:
 * calculate the real distance between a pair of characters
 * normalize that by subtracting the charspacing
 * if that normalized spacing is bigger than fontSize / 15, consider the space a
 * word boundary
 *
 * @param line
 * @return
 */
@NotNull
static Collection<PhysicalText> createWordsInLine(@NotNull final List<PhysicalText> line) {

    /* keep the characters sorted at all times. note that unfinished words are put back into
    *   this queue, and will this be picked as currentWord below
    * */
    final Queue<PhysicalText> queue = new PriorityQueue<PhysicalText>(line.size(), sortByLowerX);
    queue.addAll(line);

    /* this list of words will be returned */
    final Collection<PhysicalText> segmentedWords = new ArrayList<PhysicalText>();

    /* if we already have whitespace information */
    final boolean containsSpaces = USE_EXISTING_WHITESPACE && containsWhiteSpace(line);

    /* an approximate average charspacing distance */
    final float charSpacing = approximateCharSpacing(line);

    /* all font sizes are the same. if it is missing just guess 10 */
    final float fontSize;
    if (line.get(0).getStyle().xSize != 0) {
        fontSize = (float) line.get(0).getStyle().xSize;
    } else {
        fontSize = 10.0f;
    }

    /* this is necessary to keep track of the width of the last character we
     *  combined into a a word, else it would disappear when combining
     * */
    float currentWidth = queue.peek().getPos().getWidth();

    if (log.isDebugEnabled()) {
        printLine(line);
    }

    /**
     * iterate through all texts from left to right, and combine into words as we go
     * */
    while (!queue.isEmpty()) {
        final PhysicalText currentWord = queue.remove();
        final PhysicalText nextChar = queue.peek();

        /* we have no need for these spaces after establishing word boundaries, so skip */
        if (" ".equals(currentWord.getText())) {
            continue;
        }
        /* if it is the last in line */
        if (nextChar == null) {
            segmentedWords.add(currentWord);
            break;
        }

        /**
         * determine if we found a word boundary or not
         * */
        final boolean isWordBoundary;
        if (containsSpaces) {
            isWordBoundary = " ".equals(nextChar.getText());
        } else {

            final float distance = currentWord.getPos().distance(nextChar.getPos());
            isWordBoundary = distance - charSpacing > (fontSize / 8.0f) + charSpacing * 0.5;

            if (log.isDebugEnabled()) {
                log.debug(currentWord.getText() + "[" + currentWidth + "] " + distance + " "
                        + nextChar.getText() + "[" + nextChar.getPos().getWidth()
                        + "]: isWordBoundary=" + isWordBoundary + ", effictive distance:"
                        + (distance - charSpacing) + ", fontSize:" + (fontSize) + ", charSpacing:"
                        + charSpacing);
            }

        }

        /**
         * combine characters if necessary
         * */
        if (isWordBoundary) {
            /* save this word and continue with next */
            segmentedWords.add(currentWord);
        } else {
            /* combine the two fragments */
            PhysicalText combinedWord = currentWord.combineWith(nextChar);
            queue.remove(nextChar);
            queue.add(combinedWord);
        }

        currentWidth = nextChar.getPos().getWidth();
    }

    for (PhysicalText text : segmentedWords) {
        if (log.isDebugEnabled()) {
            log.debug("LOG00540: created " + text);
        }
    }

    return segmentedWords;
}

//latex, keep at 275

private static void printLine(List<PhysicalText> physicalTexts) {
    StringBuffer sb = new StringBuffer();
    for (PhysicalText physicalText : physicalTexts) {
        sb.append(physicalText.getText());
    }
    if (log.isDebugEnabled()) {
        log.debug("line:" + sb);
    }
}

@NotNull
@SuppressWarnings({"ObjectAllocationInLoop"})
List<PhysicalText> convertText(@NotNull final List<ETextPosition> texts) {
    final List<PhysicalText> ret = new ArrayList<PhysicalText>(texts.size() * 2);

    Collections.sort(texts, sortByLowerX);

    for (final ETextPosition text : texts) {
        ret.add(new PhysicalText(text.getCharacter(), styles.getStyleForTextPosition(text),
                text.getPos().getX(),
                text.getPos().getY(),
                text.getPos().getWidth(),
                text.getPos().getHeight(),
                (int) text.getDir()));
    }

    return ret;
}

private static boolean containsWhiteSpace(List<PhysicalText> line) {
    for (PhysicalText physicalText : line) {
        if (" ".equals(physicalText.getText())) {
            return true;
        }
    }
    return false;
}

//latex, keep this line at 315

/**
 * Tries to find an estimate of the character spacing applied to the
 * given line of characters.
 * <p/>
 * The idea is that font kerning and other local adjustments will
 * contribute relatively little to the observed distance between
 * characters, whereas the more general applied character spacing
 * will make up by far the biggest amount of the space.
 * <p/>
 * These local adjustments will contribute in both directions, so
 * in many cases we will be able to get a somewhat good approximation
 * if we average out a semi-random number of the smallest distances.
 * <p/>
 * To put some numbers to this, say we have character distances
 * varying from 3.5 to 9pt. If we iterate through the first n distances,
 * with distances ranging from 3.5 to 4.5pt (the rest being skipped
 * for being too big), the approximation of the character spacing
 * would thus end up around 4.
 * *
 *
 * @param line list of characters in the line. this must be sorted
 * @return an approximate character spacing
 */
static float approximateCharSpacing(@NotNull List<PhysicalText> line) {
    /** the real lower bound where this algorithm applies might be higher, but
     *  at least for 0 or 1 distances it would be non-functional*/
    if (line.size() <= 1) {
        return 0.0f;
    }

    final float[] distances = calculateDistancesBetweenCharacters(line);
    Arrays.sort(distances);

    /**
     * This value deserves a special notice. When it was written semi-random above,
     *  this is essentially what was meant. We start out with the smallest distance,
     *  and will keep iterating until the numbers start to be bigger. The underlying
     *  assumption here is that word spacing will never be only 1.5 times the smallest
     *  space occurring between two characters.
     *
     * The 0.6 is a quite random number, it's purpose is to avoid breaking the
     *  algorithm for negative character distances (which are common and useful),
     *  and its only properties are that it is too small to ever separate a word, and
     *  that it is a positive number :)
     */
    final float maxBoundary = Math.max(0.6f, distances[0] * 2f);

    int counted = 0;
    float sum = 0.0f;

    for (float sortedDistance : distances) {
        if (sortedDistance > maxBoundary) {
            break;
        }
        sum += sortedDistance;
        counted++;
    }

    return sum / (float) counted;
}
// latex, keep this line at 377

/**
 * Calculates a list of distances between the given list of characters in the obvious way.
 *
 * @param line list of characters. this should be sorted!
 * @return
 */
@NotNull
private static float[] calculateDistancesBetweenCharacters(@NotNull List<PhysicalText> line) {
    if (line.size() <= 1) {
        return new float[0];
    }

    final float[] distances = new float[line.size() - 1];

    for (int i = 0; i < line.size() - 1; i++) {
        final Rectangle leftChar = line.get(i).getPos();
        final Rectangle rightChar = line.get(i + 1).getPos();
        distances[i] = leftChar.distance(rightChar);
    }

    return distances;
}
}
