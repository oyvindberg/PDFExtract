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

package org.elacin.pdfextract.physical;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.tree.LayoutRegionNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class PhysicalPage {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

/**
 * The physical page number (ie the sequence encountered in the document)
 */
private final int pageNumber;

/**
 * This initially contains everything on the page. after creating the regions,
 * content will be moved
 * from here. ideally this should be quite empty after the analysis.
 */
@NotNull
private final PhysicalPageRegion mainRegion;

/**
 * Contains all the graphics on the page
 */
@NotNull
private final List<GraphicContent> allGraphics;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(@NotNull List<? extends PhysicalContent> contents,
                    @NotNull final List<GraphicContent> graphics,
                    int pageNumber) {
    this.pageNumber = pageNumber;
    allGraphics = graphics;
    mainRegion = new PhysicalPageRegion(contents, this);
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public List<GraphicContent> getAllGraphics() {
    return allGraphics;
}

@NotNull
public PhysicalPageRegion getMainRegion() {
    return mainRegion;
}

public int getPageNumber() {
    return pageNumber;
}

// -------------------------- OTHER METHODS --------------------------


public List<LayoutRegionNode> createRegionNodes() {
    List<LayoutRegionNode> ret = new ArrayList<LayoutRegionNode>();


    LayoutRegionNode regionNode = new LayoutRegionNode(false);

    List<ParagraphNode> paragraphs = mainRegion.createParagraphNodes();
    for (ParagraphNode paragraph : paragraphs) {
        regionNode.addChild(paragraph);
    }
    if (!regionNode.getChildren().isEmpty()) {
        ret.add(regionNode);
    }


    for (PhysicalPageRegion subRegion : mainRegion.getSubregions()) {
        ret.add(createRegionNode(subRegion));
    }


    return ret;
}

private static LayoutRegionNode createRegionNode(PhysicalPageRegion region) {
    LayoutRegionNode regionNode = new LayoutRegionNode(region.isGraphicalRegion());

    List<ParagraphNode> paragraphs = region.createParagraphNodes();
    for (ParagraphNode paragraph : paragraphs) {
        regionNode.addChild(paragraph);
    }

    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        final LayoutRegionNode subRegionNode = createRegionNode(subRegion);
        if (!subRegionNode.getChildren().isEmpty()) {
            regionNode.addChild(subRegionNode);
        }
    }

    return regionNode;
}
}
