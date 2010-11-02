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

import org.elacin.pdfextract.HasPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 2, 2010 Time: 1:20:36 AM To change this template
 * use File | Settings | File Templates.
 */
public class RectangleCollection<R extends HasPosition> implements HasPosition {
// ------------------------------ FIELDS ------------------------------

private final Rectangle bounds;
private final List<R> contents = new ArrayList<R>();

// --------------------------- CONSTRUCTORS ---------------------------

public RectangleCollection(final Rectangle bounds, final Collection<R> contents) {
    this.bounds = bounds;
    this.contents.addAll(contents);
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

@Override
public Rectangle getPosition() {
    return bounds;
}

// -------------------------- PUBLIC METHODS --------------------------

public List<R> searchDirection(Direction dir, HasPosition origin, float distance) {
    final List<R> ret = new ArrayList<R>();

    final Rectangle pos = origin.getPosition();
    final float x = pos.getX() + dir.xDiff * distance;
    final float y = pos.getY() + dir.yDiff * distance;
    final Rectangle search = new Rectangle(x, y, pos.getWidth(), pos.getHeight());

    for (R r : contents) {
        if (search.intersectsWith(r.getPosition())) {
            ret.add(r);
        }
    }
    ret.remove(origin);
    return ret;
}

public float getHeight() {
    return bounds.getHeight();
}

public List<R> getRectangles() {
    return contents;
}

public float getWidth() {
    return bounds.getWidth();
}

// -------------------------- ENUMERATIONS --------------------------

public enum Direction {
    N(0, 1),
    NE(1, 1),
    E(1, 0),
    SE(1, -1),
    S(0, -1),
    SW(-1, -1),
    W(-1, 0),
    NW(-1, 1);
    float xDiff;
    float yDiff;

    Direction(final float xDiff, final float yDiff) {
        this.xDiff = xDiff;
        this.yDiff = yDiff;
    }
}
}
