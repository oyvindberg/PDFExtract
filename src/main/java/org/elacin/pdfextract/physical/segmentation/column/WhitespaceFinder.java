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

package org.elacin.pdfextract.physical.segmentation.column;

import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 9, 2010 Time: 4:33:59 AM To change this template
 * use File | Settings | File Templates.
 */
class WhitespaceFinder extends AbstractWhitespaceFinder {
// --------------------------- CONSTRUCTORS ---------------------------

WhitespaceFinder(RectangleCollection region,
                 final int numWhitespacesToBeFound,
                 final float whitespaceMinWidth,
                 final float whitespaceMinHeight) {
    super(region, numWhitespacesToBeFound, whitespaceMinWidth, whitespaceMinHeight);
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
protected boolean acceptsRectangle(WhitespaceRectangle newWhitespace) {
    /** if the rectangle is not higher than 25% of the page, check whether it is surrounded
     * on all sides by text. in that case, drop it 
     * */
    if (newWhitespace.getPos().getHeight() < region.getHeight() / 4.0f) {
        if (!isNextToWhitespaceOrEdge(newWhitespace)) {
            return false;
        }
    }

    /** find all the surrounding content. make sure this rectangle is not too small.
     * This is an expensive check, which is why it is done here. i think it is still
     * correct. */
    final List<PhysicalContent> surroundings = region.findSurrounding(newWhitespace, 4);
    if (!surroundings.isEmpty()) {
        float averageHeight = 0.0f;
        for (PhysicalContent surrounding : surroundings) {
            averageHeight += surrounding.getPos().getHeight();
        }
        averageHeight /= (float) surroundings.size();

        if (averageHeight * 0.8f > newWhitespace.getPos().getHeight()) {
            return false;
        }
    }

    /** we do not want to accept whitespace rectangles which has only one or two word on each
     side (0 is fine), as these doesn't affect layout and tend to break up small paragraphs
     of text unnecessarily
     */
    final float range = 8.0f;
    final List<PhysicalContent> right = region.searchInDirectionFromOrigin(RectangleCollection.Direction.E, newWhitespace,
            range);
    int rightCount = 0;
    for (PhysicalContent physicalContent : right) {
        if (physicalContent.isText()) {
            rightCount++;
        }
    }

    if (rightCount == 1 || rightCount == 2) {
        final List<PhysicalContent> left = region.searchInDirectionFromOrigin(RectangleCollection.Direction.W,
                newWhitespace, range);

        int leftCount = 0;
        for (PhysicalContent physicalContent : left) {
            if (physicalContent.isText()) {
                leftCount++;
            }
        }

        if (leftCount == 1 || leftCount == 2) {
            return false;
        }
    }
    return true;
}

// -------------------------- OTHER METHODS --------------------------

@Override
protected float rectangleQuality(@NotNull final Rectangle r) {
    //    float aspect = Math.max(r.getHeight() /r.getWidth(), r.getWidth()/r.getHeight()) * 0.2f;


    final float aspect;
    final Rectangle pos = r.getPos();
    if (pos.getHeight() > pos.getWidth()) {
        aspect = 2.0f * pos.getHeight() / pos.getWidth();
    } else {
        aspect = pos.getWidth() / pos.getHeight();
    }

    return r.area() * aspect;//* (1 + r.getHeight() * 0.25f /*/ 2.0f*/);
}
}
