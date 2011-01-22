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
import org.elacin.pdfextract.content.PhysicalContent;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.style.StyleComparator;
import org.elacin.pdfextract.style.StyleDifference;
import org.elacin.pdfextract.style.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by IntelliJ IDEA. User: elacin Date: 08.12.10 Time: 01.27 To change this template use
 * File | Settings | File Templates.
 */
public class PageRegionSplitBySpacing {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PageRegionSplitBySpacing.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
public static void splitOfTopText(@NotNull PhysicalPageRegion r, float fractionToConsider) {


    final int minimumDistanceToSplit = (int) (Math.max(6.0f, r.getAvgFontSizeY()));

    float lastBoundary = -1000.0f;
    float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
    Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();


    final float startY = r.getPos().getY();
    final float endY = startY + r.getPos().getHeight() * fractionToConsider;

    for (float y = startY; y <= endY; y++) {
        final List<PhysicalContent> row = r.findContentAtYIndex(y);

        workingSet.addAll(row);
        for (PhysicalContent content : row) {
            minX = Math.min(content.getPos().getX(), minX);
            maxX = Math.max(content.getPos().getEndX(), maxX);
        }


        if (row.isEmpty()) {

            if (!TextUtils.listContainsStyledText(workingSet)) {
                continue;
            }


            if ((y - lastBoundary < minimumDistanceToSplit)) {
                continue;
            }

            if (sameStyleOverAndUnderHorizontalLine(r, y, workingSet)) {
                continue;
            }


            if (log.isInfoEnabled()) {
                log.info(String.format("LOG00530:split/hor at y=%s for %s ", y, r));
            }

            r.extractSubRegionFromContent(workingSet);

            workingSet.clear();
            lastBoundary = y;

            /* only extract once */
            return;

        } else {
            lastBoundary = y;
        }
    }

}

private static boolean sameStyleOverAndUnderHorizontalLine(final PhysicalPageRegion r,
                                                           final float y,
                                                           final Set<PhysicalContent> over)
{

    List<PhysicalContent> under = new ArrayList<PhysicalContent>();

    float yIndex = y;
    while (under.isEmpty() && yIndex < r.getPos().getEndY()) {
        under.addAll(r.findContentAtYIndex(yIndex));
        yIndex += 1.0f;

    }

    final Style styleOver = TextUtils.findDominatingStyle(over);
    final Style styleUnder = TextUtils.findDominatingStyle(under);

    return StyleComparator.styleCompare(styleOver, styleUnder) == StyleDifference.SAME_STYLE;

}

}
