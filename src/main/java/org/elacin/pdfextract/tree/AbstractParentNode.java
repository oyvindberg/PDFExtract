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
import org.elacin.pdfextract.text.Role;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.util.Rectangle;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 3:16:53 PM To change this
 * template use File | Settings | File Templates.
 */
public abstract class AbstractParentNode<ChildType extends AbstractNode, ParentType extends AbstractParentNode>
        extends AbstractNode<ParentType>
{
// ------------------------------ FIELDS ------------------------------

/* a cache of group position */
protected Rectangle posCache;

/* children nodes */
private final List<ChildType> children = new ArrayList<ChildType>();

// --------------------------- CONSTRUCTORS ---------------------------

public AbstractParentNode(final ChildType child) {
    addChild(child);
}

public AbstractParentNode() {
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

public final Rectangle getPosition() {
    if (posCache == null) {
        for (ChildType child : children) {
            if (posCache == null) {
                posCache = child.getPosition();
            } else {
                posCache = posCache.union(child.getPosition());
            }
        }
    }

    if (posCache == null) {
        posCache = new Rectangle(0.0F, 0.0F, 0.0F, 0.0F);
    }
    return posCache;
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

// ------------------------ OVERRIDING METHODS ------------------------

public Set<Role> getRoles() {
    Set<Role> ret = EnumSet.noneOf(Role.class);
    for (ChildType child : children) {
        if (child.getRoles() != null) {
            ret.addAll(child.getRoles());
        }
    }
    return ret;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public List<ChildType> getChildren() {
    return children;
}

// -------------------------- PUBLIC METHODS --------------------------

public final void addChild(final ChildType child) {
    child.invalidateThisAndParents();
    children.add(child);
    child.parent = this;
    child.invalidateThisAndParents();
    Collections.sort(children, getChildComparator());
    if (Loggers.getCreateTreeLog().isDebugEnabled()) {
        Loggers.getCreateTreeLog().debug(
                getClass().getSimpleName() + " : " + toString() + ": Added node " + child);
    }

    child.setRoot(getRoot());
}

public abstract boolean addWord(WordNode node);

public abstract void combineChildren();

public abstract Comparator<ChildType> getChildComparator();

public Style getStyle() {
    /* keep the value from last child*/
    return children.get(children.size() - 1).getStyle();
}

@Override
public String getText() {
    if (textCache == null) {
        StringBuilder sb = new StringBuilder();

        if (!children.isEmpty()) {
            for (ChildType child : children) {
                sb.append(child.getText());
            }
        }
        textCache = sb.toString();
    }

    return textCache;
}

// -------------------------- OTHER METHODS --------------------------

protected void appendLocalInfo(final Appendable out, final int indent) throws IOException {
    for (int i = 0; i < indent; i++) {
        out.append(" ");
    }
    out.append(getClass().getSimpleName());
    out.append("{").append(getPosition().toString());

    out.append(":\n");

    for (ChildType child : children) {
        child.appendLocalInfo(out, indent + 4);
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
 * This comparator will compare two nodes based on their position within a page TODO: add page
 * number here
 */
protected class StandardNodeComparator implements Comparator<ChildType>, Serializable {
    private static final long serialVersionUID = 3903290320365277004L;

    public int compare(final ChildType o1, final ChildType o2) {
        if (o1.getPosition().getY() < o2.getPosition().getY()) {
            return -1;
        } else if (o1.getPosition().getY() > o2.getPosition().getY()) {
            return 1;
        }

        if (o1.getPosition().getX() < o2.getPosition().getX()) {
            return -1;
        } else if (o1.getPosition().getX() > o2.getPosition().getX()) {
            return 1;
        }

        return 0;
    }
}
}
