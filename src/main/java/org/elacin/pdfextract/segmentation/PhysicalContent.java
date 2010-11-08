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

package org.elacin.pdfextract.segmentation;

import org.elacin.pdfextract.HasPosition;
import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 3, 2010 Time: 4:37:52 AM To change this template
 * use File | Settings | File Templates.
 */
public abstract class PhysicalContent implements HasPosition {
// ------------------------------ FIELDS ------------------------------

protected final Rectangle position;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalContent(final Rectangle position) {
    this.position = position;
}

public PhysicalContent(final Iterable<? extends PhysicalContent> contents) {
    /* calculate bounds for this region */
    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

    for (PhysicalContent content : contents) {
        if (content.isText()) {
            minX = Math.min(minX, content.position.getX());
            minY = Math.min(minY, content.position.getY());
            maxX = Math.max(maxX, content.position.getEndX());
            maxY = Math.max(maxY, content.position.getEndY());
        }
    }
    position = new Rectangle(minX, minY, maxX - minX, maxY - minY);
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

public Rectangle getPosition() {
    return position;
}

// -------------------------- PUBLIC METHODS --------------------------

public PhysicalText getText() {
    throw new RuntimeException("not a text");
}

public AssignablePhysicalContent getAssignablePhysicalContent() {
    throw new RuntimeException("not an AssignablePhysicalContent");
}

public boolean isText() {
    return false;
}

public boolean isAssignablePhysicalContent() {
    return false;
}

public boolean isWhitespace() {
    return false;
}

public boolean isFigure() {
    return false;
}

public boolean isPicture() {
    return false;
}


@Override
public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("{position=").append(position);
    sb.append('}');
    return sb.toString();
}
}
