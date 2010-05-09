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

import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.text.Style;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 18, 2010
 * Time: 3:16:53 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractParentNode<ChildType extends AbstractNode, ParentType extends AbstractParentNode> extends AbstractNode<ParentType> {
    // ------------------------------ FIELDS ------------------------------

    /* a cache of group position */
    protected transient Rectangle2D posCache;

    /* children nodes */
    private final List<ChildType> children = new ArrayList<ChildType>();

    // --------------------------- CONSTRUCTORS ---------------------------

    public AbstractParentNode(final ChildType child) {
        addChild(child);
    }

    public AbstractParentNode() {
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public List<ChildType> getChildren() {
        return children;
    }

    // ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        if (toStringCache == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append("{");
            if (getPosition() != null) {
                sb.append("position=").append(getPosition().toString().replace("java.awt.geom.Rectangle2D$Float", "")).append(", ");
            }
            final String text = getText();
            sb.append("'").append(text.substring(0, Math.min(text.length(), 25)));
            //        sb.append("'").append(text);
            if (text.length() > 25) sb.append("...");
            sb.append("'}");
            toStringCache = sb.toString();
        }
        return toStringCache;
    }

    // -------------------------- PUBLIC METHODS --------------------------

    public final void addChild(final ChildType child) {
        child.invalidateThisAndParents();
        children.add(child);
        child.parent = this;
        child.invalidateThisAndParents();
        Collections.sort(children, getChildComparator());
        if (Loggers.getCreateTreeLog().isDebugEnabled()) {
            Loggers.getCreateTreeLog().debug(getClass().getSimpleName() + " : " + toString() + ": Added node " + child);
        }

        child.setRoot(getRoot());
    }

    public abstract boolean addTextNode(TextNode node);

    public abstract void combineChildren();

    public abstract Comparator<ChildType> getChildComparator();

    public Rectangle2D getPosition() {
        if (posCache == null) {
            for (ChildType child : children) {
                if (posCache == null) {
                    posCache = child.getPosition();
                } else {
                    posCache = posCache.createUnion(child.getPosition());
                }
            }
        }

        if (posCache == null) {
            posCache = new Rectangle2D.Float();
        }
        return posCache;
    }

    public Style getStyle() {
        /* keep the value from last child*/
        return children.get(children.size() - 1).getStyle();
    }

    @Override
    public String getText() {
        if (textCache == null) {
            StringBuilder sb = new StringBuilder();

            if (!children.isEmpty()) {
                for (ChildType textNode : children) {
                    sb.append(textNode.getText());
                }
            }
            textCache = sb.toString();
        }

        return textCache;
    }

    // -------------------------- OTHER METHODS --------------------------

    protected void appendLocalInfo(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append(getClass().getSimpleName()).append(": ");
        sb.append(getPosition().toString().replace("java.awt.geom.Rectangle2D$Float", ""));
        sb.append(" ");
        sb.append(":\n");

        for (ChildType child : children) {
            child.appendLocalInfo(sb, indent + 4);
        }
    }

    protected void invalidateThisAndParents() {
        posCache = null;
        textCache = null;
        toStringCache = null;

        if (getParent() != null) {
            getParent().invalidateThisAndParents();
        }
    }

    // -------------------------- INNER CLASSES --------------------------

    /**
     * This comparator will compare two nodes based on their position within a page
     * TODO: add page number here
     */
    protected class StandardNodeComparator implements Comparator<ChildType>, Serializable {
        public int compare(final ChildType o1, final ChildType o2) {
            if (o1.getPosition().getY() < o2.getPosition().getY()) return -1;
            else if (o1.getPosition().getY() > o2.getPosition().getY()) return 1;

            if (o1.getPosition().getX() < o2.getPosition().getX()) return -1;
            else if (o1.getPosition().getX() > o2.getPosition().getX()) return 1;

            return 0;
        }
    }
}
