/*
 * Copyright 2010 √òyvind Berg (elacin@gmail.com)
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

package org.elacin.pdfextract.physical.paragraph;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.content.PhysicalContent;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.geom.Sorting;
import org.elacin.pdfextract.physical.line.LineSegmentator;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elacin.pdfextract.content.AssignablePhysicalContent.BLOCK_NOT_ASSIGNED;

/**
 * This code is not used now, but its kept around because it might be useful
 */
public class ParagraphSegmentatorWS {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(ParagraphSegmentatorWS.class);

final List<List<PhysicalContent>> blockContents = new ArrayList<List<PhysicalContent>>(30);
final PhysicalPageRegion region;
final Rectangle          pos;
final ParagraphSegmentator paragraphSegmentator = new ParagraphSegmentator();

@NotNull
List<PhysicalContent> block = new ArrayList<PhysicalContent>();

// --------------------------- CONSTRUCTORS ---------------------------

public ParagraphSegmentatorWS(PhysicalPageRegion region) {
    this.region = region;
    pos = region.getPos();
    paragraphSegmentator.setMedianVerticalSpacing(region.getMedianOfVerticalDistances());
}

// -------------------------- STATIC METHODS --------------------------

private static List<Integer> findLineBoundaries(int[] counts) {
    List<Integer> lineBoundaries = new ArrayList<Integer>();
    lineBoundaries.add(0);
    boolean hasFoundText = false;
    for (int i = 0; i < counts.length; i++) {
        if (hasFoundText && counts[i] < 3) {
            boolean isBoundary = true;
            //            for (int j = i +1; j < i +3 && j < counts.length; j++){
            //                if (counts[j] != 0){
            //                    isBoundary = false;
            //                    break;
            //                }
            //            }
            if (isBoundary) {
                lineBoundaries.add(i + 1);
                hasFoundText = false;
            }
        } else if (counts[i] > 0) {
            hasFoundText = true;
        }
    }
    /* add the end as well*/
    lineBoundaries.add(counts.length);

    return lineBoundaries;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public List<ParagraphNode> createParagraphNodes() {
    List<ParagraphNode> ret = new ArrayList<ParagraphNode>();


    /* follow the trails left between the whitespace and construct blocks of text from that */
    for (float y = pos.getY(); y < pos.getEndY(); y++) {
        final List<PhysicalContent> row = region.findContentAtYIndex(y);

        /* iterate through the line to find possible start of blocks */
        for (PhysicalContent contentInRow : row) {
            if (contentInRow.isText() && !contentInRow.getPhysicalText().isAssignedBlock()) {
                /* find all connected texts from this*/
                markEverythingConnectedFrom(contentInRow);
                blockContents.add(block);
                block = new ArrayList<PhysicalContent>(30);
            }
        }
    }

    /* compile paragraphs of text based on the assigned block numbers */
    for (int blockNum = 0; blockNum < blockContents.size(); blockNum++) {
        List<PhysicalContent> block = blockContents.get(blockNum);

        //        /* if the block contains graphics, separate it out as a separate region instead*/
        //        boolean graphics = false;
        //        for (PhysicalContent content : block) {
        //            if (content.isGraphicButNotSeparator()){
        //                graphics = true;
        //                break;
        //            }
        //        }
        //
        //        if (region.getContainingGraphic() == null && graphics){
        //            final Rectangle bounds = TextUtils.findBounds(block);
        //            for (Iterator<PhysicalContent> iterator = block.iterator(); iterator.hasNext();) {
        //                PhysicalContent content = iterator.next();
        //                content.getAssignable().setBlockNum(AssignablePhysicalContent.BLOCK_NOT_ASSIGNED);
        //                if (content.isGraphicButNotSeparator()) {
        //                    iterator.remove();
        //                }
        //            }
        //            GraphicContent fakeCoverGraphic = new GraphicContent(bounds, false, Color.BLACK);
        //            region.extractSubRegionFromContentAndWithGraphics(block, fakeCoverGraphic);
        //            continue;
        //        }

        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        /* reuse the assignment numbers */
        for (PhysicalContent content : block) {
            content.getAssignable().setBlockNum(BLOCK_NOT_ASSIGNED);
            minY = Math.min((int) content.getPos().getY(), minY);
            maxY = Math.max((int) content.getPos().getEndY(), maxY);
        }
        maxY++; //account for rounding
        final int blockHeight = maxY - minY;


        final List<LineNode> lines = new ArrayList<LineNode>();

        LineNode currentLine = new LineNode();


        int[] counts = new int[blockHeight];
        for (PhysicalContent content : block) {
            int contentHeight = (int) content.getPos().getHeight();
            int contentStart = (int) content.getPos().getY();
            int contentWidth = (int) content.getPos().getWidth();

            for (int contentY = 0; contentY < contentHeight; contentY++) {
                counts[contentStart + contentY - minY] += contentWidth;
            }
        }

        List<Integer> lineBoundaries = findLineBoundaries(counts);

        Collections.sort(block, Sorting.sortByLowerY);
        for (int i = 0; i < lineBoundaries.size() - 1; i++) {
            int start = minY + lineBoundaries.get(i) - 1;
            int stop = minY + lineBoundaries.get(i + 1);

            for (PhysicalContent content : block) {
                final Rectangle contentPos = content.getPos();

                if (content.getAssignable().isAssignedBlock()) {
                    continue;
                }

                if (contentPos.getY() > start - 1 && contentPos.getEndY() < stop + 1) {
                    content.getAssignable().setBlockNum(1);

                    if (content.isText()) {
                        currentLine.addChild(LineSegmentator.createWordNode(content.getPhysicalText(), region.getPageNumber()));
                    }
                }
            }

            if (!currentLine.getChildren().isEmpty()) {
                lines.add(currentLine);
                currentLine = new LineNode();
            }
        }

        /* do the final segmentation elsewhere, and add */
        ret.addAll(paragraphSegmentator.segmentParagraphs(lines));
    }

    return ret;
}

// -------------------------- OTHER METHODS --------------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private boolean markEverythingConnectedFrom(@NotNull final PhysicalContent current) {
    if (!current.isAssignablePhysicalContent()) {
        return false;
    }
    if (current.getAssignable().isAssignedBlock()) {
        return false;
    }

    current.getAssignable().setBlockNum(blockContents.size());
    block.add(current);

    final Rectangle pos = current.getPos();

    /* try searching for texts in all directions */
    for (int y = (int) pos.getY(); y < (int) pos.getEndY(); y++) {
        markBothWaysFromCurrent(current, region.findContentAtYIndex(y));
    }

    for (int x = (int) pos.getX(); x < (int) pos.getEndX(); x++) {
        markBothWaysFromCurrent(current, region.findContentAtXIndex(x));
    }
    return true;
}

private void markBothWaysFromCurrent(final PhysicalContent current,
                                     @NotNull final List<PhysicalContent> line) {
    final int currentIndex = line.indexOf(current);

    /* left/up*/
    boolean continue_ = true;
    for (int index = currentIndex - 1; index >= 0 && continue_; index--) {
        continue_ &= markEverythingConnectedFrom(line.get(index));
    }
    /* right / down */
    continue_ = true;
    for (int index = currentIndex + 1; index < line.size() && continue_; index++) {
        continue_ &= markEverythingConnectedFrom(line.get(index));
    }
}
}

