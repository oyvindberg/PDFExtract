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

package org.elacin.pdfextract.physical.region;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.content.GraphicContent;
import org.elacin.pdfextract.content.PhysicalPage;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.elacin.pdfextract.content.WhitespaceRectangle;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.geom.Sorting;
import org.elacin.pdfextract.physical.column.LayoutRecognizer;
import org.elacin.pdfextract.physical.graphics.CategorizedGraphics;
import org.elacin.pdfextract.physical.graphics.GraphicSegmentator;
import org.elacin.pdfextract.physical.graphics.GraphicSegmentatorImpl;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static org.elacin.pdfextract.geom.Sorting.createSmallestFirstQueue;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 09.12.10
 * Time: 23.24
 * To change this template use File | Settings | File Templates.
 */
public class PageSegmentator {
// ------------------------------ FIELDS ------------------------------

@NotNull
public static final  LayoutRecognizer layoutRecognizer = new LayoutRecognizer();
private static final Logger           log              = Logger.getLogger(PageSegmentator.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static void segmentPageRegionWithSubRegions(@NotNull PhysicalPage page) {
    final PhysicalPageRegion mainRegion = page.getMainRegion();


    GraphicSegmentator gSeg = new GraphicSegmentatorImpl(mainRegion.getPos().getWidth(), mainRegion.getPos().getHeight());

    final CategorizedGraphics categorizedGraphics = gSeg.segmentGraphicsUsingContentInRegion(page.getAllGraphics(), mainRegion);

    mainRegion.addContents(categorizedGraphics.getContents());


    /* first separate out what is contained by graphics */
    createGraphicRegions(categorizedGraphics, mainRegion);


    /* then perform whitespace analysis of all the regions we have now */

    recursiveAnalysis(mainRegion, categorizedGraphics);
}

// -------------------------- STATIC METHODS --------------------------

/**
 * separate out the content which is contained within a graphic.
 * sort the graphics by smallest, because they might overlap.
 *
 * @param graphics
 * @param r
 */
private static void createGraphicRegions(@NotNull CategorizedGraphics graphics,
                                         @NotNull PhysicalPageRegion r) {
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

private static void recursiveAnalysis(@NotNull PhysicalPageRegion region,
                                      CategorizedGraphics graphics) {
    PageRegionSplitBySeparators.splitRegionBySeparators(region, graphics);


    final List<WhitespaceRectangle> whitespaces = layoutRecognizer.findWhitespace(region);
    region.addWhitespace(whitespaces);

    final List<WhitespaceRectangle> columnBoundaries = LayoutRecognizer.extractColumnBoundaries(region, whitespaces);

    for (WhitespaceRectangle columnBoundary : columnBoundaries) {
        log.info("LOG01050:Region " + region + ", column boundary: " + columnBoundary);
    }

    region.addWhitespace(columnBoundaries);

    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        recursiveAnalysis(subRegion, graphics);
    }

    Collections.sort(columnBoundaries, Sorting.sortByHigherX);
    for (WhitespaceRectangle boundary : columnBoundaries) {
        final Rectangle bpos = boundary.getPos();

        Rectangle right = new Rectangle(bpos.getMiddleX(),
                bpos.getY() + 1,
                region.getPos().getEndX() - bpos.getMiddleX(), bpos.getHeight() - 1);

        region.extractSubRegionFromBound(right);
    }


}
}
