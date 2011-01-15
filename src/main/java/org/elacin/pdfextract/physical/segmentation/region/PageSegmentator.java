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
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.physical.segmentation.column.LayoutRecognizer;
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentator;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.Sorting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static org.elacin.pdfextract.util.RectangleCollection.Direction.E;
import static org.elacin.pdfextract.util.RectangleCollection.Direction.W;
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

    final LayoutRecognizer layoutRecognizer = new LayoutRecognizer();
    final PhysicalPageRegion mainRegion = page.getMainRegion();

    /* first separate out what is contained by graphics */
    createGraphicRegions(page.getGraphics(), mainRegion);

    /* then by separators */
    PageRegionSplitBySeparators.splitRegionBySeparators(mainRegion, page.getGraphics());

//    PageRegionSplitBySpacing.splitInVerticalColumns(mainRegion);

    /* i have seen some mistakes made on more complicated layouts. For papers that tends to be
    *   the first page, so nudge it a bit in the right diretion there. The reason why it is not
    * general is that it would break text ordering if erroneously applied*/
//    if (page.getPageNumber() == 1) {
//        PageRegionSplitBySpacing.splitHorizontallyBySpacing(mainRegion);
//    }

    /* then perform whitespace analysis of all the regions we have now */
    final List<WhitespaceRectangle> allWhitespaces = new ArrayList<WhitespaceRectangle>();

    recursiveAnalysis(mainRegion, layoutRecognizer, allWhitespaces, page.getGraphics());


//


//    PageRegionSplitBySpacing.splitInVerticalColumns(r);
//    PageRegionSplitBySpacing.splitHorizontallyBySpacing(r);


//    recursivelySplitRegions(mainRegion);

//    combineGraphicsWithTextAround(page.getMainRegion());

//    for (PhysicalPageRegion subRegion : page.getMainRegion().getSubregions()) {
//        if (subRegion.getContainingGraphic() != null) {
//            recursivelySplitRegions(subRegion);
//        }
//    }
}

private static void recursiveAnalysis(PhysicalPageRegion region,
                                      LayoutRecognizer layoutRecognizer,
                                      List<WhitespaceRectangle> allWhitespaces,
                                      GraphicSegmentator graphics) {

    PageRegionSplitBySeparators.splitRegionBySeparators(region, graphics);

//    final List<WhitespaceRectangle> whitespaces = layoutRecognizer.findPossibleColumns(region);
    final List<WhitespaceRectangle> whitespaces = layoutRecognizer.findWhitespace(region);

    final List<WhitespaceRectangle> columnBoundaries = new ArrayList<WhitespaceRectangle>();
    for (WhitespaceRectangle whitespace : whitespaces) {
        final Rectangle pos = whitespace.getPos();

        if (pos.getX() == region.getPos().getX() || pos.getEndX() == region.getPos().getEndX()) {
            continue;
        }
        if (pos.getHeight() < region.getPos().getHeight() / 7.0f) {
            continue;
        }

        final List<PhysicalContent> right = region.searchInDirectionFromOrigin(E, pos, 6.0f);
        final List<PhysicalContent> left = region.searchInDirectionFromOrigin(W, pos, 6.0f);

        if (left.size() > 2 && right.size() + left.size() > 10) {
            log.warn("LOG01050:Considering " + whitespace + " column boundary");
            columnBoundaries.add(whitespace);
        }
    }


//    /* invent some whitespace rectangles in case some columns are missed */
//    int[] num = new int[(int) region.getPos().getWidth()];
//    int offset = (int) region.getPos().getX();
//    for (int x = 0; x < (int) region.getPos().getWidth(); x++) {
//
//        final int realX = x + offset;
//        final List<PhysicalContent> column = region.findContentAtXIndex(realX);
//
//        for (PhysicalContent content : column) {
//            final Rectangle pos = content.getPos();
//            if ((int) pos.getX() == realX || (int) pos.getEndX() == realX) {
//                num[x]++;
//            }
//        }
//    }
//
//    for (int x = 0; x < num.length; x++) {
//        int frequency = num[x];
//        if (frequency > 15) {
//            System.out.println("x = " + (x + offset) + ": frequency " + frequency);
//        }
//    }


    /* adjust columns to real height*/
    final Rectangle rpos = region.getPos();
    for (int i = 0; i < columnBoundaries.size(); i++) {
        final WhitespaceRectangle boundary = columnBoundaries.get(i);

        final float x = boundary.getPos().getMiddleX();
        final float endX = boundary.getPos().getEndX();

        final Rectangle search = new Rectangle(x, rpos.getY(), rpos.getWidth(), rpos.getHeight());
        final List<PhysicalContent> everythingRightOf = region.findContentsIntersectingWith(search);
        Collections.sort(everythingRightOf, Sorting.sortByLowerYThenLowerX);

//        float startY = boundary.getPos().getY();
        float startY = rpos.getY();
        float lastYWithContentRight = region.getPos().getEndY();

        boolean startYFound = false;
        boolean boundaryStarted = false;
        for (int y = (int) rpos.getY() - 1; y <= rpos.getEndY() + 1; y++) {
            boolean xIsUnoccupied = true;
            boolean foundContentRightOfX = false;

            for (PhysicalContent c : everythingRightOf) {
                if (c.getPos().getY() > y || c.getPos().getEndY() < y) {
                    continue;
                }
                if (!foundContentRightOfX && c.getPos().getX() > endX) {
                    foundContentRightOfX = true;
                }

                /* if we find something blocking this row, look further down*/
                if ((c.getPos().getX() < x || c.getPos().getEndX() < endX)
                        && !(c instanceof WhitespaceRectangle)) {
                    xIsUnoccupied = false;
                    break;
                }
            }

            if (xIsUnoccupied) {
                if (!startYFound && foundContentRightOfX) {
                    startYFound = true;
                    startY = y - 1;
                }
                if (!boundaryStarted && y > boundary.getPos().getY()) {
                    boundaryStarted = true;
                }
                if (foundContentRightOfX) {
                    lastYWithContentRight = y;
                }
            } else {
                if (boundaryStarted) {
                    break;
                }
                startYFound = false;
            }
        }

        final Rectangle adjusted = new Rectangle(x, startY, 1.0f, lastYWithContentRight - startY);
        final WhitespaceRectangle newBoundary = new WhitespaceRectangle(adjusted);
        newBoundary.setScore(1000);
        columnBoundaries.set(i, newBoundary);
        whitespaces.remove(boundary);
        whitespaces.add(newBoundary);
    }


    Collections.sort(columnBoundaries, Sorting.sortByLowerX);

    for (int i = 0; i < columnBoundaries.size() - 1; i++) {
        WhitespaceRectangle left = columnBoundaries.get(i);
        WhitespaceRectangle right = columnBoundaries.get(i + 1);
        if (right.getPos().getX() - left.getPos().getX() < 15) {

            if (right.getPos().getHeight() > left.getPos().getHeight()) {
                columnBoundaries.remove(left);
                whitespaces.remove(left);
            } else {
                columnBoundaries.remove(right);
                whitespaces.remove(right);
            }
            i = -1;
        }
    }

    region.addWhitespace(whitespaces);

    Collections.sort(columnBoundaries, Sorting.sortByHigherX);
    for (WhitespaceRectangle boundary : columnBoundaries) {
        final Rectangle bpos = boundary.getPos();

//        Rectangle top = new Rectangle(bpos.getX(), rpos.getY() , rpos.getWidth(),
//                bpos.getEndY() - rpos.getEndY());
//
//        Rectangle left = new Rectangle(rpos.getX(), bpos.getY() +1, bpos.getMiddleX() - rpos.getX
//                (), bpos.getHeight() -1);

        Rectangle right = new Rectangle(bpos.getMiddleX(), bpos.getY() + 1,
                rpos.getEndX() - bpos.getMiddleX(), bpos.getHeight() - 1);

//        region.extractSubRegionFromBound(top);
        region.extractSubRegionFromBound(right);
//        region.extractSubRegionFromBound(left);
    }


    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        recursiveAnalysis(subRegion, layoutRecognizer, allWhitespaces, page.getGraphics());
    }


}

// -------------------------- STATIC METHODS --------------------------

private static void addWhiteSpaceFromRegion(List<WhitespaceRectangle> whitespaces,
                                            PhysicalPageRegion region) {
    whitespaces.addAll(region.getWhitespace());
    for (PhysicalPageRegion subRegion : region.getSubregions()) {
        addWhiteSpaceFromRegion(whitespaces, subRegion);
    }
}


private static boolean canCombine(PhysicalPageRegion mainRegion, PhysicalPageRegion graphic,
                                  PhysicalPageRegion otherRegion) {
    if (graphic.equals(otherRegion)) {
        return false;
    }

    if (graphic.getPos().intersectsWith(otherRegion.getPos())) {
        return true;
    }

    if (graphic.getPos().distance(otherRegion.getPos()) <= mainRegion.getMinimumColumnSpacing()) {
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

}
