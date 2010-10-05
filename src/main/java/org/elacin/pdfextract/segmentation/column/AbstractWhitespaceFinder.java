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

import org.elacin.pdfextract.util.IntPoint;
import org.elacin.pdfextract.util.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 3:05:06 PM To change this template use File | Settings | File Templates.
 */
abstract class AbstractWhitespaceFinder {
// ------------------------------ FIELDS ------------------------------

    /* this holds all the whitespace rectangles we have found */
    protected final List<Rectangle> foundWhitespace;

    /* this holds all the content on the page. this is unchanged */
    protected final List<Rectangle> originalObstacles;

    /* the number of whitespace we want to find */
    protected final int wantedWhitespaces;

    /* width and height of the page */
    protected final Rectangle pageBounds;
    private final PriorityQueue<QueueEntry> queue;

// --------------------------- CONSTRUCTORS ---------------------------

    public AbstractWhitespaceFinder(final List<Rectangle> texts, final int numWantedWhitespaces, final int width, final int height) {
        wantedWhitespaces = numWantedWhitespaces;

        originalObstacles = texts;

        foundWhitespace = new ArrayList<Rectangle>(numWantedWhitespaces);

        pageBounds = new Rectangle(0, 0, width * 100, height * 100);
        queue = new PriorityQueue<QueueEntry>(originalObstacles.size() * 2);
    }

// -------------------------- STATIC METHODS --------------------------

    /**
     * Finds the obstacle which is closest to the centre of the rectangle bound
     *
     * @param bound
     * @param obstacles
     * @return
     */
    private static Rectangle choosePivot(final Rectangle bound, final Iterable<Rectangle> obstacles) {
        final IntPoint centrePoint = bound.centre();
        float minDistance = Float.MAX_VALUE;
        Rectangle closestToCentre = null;

        for (Rectangle obstacle : obstacles) {

            final float distance = obstacle.distance(centrePoint);
            if (distance < minDistance) {
                minDistance = distance;
                closestToCentre = obstacle;
            }
        }
        return closestToCentre;
    }

    /**
     * Creates a rectangle based on the coordinates of corners, instead of with the normal constructor which accepts upper left corner and width/height.
     *
     * @param x1 coordinate of any corner of the rectangle
     * @param y1 (see x1)
     * @param x2 coordinate of the opposite corner
     * @param y2 (see x2)
     */
    private static Rectangle createRectangle(final int x1, final int y1, final int x2, final int y2) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);

        int width = Math.max(x1, x2) - x;
        int height = Math.max(y1, y2) - y;

        return new Rectangle(x, y, width, height);
    }

    private static Rectangle[] createSubrectanglesAroundPivot(final Rectangle pos, final Rectangle pivot) {
        return new Rectangle[]{createRectangle(pivot.getX(), pos.getY(), pos.getX(), pos.getEndY()),
                createRectangle(pos.getX(), pivot.getY(), pos.getEndX(), pos.getY()),
                createRectangle(pos.getEndX(), pos.getY(), pivot.getEndX(), pos.getEndY()),
                createRectangle(pos.getX(), pos.getEndY(), pos.getEndX(), pivot.getEndY())};
    }

    private static List<Rectangle> getObstaclesBoundedBy(final Iterable<Rectangle> obstacles, final Rectangle subrectangle, final Rectangle pivot) {
        List<Rectangle> ret = new ArrayList<Rectangle>();
        for (Rectangle obstacle : obstacles) {
            if (obstacle != null && subrectangle.intersectsWith(obstacle) && !pivot.equals(obstacle)) {
                ret.add(obstacle);
            }
        }
        return ret;
    }

    private static boolean isNotContainedByAnyObstacle(final Rectangle subrectangle, final Iterable<Rectangle> obstacles) {
        for (Rectangle obstacle : obstacles) {
            if (obstacle.contains(subrectangle)) {
                return false;
            }
        }
        return true;
    }

// -------------------------- PUBLIC METHODS --------------------------

    public List<Rectangle> findWhitespace() {
        if (foundWhitespace.isEmpty()) {
            /* first add the whole page with all the obstacles to the priority queue */
            queue.add(new QueueEntry(pageBounds, originalObstacles));

            /* continue looking for whitespace until we have the wanted number or we run out*/
            while (getNumberOfWhitespacesFound() < wantedWhitespaces) {
                final Rectangle newRectangle = findNextWhitespace();

                /* if no further rectangles exist, stop looking */
                if (newRectangle == null) {
                    break;
                }

                foundWhitespace.add(newRectangle);
            }
        }
        return selectUsefulWhitespace();
    }

// -------------------------- OTHER METHODS --------------------------

    @SuppressWarnings({"ObjectAllocationInLoop"})
    private Rectangle findNextWhitespace() {
        /* this will always choose the rectangle with the highest priority */
        while (!queue.isEmpty()) {
            final QueueEntry current = queue.remove();

            /* if we have found and marked whitespace since we added this rectangle we need to recalculate the obstacles it
                references to make sure it doesnt overlap with the ones we already have */
            if (current.numberOfWhitespaceFound != getNumberOfWhitespacesFound()) {
                updateObstacleListForQueueEntry(current);
            }

            /* if none of the obstacles are contained within outerBound, then we have a whitespace rectangle */
            if (current.obstacles.isEmpty()) {
                return current.bound;
            }

            /* choose an obstacle near the middle of the current rectangle */
            final Rectangle pivot = choosePivot(current.bound, current.obstacles);

            /**
             * Create four subrectangles, one on each side of the pivot. 
             *
             * Then, for each subrectangle, determine the obstacles located inside it, 
             *  and add it to the queue (as long as the subrectangle does not escape
             *  the current bound, and as long as it is not completely contained within
             *  an obstacle) 
             */
            final Rectangle[] subrectangles = createSubrectanglesAroundPivot(current.bound, pivot);

            for (Rectangle subrectangle : subrectangles) {
                /* check that the subrectangle is contained by the current bound. this will happen
                    if the pivot we used was itself not contained. This breaks the algorithm if it
                    happens, as we will have overlapping rectangles */
                if (!subrectangle.containedBy(current.bound)) {
                    continue;
                }

                if (subrectangle.isEmpty()) {
                    continue;
                }

                final List<Rectangle> obstaclesForSubrectangle = getObstaclesBoundedBy(current.obstacles, subrectangle, pivot);

                /**
                 * It does not make sense to include rectangles which are completely
                 *  contained within one of the obstacles, so skip those
                 */
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

    /**
     * Checks if some of the newly added whitespace rectangles overlaps with the area of this queue entry, and if so adds them to its list of obstacles
     *
     * @param entry
     */
    private void updateObstacleListForQueueEntry(final QueueEntry entry) {
        int numNewestObstaclesToCheck = getNumberOfWhitespacesFound() - entry.numberOfWhitespaceFound;

        for (int i = 0; i < numNewestObstaclesToCheck; i++) {
            final Rectangle obstacle = foundWhitespace.get(foundWhitespace.size() - 1 - i);
            if (entry.bound.intersectsWith(obstacle)) {
                entry.obstacles.add(obstacle);
            }
        }
    }

    protected abstract float rectangleQuality(Rectangle r);

    protected abstract List<Rectangle> selectUsefulWhitespace();

// -------------------------- INNER CLASSES --------------------------

    private class QueueEntry implements Comparable<QueueEntry> {
        final Rectangle bound;
        final List<Rectangle> obstacles;
        int numberOfWhitespaceFound;

        private QueueEntry(final Rectangle bound, final List<Rectangle> obstacles) {
            this.bound = bound;
            this.obstacles = obstacles;
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
            sb.append("{area=").append(bound.area() / 10000);
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
