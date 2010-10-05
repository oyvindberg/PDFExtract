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

package org.elacin.pdfextract.util;

import com.infomatiq.jsi.Point;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 19, 2010
 * Time: 9:43:07 PM
 * <p/>
 * A non-mutable rectangle, with union and intercepts bits stolen
 * from javas Rectangle2D. The problem with just using that class
 * was that is isnt available in an integer version.
 */
public class Rectangle implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private final int x, y, width, height;
    private boolean visited = false;

// --------------------------- CONSTRUCTORS ---------------------------

    public Rectangle(final int x, final int y, final int width, final int height) {
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Rectangle rectangle = (Rectangle) o;

        if (height != rectangle.height) return false;
        if (width != rectangle.width) return false;
        if (x != rectangle.x) return false;
        if (y != rectangle.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("pos{");
        sb.append(" x=").append(x);
        sb.append(", y=").append(y);
        sb.append(", w=").append(width);
        sb.append(", h=").append(height);
        sb.append(", endX=").append(x + width);
        sb.append(", endY=").append(y + height);
        sb.append('}');
        return sb.toString();
    }

// -------------------------- PUBLIC STATIC METHODS --------------------------

    /**
     * @param x1 coordinate of any corner of the rectangle
     * @param y1 (see x1)
     * @param x2 coordinate of the opposite corner
     * @param y2 (see x2)
     */
    public static Rectangle getRectangleAlternative(int x1, int y1, int x2, int y2) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);

        int width = Math.max(x1, x2) - x;
        int height = Math.max(y1, y2) - y;
        
        return new Rectangle(x, y, width, height);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(final boolean visited) {
        this.visited = visited;
    }

// -------------------------- PUBLIC METHODS --------------------------

    /**
     * Compute the area of this rectangle.
     *
     * @return The area of this rectangle
     */
    public float area() {
        return (float) width / 100.0f * (float) height / 100.0f;
    }

    public FloatPoint centre() {
        return new FloatPoint(x + (width / 2), y + (height / 2));
    }

    /**
     * Determine whether this rectangle is contained by the passed rectangle
     *
     * @param r The rectangle that might contain this rectangle
     * @return true if the passed rectangle contains this rectangle, false if
     *         it does not
     */
    public boolean containedBy(Rectangle r) {
        return r.getEndX() >= getEndX() && r.x <= x && r.getEndY() >= getEndY() && r.y <= y;
    }

    /**
     * Determine whether this rectangle contains the passed rectangle
     *
     * @param r The rectangle that might be contained by this rectangle
     * @return true if this rectangle contains the passed rectangle, false if
     *         it does not
     */
    public boolean contains(Rectangle r) {
        return getEndX() >= r.getEndX() && x <= r.x && getEndY() >= r.getEndY() && y <= r.y;
    }

      /**
   * Return the distance between this rectangle and the passed point.
   * If the rectangle contains the point, the distance is zero.
   *
   * @param p Point to find the distance to
   *
   * @return distance beween this rectangle and the passed point.
   */
  public float distance(FloatPoint p) {
    float distanceSquared = 0;

    float temp = x - p.x;
    if (temp < 0) {
      temp = p.x - getEndX();
    }

    if (temp > 0) {
      distanceSquared += (temp * temp);
    }

    temp = y - p.y;
    if (temp < 0) {
      temp = p.y - getEndY();
    }

    if (temp > 0) {
      distanceSquared += (temp * temp);
    }

    return (float) Math.sqrt(distanceSquared);
  }

    public int getEndX() {
        return x + width;
    }

    public int getEndY() {
        return y + height;
    }

    /**
     * Determine whether this rectangle intersects the passed rectangle
     *
     * @param other The rectangle that might intersect this rectangle
     * @return true if the rectangles intersect, false if they do not intersect
     */
    public boolean intersects(Rectangle other) {
        return getEndX() >= other.x && x <= other.getEndX() && getEndY() >= other.y && y <= other.getEndY();
    }

    public boolean intersectsWith(Rectangle other) {
        int otherX = other.x;
        int otherY = other.y;
        int otherW = other.width;
        int otherH = other.height;

        if (isEmpty() || otherW <= 0 || otherH <= 0) {
            return false;
        }
        return (otherX + otherW > x && otherY + otherH > y && otherX < x + width && otherY < y + height);
    }

    public boolean isEmpty() {
        return (width <= 0.0f) || (height <= 0.0f);
    }

    /**
     * I stole this code from java.awt.geom.Rectange2D, im sure the details make sense :)
     *
     * @param other
     * @return
     */
    public Rectangle union(Rectangle other) {
        int x1 = Math.min(x, other.x);
        int y1 = Math.min(y, other.y);
        int x2 = Math.max(getEndX(), other.getEndX());
        int y2 = Math.max(getEndY(), other.getEndY());
        if (x2 < x1) {
            int t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y2 < y1) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }
}
