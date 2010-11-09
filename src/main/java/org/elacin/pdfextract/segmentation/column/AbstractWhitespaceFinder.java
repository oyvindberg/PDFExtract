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
import org.elacin.pdfextract.segmentation.WhitespaceRectangle;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.elacin.pdfextract.Loggers.getInterfaceLog;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 3:05:06 PM To change this
 * template use File | Settings | File Templates.
 */
abstract class AbstractWhitespaceFinder {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(AbstractWhitespaceFinder.class);

/**
 * These are parameters to the algorithm.
 */

/* an artificial limit of the algorithm. */
private static final int MAX_QUEUE_SIZE = 100000;

/* min[Height|Width] are the thinnest rectangles we will accept */
private final float minHeight;
private final float minWidth;

/* the number of whitespace we want to find */
private final int wantedWhitespaces;

/**
 * State while working follows below
 */

/* this holds all the whitespace rectangles we have found */
private final List<WhitespaceRectangle> foundWhitespace;

/* a queue which will give us the biggest/best rectangles first */
private final PriorityQueue<QueueEntry> queue;

/* all the obstacles in the algorithm are found here, and are initially all the
    words on the page */
private final RectangleCollection region;

// --------------------------- CONSTRUCTORS ---------------------------

public AbstractWhitespaceFinder(RectangleCollection region,
                                final int numWantedWhitespaces,
                                final float minWidth,
                                final float minHeight)
{
    this.region = region;

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

/**
 * Returns true if subrectangle is completely contained withing one of the obstacles. This happens
 * rarely, but a check is necessary
 *
 * @param subrectangle
 * @param obstacles
 * @return
 */
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

/**
 * Finds the requested amount of whitespace rectangles based on the contents on the page which has
 * been provided.
 *
 * @return whitespace rectangles
 */
public List<WhitespaceRectangle> findWhitespace() {
    final long t0 = System.currentTimeMillis();

    if (foundWhitespace.isEmpty()) {
        /* first add the whole page with all the obstacles to the priority queue */
        queue.add(new QueueEntry(region.getPosition(), region.getContents()));

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
    final long time = System.currentTimeMillis() - t0;
    getInterfaceLog().debug("LOG00200:findWhitespace took " + time + " ms.");

    return foundWhitespace;
}

// -------------------------- OTHER METHODS --------------------------

@SuppressWarnings({"ObjectAllocationInLoop"})
private WhitespaceRectangle findNextWhitespace() {
    while (!queue.isEmpty()) {
        /* TODO: i can't help but feel this shouldnt be necessary */
        if (MAX_QUEUE_SIZE - 4 <= queue.size()) {
            log.warn("Queue too long");
            return null;
        }

        /* this will always choose the rectangle with the highest priority */
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
            if (current.bound.getHeight() < region.getHeight() / 4.0f) {
                if (!isNextToWhitespaceOrEdge(newWhitespace)) {
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
             * happens */
            if (!sub.bound.containedBy(current.bound)) {
                continue;
            }

            /* filter out rectangles which are deemed too thin */
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

private int getNumberOfWhitespacesFound() {
    return foundWhitespace.size();
}

/**
 * This method provides a personal touch to the algorithm described in the paper which is
 * referenced. Here we will just accept rectangles which are adjacent to either another one which we
 * have already identified, or which are adjacent to the edge of the page.
 * <p/>
 * By assuring that the we thus form continous chains of rectangles, the results seem to be much
 * better.
 *
 * @param newWhitespace
 * @return
 */

private boolean isNextToWhitespaceOrEdge(final WhitespaceRectangle newWhitespace) {
    /* accept this rectangle if it is adjacent to the edge of the page */
    //noinspection FloatingPointEquality
    if (newWhitespace.getPosition().getX() == region.getPosition().getX()
            || newWhitespace.getPosition().getY() == region.getPosition().getY()
            || newWhitespace.getPosition().getEndX() == region.getPosition().getEndX()
            || newWhitespace.getPosition().getEndY() == region.getPosition().getEndY()) {
        return true;
    }

    /* also accept if it borders one of the already identified whitespaces */
    for (WhitespaceRectangle existing : foundWhitespace) {
        if (newWhitespace.getPosition().distance(existing.getPosition()) < 1.0f) {
            return true;
        }
    }

    return false;
}


/**
 * Creates four rectangles with the remaining space left after splitting the current rectangle
 * around the pivot. Also divides the obstacles among the newly created rectangles
 *
 * @param current
 * @param pivot
 * @return
 */
private QueueEntry[] splitSearchAreaAround(final QueueEntry current, final HasPosition pivot) {
    final Rectangle p = current.bound;
    final Rectangle split = pivot.getPosition();

    final Rectangle left, above, right, below;
    left = new Rectangle(p.getX(), p.getY(), split.getX() - p.getX(), p.getHeight());
    List<HasPosition> leftObstacles = new ArrayList<HasPosition>();

    above = new Rectangle(p.getX(), p.getY(), p.getWidth(), split.getY() - p.getY());
    List<HasPosition> aboveObstacles = new ArrayList<HasPosition>();

    right = new Rectangle(split.getEndX(), p.getY(), p.getEndX() - split.getEndX(), p.getHeight());
    List<HasPosition> rightObstacles = new ArrayList<HasPosition>();

    below = new Rectangle(p.getX(), split.getEndY(), p.getWidth(), p.getEndY() - split.getEndY());
    List<HasPosition> belowObstacles = new ArrayList<HasPosition>();

    /**
     * All the obstacles in current already fit within current.bound, so we can do just a quick
     *  check to see where they belong here. this way of doing it is primarily an optimization
     */
    for (HasPosition obstacle : current.obstacles) {
        /* including the pivot will break the algorithm */
        if (obstacle.equals(pivot)) {
            continue;
        }
        if (obstacle.getPosition().getX() < split.getX()) {
            leftObstacles.add(obstacle);
        }
        if (obstacle.getPosition().getY() < split.getY()) {
            aboveObstacles.add(obstacle);
        }
        if (obstacle.getPosition().getEndX() > split.getEndX()) {
            rightObstacles.add(obstacle);
        }
        if (obstacle.getPosition().getEndY() > split.getEndY()) {
            belowObstacles.add(obstacle);
        }
    }

    return new QueueEntry[]{new QueueEntry(left, leftObstacles),
                            new QueueEntry(above, aboveObstacles),
                            new QueueEntry(right, rightObstacles),
                            new QueueEntry(below, belowObstacles)};
}

/**
 * Checks if some of the newly added whitespace rectangles, that is those discovered after this
 * queue entry was added to the queue, overlaps with the area of this queue entry, and if so adds
 * them to this list of obstacles .
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

/**
 * This is the quality function by which we sort rectangles to choose the 'best' one first. The
 * current function bases itself on the area of the rectangle, and then heavily prefers high ones
 *
 * @param r
 * @return
 */
protected abstract float rectangleQuality(Rectangle r);

// -------------------------- INNER CLASSES --------------------------

private class QueueEntry implements Comparable<QueueEntry> {
    final Rectangle bound;
    final List<HasPosition> obstacles;
    final int numberOfWhitespaceFound;
    private final float quality;

    private QueueEntry(final Rectangle bound, final List<? extends HasPosition> positions) {
        this.bound = bound;
        /* damn it, i honestly fail to understand why i cant really make any use of obstacles
        *   as it is now. */
        //noinspection unchecked
        obstacles = (List<HasPosition>) positions;
        numberOfWhitespaceFound = getNumberOfWhitespacesFound();
        quality = rectangleQuality(bound);
    }

    @Override
    public int compareTo(final QueueEntry o) {
        return Float.compare(o.quality, quality);
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
