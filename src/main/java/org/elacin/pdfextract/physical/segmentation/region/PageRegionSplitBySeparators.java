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
import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.util.Rectangle;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 09.12.10
 * Time: 23.36
 * To change this template use File | Settings | File Templates.
 */
public class PageRegionSplitBySeparators {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PageRegionSplitBySeparators.class);

// -------------------------- STATIC METHODS --------------------------

/**
 * Divide the region r by horizontal and vertical separators
 *
 * @param r
 */
static void splitRegionBySeparators(PhysicalPageRegion r) {

    for (int i = 0; i < r.getContents().size(); i++) {

        PhysicalContent c = r.getContents().get(i);
        if (c.isGraphic() && c.getGraphicContent().isHorizontalSeparator()) {
            GraphicContent hsep = c.getGraphicContent();
            if (hsep.getPos().getWidth() < r.getPos().getWidth() * 0.6f) {
                continue;
            }

            /* search to see if this separator does not intersect with anything*/
            Rectangle search = new Rectangle(0, hsep.getPos().getY(), r.getWidth(),
            hsep.getPos().getHeight());

            final List<PhysicalContent> list = r.findContentsIntersectingWith(search);
            if (list.contains(hsep)) {
                list.remove(hsep);
            }
            if (list.isEmpty()) {
                Rectangle everythingAboveSep = new Rectangle(r.getPos().getX(), 0.0f,
                r.getWidth() + 1, hsep.getPos().getY());

                if (log.isInfoEnabled()) {
                    log.info("LOG00880:split/hsep: " + hsep + ",
                    extracting area at: " + everythingAboveSep);
                }

                r.removeContent(hsep);
                i--;

                r.extractSubRegionFromBound(everythingAboveSep);
            }
        } else if (c.isGraphic() && c.getGraphicContent().isVerticalSeparator()) {
            GraphicContent vsep = c.getGraphicContent();

            //todo
        }
    }
}
}
