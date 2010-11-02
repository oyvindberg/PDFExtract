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

protected final float minHeight;
protected final float minWidth;

/* this holds all the whitespace rectangles we have found */
protected final List<WhitespaceRectangle> foundWhitespace;

/* the number of whitespace we want to find */
protected final int wantedWhitespaces;

private final PriorityQueue<QueueEntry> queue;

private final RectangleCollection<? extends HasPosition> page;

// --------------------------- CONSTRUCTORS ---------------------------

public AbstractWhitespaceFinder(RectangleCollection<? extends HasPosition> page,
                                final int numWantedWhitespaces,
                                final float minWidth,
                                final float minHeight)
{
    this.page = page;

    wantedWhitespaces = numWantedWhitespaces;
    foundWhitespace = new ArrayList<WhitespaceRectangle>(numWantedWhitespaces);

    queue = new PriorityQueue<QueueEntry>(MAX_QUEUE_SIZE);

    this.minWidth = minWidth;
    this.minHeight = minHeight;
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
        if (MAX_QUEUE_SIZE - 4 <= queue.size()) {
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
            final WhitespaceRectangle newWhitespace = new WhitespaceRectangle(current.bound);
            if (current.bound.getHeight() < page.getHeight() / 4.0f) {
                if (!doesTouchAnotherWhitespace(newWhitespace)) {
                    continue;
                }
            }
            return newWhitespace;
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

        final QueueEntry[] subrectangles = splitSearchAreaAround(current, pivot);

        for (QueueEntry sub : subrectangles) {
            /** check that the subrectangle is contained by the current bound. this will happen
             * if the pivot we used was itself not contained. This breaks the algorithm if it
             * happens, as we will have overlapping rectangles */
            if (!sub.bound.containedBy(current.bound)) {
                continue;
            }

            if (sub.bound.getWidth() < minWidth || sub.bound.getHeight() < minHeight) {
                continue;
            }

            /** It does not make sense to include rectangles which are completely
             *  contained within one of the obstacles, so skip those */
            if (isNotContainedByAnyObstacle(sub.bound, sub.obstacles)) {
                queue.add(sub);
            }
        }
    }

    /* if we ran out of rectangles in the queue, return null to signal that. */
    //noinspection ReturnOfNull
    return null;
}

private boolean doesTouchAnotherWhitespace(final WhitespaceRectangle newWhitespace) {
    if (newWhitespace.getX() == 0.0f || newWhitespace.getY() == 0.0f
            || newWhitespace.getEndX() == page.getWidth()
            || newWhitespace.getEndY() == page.getHeight()) {
        return true;
    }

    for (WhitespaceRectangle existing : foundWhitespace) {
        if (newWhitespace.distance(existing) < 1.0f) {
            return true;
        }
    }

    return false;
}

private int getNumberOfWhitespacesFound() {
    return foundWhitespace.size();
}

private QueueEntry[] splitSearchAreaAround(final QueueEntry current, final HasPosition pivot) {
    final Rectangle pos = current.bound;
    final Rectangle pivotPos = pivot.getPosition();

    Rectangle left = new Rectangle(pos.getX(), pos.getY(), pivotPos.getX() - pos.getX(),
                                   pos.getHeight());
    List<HasPosition> leftObstacles = new ArrayList<HasPosition>();

    Rectangle above = new Rectangle(pos.getX(), pos.getY(), pos.getWidth(),
                                    pivotPos.getY() - pos.getY());
    List<HasPosition> aboveObstacles = new ArrayList<HasPosition>();

    Rectangle right = new Rectangle(pivotPos.getEndX(), pos.getY(),
                                    pos.getEndX() - pivotPos.getEndX(), pos.getHeight());
    List<HasPosition> rightObstacles = new ArrayList<HasPosition>();

    Rectangle below = new Rectangle(pos.getX(), pivotPos.getEndY(), pos.getWidth(),
                                    pos.getEndY() - pivotPos.getEndY());
    List<HasPosition> belowObstacles = new ArrayList<HasPosition>();

    /**
     * All the obstacles in current already fit within current.bound, so we can do just a quick
     *  check to see where they belong here. this is primarily an optimization
     */

    for (HasPosition obstacle : current.obstacles) {
        if (obstacle.equals(pivot)) {
            continue;
        }

        final Rectangle oPos = obstacle.getPosition();

        if (oPos.getX() < pivotPos.getX()) {
            leftObstacles.add(obstacle);
        }
        if (oPos.getY() < pivotPos.getY()) {
            aboveObstacles.add(obstacle);
        }
        if (oPos.getEndX() > pivotPos.getEndX()) {
            rightObstacles.add(obstacle);
        }
        if (oPos.getEndY() > pivotPos.getEndY()) {
            belowObstacles.add(obstacle);
        }
    }

    return new QueueEntry[]{new QueueEntry(left, leftObstacles),
                            new QueueEntry(above, aboveObstacles),
                            new QueueEntry(right, rightObstacles),
                            new QueueEntry(below, belowObstacles)};
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
    final List<HasPosition> obstacles;///= new ArrayList<HasPosition>();
    int numberOfWhitespaceFound;
    private final float q;

    private QueueEntry(final Rectangle bound, final List<? extends HasPosition> positions) {
        this.bound = bound;
        obstacles = (List<HasPosition>) positions;
        numberOfWhitespaceFound = getNumberOfWhitespacesFound();
        q = rectangleQuality(bound);
    }

    @Override
    public int compareTo(final QueueEntry o) {
        return Float.compare(o.q, q);
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
