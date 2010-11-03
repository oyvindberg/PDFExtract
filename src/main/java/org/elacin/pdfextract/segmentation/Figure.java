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

import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 3, 2010 Time: 4:43:12 PM To change this template
 * use File | Settings | File Templates.
 */
public class Figure extends AssignablePhysicalContent {
// ------------------------------ FIELDS ------------------------------

private final boolean filled;

// --------------------------- CONSTRUCTORS ---------------------------

public Figure(final Rectangle position, boolean filled) {
    super(position);
    this.filled = filled;
}

// ------------------------ OVERRIDING METHODS ------------------------

//@Override
//public boolean isAssignablePhysicalContent() {
//    return isFilled();
//}

@Override
public boolean isFigure() {
    return true;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isFilled() {
    return filled;
}
}
