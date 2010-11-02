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
import org.elacin.pdfextract.segmentation.column.LayoutRecognizer;
import org.elacin.pdfextract.segmentation.column.WhitespaceRectangle;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.*;

import static org.elacin.pdfextract.Loggers.getInterfaceLog;

public class PhysicalPage extends RectangleCollection<PhysicalText> {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);
private final int pageNumber;

/* minimum font sizes for the page */
private final float minFontSizeX, minFontSizeY;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<PhysicalText> words, final float h, final float w, int pageNumber) {
    super(new Rectangle(0, 0, w, h), words);
    this.pageNumber = pageNumber;

    /* find minimum font sizes, and set word indices */
    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
    for (int i = 0, wordsSize = words.size(); i < wordsSize; i++) {
        final PhysicalText word = words.get(i);
        word.index = i;
        minX = Math.min(minX, word.getStyle().xSize);
        minY = Math.min(minY, word.getStyle().ySize);
    }
    minFontSizeX = minX;
    minFontSizeY = minY;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public float getMinFontSizeX() {
    return minFontSizeX;
}

public float getMinFontSizeY() {
    return minFontSizeY;
}

public int getPageNumber() {
    return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

public PageNode compileLogicalPage() {
    long t0 = System.currentTimeMillis();


    final List<WhitespaceRectangle> whitespaces = LayoutRecognizer.findColumnsForPage(this);

    /* establish column boundaries for every y-index */
    List<ColumnBoundaryInterval> columnLayout = new ArrayList<ColumnBoundaryInterval>();
    final int interval = 10;
    List<Integer> currentColumnBoundaries = new ArrayList<Integer>();
    int currentBoundariesStartedAt;
    boolean continuingInterval = false;

    int height = (int) getPosition().getHeight();

    Map<Integer, List<Integer>> columns = new HashMap<Integer, List<Integer>>(height);

    for (int y = 0; y < (int) height; y++) {
        /* find boundaries for this Y */
        final List<HasPosition> texts = selectIntersectingWithYIndex(getWords(), y);
        final List<HasPosition> spaces = selectIntersectingWithYIndex(whitespaces, y);
        List<Integer> boundaries = findColumnBoundaries(texts, spaces);

        columns.put(y, boundaries);

        /** if this set of boundaries does not mach the current one, save that interval and
         start the new one */
        if (!boundaries.isEmpty() || !boundaries.equals(currentColumnBoundaries)) {
        }
    }


    PageNode ret = new PageNode(pageNumber);
    ret.addWhitespaces(whitespaces);
    ret.addColumns(columns);

    getInterfaceLog().debug("LOG00190:compileLogicalPage:" + (System.currentTimeMillis() - t0));

    return ret;
}

public Rectangle getPageDimensions() {
    return getPosition();
}

public List<PhysicalText> getWords() {
    return getRectangles();
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

    /* check that there is more text on the same line */
    final PhysicalText nextText = getNextText(contents, i);
    if (nextText == null) {
        return true;
    }

    /** there will at times be thin whitespace rectangles which crosses in between two words which
     *  logically belongs together. Compare the distance between the two words to their average
     *  character width to filter out those cases
     */
    float min = 2.0f * Math.min(lastText.getAverageCharacterWidth(),
                                nextText.getAverageCharacterWidth());
    float distance = nextText.getPosition().getX() - lastText.getPosition().getEndX();
    if (distance < min) {
        return false;
    }

    return true;
}

private List<HasPosition> selectIntersectingWithYIndex(final List<? extends HasPosition> positions,
                                                       float y)
{
    final Rectangle searchRectangle = new Rectangle(0.0F, y, getWidth(), 1);
    final List<HasPosition> result = new ArrayList<HasPosition>(20);

    for (HasPosition position : positions) {
        if (searchRectangle.intersectsWith(position.getPosition())) {
            result.add(position);
        }
    }

    return result;
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
