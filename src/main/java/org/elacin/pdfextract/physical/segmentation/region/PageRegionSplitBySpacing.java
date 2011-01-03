/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.physical.segmentation.region;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.content.HasPosition;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 08.12.10
 * Time: 01.27
 * To change this template use File | Settings | File Templates.
 */
public class PageRegionSplitBySpacing {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PageRegionSplitBySpacing.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
public static void splitHorizontallyBySpacing(@NotNull PhysicalPageRegion r) {

    /* If the size of this region is this small, dont bother splitting it further */
    if (r.getPos().getHeight() < r.getAvgFontSizeY() * 2) {
        return;
    }
    if (r.getPos().getWidth() < r.getAvgFontSizeX() * 2) {
        return;
    }


    /* start of by finding the median of a sample of vertical distances */
    int median = r.getMedianOfVerticalDistances();

    /* if none was found, return an empty list */
    if (median == -1) {
        return;
    }
    /* increase it a bit */
    median++;

    final int minimumDistanceToSplit = (int) (Math.max(6.0f, r.getAvgFontSizeY()) * 2);

    float lastBoundary = -1000.0f;
    float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
    Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();

    for (float y = r.getPos().getY(); y <= r.getPos().getEndY(); y++) {
        final List<PhysicalContent> row = r.findContentAtYIndex(y);

        workingSet.addAll(row);
        for (PhysicalContent content : row) {
            minX = Math.min(content.getPos().getX(), minX);
            maxX = Math.max(content.getPos().getEndX(), maxX);
        }


        if (row.isEmpty()) {

            if (!TextUtils.listContainsStyledText(workingSet)) {
                continue;
            }


            if ((y - lastBoundary < minimumDistanceToSplit)) {
                continue;
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("LOG00530:split/hor at y=%s for %s ", y, r));
            }

            r.extractSubRegionFromContent(workingSet);

            workingSet.clear();
            lastBoundary = y;
        } else {
            lastBoundary = y;
        }
    }

}

@NotNull
public static void splitInVerticalColumns(@NotNull PhysicalPageRegion r) {
    /**
     * Dont bother splitting columns narrower than this
     */
    if (r.getPos().getWidth() < r.getAvgFontSizeX() * 3) {
        return;
    }

    /* state for the algorithm */
    Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();
    float lastBoundary = -1000.0f;
    float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;


    /**
     * check for every x index if is a column boundary
     */
    for (float x = r.getPos().getX(); x <= r.getPos().getEndX(); x++) {
        /* start by finding the content of this column, that is everything which
              intersects, and make a decision based on that
           */

        final List<PhysicalContent> column = r.findContentAtXIndex(x);

//        for (PhysicalContent physicalContent : column) {
//            if (physicalContent.getPos().getX() > x + 1) {
//                throw new RuntimeException();
//            }
//        }

        workingSet.addAll(column);

        /* keep track of current vertical bounds */
        for (PhysicalContent newContent : column) {
            minY = Math.min(newContent.getPos().getY(), minY);
            maxY = Math.max(newContent.getPos().getEndY(), maxY);
        }

        /**
         * Check if this column could be a boundary... there are a whole group of checks here :)
         */
        if (!TextUtils.listContainsStyledText(column)) {
            if (!TextUtils.listContainsStyledText(workingSet)) {
                continue;
            }

            if (columnContainsBlockingGraphics(column, x, minY, maxY)) {
                continue;
            }

            HasPosition search = new Rectangle(x, minY, r.getPos().getEndX(), maxY);
            final List<PhysicalContent> contentRight = r.findContentsIntersectingWith(search);
            final int numberOfTextsOnRight = contentRight.size();

            if (numberOfTextsOnRight < 10) {
                continue;
            }

            if (r.columnBoundaryWouldBeTooNarrow(lastBoundary, x)) {
                continue;
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("LOG00510:split/vert at x =%s for %s ", x, r));
            }

            r.extractSubRegionFromContent(workingSet);

            /* reset vertical values */
            minY = Float.MAX_VALUE;
            maxY = Float.MIN_VALUE;


            workingSet.clear();
            lastBoundary = x;
        } else {
            lastBoundary = x;
        }
    }
}

// -------------------------- STATIC METHODS --------------------------

private static boolean columnContainsBlockingGraphics(@NotNull final List<PhysicalContent>
column,
                                                      final float x,
                                                      final float minY,
                                                      final float maxY) {
    HasPosition search = new Rectangle(x, minY, 1.0f, maxY - minY);
    for (PhysicalContent obstacle : column) {
        if (obstacle.isGraphic()) {
            if (obstacle.getPos().intersectsWith(search)) {
                return true;
            }
        }
    }
    return false;
}

}
