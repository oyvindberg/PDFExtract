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

package org.elacin.pdfextract.physical.column;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.content.PhysicalContent;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.elacin.pdfextract.content.WhitespaceRectangle;
import org.elacin.pdfextract.geom.MathUtils;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.geom.Sorting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elacin.pdfextract.geom.RectangleCollection.Direction.E;
import static org.elacin.pdfextract.geom.RectangleCollection.Direction.W;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 12:54:21 PM To change this
 * template use File | Settings | File Templates.
 */
public class ColumnFinder {
// ------------------------------ FIELDS ------------------------------

public static final  float  DEFAULT_COLUMN_WIDTH = 2.0f;
private static final Logger log                  = Logger.getLogger(ColumnFinder.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
public static List<WhitespaceRectangle> extractColumnBoundaries(@NotNull PhysicalPageRegion region,
                                                                @NotNull List<WhitespaceRectangle> whitespaces)
{
    final List<WhitespaceRectangle> columnBoundaries = selectCandidateColumnBoundaries(region,
                                                                                       whitespaces);

    /* adjust columns to real height*/
    adjustColumnHeights(region, columnBoundaries);


    combineColumnBoundaries(region, columnBoundaries);

    return columnBoundaries;
}

@NotNull
public static List<WhitespaceRectangle> findWhitespace(@NotNull final PhysicalPageRegion region) {
    final long t0 = System.currentTimeMillis();

    //    final int numWhitespaces = Math.max(30, Math.min(45, region.getContents().size() / 8));
    final int numWhitespaces = 50;

    AbstractWhitespaceFinder finder = new WhitespaceFinder(region, numWhitespaces,
                                                           region.getMinimumColumnSpacing(),
                                                           region.getMinimumRowSpacing());

    final List<WhitespaceRectangle> ret = finder.findWhitespace();

    final long time = System.currentTimeMillis() - t0;
    log.warn(String.format("LOG00380:%d of %d whitespaces for %s in %d ms", ret.size(),
                           numWhitespaces, region, time));

    return ret;
}

// -------------------------- STATIC METHODS --------------------------

private static void adjustColumnHeights(@NotNull PhysicalPageRegion region,
                                        @NotNull List<WhitespaceRectangle> columnBoundaries)
{
    final Rectangle rpos = region.getPos();

    for (int i = 0; i < columnBoundaries.size(); i++) {
        final WhitespaceRectangle boundary = columnBoundaries.get(i);

        /* calculate appropriate narrow bound in the horizontal middle of the boundary*/
        final float boundaryStartX = Math.max(boundary.getPos().getMiddleX() - 1.0f,
                                              boundary.getPos().getX());
        final float boundaryEndX = Math.min(boundary.getPos().getMiddleX() + 1.0f,
                                            boundary.getPos().getEndX());

        /* find surrounding content */
        final List<PhysicalContent> everythingRightOf = findAllContentsRightOf(region, rpos,
                                                                               boundaryStartX);
        Collections.sort(everythingRightOf, Sorting.sortByLowerYThenLowerX);
        final List<PhysicalContent> closeOnLeft = findAllContentsImmediatelyLeftOf(region, rpos,
                                                                                   boundaryStartX);
        Collections.sort(closeOnLeft, Sorting.sortByLowerYThenLowerX);

        float startY = rpos.getY();
        float lastYWithContentRight = rpos.getEndY();

        boolean startYFound = false;
        boolean boundaryStarted = false;
        for (int y = (int) rpos.getY(); y <= (int) (rpos.getEndY() + 1); y++) {
            boolean blocked = false;
            boolean foundContentRightOfX = false;

            for (int j = 0; j < everythingRightOf.size(); j++) {
                PhysicalContent possibleBlocker = everythingRightOf.get(j);

                if (possibleBlocker instanceof WhitespaceRectangle) {
                    continue;
                }

                if (possibleBlocker.getPos().getY() >= y
                            || possibleBlocker.getPos().getEndY() <= y) {
                    continue;
                }

                if (!foundContentRightOfX && possibleBlocker.getPos().getX() > boundaryEndX - 1) {
                    foundContentRightOfX = true;
                }

                /** if we find something blocking this row, start looking further down*/

                /* content will be blocking if it intersects, naturally */
                if (possibleBlocker.getPos().getX() < boundaryStartX
                            && !(possibleBlocker instanceof WhitespaceRectangle)) {
                    blocked = true;
                    break;
                }

                /* also check if this column boundary would separate two words which otherwise are very close*/
                final float possibleBlockerMiddleY = possibleBlocker.getPos().getMiddleY();
                for (PhysicalContent left : closeOnLeft) {
                    if (left instanceof WhitespaceRectangle) {
                        continue;
                    }
                    if (possibleBlockerMiddleY < left.getPos().getY()
                                || possibleBlockerMiddleY > left.getPos().getEndY()) {
                        continue;
                    }

                    if (left.getPos().distance(possibleBlocker) < 6) {
                        blocked = true;
                        break;
                    }
                }
            }

            if (blocked) {
                if (boundaryStarted) {
                    break;
                }
                startYFound = false;
            } else {
                if (!startYFound && foundContentRightOfX) {
                    startYFound = true;
                    startY = (float) (y - 1);
                }
                if (!boundaryStarted && y > boundary.getPos().getY()) {
                    boundaryStarted = true;
                }
                if (foundContentRightOfX) {
                    lastYWithContentRight = y;
                }
            }
        }

        final Rectangle adjusted = new Rectangle(boundaryStartX, startY, 1.0f,
                                                 lastYWithContentRight - startY);
        final WhitespaceRectangle newBoundary = new WhitespaceRectangle(adjusted);
        newBoundary.setScore(1000);
        columnBoundaries.set(i, newBoundary);
    }
}

private static void combineColumnBoundaries(@NotNull PhysicalPageRegion region,
                                            @NotNull List<WhitespaceRectangle> columnBoundaries)
{
    for (int i = 0; i < columnBoundaries.size() - 1; i++) {
        WhitespaceRectangle left = columnBoundaries.get(i);
        WhitespaceRectangle right = columnBoundaries.get(i + 1);

        final Rectangle rpos = right.getPos();
        final Rectangle lpos = left.getPos();

        if (Math.abs(rpos.getX() - lpos.getX()) < 50) {
            /* combine the two. try first to pick a column index at the right hand side*/
            final float startY = Math.min(rpos.getY(), lpos.getY());
            final float endY = Math.max(rpos.getEndY(), lpos.getEndY());
            float endX = Math.max(rpos.getEndX(), lpos.getEndX());
            float startX = endX - DEFAULT_COLUMN_WIDTH;

            Rectangle newPos = new Rectangle(startX, startY, DEFAULT_COLUMN_WIDTH, endY - startY);

            final List<PhysicalContent> intersectingR = region.findContentsIntersectingWith(newPos);

            /* if the first try intersected with something - try left*/
            if (!intersectingR.isEmpty()) {
                startX = Math.max(rpos.getX(), lpos.getX());
                newPos = new Rectangle(startX, startY, DEFAULT_COLUMN_WIDTH, endY - startY);
            }

            final List<PhysicalContent> intersectingL = region.findContentsIntersectingWith(newPos);
            if (!intersectingL.isEmpty()) {
                continue;
            }

            log.warn("LOG01300:Combining column boundaries " + rpos + " and " + lpos);

            WhitespaceRectangle newBoundary = new WhitespaceRectangle(newPos);
            newBoundary.setScore(1000);
            columnBoundaries.set(i, newBoundary);
            columnBoundaries.remove(i + 1);
            i--;
            Collections.sort(columnBoundaries, Sorting.sortByLowerX);
        }
    }
}

@NotNull
private static List<PhysicalContent> findAllContentsImmediatelyLeftOf(@NotNull PhysicalPageRegion region,
                                                                      @NotNull Rectangle rpos,
                                                                      float x)
{
    final float lookLeft = 10.0f;
    final Rectangle search = new Rectangle(x - lookLeft, rpos.getY(), lookLeft, rpos.getHeight());
    return region.findContentsIntersectingWith(search);
}

@NotNull
private static List<PhysicalContent> findAllContentsRightOf(@NotNull PhysicalPageRegion region,
                                                            @NotNull Rectangle rpos,
                                                            float x)
{
    final Rectangle search = new Rectangle(x, rpos.getY(), rpos.getWidth(), rpos.getHeight());
    return region.findContentsIntersectingWith(search);
}

@NotNull
private static List<WhitespaceRectangle> selectCandidateColumnBoundaries(@NotNull PhysicalPageRegion region,
                                                                         @NotNull List<WhitespaceRectangle> whitespaces)
{
    final List<WhitespaceRectangle> columnBoundaries = new ArrayList<WhitespaceRectangle>();
    for (WhitespaceRectangle whitespace : whitespaces) {
        final Rectangle pos = whitespace.getPos();

        final float posX = pos.getX();
        final float posEndX = pos.getEndX();
        if (posX == region.getPos().getX() || posEndX == region.getPos().getEndX()) {
            continue;
        }

        if (pos.getHeight() / pos.getWidth() <= 1.5f) {
            continue;
        }

        /* count how much text is to the immediate left of the current whitespace */
        final List<PhysicalContent> left = region.searchInDirectionFromOrigin(W, pos, 10.0f);
        Collections.sort(left, Sorting.sortByHigherX);

        int leftCount = 0;
        for (PhysicalContent content : left) {
            if (content instanceof WhitespaceRectangle) {
                continue;
            }
            if (MathUtils.isWithinVariance(content.getPos().getEndX(), posX, 3.0f)) {
                leftCount++;
            }
        }
        if (leftCount == 0) {
            continue;
        }

        /* and how much is to the right */
        final List<PhysicalContent> right = region.searchInDirectionFromOrigin(E, pos, 10.0f);
        Collections.sort(right, Sorting.sortByLowerX);
        int rightCount = 0;
        for (PhysicalContent content : right) {
            if (content instanceof WhitespaceRectangle) {
                continue;
            }
            if (MathUtils.isWithinVariance(content.getPos().getX(), posEndX, 3.0f)) {
                rightCount++;
            }
        }
        if (rightCount == 0) {
            continue;
        }

        if (leftCount > 3 || rightCount > 3) {
            columnBoundaries.add(whitespace);
        }
    }
    return columnBoundaries;
}
}
