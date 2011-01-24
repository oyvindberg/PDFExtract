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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.elacin.pdfextract.Constants.COLUMNS_MIN_COLUMN_WIDTH;
import static org.elacin.pdfextract.Constants.WHITESPACE_NUMBER_WANTED;
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

    filter(region, columnBoundaries);

    combineColumnBoundaries(region, columnBoundaries);

    return columnBoundaries;
}

private static String chars = "()[]abcdef123456789o.• ";

private static void filter(final PhysicalPageRegion r, final List<WhitespaceRectangle> boundaries) {

    List<WhitespaceRectangle> toRemove = new ArrayList<WhitespaceRectangle>();
    Collections.sort(boundaries, Sorting.sortByLowerX);
    for (int i = boundaries.size() - 1; i >= 0; i--) {
        final WhitespaceRectangle boundary = boundaries.get(i);

        if (boundary.getPos().getHeight() < r.getPos().getHeight() * 0.15f) {
            toRemove.add(boundary);
            continue;
        }

        final float boundaryToTheLeft;
        if (i == 0) {
            boundaryToTheLeft = r.getPos().getX();
        } else {
            boundaryToTheLeft = boundaries.get(i - 1).getPos().getEndX();
        }
        final Rectangle bpos = boundary.getPos();
        final float searchWidth = bpos.getX() - boundaryToTheLeft;

        if (searchWidth <= 0) {
            toRemove.add(boundary);
            continue;
        }

        Rectangle search = new Rectangle(boundaryToTheLeft, bpos.getY(), searchWidth,
                                         bpos.getHeight());

        final List<PhysicalContent> contentToTheLeft = r.findContentsIntersectingWith(search);

        /* demand a certain amount of words on the left side to split */
        if (contentToTheLeft.size() < 4) {
            toRemove.add(boundary);
            continue;
        }

        StringBuffer sb = new StringBuffer();
        for (PhysicalContent content : contentToTheLeft) {
            if (content.isText()) {
                sb.append(content.getPhysicalText().getText());
            }
        }
        boolean foundCharsExceptThoseInString = false;
        for (int j = 0; j < sb.length(); j++) {
            if (chars.indexOf(sb.charAt(j)) == -1) {
                foundCharsExceptThoseInString = true;
                break;
            }
        }
        if (!foundCharsExceptThoseInString) {
            toRemove.add(boundary);
            continue;
        }

        if (boundary.getPos().getX() < r.getPos().getX() + r.getPos().getWidth() * 0.05f) {
            toRemove.add(boundary);
            continue;
        }

        if (boundary.getPos().getEndX() > r.getPos().getEndX() - r.getPos().getWidth() * 0.05f) {
            toRemove.add(boundary);
            continue;
        }


    }
    if (log.isDebugEnabled()) {
        log.debug("Removing columns" + toRemove);
    }

    boundaries.removeAll(toRemove);

}

@NotNull
public static List<WhitespaceRectangle> findWhitespace(@NotNull final PhysicalPageRegion region) {
    final long t0 = System.currentTimeMillis();

    //    final int numWhitespaces = Math.max(30, Math.min(45, region.getContents().size() / 8));
    final int numWhitespaces = WHITESPACE_NUMBER_WANTED;

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
    final Collection<WhitespaceRectangle> newBoundaries = new ArrayList<WhitespaceRectangle>();

    for (final WhitespaceRectangle boundary : columnBoundaries) {
        final Rectangle bpos = boundary.getPos();

        /* calculate three possible columns, on the left and right side of the rectangle,
            and along the middle
         */
        final float ADJUST = 1.0f;
        final float leftX = Math.min(bpos.getX() + ADJUST, bpos.getEndX());
        final float leftEndX = Math.min(leftX + COLUMNS_MIN_COLUMN_WIDTH, bpos.getEndX() - ADJUST);
        final WhitespaceRectangle left = adjustColumn(region, boundary, leftX, leftEndX);

        final float midX = bpos.getMiddleX();
        final float midEndX = Math.min(midX + COLUMNS_MIN_COLUMN_WIDTH, bpos.getEndX());
        final WhitespaceRectangle middle = adjustColumn(region, boundary, midX, midEndX);

        final float rightEndX = Math.max(bpos.getEndX() - ADJUST, bpos.getX());
        final float rightX = Math.max(rightEndX - COLUMNS_MIN_COLUMN_WIDTH, bpos.getX());
        final WhitespaceRectangle right = adjustColumn(region, boundary, rightX, rightEndX);

        /* then choose the tallest */
        final float lHeight = (left == null ? -1.0f : left.getPos().getHeight());
        final float mHeight = (middle == null ? -1.0f : middle.getPos().getHeight());
        final float rHeight = (right == null ? -1.0f : right.getPos().getHeight());

        @Nullable final WhitespaceRectangle adjusted;
        if (lHeight > mHeight && lHeight > rHeight) {
            adjusted = left;
        } else if (rHeight > mHeight && rHeight > lHeight) {
            adjusted = right;
        } else {
            if (middle != null) {
                adjusted = middle;
            } else if (right != null) {
                adjusted = right;
            } else if (left != null) {
                adjusted = left;
            } else {
                adjusted = null;
            }

        }
        if (adjusted != null && !newBoundaries.contains(adjusted)) {
            newBoundaries.add(adjusted);
        }
    }
    columnBoundaries.clear();
    columnBoundaries.addAll(newBoundaries);

}

@Nullable
private static WhitespaceRectangle adjustColumn(final PhysicalPageRegion region,
                                                final WhitespaceRectangle boundary,
                                                final float boundaryStartX,
                                                final float boundaryEndX)
{

    final Rectangle rpos = region.getPos();

    /* find surrounding content */
    final List<PhysicalContent> everythingRightOf = findAllContentsRightOf(region, boundaryStartX);
    Collections.sort(everythingRightOf, Sorting.sortByLowerYThenLowerX);

    final List<PhysicalContent> closeOnLeft = findAllContentsImmediatelyLeftOfX(region,
                                                                                boundaryStartX);
    Collections.sort(closeOnLeft, Sorting.sortByLowerYThenLowerX);


    float realBoundaryY = rpos.getY();
    float realBoundaryEndY = rpos.getEndY();

    boolean startYFound = false;
    boolean boundaryStarted = false;
    for (int y = (int) rpos.getY(); y <= (int) (rpos.getEndY() + 1); y++) {

        boolean foundContentRightOfX;

        PhysicalContent closestOnRight = getClosestToTheRightAtY(everythingRightOf, y,
                                                                 boundary.getPos().getEndX());

        if (closestOnRight == null) {
            continue;
        } else {
            foundContentRightOfX = true;
        }

        //        if (!foundContentRightOfX && closestOnRight.getPos().getX() > boundaryEndX - 1) {
        //            foundContentRightOfX = true;
        //        }

        /** if we find something blocking this row, start looking further down*/

        /* content will be blocking if it intersects, naturally */
        boolean blocked = false;
        if (closestOnRight.getPos().getX() <= boundaryStartX) {
            blocked = true;

        } else {

            /* also check if this column boundary would separate two words which otherwise are very close*/
            for (PhysicalContent left : closeOnLeft) {

                if (left instanceof WhitespaceRectangle) {
                    continue;
                }

                if (y < left.getPos().getY()) {
                    continue;
                }
                if (y > left.getPos().getEndY()) {
                    continue;
                }

                if (closestOnRight.getPos().getX() - left.getPos().getEndX() < 6.0f) {
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
                realBoundaryY = (float) (y - 1);
            }
            if (!boundaryStarted && y > (int) boundary.getPos().getY()) {
                boundaryStarted = true;
            }
            realBoundaryEndY = y;
        }
    }
    if (!startYFound) {
        return null;
    }

    final Rectangle adjusted = new Rectangle(boundaryStartX, realBoundaryY + 2, 1.0f,
                                             realBoundaryEndY - realBoundaryY);
    final WhitespaceRectangle newBoundary = new WhitespaceRectangle(adjusted);
    newBoundary.setScore(1000);
    return newBoundary;
}

/**
 * @param everythingRightOf x sorted list
 * @param y                 row to look at
 * @param endX
 * @return
 */
@Nullable
private static PhysicalContent getClosestToTheRightAtY(final List<PhysicalContent> everythingRightOf,
                                                       final int y,
                                                       final float endX)
{
    PhysicalContent closest = null;
    float minDistance = Float.MAX_VALUE;

    for (int j = 0; j < everythingRightOf.size(); j++) {
        PhysicalContent content = everythingRightOf.get(j);

        if (content instanceof WhitespaceRectangle) {
            continue;
        }

        final Rectangle blockerPos = content.getPos();

        if (blockerPos.getEndY() < y) {
            continue;
        }
        if (blockerPos.getY() > y) {
            break;
        }

        float distance = blockerPos.getX() - endX;
        if (distance < minDistance) {
            minDistance = distance;
            closest = content;
        }

    }
    return closest;
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
private static List<PhysicalContent> findAllContentsImmediatelyLeftOfX(@NotNull PhysicalPageRegion region,
                                                                       float x)
{
    final float lookLeft = 10.0f;
    final Rectangle rpos = region.getPos();
    final Rectangle search = new Rectangle(x - lookLeft, rpos.getY(), lookLeft, rpos.getHeight());
    return region.findContentsIntersectingWith(search);
}

@NotNull
private static List<PhysicalContent> findAllContentsRightOf(@NotNull PhysicalPageRegion region,
                                                            float x)
{

    final Rectangle search = new Rectangle(x, region.getPos().getY(), region.getPos().getWidth(),
                                           region.getPos().getHeight());
    return region.findContentsIntersectingWith(search);
}

@NotNull
private static List<WhitespaceRectangle> selectCandidateColumnBoundaries(@NotNull PhysicalPageRegion region,
                                                                         @NotNull List<WhitespaceRectangle> whitespaces)
{
    final float LOOKAHEAD = 10.0f;
    final float HALF_LOOKAHEAD = LOOKAHEAD / 2;

    final List<WhitespaceRectangle> columnBoundaries = new ArrayList<WhitespaceRectangle>();
    for (WhitespaceRectangle whitespace : whitespaces) {
        final Rectangle pos = whitespace.getPos();

        final float posX = pos.getX();
        final float posEndX = pos.getEndX();


        if (pos.getHeight() / pos.getWidth() <= 1.5f) {
            continue;
        }

        final Rectangle smallerPos = pos.getAdjustedBy(-1.0f);

        /* count how much text is to the immediate left of the current whitespace */
        final List<PhysicalContent> left = region.searchInDirectionFromOrigin(W, smallerPos,
                                                                              LOOKAHEAD);
        Collections.sort(left, Sorting.sortByHigherX);

        int leftCount = 0;
        for (PhysicalContent content : left) {
            if (content instanceof WhitespaceRectangle) {
                continue;
            }
            if (MathUtils.isWithinVariance(content.getPos().getEndX(), posX + HALF_LOOKAHEAD,
                                           LOOKAHEAD)) {
                leftCount++;
            }
        }

        /* and how much is to the right */

        final List<PhysicalContent> right = region.searchInDirectionFromOrigin(E, smallerPos,
                                                                               LOOKAHEAD);
        Collections.sort(right, Sorting.sortByLowerX);
        int rightCount = 0;
        for (PhysicalContent content : right) {
            if (content instanceof WhitespaceRectangle) {
                continue;
            }
            if (MathUtils.isWithinVariance(content.getPos().getX(), posEndX + HALF_LOOKAHEAD,
                                           LOOKAHEAD)) {
                rightCount++;
            }
        }

        if (leftCount == 0 && rightCount < 8) {
            continue;
        }

        if (rightCount == 0 && leftCount < 8) {
            continue;
        }

        if (leftCount >= 3 || rightCount >= 3) {
            columnBoundaries.add(whitespace);
            whitespace.setScore(500);
        }
    }
    return columnBoundaries;
}
}
