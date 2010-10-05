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

import org.elacin.pdfextract.util.Rectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Sep 9, 2010
 * Time: 4:33:59 AM
 * To change this template use File | Settings | File Templates.
 */
class VerticalWhitespaceFinder extends AbstractWhitespaceFinder {
// ------------------------------ FIELDS ------------------------------

    private static final int WHITESPACE_MIN_WIDTH = 300;

// --------------------------- CONSTRUCTORS ---------------------------

    VerticalWhitespaceFinder(final List<Rectangle> texts, final int numWhitespacesToBeFound, final int width, final int height) {
        super(texts, numWhitespacesToBeFound, width, height);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * This is the quality function by which we sort rectangles to choose the 'best' one first.
     * <p/>
     * The current function bases itself on the area of the rectangle, and then heavily prefers high and narrow ones
     *
     * @param r
     * @return
     */
    @Override
    protected float rectangleQuality(final Rectangle r) {
        return r.area() * (float) (r.getHeight() / Math.max(1, r.getWidth()));
    }

    @Override
    protected List<Rectangle> selectUsefulWhitespace() {
        List<Rectangle> ret = new ArrayList<Rectangle>();
        for (Rectangle whitespace : foundWhitespace) {
            if (whitespace.getWidth() <= WHITESPACE_MIN_WIDTH) {
                continue;
            }

            

            ret.add(whitespace);
        }

        return ret;
    }
}
