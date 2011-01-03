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

package org.elacin.pdfextract.physical.segmentation.region;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.PhysicalPage;
import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentator;
import org.elacin.pdfextract.style.Style;

import java.util.PriorityQueue;

import static org.elacin.pdfextract.util.Sorting.createSmallestFirstQueue;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 09.12.10
 * Time: 23.24
 * To change this template use File | Settings | File Templates.
 */
public class PageSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PageSegmentator.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static void segmentPageRegionWithSubRegions(PhysicalPage page) {

    /* first separate out regions which obviously belongs together, this will be things like
     *  main head lines. This is what most oftenly breaks out from standard logic */


    createGraphicRegions(page.getGraphics(), page.getMainRegion());

    recursivelySplitRegions(page.getMainRegion());

    combineGraphicsWithTextAround(page.getMainRegion());

    for (PhysicalPageRegion subRegion : page.getMainRegion().getSubregions()) {
        if (subRegion.getContainingGraphic() != null) {
            recursivelySplitRegions(subRegion);
        }
    }
}

// -------------------------- STATIC METHODS --------------------------

private static boolean canCombine(PhysicalPageRegion mainRegion, PhysicalPageRegion graphic,
                                  PhysicalPageRegion otherRegion) {
    if (graphic.equals(otherRegion)) {
        return false;
    }

    if (graphic.getPos().intersectsWith(otherRegion.getPos())) {
        return true;
    }

    if (graphic.getPos().distance(otherRegion.getPos()) <= mainRegion.getMinimumColumnSpacing())
    {
        return true;
    }

    return false;
}

private static void combineGraphicsWithTextAround(PhysicalPageRegion r) {
    for (int i = 0; i < r.getSubregions().size(); i++) {
        PhysicalPageRegion graphic = r.getSubregions().get(i);

        if (graphic.getContainingGraphic() != null) {
            for (PhysicalPageRegion otherRegion : r.getSubregions()) {
                if (canCombine(r, graphic, otherRegion)) {

                    /* if this subregion does not contain others, combine the whole thing */
                    if (otherRegion.getSubregions().isEmpty()) {
                        moveRegionToIntoGraphic(r, graphic, otherRegion);
                        /* restart */
                        i = -1;
                        break;
                    }

                    /* else, consider each subsubregion separately */
                    boolean added = false;
                    for (PhysicalPageRegion subsubRegion : otherRegion.getSubregions()) {
                        if (canCombine(r, graphic, subsubRegion)) {
                            moveRegionToIntoGraphic(otherRegion, graphic, subsubRegion);
                            added = true;
                            break;
                        }
                    }

                    /* if none of the subregions qualified - again the whole thing */
                    if (added) {
                        /* restart */
                        i = -1;
                        break;
                    }
                }
            }
        }
    }
}

/**
 * separate out the content which is contained within a graphic.
 * sort the graphics by smallest, because they might overlap.
 *
 * @param graphics
 * @param r
 */
private static void createGraphicRegions(GraphicSegmentator graphics, PhysicalPageRegion r) {
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

private static void moveRegionToIntoGraphic(PhysicalPageRegion origin,
                                            PhysicalPageRegion destination,
                                            PhysicalPageRegion toBeMoved) {
    assert destination.getContainingGraphic() != null;
    assert origin.getSubregions().contains(toBeMoved);

    origin.removeContent(toBeMoved);
    origin.getSubregions().remove(toBeMoved);

    if (toBeMoved.getPos().intersectsWith(destination)) {
        log.warn("LOG00990:Combined region " + toBeMoved + " with " + destination);
        destination.addContents(toBeMoved.getContents());
    } else {
        log.warn("LOG01010:Moved region " + toBeMoved + " from " + origin + " to " + destination)
        ;
        destination.getSubregions().add(toBeMoved);
    }
}

static void recursivelySplitRegions(PhysicalPageRegion r) {
    /* dont split content within a graphic */
    if (r.getContainingGraphic() != null) {
        return;
    }

    PageRegionSplitBySeparators.splitRegionBySeparators(r);
    PageRegionSplitBySpacing.splitInVerticalColumns(r);
    PageRegionSplitBySpacing.splitHorizontallyBySpacing(r);
    PageRegionSplitBySpacing.splitInVerticalColumns(r);
    PageRegionSplitBySpacing.splitHorizontallyBySpacing(r);

    /* and then its subregions */
    for (int i = 0, subregionsSize = r.getSubregions().size(); i < subregionsSize; i++) {
        PhysicalPageRegion subRegion = r.getSubregions().get(i);
        recursivelySplitRegions(subRegion);
    }
}
}
