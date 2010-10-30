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

import org.elacin.pdfextract.HasPosition;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:36:44 PM To change this
 * template use File | Settings | File Templates.
 */
public class PhysicalText implements HasPosition {
// ------------------------------ FIELDS ------------------------------


public int index;
final float distanceToPreceeding;
float charSpacing;
final String content;
final Style style;
private final Rectangle position;

// --------------------------- CONSTRUCTORS ---------------------------

PhysicalText(final String content,
             final Style style,
             final float x,
             final float y,
             final float width,
             final float height,
             final float distanceToPreceeding)
{
    this(content, style, new Rectangle(x, y, width, height), distanceToPreceeding);
}

PhysicalText(final String content,
             final Style style,
             final Rectangle position,
             final float distanceToPreceeding)
{
    this.distanceToPreceeding = distanceToPreceeding;
    this.style = style;
    this.content = content;
    this.position = position;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

public Rectangle getPosition() {
    return position;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Text");
    sb.append("{d=").append(distanceToPreceeding);
    sb.append(", pos=").append(position);
    sb.append(", text='").append(content).append('\'');
    sb.append(", style=").append(style);
    sb.append(", charSpacing=").append(charSpacing);
    sb.append('}');
    return sb.toString();
}

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static boolean isCloseEnoughToBelongToSameWord(final PhysicalText otherText) {
    return otherText.distanceToPreceeding <= otherText.charSpacing;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public float getCharSpacing() {
    return charSpacing;
}

public String getContent() {
    return content;
}

public float getDistanceToPreceeding() {
    return distanceToPreceeding;
}

public Style getStyle() {
    return style;
}

public float getAverageCharacterWidth() {
    return getPosition().getWidth() / (float) getContent().length();
}


// -------------------------- PUBLIC METHODS --------------------------

public PhysicalText combineWith(final PhysicalText next) {
    return new PhysicalText(content + next.content, style, position.union(next.position),
                            distanceToPreceeding);
}

public boolean isSameStyleAs(final PhysicalText next) {
    return style.equals(next.style);
}
}
