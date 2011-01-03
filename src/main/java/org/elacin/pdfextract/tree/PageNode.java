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

import org.elacin.pdfextract.physical.content.HasPosition;
import org.elacin.pdfextract.util.Sorting;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 23, 2010 Time: 9:33:52 PM To change this
 * template use File | Settings | File Templates.
 */
public class PageNode extends AbstractParentNode<LayoutRegionNode, DocumentNode> {
// ------------------------------ FIELDS ------------------------------

private final int pageNumber;
@NotNull
private final Map<Color, List<HasPosition>> debugFeatures = new HashMap<Color,
List<HasPosition>>();

// --------------------------- CONSTRUCTORS ---------------------------

public PageNode(int pageNumber) {
    this.pageNumber = pageNumber;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface XmlPrinter ---------------------

@Override
public void writeXmlRepresentation(@NotNull final Appendable out,
                                   final int indent,
                                   final boolean verbose) throws IOException {
    for (int i = 0; i < indent; i++) {
        out.append(" ");
    }
    out.append("<page");
    out.append(" num=\"").append(Integer.toString(pageNumber)).append("\"");
    if (verbose) {
        getPos().writeXmlRepresentation(out, indent, verbose);
    }
    out.append(">\n");

    for (LayoutRegionNode child : getChildren()) {
        child.writeXmlRepresentation(out, indent + 4, verbose);
    }

    for (int i = 0; i < indent; i++) {
        out.append(" ");
    }
    out.append("</page>\n");
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public Map<Color, List<HasPosition>> getDebugFeatures() {
    return debugFeatures;
}

public int getPageNumber() {
    return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

public void addDebugFeatures(final Color color, final List<? extends HasPosition> list) {
    if (!debugFeatures.containsKey(color)) {
        debugFeatures.put(color, new ArrayList<HasPosition>());
    }
    debugFeatures.get(color).addAll(list);
}

/**
 * Returns a Comparator which compares coordinates within a page
 */
@NotNull
@Override
public Comparator<HasPosition> getChildComparator() {
    return Sorting.regionComparator;
}
}
