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

package org.elacin.pdfextract.builder;

import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Aug 25, 2010 Time: 10:16:25 PM To change this
 * template use File | Settings | File Templates.
 */
public class PageColumn {
// ------------------------------ FIELDS ------------------------------

private final Rectangle pos;
private final int numLinesStart;

// --------------------------- CONSTRUCTORS ---------------------------

public PageColumn(final Rectangle pos, final int numLinesStart) {
    this.pos = pos;
    this.numLinesStart = numLinesStart;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("PageColumn");
    sb.append("{numLinesStart=").append(numLinesStart);
    sb.append(", pos=").append(pos);
    sb.append('}');
    return sb.toString();
}

// --------------------- GETTER / SETTER METHODS ---------------------

public Rectangle getPos() {
    return pos;
}
}
