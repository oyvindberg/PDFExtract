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

import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 18, 2010
 * Time: 2:31:58 PM
 */

/**
 * This class represents one word read from the PDF, and all the information
 * about that word, including style, position, distance to former word (if
 * this is not the first word on a line, and the textual representation.
 * <p/>
 * Note that all whitespace characters will have been stripped from the text.
 */
public class WordNode extends AbstractNode<LineNode> {
    // ------------------------------ FIELDS ------------------------------

    public final String text;
    protected final Rectangle2D position;
    protected final Style style;
    private final int pageNum;
    private final float wordSpacing;
    private final float charSpacing;

    // --------------------------- CONSTRUCTORS ---------------------------

    public WordNode(final Rectangle2D position, final int pageNum, final Style style, final String text, final float wordSpacing, final float charSpacing) {
        this.position = position;
        this.pageNum = pageNum;
        this.style = style;
        this.text = text;
        this.wordSpacing = wordSpacing;
        this.charSpacing = charSpacing;
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public float getCharSpacing() {
        return charSpacing;
    }

    public int getPageNum() {
        return pageNum;
    }

    public Rectangle2D getPosition() {
        return position;
    }

    public Style getStyle() {
        return style;
    }

    @Override
    public String getText() {
        return text;
    }

    public float getWordSpacing() {
        return wordSpacing;
    }

    // ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        if (toStringCache == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ pos=").append(position.toString().replace("java.awt.geom.Rectangle2D$Float", ""));
            sb.append(", ").append(style);
            if (!roles.isEmpty()) {
                sb.append(", ").append(roles.keySet());
            }
            sb.append(", text='").append(text).append('\'');
            sb.append('}');
            toStringCache = sb.toString();
        }
        return toStringCache;
    }

    // -------------------------- OTHER METHODS --------------------------

    protected void appendLocalInfo(final StringBuilder sb, final int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append(toString());
        sb.append("\n");
    }

    protected void invalidateThisAndParents() {
        toStringCache = null;
        if (getParent() != null) {
            getParent().invalidateThisAndParents();
        }
    }
}
