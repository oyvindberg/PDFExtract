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

import org.elacin.pdfextract.content.PhysicalContent;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.geom.RectangleCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 18.01.11 Time: 22.21 To change this template use
 * File | Settings | File Templates.
 */
public class ContentGrouper {
// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
final List<RectangleCollection> allBlocks = new ArrayList<RectangleCollection>(30);

@NotNull
RectangleCollection currentBlock = new RectangleCollection(new ArrayList<PhysicalContent>(), null);

@NotNull
final PhysicalPageRegion region;

@NotNull
public final Rectangle pos;


public ContentGrouper(@NotNull PhysicalPageRegion region) {
    this.region = region;
    pos = region.getPos();
}


public List<RectangleCollection> findBlocksOfContent() {

    /** if this is contained in a grapic, just output the lines */
    if (region.isGraphicalRegion()) {
        for (PhysicalContent content : region.getContents()) {
            if (content.isGraphic() || content.isText()) {
                currentBlock.addContent(content);
                content.getAssignable().setBlockNum(allBlocks.size());
            }
        }
        allBlocks.add(currentBlock);
        return allBlocks;
    }

    /**
     *  If not, use the whitespace added to the region to determine blocks of text
     * */

    /* follow the trails left between the whitespace and construct blocks of text from that */
    for (float y = pos.getY(); y < pos.getEndY(); y++) {
        final List<PhysicalContent> row = region.findContentAtYIndex(y);

        /* iterate through the line to find possible start of blocks */
        for (PhysicalContent contentInRow : row) {
            if (contentInRow.isAssignable() && !contentInRow.getAssignable().isAssignedBlock()) {
                /* find all connected texts from this*/
                markEverythingConnectedFrom(contentInRow);
                allBlocks.add(currentBlock);
                currentBlock = new RectangleCollection(new ArrayList<PhysicalContent>(), null);
            }
        }
    }
    if (!currentBlock.getContents().isEmpty()) {
        allBlocks.add(currentBlock);
    }

    return allBlocks;
}

// -------------------------- OTHER METHODS --------------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private boolean markEverythingConnectedFrom(@NotNull final PhysicalContent current) {
    if (!current.isAssignable()) {
        return false;
    }
    if (current.getAssignable().isAssignedBlock()) {
        return false;
    }

    if (current.isGraphic() && current.getGraphicContent().isSeparator()) {
        current.getAssignable().setBlockNum(allBlocks.size());
        return false;
    }

    current.getAssignable().setBlockNum(allBlocks.size());
    currentBlock.addContent(current);

    /* try searching for texts in all directions */

    for (int y = (int) current.getPos().getY(); y < (int) current.getPos().getEndY(); y++) {
        markBothWaysFromCurrent(current, region.findContentAtYIndex(y));

    }
    for (int x = (int) current.getPos().getX(); x < (int) current.getPos().getEndX(); x++) {
        markBothWaysFromCurrent(current, region.findContentAtXIndex(x));
    }
    return true;
}

private void markBothWaysFromCurrent(final PhysicalContent current,
                                     @NotNull final List<PhysicalContent> line)
{
    final int currentIndex = line.indexOf(current);
    boolean continue_ = true;

    /* left/up*/
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
