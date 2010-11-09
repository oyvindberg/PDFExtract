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

import org.apache.log4j.Logger;
import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 3, 2010 Time: 4:43:12 PM To change this template
 * use File | Settings | File Templates.
 */
public class GraphicContent extends AssignablePhysicalContent {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(GraphicContent.class);
private final boolean filled;
private final boolean picture;

private volatile boolean canBeAssigned = false;

// --------------------------- CONSTRUCTORS ---------------------------

public GraphicContent(final Rectangle position, boolean picture, boolean filled) {
    super(position);
    this.filled = filled;
    this.picture = picture;

    if (log.isDebugEnabled()) {
        log.debug("LOG00280:GraphicContent at " + position + ", filled: " + filled + ", picture = "
                + picture);
    }
    ;
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
public boolean isAssignablePhysicalContent() {
    return canBeAssigned;
}

@Override
public boolean isFigure() {
    return true;
}

@Override
public boolean isPicture() {
    return picture;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isFilled() {
    return filled;
}

public void setCanBeAssigned(final boolean canBeAssigned) {
    this.canBeAssigned = canBeAssigned;
}
}
