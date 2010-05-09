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


import org.elacin.pdfextract.text.Role;
import org.elacin.pdfextract.text.Style;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 23, 2010
 * Time: 7:44:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractNode<ParentType extends AbstractParentNode> implements Serializable {
    // ------------------------------ FIELDS ------------------------------

    protected final Map<Role, String> roles = new EnumMap<Role, String>(Role.class);
    protected ParentType parent;
    protected DocumentNode root;

    /* a cache of current text */
    protected String textCache;

    /* a cache of current toString-String */
    protected String toStringCache;

    // --------------------- GETTER / SETTER METHODS ---------------------

    public ParentType getParent() {
        return parent;
    }

    public DocumentNode getRoot() {
        return root;
    }

    public void setRoot(final DocumentNode root) {
        this.root = root;
    }

    // -------------------------- PUBLIC METHODS --------------------------

    public void addRole(Role r, String reason) {
        roles.put(r, reason);
    }

    public PageNode getPage() {
        AbstractNode current = this;
        while (current != null) {
            if (current instanceof PageNode) {
                return (PageNode) current;
            }
            current = current.getParent();
        }
        return null;
    }

    public abstract Rectangle2D getPosition();

    public abstract Style getStyle();

    public abstract String getText();

    public boolean hasRole(Role r) {
        return roles.containsKey(r);
    }

    public boolean overlapsWith(final AbstractNode two) {
        return getPosition().intersects(two.getPosition());
    }

    public String printTree() {
        StringBuilder sb = new StringBuilder();
        appendLocalInfo(sb, 0);
        return sb.toString();
    }

    // -------------------------- OTHER METHODS --------------------------

    protected abstract void appendLocalInfo(StringBuilder sb, int indent);

    protected abstract void invalidateThisAndParents();
}
