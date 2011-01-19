/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.physical;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.content.*;
import org.elacin.pdfextract.geom.MathUtils;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.geom.Sorting;
import org.elacin.pdfextract.physical.column.ColumnFinder;
import org.elacin.pdfextract.physical.graphics.CategorizedGraphics;
import org.elacin.pdfextract.physical.graphics.GraphicSegmentator;
import org.elacin.pdfextract.physical.graphics.GraphicSegmentatorImpl;
import org.elacin.pdfextract.physical.line.LineSegmentator;
import org.elacin.pdfextract.physical.paragraph.ParagraphSegmentator;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.elacin.pdfextract.geom.Sorting.createSmallestFirstQueue;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 09.12.10 Time: 23.24 To change this template use
 * File | Settings | File Templates.
 */
public class PageSegmentator {
// ------------------------------ FIELDS ------------------------------

@NotNull
private static final Logger log = Logger.getLogger(PageSegmentator.class);


private int regionNumber = 0, blockNumber = 0;
private static final ParagraphSegmentator paragraphSegmentator = new ParagraphSegmentator();

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static PageNode analyzePage(@NotNull PhysicalPage page) {
    final PhysicalPageRegion mainRegion = page.getMainRegion();

    final ParagraphNumberer numberer = new ParagraphNumberer(page.getPageNumber());

    GraphicSegmentator graphicSegmentator = new GraphicSegmentatorImpl(mainRegion.getPos());

    final CategorizedGraphics categorizedGraphics
            = graphicSegmentator.categorizeGraphics(page.getAllGraphics(), mainRegion);

    mainRegion.addContents(categorizedGraphics.getContents());


    /* first separate out what is contained by graphics */
    extractGraphicalRegions(categorizedGraphics, mainRegion);


    /* This will detect column boundaries and split up all regions */
    recursivelyDivide(mainRegion, categorizedGraphics);


    /* first create the page node which will hold everything */
    final PageNode ret = new PageNode(page.getPageNumber());

    final List<ParagraphNode> allParagraphs = new ArrayList<ParagraphNode>();


    createParagraphsForRegion(mainRegion, numberer, allParagraphs);

    ret.addChildren(allParagraphs);

    if (log.isInfoEnabled()) {
        log.info("LOG00940:Page had " + ret.getChildren().size() + " paragraphs");
    }

    return ret;

}

private static void createParagraphsForRegion(final PhysicalPageRegion region,
                                              final ParagraphNumberer numberer,
                                              final List<ParagraphNode> allParagraphs)
{

    numberer.newRegion();

    final ContentGrouper contentGrouper = new ContentGrouper(region);
    final List<List<PhysicalContent>> blocks = contentGrouper.findBlocksOfContent();

    for (List<PhysicalContent> block : blocks) {

        /** start by separating all the graphical content in a block.
         *  This is surely an oversimplification, but it think it should work for our
         *  purposes. This very late combination of graphics will be used to grab all text
         *  which is contained within
         * */
        @Nullable Rectangle graphicBounds = extractNonTextContent(block);


        final List<LineNode> lines = LineSegmentator.createLinesFromBlocks(block,
                                                                           numberer.getPage());


        /** separate out everything related to graphics in this part of the page into a single
         *   paragraph */
        if (graphicBounds != null || region.isGraphicalRegion()) {
            numberer.newParagraph();
            ParagraphNode graphical = new ParagraphNode(true, numberer.getParagraphId(true));

            for (Iterator<LineNode> iterator = lines.iterator(); iterator.hasNext();) {
                final LineNode line = iterator.next();
                if (graphicBounds == null || graphicBounds.intersectsWith(line.getPos())) {
                    graphical.addChild(line);
                    iterator.remove();
                }
            }
            allParagraphs.add(graphical);
        }

        paragraphSegmentator.setMedianVerticalSpacing(region.getMedianOfVerticalDistances());
        /* then add the rest of the paragraphs */
        allParagraphs.addAll(paragraphSegmentator.segmentParagraphsByStyleAndDistance(lines,
                                                                                      numberer));
    }


    final List<PhysicalPageRegion> subregions = region.getSubregions();
    Collections.sort(subregions, Sorting.regionComparator);

    for (PhysicalPageRegion subregion : subregions) {
        createParagraphsForRegion(subregion, numberer, allParagraphs);
    }
}

private static Rectangle extractNonTextContent(final List<PhysicalContent> block) {
    List<PhysicalContent> nontextualContent = new ArrayList<PhysicalContent>();

    for (Iterator<PhysicalContent> iterator = block.iterator(); iterator.hasNext();) {
        final PhysicalContent content = iterator.next();
        if (!content.isText()) {
            nontextualContent.add(content);
            iterator.remove();
        }
    }

    @Nullable Rectangle nonTextualBounds = null;
    if (!nontextualContent.isEmpty()) {
        nonTextualBounds = MathUtils.findBounds(nontextualContent);

    }
    return nonTextualBounds;
}


// -------------------------- STATIC METHODS --------------------------

/**
 * separate out the content which is contained within a graphic. sort the graphics by smallest,
 * because they might overlap.
 *
 * @param graphics
 * @param r
 */
private static void extractGraphicalRegions(@NotNull CategorizedGraphics graphics,
                                            @NotNull PhysicalPageRegion r)
{
    PriorityQueue<GraphicContent> queue = createSmallestFirstQueue(graphics.getContainers());

    while (!queue.isEmpty()) {
        final GraphicContent graphic = queue.remove();
        try {
            r.extractSubRegionFromGraphic(graphic);
        } catch (Exception e) {
            log.info("LOG00320:Could not divide page::" + e.getMessage());
            if (graphic.getPos().area() < r.getPos().area() * 0.4f) {
                if (log.isInfoEnabled()) {
                    log.info("LOG00690:Adding " + graphic + " as content");
                }
                graphic.setCanBeAssigned(true);
                graphic.setStyle(Style.GRAPHIC_IMAGE);
                r.addContent(graphic);
            } else {
                graphics.getGraphicsToRender().remove(graphic);
            }
        }
    }
}

private static void recursivelyDivide(@NotNull PhysicalPageRegion region,
                                      CategorizedGraphics graphics)
{
    PageRegionSplitBySeparators.splitRegionBySeparators(region, graphics);


    final List<WhitespaceRectangle> whitespaces = ColumnFinder.findWhitespace(region);
    region.addWhitespace(whitespaces);

    final List<WhitespaceRectangle> columnBoundaries = ColumnFinder.extractColumnBoundaries(region,
                                                                                            whitespaces);

    for (WhitespaceRectangle columnBoundary : columnBoundaries) {
        log.info("LOG01050:Region " + region + ", column boundary: " + columnBoundary);
    }

    region.addWhitespace(columnBoundaries);

    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        recursivelyDivide(subRegion, graphics);
    }

    Collections.sort(columnBoundaries, Sorting.sortByHigherX);
    for (WhitespaceRectangle boundary : columnBoundaries) {
        final Rectangle bpos = boundary.getPos();

        Rectangle right = new Rectangle(bpos.getMiddleX(), bpos.getY() + 1,
                                        region.getPos().getEndX() - bpos.getMiddleX(),
                                        bpos.getHeight() - 1);

        region.extractSubRegionFromBound(right);
    }


}
}
