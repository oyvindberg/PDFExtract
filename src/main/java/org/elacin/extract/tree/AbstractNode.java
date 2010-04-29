package org.elacin.extract.tree;


import org.elacin.extract.text.Role;
import org.elacin.extract.text.Style;

import java.awt.*;
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

    // --------------------- GETTER / SETTER METHODS ---------------------

    public ParentType getParent() {
        return parent;
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
