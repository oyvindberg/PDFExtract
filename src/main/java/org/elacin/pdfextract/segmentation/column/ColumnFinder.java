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

import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 12:54:21 PM To change this template use File | Settings | File Templates.
 */
public class ColumnFinder {
    // ------------------------------ FIELDS ------------------------------

    private static final int NUM_WHITESPACES_TO_BE_FOUND = 30;

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    public static List<Rectangle> findColumnsFromWordNodes(final List<WordNode> words, final int width, final int height) {
        final long t0 = System.currentTimeMillis();

        List<Rectangle> obstacles = new ArrayList<Rectangle>();
        for (WordNode word : words) {
            obstacles.add(word.getPosition());
        }

        final VerticalWhitespaceFinder verticalWhitespaceFinder = new VerticalWhitespaceFinder(obstacles, NUM_WHITESPACES_TO_BE_FOUND, width, height);

        final List<Rectangle> ret = verticalWhitespaceFinder.findWhitespace();
        System.out.println("findColumnsFromWordNodes took " + (System.currentTimeMillis() - t0) + " ms.");

//        obstacles.addAll(ret);

        //        AbstractWhitespaceFinder horFinder = new HorizontalWhitespaceFinder(obstacles, 40, width, height);
        //        ret.addAll(horFinder.findWhitespace());

        return ret;
    }
}
