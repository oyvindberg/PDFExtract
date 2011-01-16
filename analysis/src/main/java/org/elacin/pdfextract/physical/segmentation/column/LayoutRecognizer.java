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

import org.apache.log4j.Logger;
import org.elacin.pdfextract.content.PhysicalPageRegion;
import org.elacin.pdfextract.content.WhitespaceRectangle;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 12:54:21 PM To change this
 * template use File | Settings | File Templates.
 */
public class LayoutRecognizer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(LayoutRecognizer.class);

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public List<WhitespaceRectangle> findWhitespace(@NotNull final PhysicalPageRegion region) {
    final long t0 = System.currentTimeMillis();

//    final int numWhitespaces = Math.max(30, Math.min(45, region.getContents().size() / 8));
    final int numWhitespaces = 50;

    AbstractWhitespaceFinder finder = new WhitespaceFinder(region, numWhitespaces,
            region.getMinimumColumnSpacing(), region.getMinimumRowSpacing());

    final List<WhitespaceRectangle> ret = finder.findWhitespace();

    final long time = System.currentTimeMillis() - t0;
    log.warn(String.format("LOG00380:%d of %d whitespaces for %s in %d ms", ret.size(),
            numWhitespaces, region, time));

    return ret;
}
}
