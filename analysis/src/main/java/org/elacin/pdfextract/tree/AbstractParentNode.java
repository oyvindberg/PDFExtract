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

import org.elacin.pdfextract.geom.MathUtils;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.logical.text.Role;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 3:16:53 PM To change this
 * template use File | Settings | File Templates.
 */
public abstract class AbstractParentNode<ChildType extends AbstractNode, ParentType extends AbstractParentNode>
        extends AbstractNode<ParentType> {
// ------------------------------ FIELDS ------------------------------

/* a cache of group position */
@Nullable
protected Rectangle posCache;

/* children nodes */
@NotNull
private final List<ChildType> children = new ArrayList<ChildType>();

// --------------------------- CONSTRUCTORS ---------------------------

public AbstractParentNode(@NotNull final ChildType child) {
    addChild(child);
}

public AbstractParentNode() {
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

@NotNull
public Rectangle getPos() {
    if (posCache == null) {
        posCache = MathUtils.findBounds(children);
    }
    return posCache;
}

// ------------------------ CANONICAL METHODS ------------------------

@NotNull
@Override
public String toString() {
    if (toStringCache == null) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getText());
        toStringCache = sb.toString();
    }

    return toStringCache;
}

// ------------------------ OVERRIDING METHODS ------------------------

@SuppressWarnings({"unchecked"})
/* someone explain me how to avoid this  and i would be happy! */
@NotNull
public EnumSet<Role> getRoles() {
    EnumSet<Role> ret = EnumSet.noneOf(Role.class);

    for (ChildType child : children) {
        ret.addAll(child.getRoles());

    }
    return ret;
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public List<ChildType> getChildren() {
    return children;
}

// -------------------------- PUBLIC METHODS --------------------------

public final void addChild(@NotNull final ChildType child) {
    child.invalidateThisAndParents();
    children.add(child);
    child.parent = this;
    child.invalidateThisAndParents();
    Collections.sort(children, getChildComparator());
    child.setRoot(getRoot());
}

public final void addChildren(@NotNull final List<ChildType> newChildren) {
    for (ChildType child : newChildren) {
        child.invalidateThisAndParents();
        children.add(child);
        child.parent = this;
        child.setRoot(getRoot());
    }
    Collections.sort(children, getChildComparator());
    invalidateThisAndParents();
}

@NotNull
public abstract Comparator<ChildType> getChildComparator();

public Style getStyle() {
    /* keep the value from last child*/
    return children.get(children.size() - 1).getStyle();
}

@NotNull
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

    public int compare(@NotNull final ChildType o1, @NotNull final ChildType o2) {
        if (o1.getPos().getY() < o2.getPos().getY()) {
            return -1;
        } else if (o1.getPos().getY() > o2.getPos().getY()) {
            return 1;
        }

        if (o1.getPos().getX() < o2.getPos().getX()) {
            return -1;
        } else if (o1.getPos().getX() > o2.getPos().getX()) {
            return 1;
        }

        return 0;
    }
}
}
