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

package org.elacin.pdfextract.builder;

import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentNode;

import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 11, 2010
 * Time: 11:36:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Analyze {
    // -------------------------- PUBLIC STATIC METHODS --------------------------
    final DocumentNode root;
    private StyleFactory styles;

    public Analyze(final DocumentNode root) {
        this.root = root;
        styles = root.getStyles();
    }

    public void analyze(final Map<Integer, Vector<List<TextPosition>>> textForPage) {
        final StyleFactory sf = root.getStyles();

        /* iterate through all TextPositions */
        for (Iterable<List<TextPosition>> lists : textForPage.values()) {
            for (List<TextPosition> scopedList : lists) {
                /* all TextPositions in scopedList belong in the same (article in a) page, so keep search scope there */

                for (int i = 0; i < scopedList.size(); i++) {
                    /* now for every TextPosition, try to locate another one following in the text with the same
                        style, and another one on the following line. Use the distance between in X and Y values,
                        respectively, to calculate a document-wide mean for that certain style.
                     */

                    final TextPosition text = scopedList.get(i);

                    /* first find one on the same line, but with a higher X value */
                    final TextPosition nextOnSameLine = findNextOnSameLine(scopedList, i);
                }
            }
        }
    }

    // -------------------------- STATIC METHODS --------------------------

    private TextPosition findNextOnSameLine(final List<TextPosition> scopedList, final int currentIndex) {
        TextPosition current = scopedList.get(currentIndex);
        Style currentStyle = styles.getStyleForTextPosition(current);

        int j = currentIndex;

        TextPosition ret = null;
        while (true) {
            j++;
            if (j == scopedList.size()) {
                break;
            }
            final TextPosition xSearch = scopedList.get(j);

            /* if xSearch is no longer on the same line, return */
            if (xSearch.getYDirAdj() > current.getYDirAdj()) break;

        }

        return ret;
    }
}
