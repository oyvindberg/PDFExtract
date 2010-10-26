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


import org.elacin.pdfextract.HasPosition;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.text.Role;
import org.elacin.pdfextract.text.Style;

import java.io.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 23, 2010 Time: 7:44:33 AM To change this
 * template use File | Settings | File Templates.
 */
public abstract class AbstractNode<ParentType extends AbstractParentNode>
        implements Serializable, HasPosition
{
// ------------------------------ FIELDS ------------------------------

protected Map<Role, String> roles;
protected ParentType parent;
protected DocumentNode root;

/* a cache of current text */
protected String textCache;

/* a cache of current toString-String */
protected transient String toStringCache;

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
    Loggers.getCreateTreeLog().warn(this + " got assigned role " + r);
    if (roles == null) {
        roles = new EnumMap<Role, String>(Role.class);
    }
    roles.put(r, reason);
}

public PageNode getPage() {
    AbstractNode current = this;
    while (current != null) {
        if (current instanceof PageNode) {
            return (PageNode) current;
        }
        current = current.parent;
    }
    return null;
}

public Set<Role> getRoles() {
    if (roles == null) {
        return null;
    }
    return roles.keySet();
}

public abstract Style getStyle();

public abstract String getText();

public boolean hasRole(Role r) {
    if (roles == null) {
        return false;
    }

    return roles.containsKey(r);
}

public boolean overlapsWith(final HasPosition two) {
    return getPosition().intersectsWith(two.getPosition());
}

public void printTree(String filename) {
    Loggers.getPdfExtractorLog().info("Opening " + filename + " for output");
    PrintStream stream = null;
    try {
        stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(filename, false),
                                                          8192 * 4), false, "UTF-8");
        printTree(stream);
    } catch (Exception e) {
        Loggers.getPdfExtractorLog().error("Could not open output file", e);
    } finally {
        if (stream != null) {
            stream.close();
        }
    }
}

public void printTree(final PrintStream output) {
    long t1 = System.currentTimeMillis();
    try {
        appendLocalInfo(output, 0);
        Loggers.getPdfExtractorLog().info(
                "printTree took " + (System.currentTimeMillis() - t1) + " ms");
    } catch (IOException e) {
        Loggers.getPdfExtractorLog().warn("error while printing tree", e);
    }
}

// -------------------------- OTHER METHODS --------------------------

protected abstract void appendLocalInfo(Appendable sb, int indent) throws IOException;

protected abstract void invalidateThisAndParents();
}
