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
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentator;
import org.elacin.pdfextract.physical.segmentation.region.PageSegmentator;
import org.elacin.pdfextract.tree.LayoutRegionNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
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
private final GraphicSegmentator graphics;

private final int numberOfWords;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(@NotNull List<? extends PhysicalContent> contents,
                    @NotNull final GraphicSegmentator graphics,
                    int pageNumber) throws Exception {
    this.pageNumber = pageNumber;
    this.graphics = graphics;

    mainRegion = new PhysicalPageRegion(contents, this);
    numberOfWords = contents.size();

    graphics.segmentGraphicsUsingContentInRegion(mainRegion);
    mainRegion.addContents(graphics.getContents());
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public GraphicSegmentator getGraphics() {
    return graphics;
}

@NotNull
public PhysicalPageRegion getMainRegion() {
    return mainRegion;
}

public int getPageNumber() {
    return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public PageNode compileLogicalPage() {
    long t0 = System.currentTimeMillis();


    PageSegmentator.segmentPageRegionWithSubRegions(this);

    PageNode page = new PageNode(pageNumber);

    List<LayoutRegionNode> regions = createRegionNodes();
    for (LayoutRegionNode regionNode : regions) {
        page.addChild(regionNode);
    }
    if (log.isInfoEnabled()) {
        log.info("LOG00940:Page had " + regions.size() + " regions");
    }

    addRenderingInformation(page);

    if (log.isInfoEnabled()) {
        log.info("LOG00230:compileLogicalPage took " + (System.currentTimeMillis() - t0) + " ms");
    }
    return page;
}

// -------------------------- OTHER METHODS --------------------------

private void addRenderingInformation(PageNode page) {
    page.addDebugFeatures(Color.CYAN, graphics.getGraphicsToRender());

    final List<WhitespaceRectangle> whitespaces = new ArrayList<WhitespaceRectangle>();
    addWhiteSpaceFromRegion(whitespaces, mainRegion);
    page.addDebugFeatures(Color.GREEN, whitespaces);
}

private static void addWhiteSpaceFromRegion(List<WhitespaceRectangle> whitespaces,
                                            PhysicalPageRegion region) {
    whitespaces.addAll(region.getWhitespace());
    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        addWhiteSpaceFromRegion(whitespaces, subRegion);
    }
}

private List<LayoutRegionNode> createRegionNodes() {
    List<LayoutRegionNode> ret = new ArrayList<LayoutRegionNode>();


    LayoutRegionNode regionNode = new LayoutRegionNode(false);

    List<ParagraphNode> paragraphs = mainRegion.createParagraphNodes();
    for (ParagraphNode paragraph : paragraphs) {
        System.out.println("paragraph = " + paragraph);
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
    LayoutRegionNode regionNode = new LayoutRegionNode(region.getContainingGraphic() != null);

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
