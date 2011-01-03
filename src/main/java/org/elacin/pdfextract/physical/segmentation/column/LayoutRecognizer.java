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

package org.elacin.pdfextract.physical.segmentation.column;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.RectangleCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elacin.pdfextract.util.Loggers.getInterfaceLog;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 12:54:21 PM To change this
 * template use File | Settings | File Templates.
 */
public class LayoutRecognizer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(LayoutRecognizer.class);

// -------------------------- STATIC METHODS --------------------------

@Nullable
private static PhysicalText getNextText(@NotNull final List<PhysicalContent> contents,
                                        final int i) {

    for (int j = i; j < contents.size(); j++) {
        final PhysicalContent next = contents.get(j);
        if (next.isText()) {
            return next.getPhysicalText();
        }
    }
    return null;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public Map<Integer, List<Integer>> findColumnsForPage(@NotNull final RectangleCollection region,
                                                      final PageNode ret) {

    final long t0 = System.currentTimeMillis();

    int height = (int) region.getPos().getHeight();

    /**
     * Establish column boundaries for every y-index
     * */

    Map<Integer, List<Integer>> columns = new HashMap<Integer, List<Integer>>(height);
    for (int y = 0; y < height; y++) {
        /* find boundaries for this y */
        final List<PhysicalContent> texts = region.findContentAtYIndex(y);
        List<Integer> boundaries = findColumnBoundaries(region, texts);

        /* and save it for the next pass */
        columns.put(y, boundaries);
    }


    /* then 'smooth' the columns */
    List<ColumnBoundaryInterval> columnLayout = new ArrayList<ColumnBoundaryInterval>();
    for (int y = 0; y < height; y++) {
        final List<Integer> boundaries = columns.get(y);
        for (int columnEnd : boundaries) {
            if (0 == columnEnd) {
                continue;
            }
            /** here we have a column ending for line y. now check if:
             *      - y + 1 exists
             *      - y + 1 has a column ending left of the current
             *      - that column ending can be moved rightwards to match this, /without/
             *          including more text
             * */
            //            while (true){
            //
            //            }
        }
    }

    if (getInterfaceLog().isDebugEnabled()) {
        getInterfaceLog().debug("LOG00190:compileLogicalPage:" + (System.currentTimeMillis() -
        t0));
    }

    return columns;
}

@NotNull
public List<WhitespaceRectangle> findWhitespace(@NotNull final PhysicalPageRegion region) {
    final long t0 = System.currentTimeMillis();

    final int numWhitespaces = Math.max(10, Math.min(40, region.getContents().size() / 10));
    AbstractWhitespaceFinder vert = new WhitespaceFinder(region, numWhitespaces,
            region.getAvgFontSizeX()
                    * 0.4f, region.getAvgFontSizeY());
    final List<WhitespaceRectangle> ret = vert.findWhitespace();


    final long time = System.currentTimeMillis() - t0;
    log.warn(String.format("LOG00380:%d of %d whitespaces for %s in %d ms", ret.size(),
    numWhitespaces, region, time));
    return ret;
}

// -------------------------- OTHER METHODS --------------------------

/**
 * Returns a list of integer x indices where a string of character ends, and a whitespace starts.
 * these indices will be considered start of columns
 */
@NotNull
private List<Integer> findColumnBoundaries(@NotNull final RectangleCollection region,
                                           @NotNull final List<PhysicalContent> line) {

    List<Integer> boundaries = new ArrayList<Integer>();

    boundaries.add(1);
    if (!line.isEmpty()) {
        PhysicalText lastText = null;

        boolean startOfColumnMarked = true;
        for (int i = 0; i < line.size(); i++) {
            while (i + 1 < line.size() && line.get(i + 1).isWhitespace()) {
                i++;
            }
            final PhysicalContent content = line.get(i);

            if (content.isWhitespace()) {
                if (isNewBoundary(line, lastText, i)) {
                    int boundary = (int) content.getPos().getEndX();
                    boundary = Math.min(boundary, (int) region.getWidth() - 1);
                    boundaries.add(boundary);
                    startOfColumnMarked = false;
                }
                lastText = null;
            } else {
                lastText = (PhysicalText) content;
                if (!startOfColumnMarked) {
                    startOfColumnMarked = true;
                    int boundary = (int) content.getPos().getX();

                    if (i != 0) {
                        final PhysicalContent preceeding = line.get(i - 1);
                        if (preceeding.isWhitespace()) {
                            boundary = (int) preceeding.getPos().getEndX();
                        }
                    }
                    boundary = Math.min(boundary, (int) region.getWidth() - 1);
                    boundaries.add(boundary);
                }
            }
        }
    } else {
        boundaries.add((int) region.getWidth() - 1);
    }
    return boundaries;
}

private boolean isNewBoundary(@NotNull final List<PhysicalContent> contents,
                              @Nullable final PhysicalText lastText,
                              final int i) {
    /* first check that there has in fact been preceeding text */
    if (lastText == null) {
        return false;
    }

    /* if the preceeding word was the last on the line, end this column */
    final PhysicalText nextText = getNextText(contents, i);
    if (nextText == null) {
        return true;
    }

    if (contents.size() - 1 == i) {
        return true;
    }

    /** there will at times be thin whitespace rectangles which crosses in between two words
    which
     *  logically belongs together. Compare the distance between the two words to their average
     *  character width to filter out those cases
     */
    float min = 2.0f
            * Math.min(lastText.getAverageCharacterWidth(), nextText.getAverageCharacterWidth());
    float distance = nextText.getPos().getX() - lastText.getPos().getEndX();
    if (distance < min) {
        return false;
    }

    return true;
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
