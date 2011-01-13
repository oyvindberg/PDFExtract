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

import org.elacin.pdfextract.physical.AbstractWhitespaceFinder;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.List;

import static org.elacin.pdfextract.util.RectangleCollection.Direction.E;
import static org.elacin.pdfextract.util.RectangleCollection.Direction.W;

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

//    if (newWhitespace.getPos().getWidth() < 2.0f || newWhitespace.getPos().getHeight() < 2.0f){
//        return false;
//    }

    /** we do not want to accept whitespace rectangles which has only one or two words on each
     side (0 is fine), as these doesn't affect layout and tend to break up small paragraphs
     of text unnecessarily
     */

    /* decrease the size a tiny bit, so we don't include what blocked the rectangle, especially
    *   above and below*/
    Rectangle search = newWhitespace.getPos().getAdjustedBy(-1.0f);

    final float range = 8.0f;
    final List<PhysicalContent> right = region.searchInDirectionFromOrigin(E, search, range);
    int rightCount = 0;
    for (PhysicalContent content : right) {
        if (content.isText()) {
            rightCount++;
        }
    }

    if (rightCount == 1 || rightCount == 2) {
        final List<PhysicalContent> left = region.searchInDirectionFromOrigin(W, search, range);

        int leftCount = 0;
        for (PhysicalContent content : left) {
            if (content.isText()) {
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


//@Override
protected float rectangleQuality(Rectangle r) {
    final Rectangle pos = r.getPos();

    final float temp = Math.abs(
            MathUtils.log(pos.getHeight() / pos.getWidth()) / MathUtils.log(2.0f));

    final float weight;
    if (temp < 3) {
        weight = 0.5f;
    } else if (temp >= 3 && temp <= 5) {
        weight = 2.5f;
    } else if (temp > 5) {
        weight = 1.0f;
    } else {
        weight = 0.1f;
    }

    return MathUtils.sqrt(pos.area() * weight);
}

}
