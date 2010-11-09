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

package org.elacin.pdfextract.segmentation;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Contains physicaltexts and whitespaces
 */
public class PhysicalPage {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

/**
 * The physical page number (ie the sequence encountered in the document)
 */
private final int pageNumber;

/**
 * During analysis this will contain a list of figures which has been confirmed to contain text,
 * before they are separated into PhysicalPageRegions a bit later in the process.
 */
private final List<GraphicContent> graphicalRegions = new ArrayList<GraphicContent>();
/**
 * This contains all images except those which has been dropped (because they are too big), and is
 * only here for rendering purposes.
 */
private final List<GraphicContent> graphicsToRender = new ArrayList<GraphicContent>();

/**
 * The analysis will split the contents into regions based on how things are contained within images
 * or within detected columns.
 */
private final List<PhysicalPageRegion> regions = new ArrayList<PhysicalPageRegion>();

/**
 * This initially contains everything on the page. after creating the regions, content will be moved
 * from here. ideally this should be quite empty after the analysis.
 */
private final PhysicalPageRegion originalWholePage;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<? extends PhysicalContent> contents,
                    final List<GraphicContent> graphicContents,
                    int pageNumber) throws Exception
{
    this.pageNumber = pageNumber;
    originalWholePage = new PhysicalPageRegion(contents, pageNumber);

    /* sort graphics into two categories */
    List<GraphicContent> addToContents = new ArrayList<GraphicContent>();

    for (GraphicContent graphic : graphicContents) {
        if (isTooSmallPicture(originalWholePage, graphic)) {
            graphic.setCanBeAssigned(true);
            addToContents.add(graphic);
            graphicsToRender.add(graphic);
        } else if (isTooBigPicture(originalWholePage, graphic)) {
            //            addToContents.add(graphic);
        } else if (!doesContainSomething(originalWholePage, graphic)) {
            graphic.setCanBeAssigned(true);
            addToContents.add(graphic);
            graphicsToRender.add(graphic);
        } else {
            graphicalRegions.add(graphic);
            graphicsToRender.add(graphic);
        }
    }

    originalWholePage.addContent(addToContents);
    //    graphicalRegions.addAll(graphicContents);
    regions.add(originalWholePage);
}

// -------------------------- STATIC METHODS --------------------------

private static boolean doesContainSomething(final PhysicalPageRegion region,
                                            final GraphicContent graphic)
{
    for (PhysicalContent content : region.getContents()) {
        if (graphic.getPosition().contains(content.getPosition())) {
            return true;
        }
    }
    return false;
}

private static boolean isTooBigPicture(final RectangleCollection region,
                                       final PhysicalContent graphic)
{
    return graphic.getPosition().area() >= region.getPosition().area();
}

private static boolean isTooSmallPicture(final PhysicalPageRegion region,
                                         final GraphicContent graphic)
{
    return graphic.getPosition().getHeight() < region.getAvgFontSizeY()
            || graphic.getPosition().getWidth() < region.getAvgFontSizeX();
}

// --------------------- GETTER / SETTER METHODS ---------------------

public int getPageNumber() {
    return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

public PageNode compileLogicalPage() {

    /** separate out the content which is contained within a graphic. sort them by smallest,
     * because they frequently overlap. filter out the ones which covers the whole page */
    final Comparator<GraphicContent> smallestComparator = new Comparator<GraphicContent>() {
        @Override
        public int compare(final GraphicContent o1, final GraphicContent o2) {
            return Float.compare(o1.getPosition().getHeight(), o2.getPosition().getHeight());
        }
    };
    PriorityQueue<GraphicContent> queue = new PriorityQueue<GraphicContent>(Math.max(1,
                                                                                     graphicalRegions.size()),
                                                                            smallestComparator);
    queue.addAll(graphicalRegions);

    while (!queue.isEmpty()) {
        final GraphicContent graphic = queue.remove();

        try {
            final PhysicalPageRegion region = originalWholePage.getSubRegion(graphic);
            if (null != region) {
                regions.add(region);
                if (log.isInfoEnabled()) {
                    log.info("LOG00340:Added subregion " + region);
                }
            }
        } catch (Exception e) {
            log.warn("LOG00320:Error while dividing page by " + graphic + ":" + e.getMessage());
            graphicsToRender.remove(graphic);
        }
    }

    originalWholePage.addContent(graphicalRegions);


    long t0 = System.currentTimeMillis();
    PageNode ret = new PageNode(pageNumber);

    final List<WhitespaceRectangle> allWhitespace = new ArrayList<WhitespaceRectangle>();
    for (PhysicalPageRegion region : regions) {
        List<ParagraphNode> paragraphs = region.createParagraphNodes();
        for (ParagraphNode paragraph : paragraphs) {
            ret.addChild(paragraph);
        }
    }

    //        ret.addColumns(layoutRecognizer.findColumnsForPage(wholePage, ret));
    ret.addWhitespace(allWhitespace);
    ret.addGraphics(graphicsToRender);

    if (log.isInfoEnabled()) {
        log.info("LOG00230:compileLogicalPage took " + (System.currentTimeMillis() - t0) + " ms");
    }
    return ret;
}

public RectangleCollection getContents() {
    return originalWholePage;
}
}
