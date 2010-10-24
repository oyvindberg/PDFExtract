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

package org.elacin.pdfextract.tree;

import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.util.Rectangle;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 18, 2010
 * Time: 2:31:58 PM
 */

/**
 * This class represents one word read from the PDF, and all the information about that word,
 * including style, position, distance to former word (if this is not the first word on a line, and
 * the textual representation.
 * <p/>
 * Note that all whitespace characters will have been stripped from the text.
 */
public class WordNode extends AbstractNode<LineNode> {
// ------------------------------ FIELDS ------------------------------

public final String text;
protected final Rectangle position;
protected final Style style;
private final int pageNum;
private final float charSpacing;

// --------------------------- CONSTRUCTORS ---------------------------

public WordNode(final Rectangle position,
                final int pageNum,
                final Style style,
                final String text,
                final float charSpacing)
{
    this.position = position;
    this.pageNum = pageNum;
    this.style = style;
    this.text = text;
    this.charSpacing = charSpacing;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface TextWithPosition ---------------------

public Rectangle getPosition() {
    return position;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public String toString() {
    if (toStringCache == null) {
        final StringBuilder sb = new StringBuilder();
        try {
            appendLocalInfo(sb, 0);
        } catch (IOException e) {
            throw new RuntimeException("Could not write string", e);
        }
        toStringCache = sb.toString();
    }
    return toStringCache;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public float getCharSpacing() {
    return charSpacing;
}

public int getPageNum() {
    return pageNum;
}

public Style getStyle() {
    return style;
}

@Override
public String getText() {
    return text;
}

// -------------------------- PUBLIC METHODS --------------------------

public boolean isPartOfSameWordAs(final WordNode nextNode) {
    float distance = nextNode.position.getX() - position.getEndX();
    return distance <= charSpacing;// * 1.01f;
}

// -------------------------- OTHER METHODS --------------------------

protected void appendLocalInfo(final Appendable out, final int indent) throws IOException {
    for (int i = 0; i < indent; i++) {
        out.append(" ");
    }
    out.append("WordNode{");
    out.append("'").append(text).append("\' ");

    out.append(position.toString());
    out.append(", charSpacing=").append(String.valueOf(charSpacing));

    out.append(", style:").append(style.toString());

    if (roles != null) {
        out.append(", ").append(roles.keySet().toString());
    }
    out.append('}');
    out.append("\n");
}

protected void invalidateThisAndParents() {
    textCache = null;
    toStringCache = null;
    if (getParent() != null) {
        getParent().invalidateThisAndParents();
    }
}
}
