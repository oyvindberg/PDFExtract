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

package org.elacin.pdfextract.segmentation;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.HasPosition;
import org.elacin.pdfextract.segmentation.column.ColumnFinder;
import org.elacin.pdfextract.segmentation.word.PhysicalText;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.Rectangle;

import java.util.*;

public class PhysicalPage {
// ------------------------------ FIELDS ------------------------------

private static final Logger logger = Logger.getLogger(PhysicalPage.class);
private final List<PhysicalText> words;
private final float height;
private final float width;
private final int pageNumber;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<PhysicalText> words, final float h, final float w, int pageNumber) {
    height = h;
    width = w;
    this.words = words;
    this.pageNumber = pageNumber;

    for (int i = 0, wordsSize = words.size(); i < wordsSize; i++) {
        words.get(i).index = i;
    }
}

// -------------------------- PUBLIC METHODS --------------------------

public PageNode compileLogicalPage() {
    long t0 = System.currentTimeMillis();

    PageNode ret = new PageNode(pageNumber);
    final List<Rectangle> whitespaces = ColumnFinder.findColumnsFromWordNodes(words, width, height);

    /* establish column boundaries for every y-index */
    List<ColumnBoundaryInterval> columnLayout = new ArrayList<ColumnBoundaryInterval>();
    final int interval = 10;
    List<Integer> currentColumnBoundaries = new ArrayList<Integer>();
    int currentBoundariesStartedAt;
    boolean continuingInterval = false;

    Map<Integer, List<Integer>> columns = new HashMap<Integer, List<Integer>>((int) height);
    for (int y = 0; y < (int) height; y++) {
        /* find boundaries for this Y */
        final List<HasPosition> texts = selectIntersectingWithYIndex(words, y);
        final List<HasPosition> spaces = selectIntersectingWithYIndex(whitespaces, y);
        List<Integer> boundaries = findColumnBoundaries(texts, spaces);

        columns.put(y, boundaries);

        /** if this set of boundaries does not mach the current one, save that interval and
         start the new one */
        //        System.out.println(y + " boundaries = " + boundaries + ": contents = " + texts);

        if (!boundaries.isEmpty() || !boundaries.equals(currentColumnBoundaries)) {
        }
    }

    //    for (PhysicalText word : words) {
    //        final List<PhysicalText> result = findWordsInDirectionFromWord(Direction.E, word, 10.0f);
    //    }


    ret.addWhitespaces(whitespaces);
    ret.addColumns(columns);

    logger.warn("compileLogicalPage:" + (System.currentTimeMillis() - t0));

    return ret;
}

// -------------------------- OTHER METHODS --------------------------

/**
 * Returns a list of integer x indices where a string of character ends, and a whitespace starts.
 * these indices will be considered start of columns
 *
 * @param texts
 * @param spaces
 * @return
 */
private List<Integer> findColumnBoundaries(final Collection<HasPosition> texts,
                                           final Collection<HasPosition> spaces)
{
    List<Integer> boundaries = new ArrayList<Integer>();

    List<HasPosition> lineContents = new ArrayList<HasPosition>();
    lineContents.addAll(texts);
    lineContents.addAll(spaces);

    final Comparator<HasPosition> sortByXIndex = new Comparator<HasPosition>() {
        @Override
        public int compare(final HasPosition o1, final HasPosition o2) {
            return Float.compare(o1.getPosition().getX(), o2.getPosition().getX());
        }
    };
    Collections.sort(lineContents, sortByXIndex);

    if (!spaces.isEmpty() && !texts.isEmpty()) {
        PhysicalText lastText = null;

        for (int i = 0; i < lineContents.size(); i++) {
            final HasPosition content = lineContents.get(i);
            if (content instanceof Rectangle) {
                if (isNewBoundary(lineContents, lastText, i)) {
                    boundaries.add((int) lastText.getPosition().getEndX());
                }
                lastText = null;
            } else {
                lastText = (PhysicalText) content;
            }
        }
    }
    return boundaries;
}

private List<PhysicalText> findWordsInDirectionFromWord(Direction dir,
                                                        PhysicalText text,
                                                        float distance)
{
    final List<PhysicalText> ret = new ArrayList<PhysicalText>();

    final Rectangle pos = text.getPosition();
    final float x = pos.getX() + dir.xDiff * distance;
    final float y = pos.getY() + dir.yDiff * distance;
    final Rectangle search = new Rectangle(x, y, pos.getWidth(), pos.getHeight());

    for (PhysicalText physicalText : words) {
        if (search.intersectsWith(physicalText.getPosition())) {
            ret.add(physicalText);
        }
    }
    ret.remove(text);
    return ret;
}

private PhysicalText getNextText(final List<HasPosition> contents, final int i) {
    for (int j = i; j < contents.size(); j++) {
        final HasPosition next = contents.get(j);
        if (next instanceof PhysicalText) {
            return (PhysicalText) next;
        }
    }
    return null;
}

private boolean isNewBoundary(final List<HasPosition> contents,
                              final PhysicalText lastText,
                              final int i)
{
    /* first check that there has in fact been preceeding text */
    if (lastText == null) {
        return false;
    }

    /* check that there are more text on the same line */
    final PhysicalText nextText = getNextText(contents, i);
    if (nextText == null) {
        return true;
    }

    /** there will at times be thin whitespace rectangles which crosses in between two words which
     *  logically belongs together. Compare the distance between the two words to their average
     *  character width to filter out those cases
     */
    float min = Math.min(lastText.getAverageCharacterWidth(), nextText.getAverageCharacterWidth());
    float distance = nextText.getPosition().getX() - lastText.getPosition().getEndX();
    if (distance < min) {
        return false;
    }

    /** Some times, */

    return true;
}

private List<HasPosition> selectIntersectingWithYIndex(final List<? extends HasPosition> positions,
                                                       float y)
{
    final Rectangle searchRectangle = new Rectangle(0.0F, y, width, 1);
    final List<HasPosition> result = new ArrayList<HasPosition>(20);

    for (HasPosition position : positions) {
        if (searchRectangle.intersectsWith(position.getPosition())) {
            result.add(position);
        }
    }

    return result;
}

// -------------------------- ENUMERATIONS --------------------------

enum Direction {
    N(0, 1),
    NE(1, 1),
    E(1, 0),
    SE(1, -1),
    S(0, -1),
    SW(-1, -1),
    W(-1, 0),
    NW(-1, 1);
    float xDiff;
    float yDiff;

    Direction(final float xDiff, final float yDiff) {
        this.xDiff = xDiff;
        this.yDiff = yDiff;
    }
}

// -------------------------- INNER CLASSES --------------------------

class ColumnBoundaryInterval {
    final int y, endy;
    final int[] columnBoundaries;

    ColumnBoundaryInterval(final int[] columnBoundaries, final int endy, final int y) {
        this.columnBoundaries = columnBoundaries;
        this.endy = endy;
        this.y = y;
    }
}
}
