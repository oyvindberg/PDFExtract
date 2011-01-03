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

package org.elacin.pdfextract.physical.content;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.PhysicalPage;
import org.elacin.pdfextract.physical.segmentation.column.LayoutRecognizer;
import org.elacin.pdfextract.physical.segmentation.line.LineSegmentator;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This code is not used now, but its kept around because it might be useful
 */
public class PhysicalPageRegionWhitespace extends PhysicalPageRegion {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPageRegionWhitespace.class);

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPageRegionWhitespace(@NotNull final Collection<? extends PhysicalContent>
contents,
                                    @Nullable final PhysicalContent containedIn,
                                    @NotNull final PhysicalPage page) {
    super(contents, containedIn, page);
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public List<ParagraphNode> createParagraphNodes(@NotNull final LineSegmentator segmentator) {
    /* start off by finding whitespace */
    final List<WhitespaceRectangle> whitespace = new LayoutRecognizer().findWhitespace(this);
    addWhitespace(whitespace);

    /* then follow the trails left between the whitespace and construct blocks of text from that
    */
    int blockNum = 0;
    for (float y = getPos().getY(); y < getPos().getEndY(); y++) {
        final List<PhysicalContent> row = findContentAtYIndex(y);

        /* iterate through the line to find possible start of blocks */
        for (PhysicalContent contentInRow : row) {
            if (contentInRow.isText() && !contentInRow.getPhysicalText().isAssignedBlock()) {
                /* find all connected texts and mark with this blockNum*/
                final PhysicalText text = contentInRow.getPhysicalText();
                markEverythingConnectedFrom(contentInRow, blockNum, text.getRotation());

                blockNum++;
            }
        }
    }

    /* compile paragraphs of text based on the assigned block numbers */
    List<ParagraphNode> ret = new ArrayList<ParagraphNode>();

    for (int i = 0; i < blockNum; i++) {
        //		ParagraphNode paragraphNode = new ParagraphNode();
        /* collect all the words logically belonging to this paragraph */
        List<PhysicalContent> paragraphContent = new ArrayList<PhysicalContent>();
        for (PhysicalContent word : getContents()) {
            if (word.isAssignablePhysicalContent()) {
                if (word.getAssignablePhysicalContent().getBlockNum() == i) {
                    if (word.isText()) {
                        paragraphContent.add(word);
                        //						paragraphNode.addWord(createWordNode(word.getText()));
                    }
                }
            }
        }
        /* then group words in lines */
        final List<LineNode> lines = lineSegmentator.segmentLines(this);
        final ParagraphNode paragraph = new ParagraphNode();
        for (LineNode line : lines) {
            paragraph.addChild(line);
        }
        ret.add(paragraph);
    }

    return ret;
}

// -------------------------- OTHER METHODS --------------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private boolean markEverythingConnectedFrom(@NotNull final PhysicalContent current,
                                            final int blockNum,
                                            final int rotation) {
    if (!current.isAssignablePhysicalContent()) {
        return false;
    }
    if (current.getAssignablePhysicalContent().isAssignedBlock()) {
        return false;
    }
    if (current.isText() && current.getPhysicalText().getRotation() != rotation) {
        return false;
    }

    current.getAssignablePhysicalContent().setBlockNum(blockNum);

    final Rectangle pos = current.getPos();

    /* try searching for texts in all directions */
    for (int y = (int) pos.getY(); y < (int) pos.getEndY(); y++) {
        markBothWaysFromCurrent(current, blockNum, findContentAtYIndex(y), rotation);
    }

    for (int x = (int) pos.getX(); x < (int) pos.getEndX(); x++) {
        markBothWaysFromCurrent(current, blockNum, findContentAtXIndex(x), rotation);
    }
    return true;
}

private void markBothWaysFromCurrent(final PhysicalContent current,
                                     final int blockNum,
                                     @NotNull final List<PhysicalContent> line,
                                     final int rotation) {
    final int currentIndex = line.indexOf(current);

    /* left/up*/
    boolean continue_ = true;
    for (int index = currentIndex - 1; index >= 0 && continue_; index--) {
        continue_ &= markEverythingConnectedFrom(line.get(index), blockNum, rotation);
    }
    /* right / down */
    continue_ = true;
    for (int index = currentIndex + 1; index < line.size() && continue_; index++) {
        continue_ &= markEverythingConnectedFrom(line.get(index), blockNum, rotation);
    }
}
}

