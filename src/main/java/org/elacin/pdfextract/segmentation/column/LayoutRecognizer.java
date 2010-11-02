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

package org.elacin.pdfextract.segmentation.column;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.segmentation.PhysicalPage;

import java.util.List;

import static org.elacin.pdfextract.Loggers.getInterfaceLog;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 12:54:21 PM To change this
 * template use File | Settings | File Templates.
 */
public class LayoutRecognizer {
// ------------------------------ FIELDS ------------------------------

private static final int NUM_WHITESPACES_TO_BE_FOUND = 50;
private static final Logger log = Logger.getLogger(LayoutRecognizer.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static List<WhitespaceRectangle> findColumnsForPage(final PhysicalPage page) {
    final long t0 = System.currentTimeMillis();

    AbstractWhitespaceFinder vert = new VerticalWhitespaceFinder(page, NUM_WHITESPACES_TO_BE_FOUND,
                                                                 page.getMinFontSizeX(),
                                                                 page.getMinFontSizeY());

    final List<WhitespaceRectangle> whitespace = vert.findWhitespace();

    final long time = System.currentTimeMillis() - t0;
    getInterfaceLog().debug("LOG00200:findColumnsForPage took " + time + " ms.");
    return whitespace;
}
}
