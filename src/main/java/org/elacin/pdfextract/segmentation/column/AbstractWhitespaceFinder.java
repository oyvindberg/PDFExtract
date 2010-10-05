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

import org.elacin.pdfextract.util.FloatPoint;
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

    public AbstractWhitespaceFinder(final Collection<Rectangle> texts, final int numWhitespacesToBeFound, final int width, final int height) {
        this.numWhitespacesToBeFound = numWhitespacesToBeFound;
        allObstacles = new ArrayList<Rectangle>(texts.size() + numWhitespacesToBeFound);
        documentBounds = new Rectangle(0, 0, width * 100, height * 100);
        originalObstacles = texts.size();
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
            if (queue.size() > 100000) {
                throw new RuntimeException("Way too many elements!");
            }

            final QueueEntry current = queue.remove();

            /* if we have found and marked whitespace since we added this rectangle we need to recalculate the obstacles it
                references to make sure it doesnt overlap with the ones we alreadu have
                */
            final Rectangle pos = current.bound;
            if (current.numberOfObstaclesFound != numAlreadyFound) {
                updateObstacleListForQueueEntry(current);
            }

            /* if none of the obstacles are contained within outerBound, then we have a whitespace rectangle */
            if (current.obstacles.isEmpty()) {
                return pos;
            }

            /* choose an obstacle near the middle of the current rectangle */
            Rectangle pivot = choosePivot(pos, current.obstacles);

            /* create four subrectangles, one on every side of it */
            Rectangle[] subrectangles = new Rectangle[]{Rectangle.getRectangleAlternative(pivot.getX(), pos.getY(), pos.getX(), pos.getEndY()),
                    Rectangle.getRectangleAlternative(pos.getX(), pivot.getY(), pos.getEndX(), pos.getY()),
                    Rectangle.getRectangleAlternative(pos.getEndX(), pos.getY(), pivot.getEndX(), pos.getEndY()),
                    Rectangle.getRectangleAlternative(pos.getX(), pos.getEndY(), pos.getEndX(), pivot.getEndY())};

            /* calculate a quality for each of them (depending on their size), find a list of all
               the obstacles contained within each one, and enqueue them. */
            for (Rectangle subrectangle : subrectangles) {
                if (!subrectangle.containedBy(pos)) {
                    continue;
                }
                if (subrectangle.area() == 0.0f) {
                    continue;
                }

                final List<Rectangle> obstaclesForSubrectangle = getObstaclesBoundedBy(current.obstacles, subrectangle, pivot);

                boolean addRectangle = true;
                for (Rectangle obstacle : obstaclesForSubrectangle) {
                    if (obstacle == null) {
                        break;
                    } else if (obstacle.contains(subrectangle)) {
                        addRectangle = false;
                        break;
                    }
                }

                if (addRectangle) {
                    final QueueEntry queueEntry = new QueueEntry(subrectangle, obstaclesForSubrectangle, numAlreadyFound);
                    if (!queue.contains(queueEntry)) {
                        queue.add(queueEntry);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds the obstacle which is closest to the centre of the rectangle bound
     *
     * @param bound
     * @param obstacles
     * @return
     */
    private Rectangle choosePivot(final Rectangle bound, final List<Rectangle> obstacles) {
        final FloatPoint centrePoint = bound.centre();
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

    private List<Rectangle> findWhitespace(final Rectangle outerBound) {
        System.out.println("outerBound = " + outerBound);

        final long t0 = System.currentTimeMillis();

        List<Rectangle> ret = new ArrayList<Rectangle>(numWhitespacesToBeFound);
        PriorityQueue<QueueEntry> queue = new PriorityQueue<QueueEntry>(allObstacles.size());

        /* first add the whole page */
        queue.add(new QueueEntry(outerBound, allObstacles, ret.size()));

        while (ret.size() < numWhitespacesToBeFound) {
            final long t1 = System.currentTimeMillis();

            final Rectangle newRectangle = findNextWhitespace(queue, ret.size());
            System.out.println("t1 = " + (System.currentTimeMillis() - t1));

            /* if no further rectangles exist stop looking */
            if (newRectangle == null) {
                break;
            }

            ret.add(newRectangle);
            allObstacles.add(newRectangle);
        }
        System.out.println("t0 = " + (System.currentTimeMillis() - t0));
        return ret;
    }

    private List<Rectangle> getObstaclesBoundedBy(final List<Rectangle> obstacles, final Rectangle subrectangle, final Rectangle pivot) {
        List<Rectangle> ret = new ArrayList<Rectangle>();
        for (Rectangle obstacle : obstacles) {
            if (obstacle != null && subrectangle.intersects(obstacle) && !pivot.equals(obstacle)) {
                ret.add(obstacle);
            }
        }
        return ret;
    }

    private void updateObstacleListForQueueEntry(final QueueEntry current) {
        int numNewestObstaclesToCheck = allObstacles.size() - originalObstacles - current.numberOfObstaclesFound;

        for (int i = 0; i < numNewestObstaclesToCheck; i++) {
            final Rectangle obstacle = allObstacles.get(allObstacles.size() - 1 - i);
            if (current.bound.intersects(obstacle)) {
                current.obstacles.add(obstacle);
            }
        }
    }

    protected abstract float rectangleQuality(Rectangle r);

    protected abstract List<Rectangle> selectUsefulWhitespace(final List<Rectangle> foundWhitespace);

// -------------------------- INNER CLASSES --------------------------

    private  class QueueEntry implements Comparable<QueueEntry> {
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
