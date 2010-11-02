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

package org.elacin.pdfextract.segmentation.column;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.HasPosition;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 3:05:06 PM To change this
 * template use File | Settings | File Templates.
 */
abstract class AbstractWhitespaceFinder {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(AbstractWhitespaceFinder.class);

protected static final int MAX_QUEUE_SIZE = 100000;

protected final float whitespaceMinHeight;
protected final float minWidth;

/* this holds all the whitespace rectangles we have found */
protected final List<WhitespaceRectangle> foundWhitespace;

/* this holds all the content on the page. this is unchanged */
//protected final List<ObstacleType> originalObstacles;

/* the number of whitespace we want to find */
protected final int wantedWhitespaces;

private final PriorityQueue<QueueEntry> queue;
private final RectangleCollection<? extends HasPosition> page;

// --------------------------- CONSTRUCTORS ---------------------------

public AbstractWhitespaceFinder(RectangleCollection<? extends HasPosition> page,
                                final int numWantedWhitespaces,
                                final float minWidth,
                                final float whitespaceMinHeight)
{
    this.page = page;

    wantedWhitespaces = numWantedWhitespaces;
    foundWhitespace = new ArrayList<WhitespaceRectangle>(numWantedWhitespaces);

    queue = new PriorityQueue<QueueEntry>(MAX_QUEUE_SIZE);

    this.minWidth = minWidth;
    this.whitespaceMinHeight = whitespaceMinHeight;
}

// -------------------------- STATIC METHODS --------------------------

/**
 * Finds the obstacle which is closest to the centre of the rectangle bound
 *
 * @param bound
 * @param obstacles
 * @return
 */
private static HasPosition choosePivot(final Rectangle bound,
                                       final List<? extends HasPosition> obstacles)
{
    if (obstacles.size() == 1) {
        return obstacles.get(0);
    }
    final FloatPoint centrePoint = bound.centre();
    float minDistance = Float.MAX_VALUE;
    HasPosition closestToCentre = null;

    for (HasPosition obstacle : obstacles) {
        final float distance = obstacle.getPosition().distance(centrePoint);
        if (distance < minDistance) {
            minDistance = distance;
            closestToCentre = obstacle;
        }
    }
    return closestToCentre;
}

/**
 * Creates a rectangle based on the coordinates of corners, instead of with the normal constructor
 * which accepts upper left corner and width/height.
 *
 * @param x1 coordinate of any corner of the rectangle
 * @param y1 (see x1)
 * @param x2 coordinate of the opposite corner
 * @param y2 (see x2)
 */
private static Rectangle createRectangle(final float x1,
                                         final float y1,
                                         final float x2,
                                         final float y2)
{
    float x = Math.min(x1, x2);
    float y = Math.min(y1, y2);

    float width = Math.max(x1, x2) - x;
    float height = Math.max(y1, y2) - y;

    return new Rectangle(x, y, width, height);
}

private static Rectangle[] createSubrectanglesAroundPivot(final Rectangle pos,
                                                          final Rectangle pivot)
{
    Rectangle one = createRectangle(pivot.getX(), pos.getY(), pos.getX(), pos.getEndY());
    Rectangle two = createRectangle(pos.getX(), pivot.getY(), pos.getEndX(), pos.getY());
    Rectangle three = createRectangle(pos.getEndX(), pos.getY(), pivot.getEndX(), pos.getEndY());
    Rectangle four = createRectangle(pos.getX(), pos.getEndY(), pos.getEndX(), pivot.getEndY());

    return new Rectangle[]{one, two, three, four};
}

private static List<HasPosition> getObstaclesBoundedBy(final Iterable<? extends HasPosition> obstacles,
                                                       final HasPosition subrectangle,
                                                       final HasPosition pivot)
{
    List<HasPosition> ret = new ArrayList<HasPosition>();
    for (HasPosition obstacle : obstacles) {
        if (obstacle != null && subrectangle.getPosition().intersectsWith(obstacle)
                && !pivot.equals(obstacle)) {
            ret.add(obstacle);
        }
    }
    return ret;
}

private static boolean isNotContainedByAnyObstacle(final Rectangle subrectangle,
                                                   final Iterable<HasPosition> obstacles)
{
    for (HasPosition obstacle : obstacles) {
        if (obstacle.getPosition().contains(subrectangle)) {
            return false;
        }
    }
    return true;
}

// -------------------------- PUBLIC METHODS --------------------------

public List<WhitespaceRectangle> findWhitespace() {
    if (foundWhitespace.isEmpty()) {
        /* first add the whole page with all the obstacles to the priority queue */
        queue.add(new QueueEntry(page.getPosition(), page.getRectangles()));

        /* continue looking for whitespace until we have the wanted number or we run out*/
        while (getNumberOfWhitespacesFound() < wantedWhitespaces) {
            final WhitespaceRectangle newRectangle = findNextWhitespace();

            /* if no further rectangles exist, stop looking */
            if (newRectangle == null) {
                break;
            }

            foundWhitespace.add(newRectangle);
        }
    }

    return foundWhitespace;
}

// -------------------------- OTHER METHODS --------------------------

@SuppressWarnings({"ObjectAllocationInLoop"})
private WhitespaceRectangle findNextWhitespace() {
    /* this will always choose the rectangle with the highest priority */
    while (!queue.isEmpty()) {
        /* TODO: i can't help but feel this shouldnt be necessary */
        if (MAX_QUEUE_SIZE < queue.size()) {
            log.warn("Queue too long");
            return null;
        }

        final QueueEntry current = queue.remove();

        /** If we have found and marked whitespace since we added this rectangle we need to
         *  recalculate the obstacles it references to make sure it doesnt overlap with the ones
         *  we already have */
        if (current.numberOfWhitespaceFound != getNumberOfWhitespacesFound()) {
            updateObstacleListForQueueEntry(current);
        }

        /** If none of the obstacles are contained within outerBound, then we found a rectangle */
        if (current.obstacles.isEmpty()) {
            /* if the rectangle is not higher than 25% of the page, check whether it is
                surrounded on all sides by text. in that case, drop it
             */
            if (current.bound.getHeight() < page.getHeight() / 4.0f) {
                if (isSurroundedByObstaclesOnAllSides(current.bound)) {
                    continue;
                }
            }
            return new WhitespaceRectangle(current.bound);
        }

        /* choose an obstacle near the middle of the current rectangle */
        final HasPosition pivot = choosePivot(current.bound, current.obstacles);

        /** Create four subrectangles, one on each side of the pivot.
         *
         * Then, for each subrectangle, determine the obstacles located inside it,
         *  and add it to the queue (as long as the subrectangle does not escape
         *  the current bound, and as long as it is not completely contained within
         *  an obstacle)
         */
        final Rectangle[] subrectangles = createSubrectanglesAroundPivot(current.bound,
                                                                         pivot.getPosition());

        for (Rectangle subrectangle : subrectangles) {
            /** check that the subrectangle is contained by the current bound. this will happen
             * if the pivot we used was itself not contained. This breaks the algorithm if it
             * happens, as we will have overlapping rectangles */
            if (!subrectangle.containedBy(current.bound)) {
                continue;
            }

            if (subrectangle.getWidth() < minWidth
                    || subrectangle.getHeight() < whitespaceMinHeight) {
                continue;
            }

            final List<HasPosition> obstaclesForSubrectangle = getObstaclesBoundedBy(
                    current.obstacles, subrectangle, pivot);

            /** It does not make sense to include rectangles which are completely
             *  contained within one of the obstacles, so skip those */
            if (isNotContainedByAnyObstacle(subrectangle, obstaclesForSubrectangle)) {
                queue.add(new QueueEntry(subrectangle, obstaclesForSubrectangle));
            }
        }
    }

    /* if we ran out of rectangles in the queue, return null to signal that. */
    //noinspection ReturnOfNull
    return null;
}

private int getNumberOfWhitespacesFound() {
    return foundWhitespace.size();
}

private boolean isSurroundedByObstaclesOnAllSides(final Rectangle bound) {

    for (WhitespaceRectangle whitespaceRectangle : foundWhitespace) {
        if (bound.distance(whitespaceRectangle) < 2.0f) {
            return false;
        }
    }


    //    final float distance = 1.0f;
    //    return !page.searchDirection(Direction.E, bound, distance).isEmpty() && !page.searchDirection(
    //            Direction.W, bound, distance).isEmpty() && !page.searchDirection(Direction.N, bound,
    //                                                                             distance).isEmpty()
    //            && !page.searchDirection(Direction.S, bound, distance).isEmpty();
    //

    return true;
}

/**
 * Checks if some of the newly added whitespace rectangles overlaps with the area of this queue
 * entry, and if so adds them to its list of obstacles
 *
 * @param entry
 */
private void updateObstacleListForQueueEntry(final QueueEntry entry) {
    int numNewestObstaclesToCheck = getNumberOfWhitespacesFound() - entry.numberOfWhitespaceFound;

    for (int i = 0; i < numNewestObstaclesToCheck; i++) {
        final HasPosition obstacle = foundWhitespace.get(foundWhitespace.size() - 1 - i);
        if (entry.bound.intersectsWith(obstacle)) {
            entry.obstacles.add(obstacle);
        }
    }
}

protected abstract float rectangleQuality(Rectangle r);

// -------------------------- INNER CLASSES --------------------------

private class QueueEntry implements Comparable<QueueEntry> {
    final Rectangle bound;
    final List<HasPosition> obstacles = new ArrayList<HasPosition>();
    int numberOfWhitespaceFound;

    private QueueEntry(final Rectangle bound, final List<? extends HasPosition> obstacles) {
        this.bound = bound;
        this.obstacles.addAll(obstacles);
        numberOfWhitespaceFound = getNumberOfWhitespacesFound();
    }

    @Override
    public int compareTo(final QueueEntry o) {
        return Float.compare(rectangleQuality(o.bound), rectangleQuality(bound));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("QueueEntry");
        sb.append("{area=").append(bound.area());
        sb.append(", bound=").append(bound);
        sb.append(", obstacles=").append(obstacles.size());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final QueueEntry that = (QueueEntry) o;

        return !(bound != null ? !bound.equals(that.bound) : that.bound != null);
    }

    @Override
    public int hashCode() {
        return bound != null ? bound.hashCode() : 0;
    }
}
}
