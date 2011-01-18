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

import org.elacin.pdfextract.geom.HasPosition;
import org.elacin.pdfextract.geom.Sorting;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 23, 2010 Time: 9:33:52 PM To change this
 * template use File | Settings | File Templates.
 */
public class PageNode extends AbstractParentNode<LayoutRegionNode, DocumentNode> {
// ------------------------------ FIELDS ------------------------------

private final int pageNumber;

// --------------------------- CONSTRUCTORS ---------------------------

public PageNode(int pageNumber) {
    this.pageNumber = pageNumber;
}

// ------------------------ OVERRIDING METHODS ------------------------

/**
 * Returns a Comparator which compares coordinates within a page
 */
@NotNull
@Override
public Comparator<HasPosition> getChildComparator() {
    return Sorting.regionComparator;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public int getPageNumber() {
    return pageNumber;
}
}
