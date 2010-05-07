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

package org.elacin.extract.tree;

import org.elacin.extract.text.Style;

import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 18, 2010
 * Time: 2:31:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class TextNode extends AbstractNode<LineNode> {
    // ------------------------------ FIELDS ------------------------------

    public final String text;
    protected final Rectangle2D position;
    protected final Style style;
    private final int pageNum;

    // --------------------------- CONSTRUCTORS ---------------------------

    public TextNode(final Rectangle2D position, final int pageNum, final Style style, final String text) {
        this.position = position;
        this.pageNum = pageNum;
        this.style = style;
        this.text = text;
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

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

    // ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ pos=").append(position.toString().replace("java.awt.geom.Rectangle2D$Float", ""));
        sb.append(", ").append(style);
        if (!roles.isEmpty()) {
            sb.append(", ").append(roles.keySet());
        }
        sb.append(", text='").append(text).append('\'');
        sb.append('}');
        return sb.toString();
    }

    // -------------------------- PUBLIC METHODS --------------------------

    public boolean isSpaceBetweenThisAnd(final TextNode nextNode) {
        if (position.getX() + position.getWidth() + style.widthOfSpace * 0.1 <= nextNode.position.getX()) return true;
        if (text.endsWith(" ")) return true;
        if (nextNode.text.startsWith(" ")) return true;

        return false;
    }

    // -------------------------- OTHER METHODS --------------------------

    protected void appendLocalInfo(final StringBuilder sb, final int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append("{pos=").append(position.toString().replace("java.awt.geom.Rectangle2D$Float", ""));
        sb.append(", ").append(style);
        if (!roles.isEmpty()) {
            sb.append(", ").append(roles.keySet());
        }
        sb.append(", text='").append(text).append('\'');
        sb.append("}\n");
    }

    protected void invalidateThisAndParents() {
        if (getParent() != null) {
            getParent().invalidateThisAndParents();
        }
    }
}
