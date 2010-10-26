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

import java.util.ArrayList;
import java.util.List;

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
class ColumnBoundaryInterval {
    final int y, endy;
    final int[] columnBoundaries;

    ColumnBoundaryInterval(final int[] columnBoundaries, final int endy, final int y) {
        this.columnBoundaries = columnBoundaries;
        this.endy = endy;
        this.y = y;
    }
}

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

    for (int y = 0; y < (int) height; y += interval) {
        /* find boundaries for this Y */
        final List<HasPosition> texts = selectIntersectingWithYIndex(words, y);
        final List<HasPosition> spaces = selectIntersectingWithYIndex(whitespaces, y);
        List<Integer> boundaries = findColumnBoundaries(texts, spaces);

        /* if this set of boundaries does not mach the current one, save that
            interval and start the new one */
        if (!boundaries.equals(currentColumnBoundaries)) {

        }

    }

    //    for (PhysicalText word : words) {
    //        final List<PhysicalText> result = findWordsInDirectionFromWord(Direction.E, word, 10.0f);
    //    }


    ret.addWhitespaces(whitespaces);

    logger.warn("compileLogicalPage:" + (System.currentTimeMillis() - t0));

    return ret;
}

private List<Integer> findColumnBoundaries(final List<HasPosition> texts,
                                           final List<HasPosition> spaces)
{
    List<Integer> boundaries = new ArrayList<Integer>();

    if (spaces.isEmpty() || texts.isEmpty()) {
        boundaries.add(0);
    } else {
        for (int i = 0, textsSize = texts.size(); i < textsSize; i++) {
            final HasPosition pos = texts.get(i);

            boundaries.add(spaces.size());
        }
    }
    return boundaries;
}

// -------------------------- OTHER METHODS --------------------------

private List<HasPosition> selectIntersectingWithYIndex(final List<? extends HasPosition> positions,
                                                       float y)
{
    final Rectangle searchRectangle = new Rectangle(0.0F, y, width, y + 1);
    final List<HasPosition> result = new ArrayList<HasPosition>(20);

    for (HasPosition position : positions) {
        if (searchRectangle.intersectsWith(position.getPosition())) {
            result.add(position);
        }
    }

    return result;
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
}
