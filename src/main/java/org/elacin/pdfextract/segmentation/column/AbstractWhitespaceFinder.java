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
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Sep 23, 2010
 * Time: 3:05:06 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractWhitespaceFinder {
// ------------------------------ FIELDS ------------------------------

    protected static final int WHITESPACE_MIN_HEIGHT = 700;
    protected static final int WHITESPACE_MIN_WIDTH = 300;
    protected final List<Rectangle> allObstacles;
    protected final int numWhitespacesToBeFound;
    protected final Rectangle documentBounds;
    protected int originalObstacles;

// --------------------------- CONSTRUCTORS ---------------------------

    public AbstractWhitespaceFinder(final Collection<Rectangle> texts, final int wantedWhitespaces, final int width, final int height) {
        numWhitespacesToBeFound = wantedWhitespaces;
        allObstacles = new ArrayList<Rectangle>(texts.size() + wantedWhitespaces);
        documentBounds = new Rectangle(0, 0, width * 100, height * 100);
        originalObstacles = texts.size();
    }

// -------------------------- STATIC METHODS --------------------------

    /**
     * Finds the obstacle which is closest to the centre of the rectangle bound
     *
     * @param bound
     * @param obstacles
     * @return
     */
    private static Rectangle choosePivot(final Rectangle bound, final List<Rectangle> obstacles) {
        final IntPoint centrePoint = bound.centre();
        float minDistance = Float.MAX_VALUE;
        Rectangle closestToCentre = null;

        for (Rectangle obstacle : obstacles) {
            if (obstacle == null) {
                continue;
            }
            final float distance = obstacle.distance(centrePoint);
            if (distance < minDistance) {
                minDistance = distance;
                closestToCentre = obstacle;
            }
        }
        return closestToCentre;
    }

    /**
     *
     * Creates a rectangle based on the coordinates of corners, instead
     *  of with the normal constructor which accepts upper left corner
     *  and width/height.
     * @param x1 coordinate of any corner of the rectangle
     * @param y1 (see x1)
     * @param x2 coordinate of the opposite corner
     * @param y2 (see x2)
     */
    private static Rectangle createRectangle(int x1, int y1, int x2, int y2) {
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

    private static List<Rectangle> getObstaclesBoundedBy(final List<Rectangle> obstacles, final Rectangle subrectangle, final Rectangle pivot) {
        List<Rectangle> ret = new ArrayList<Rectangle>();
        for (Rectangle obstacle : obstacles) {
            if (obstacle != null && subrectangle.intersectsWith(obstacle) && !pivot.equals(obstacle)) {
                ret.add(obstacle);
            }
        }
        return ret;
    }

    private static boolean isNotContainedByAnyObstacle(final Rectangle subrectangle, final List<Rectangle> obstacles) {
        for (Rectangle obstacle : obstacles) {
            if (obstacle.contains(subrectangle)) {
                return false;
            }
        }
        return true;
    }

// -------------------------- PUBLIC METHODS --------------------------

    public List<Rectangle> analyze() {
        final List<Rectangle> foundWhitespace = findWhitespace(documentBounds);

        return selectUsefulWhitespace(foundWhitespace);
    }

// -------------------------- OTHER METHODS --------------------------

    @SuppressWarnings({"ObjectAllocationInLoop"})
    private Rectangle findNextWhitespace(final PriorityQueue<QueueEntry> queue, final int numAlreadyFound) {
        /* this will always choose the rectangle with the highest priority */
        while (!queue.isEmpty()) {
            final QueueEntry current = queue.remove();

            /* if we have found and marked whitespace since we added this rectangle we need to recalculate the obstacles it
                references to make sure it doesnt overlap with the ones we already have */
            if (current.numberOfObstaclesFound != numAlreadyFound) {
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
                    queue.add(new QueueEntry(subrectangle, obstaclesForSubrectangle, numAlreadyFound));
                }
            }
        }

        /* if we ran out of rectangles in the queue, return null to signal that. */
        //noinspection ReturnOfNull
        return null;
    }

    private List<Rectangle> findWhitespace(final Rectangle outerBound) {
        final long t0 = System.currentTimeMillis();

        List<Rectangle> ret = new ArrayList<Rectangle>(numWhitespacesToBeFound);
        PriorityQueue<QueueEntry> queue = new PriorityQueue<QueueEntry>(allObstacles.size());

        /* first add the whole page */
        queue.add(new QueueEntry(outerBound, allObstacles, ret.size()));

        /* continue looking for whitespace until we have the wanted number or we run out*/
        while (ret.size() < numWhitespacesToBeFound) {
            final Rectangle newRectangle = findNextWhitespace(queue, ret.size());

            /* if no further rectangles exist stop looking */
            if (newRectangle == null) {
                break;
            }

            ret.add(newRectangle);
            allObstacles.add(newRectangle);
        }
        System.out.println("findWhitespace took " + (System.currentTimeMillis() - t0) + " ms.");
        return ret;
    }

    /**
     * Checks all the obstacles which are newer than this entry, and sees if they intersects with
     *  the bound of the entry. if so, those obstacles are added to the entrys list of obstacles.
     * @param entry
     */
    private void updateObstacleListForQueueEntry(final QueueEntry entry) {
        int numNewestObstaclesToCheck = allObstacles.size() - originalObstacles - entry.numberOfObstaclesFound;

        for (int i = 0; i < numNewestObstaclesToCheck; i++) {
            final Rectangle obstacle = allObstacles.get(allObstacles.size() - 1 - i);
            if (entry.bound.intersectsWith(obstacle)) {
                entry.obstacles.add(obstacle);
            }
        }
    }

    protected abstract float rectangleQuality(Rectangle r);

    protected abstract List<Rectangle> selectUsefulWhitespace(final List<Rectangle> foundWhitespace);

// -------------------------- INNER CLASSES --------------------------

    private class QueueEntry implements Comparable<QueueEntry> {
        final Rectangle bound;
        final List<Rectangle> obstacles;
        int numberOfObstaclesFound;

        private QueueEntry(final Rectangle bound, final List<Rectangle> obstacles, int numberOfObstaclesFound) {
            this.bound = bound;
            this.obstacles = obstacles;
            this.numberOfObstaclesFound = numberOfObstaclesFound;
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
