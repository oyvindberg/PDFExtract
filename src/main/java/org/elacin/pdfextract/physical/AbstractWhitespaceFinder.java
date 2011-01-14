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

package org.elacin.pdfextract.physical;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.content.HasPosition;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 3:05:06 PM To change this
 * template use File | Settings | File Templates.
 */
public abstract class AbstractWhitespaceFinder {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(AbstractWhitespaceFinder.class);

/**
 * These are parameters to the algorithm.
 */

/* an artificial limit of the algorithm. */
private static final int MAX_QUEUE_SIZE = 100000;

/* all the obstacles in the algorithm are found here, and are initially all the
    words on the page */
protected final RectangleCollection region;

/* min[Height|Width] are the thinnest rectangles we will accept */
private final float minHeight;
private final float minWidth;

/* the number of whitespace we want to find */
private final int wantedWhitespaces;


/**
 * State while working follows below
 */

/* this holds all the whitespace rectangles we have found */
@NotNull
private final List<WhitespaceRectangle> foundWhitespace;

/* a queue which will give us the biggest/best rectangles first */
@NotNull
private final PriorityQueue<QueueEntry> queue;

/* this holds a list of all queue entries which are not yet accepted. Upon finding a new
* whitespace rectangle, these are added back to the queue. */
private final List<QueueEntry> notYetAccepted = new ArrayList<QueueEntry>();

// --------------------------- CONSTRUCTORS ---------------------------

public AbstractWhitespaceFinder(RectangleCollection region,
                                final int numWantedWhitespaces,
                                final float minWidth,
                                final float minHeight) {
    this.region = region;

    wantedWhitespaces = numWantedWhitespaces;
    foundWhitespace = new ArrayList<WhitespaceRectangle>(numWantedWhitespaces);

    queue = new PriorityQueue<QueueEntry>(MAX_QUEUE_SIZE);

    this.minWidth = minWidth;
    this.minHeight = minHeight;
}

// -------------------------- PUBLIC METHODS --------------------------

/**
 * Finds up to the requested amount of whitespace rectangles based on the contents on the page
 * which has been provided.
 *
 * @return whitespace rectangles
 */
@NotNull
public List<WhitespaceRectangle> findWhitespace() {
    if (foundWhitespace.isEmpty()) {
        /* first add the whole page (all its contents as obstacle)s to the priority queue */
        queue.add(new QueueEntry(region.getPos(), region.getContents()));

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

private int getNumberOfWhitespacesFound() {
    return foundWhitespace.size();
}

@Nullable
@SuppressWarnings({"ObjectAllocationInLoop"})
private WhitespaceRectangle findNextWhitespace() {

    queue.addAll(notYetAccepted);
    notYetAccepted.clear();

    while (!queue.isEmpty()) {

        /* Place an upper bound. If we reach this queue size we should already have enough data */
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

        /* if this contains no obstacles (or just barely touches on exactly one) we have
        *   found a new whitespace rectangle
        * */
        if (isEmptyEnough(current)) {
            final WhitespaceRectangle newWhitespace = new WhitespaceRectangle(current.bound);

            /** if the rectangle is not higher than 25% of the page, check whether it is surrounded
             * on all sides by text. in that case, drop it */
            if (!isNextToWhitespaceOrEdge(newWhitespace)) {
                notYetAccepted.add(current);
                continue;
            }

            /* a subclass might decide to put some further demands on what we accept*/
            if (acceptsRectangle(newWhitespace)) {
                return newWhitespace;
            } else {
                continue;
            }
        }

        /* choose an obstacle near the middle of the current rectangle */
        final HasPosition pivot = choosePivot(current.bound, current.obstacles);

        /**
         * Create four subrectangles, one on each side of the pivot, and determine the obstacles
         *  located inside it. Then add each subrectangle to the queue (as long as it is
         *  not completely contained within an obstacle)
         */
        final QueueEntry[] subrectangles = splitSearchAreaAround(current, pivot);

        for (QueueEntry sub : subrectangles) {
            if (sub == null) {
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

/**
 * Checks if some of the newly added whitespace rectangles, that is those discovered after this
 * queue entry was added to the queue, overlaps with the area of this queue entry, and if so adds
 * them to this list of obstacles .
 */
private void updateObstacleListForQueueEntry(@NotNull final QueueEntry entry) {
    int numNewestObstaclesToCheck = getNumberOfWhitespacesFound() - entry
            .numberOfWhitespaceFound;

    for (int i = 0; i < numNewestObstaclesToCheck; i++) {
        final HasPosition obstacle = foundWhitespace.get(foundWhitespace.size() - 1 - i);
        if (entry.bound.intersectsExclusiveWith(obstacle)) {
            entry.obstacles.add(obstacle);
        }
    }
}

/**
 * If none of the obstacles are contained within outerBound, then we found a rectangle
 */
private boolean isEmptyEnough(QueueEntry current) {

    /* accept a small intersection */
    if (current.obstacles.size() <= 3) {
        final float boundArea = current.bound.area();

        for (HasPosition obstacle : current.obstacles) {
            final float intersect = current.bound.intersection(obstacle.getPos()).area();
            if (intersect > obstacle.getPos().area() * 0.3f || intersect > boundArea * 0.4f) {
                return false;
            }

        }
        return true;

    }
    return current.obstacles.isEmpty();
}

protected boolean acceptsRectangle(WhitespaceRectangle newWhitespace) {
    return true;
}

/**
 * Finds the obstacle which is closest to the centre of the rectangle bound
 */
@Nullable
private static HasPosition choosePivot(@NotNull final Rectangle bound,
                                       @NotNull final List<? extends HasPosition> obstacles) {
    if (obstacles.size() == 1) {
        return obstacles.get(0);
    }
    final FloatPoint centrePoint = bound.centre();
    float minDistance = Float.MAX_VALUE;
    HasPosition closestToCentre = null;

    for (HasPosition obstacle : obstacles) {
        final float distance = obstacle.getPos().distance(centrePoint);
        if (distance < minDistance) {
            minDistance = distance;
            closestToCentre = obstacle;
        }
    }
    return closestToCentre;
}

/**
 * Creates four rectangles with the remaining space left after splitting the current rectangle
 * around the pivot. Also divides the obstacles among the newly created rectangles
 */
private QueueEntry[] splitSearchAreaAround(@NotNull final QueueEntry current,
                                           @NotNull final HasPosition pivot) {
    final Rectangle p = current.bound;
    final Rectangle split = pivot.getPos();

    /* Everything inside here was the definitely most expensive parts of the implementation,
    *   so this is quite optimized to avoid too many float point comparisons and needless
    *   object creations. This cut execution time by 90% :)*/


    Rectangle left = null;
    List<HasPosition> leftObstacles = null;
    final float leftWidth = split.getX() - p.getX();
    if (split.getX() > p.getX() && leftWidth > minWidth) {
        left = new Rectangle(p.getX(), p.getY(), leftWidth, p.getHeight());
        leftObstacles = new ArrayList<HasPosition>();
    }

    Rectangle above = null;
    List<HasPosition> aboveObstacles = null;
    final float aboveHeight = split.getY() - p.getY();
    if (split.getY() > p.getY() && aboveHeight > minHeight) {
        aboveObstacles = new ArrayList<HasPosition>();
        above = new Rectangle(p.getX(), p.getY(), p.getWidth(), aboveHeight);
    }


    Rectangle right = null;
    List<HasPosition> rightObstacles = null;
    final float rightWidth = p.getEndX() - split.getEndX();
    if (split.getEndX() < p.getEndX() && rightWidth > minWidth) {
        right = new Rectangle(split.getEndX(), p.getY(), rightWidth, p.getHeight());
        rightObstacles = new ArrayList<HasPosition>();
    }


    Rectangle below = null;
    List<HasPosition> belowObstacles = null;
    final float belowHeight = p.getEndY() - split.getEndY();
    if (split.getEndY() < p.getEndY() && belowHeight > minHeight) {
        below = new Rectangle(p.getX(), split.getEndY(), p.getWidth(), belowHeight);
        belowObstacles = new ArrayList<HasPosition>();
    }


    /**
     * All the obstacles in current already fit within current.bound, so we can do just a quick
     *  check to see where they belong here. this way of doing it is primarily an optimization
     */
    for (HasPosition obstacle : current.obstacles) {
        /* including the pivot will break the algorithm */
        if (obstacle.equals(pivot)) {
            continue;
        }
        if (left != null && obstacle.getPos().getX() < split.getX()) {
            leftObstacles.add(obstacle);
        }
        if (above != null && obstacle.getPos().getY() < split.getY()) {
            aboveObstacles.add(obstacle);
        }
        if (right != null && obstacle.getPos().getEndX() > split.getEndX()) {
            rightObstacles.add(obstacle);
        }
        if (below != null && obstacle.getPos().getEndY() > split.getEndY()) {
            belowObstacles.add(obstacle);
        }
    }


    return new QueueEntry[]{left == null ? null : new QueueEntry(left, leftObstacles),
            right == null ? null : new QueueEntry(right, rightObstacles),
            above == null ? null : new QueueEntry(above, aboveObstacles),
            below == null ? null : new QueueEntry(below, belowObstacles)};
}

/**
 * Returns true if subrectangle is completely contained withing one of the obstacles. This
 * happens
 * rarely, but a check is necessary
 */
private static boolean isNotContainedByAnyObstacle(@NotNull final Rectangle subrectangle,
                                                   @NotNull final Iterable<HasPosition>
                                                           obstacles) {
    for (HasPosition obstacle : obstacles) {
        if (obstacle.getPos().contains(subrectangle)) {
            return false;
        }
    }
    return true;
}

// -------------------------- OTHER METHODS --------------------------

/**
 * This is the quality function by which we sort rectangles to choose the 'best' one first. The
 * current function bases itself on the area of the rectangle, and then heavily prefers high ones
 */
protected abstract float rectangleQuality(Rectangle r);

/**
 * This method provides a personal touch to the algorithm described in the paper which is
 * referenced. Here we will just accept rectangles which are adjacent to either another one which
 * we
 * have already identified, or which are adjacent to the edge of the page. <p/> By assuring that
 * the
 * we thus form continous chains of rectangles, the results seem to be much better.
 */

protected boolean isNextToWhitespaceOrEdge(@NotNull final WhitespaceRectangle newWhitespace) {
    /* accept this rectangle if it is adjacent to the edge of the page */
    //noinspection FloatingPointEquality
    final float l = 1.0f;

    if (newWhitespace.getPos().getX() <= region.getPos().getX() + l
            || newWhitespace.getPos().getY() <= region.getPos().getY() + l
            || newWhitespace.getPos().getEndX() >= region.getPos().getEndX() - l
            || newWhitespace.getPos().getEndY() >= region.getPos().getEndY() - l) {
        return true;
    }

    /* also accept if it borders one of the already identified whitespaces */
    for (WhitespaceRectangle existing : foundWhitespace) {
        if (newWhitespace.getPos().distance(existing.getPos()) < 0.01f) {
            return true;
        }
    }

    return false;
}

// -------------------------- INNER CLASSES --------------------------

private class QueueEntry implements Comparable<QueueEntry> {
    final         Rectangle         bound;
    @NotNull
    final         List<HasPosition> obstacles;
    final         int               numberOfWhitespaceFound;
    private final float             quality;

    private QueueEntry(final Rectangle bound, final List<? extends HasPosition> positions) {
        this.bound = bound;
        /* damn it, i honestly fail to understand why this cast is needed. */
        //noinspection unchecked
        obstacles = (List<HasPosition>) positions;
        numberOfWhitespaceFound = getNumberOfWhitespacesFound();
        quality = rectangleQuality(bound);
    }

    public int compareTo(@NotNull final QueueEntry o) {
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
    public boolean equals(@Nullable final Object o) {
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
