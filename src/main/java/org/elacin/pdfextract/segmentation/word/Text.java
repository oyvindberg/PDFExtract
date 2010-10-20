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

package org.elacin.pdfextract.segmentation.word;

import org.elacin.pdfextract.text.Style;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:36:44 PM To change this template use File |
 * Settings | File Templates.
 */
class Text {
    // ------------------------------ FIELDS ------------------------------

    final float x, y, width, height, distanceToPreceeding;
    float charSpacing;
    final String content;
    final Style style;

    // --------------------------- CONSTRUCTORS ---------------------------

    Text(final String content, final Style style, final float x, final float y, final float width, final float height, final float distanceToPreceeding) {
        this.distanceToPreceeding = distanceToPreceeding;
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
        this.style = style;
        this.content = content;
    }

    // ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Text");
        sb.append("{d=").append(distanceToPreceeding);
        sb.append(", x=").append(x);
        sb.append(", endX=").append(x + width);
        sb.append(", y=").append(y);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", text='").append(content).append('\'');
        sb.append(", style=").append(style);
        sb.append(", charSpacing=").append(charSpacing);
        sb.append('}');
        return sb.toString();
    }
}
