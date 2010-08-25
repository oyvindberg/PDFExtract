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

    // --------------------------- CONSTRUCTORS ---------------------------

    public Rectangle(final int x, final int y, final int width, final int height) {
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
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

    // ------------------------ CANONICAL METHODS ------------------------

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

    // -------------------------- PUBLIC METHODS --------------------------

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

    public int getEndX() {
        return x + width;
    }

    public int getEndY() {
        return y + height;
    }
}
