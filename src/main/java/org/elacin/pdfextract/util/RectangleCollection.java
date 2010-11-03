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

import org.elacin.pdfextract.segmentation.PhysicalContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 2, 2010 Time: 1:20:36 AM To change this template
 * use File | Settings | File Templates.
 */
public class RectangleCollection extends PhysicalContent {
// ------------------------------ FIELDS ------------------------------

protected final List<PhysicalContent> contents = new ArrayList<PhysicalContent>();

// --------------------------- CONSTRUCTORS ---------------------------

public RectangleCollection(final Rectangle bounds, final List<? extends PhysicalContent> contents) {
    super(bounds);
    this.contents.addAll(contents);
}

// -------------------------- PUBLIC METHODS --------------------------

public List<PhysicalContent> findRectanglesIntersectingWith(final Rectangle search) {
    final List<PhysicalContent> ret = new ArrayList<PhysicalContent>();
    for (PhysicalContent r : contents) {
        if (search.intersectsWith(r.getPosition())) {
            ret.add(r);
        }
    }
    return ret;
}

public List<PhysicalContent> findTextsAroundPosition(final Rectangle bound) {
    Rectangle searchRectangle = new Rectangle(bound.getX() - 5, bound.getY() - 5,
                                              bound.getWidth() + 5, bound.getHeight() + 5);

    final List<PhysicalContent> ret = findRectanglesIntersectingWith(searchRectangle);
    if (ret.contains(bound)) {
        ret.remove(bound);
    }

    return ret;
}

public List<? extends PhysicalContent> getContent() {
    return contents;
}

public float getHeight() {
    return getPosition().getHeight();
}

public float getWidth() {
    return getPosition().getWidth();
}

public List<PhysicalContent> searchDirection(Direction dir,
                                             PhysicalContent origin,
                                             float distance)
{
    final Rectangle pos = origin.getPosition();
    final float x = pos.getX() + dir.xDiff * distance;
    final float y = pos.getY() + dir.yDiff * distance;
    final Rectangle search = new Rectangle(x, y, pos.getWidth(), pos.getHeight());

    final List<PhysicalContent> ret = findRectanglesIntersectingWith(search);
    ret.remove(origin);
    return ret;
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
