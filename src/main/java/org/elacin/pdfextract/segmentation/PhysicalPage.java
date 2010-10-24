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

package org.elacin.pdfextract.segmentation;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.segmentation.column.ColumnFinder;
import org.elacin.pdfextract.segmentation.word.PhysicalText;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class PhysicalPage {
// ------------------------------ FIELDS ------------------------------

private final List<PhysicalText> words;
private final float height;
private final float width;
private final int pageNumber;
private final Logger logger = Logger.getLogger(PhysicalPage.class);

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<PhysicalText> words,
                    final float height,
                    final float width,
                    int pageNumber)
{
    this.height = height;
    this.width = width;
    this.words = words;
    this.pageNumber = pageNumber;

    long t0 = System.currentTimeMillis();

    for (int i = 0, wordsSize = words.size(); i < wordsSize; i++) {
        words.get(i).index = i;
    }

    int counter = 0;
    for (int i = 0; i < 200; i++) {
        for (int y = 0; y < (int) height; y += 10) {
            int yEnd = y + 10;
            final List<PhysicalText> result = findWordsBetweenYValues(y, yEnd);
            counter++;
        }

        for (PhysicalText word : words) {
            final List<PhysicalText> result = findWordsInDirectionFromWord(Direction.E, word, 10);
            counter++;
        }
    }


    logger.warn("Tree construction took "
            + (System.currentTimeMillis() - t0)
            + "ms. counter: "
            + counter);
}

// -------------------------- PUBLIC METHODS --------------------------

public PageNode compileLogicalPage() {
    PageNode ret = new PageNode(pageNumber);
    final List<Rectangle> whitespaces = ColumnFinder.findColumnsFromWordNodes(words, width, height);


    /* use the testpositions to determine column boundaries */

    /* segment w*/
    //        for (WordNode word : words) {
    //            ret.addWord(word);
    //        }

    ret.addWhitespaces(whitespaces);
    return ret;
}

// -------------------------- OTHER METHODS --------------------------

private List<PhysicalText> findWordsBetweenYValues(final int y, final int yEnd) {
    final Rectangle searchRectangle = new Rectangle(0, y, width, yEnd - y);
    final List<PhysicalText> result = new ArrayList<PhysicalText>(50);

    for (PhysicalText word : words) {
        if (searchRectangle.intersectsWith(word.getPosition())) {
            result.add(word);
        }
    }

    return result;
}

private List<PhysicalText> findWordsInDirectionFromWord(Direction dir,
                                                        PhysicalText text,
                                                        int distance)
{
    final List<PhysicalText> ret = new ArrayList<PhysicalText>(50);

    final Rectangle pos = text.getPosition();
    final float x1 = pos.getX() + (float) (dir.xDiff * distance);
    final float w = pos.getWidth();
    final float y1 = pos.getY() + (float) (dir.yDiff * distance);
    final float h = pos.getHeight();
    final Rectangle search = new Rectangle(x1, y1, w, h);

    for (PhysicalText physicalText : words) {
        if (search.intersectsWith(physicalText.getPosition())) {
            ret.add(physicalText);
        }
    }
    ret.remove(text);
    return ret;
}

// -------------------------- ENUMERATIONS --------------------------

enum Direction {
    N(0, 1),
    NE(1, 1),
    E(1, 0),
    SE(1, -1),
    S(0, -1),
    SW(-1, -1),
    W(-1, 0),
    NW(-1, 1);
    int xDiff;
    int yDiff;

    Direction(final int xDiff, final int yDiff) {
        this.xDiff = xDiff;
        this.yDiff = yDiff;
    }
}
}
