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

import org.elacin.pdfextract.segmentation.PhysicalPage;
import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 9, 2010 Time: 4:33:59 AM To change this template
 * use File | Settings | File Templates.
 */
class VerticalWhitespaceFinder extends AbstractWhitespaceFinder {
// --------------------------- CONSTRUCTORS ---------------------------

VerticalWhitespaceFinder(PhysicalPage page,
                         final int numWhitespacesToBeFound,
                         final float whitespaceMinWidth,
                         final float whitespaceMinHeight)
{
    super(page, numWhitespacesToBeFound, whitespaceMinWidth, whitespaceMinHeight);
}

// -------------------------- OTHER METHODS --------------------------

@Override
protected float rectangleQuality(final Rectangle r) {

    //    float aspect = Math.max(r.getHeight() /r.getWidth(), r.getWidth()/r.getHeight()) * 0.2f;

    return r.area() * (1 + r.getHeight() * 0.25f /*/ 2.0f*/);
}
}
