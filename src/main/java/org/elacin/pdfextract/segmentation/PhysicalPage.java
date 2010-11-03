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
import org.elacin.pdfextract.segmentation.column.LayoutRecognizer;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.*;

/**
 * Contains physicaltexts and whitespaces
 */
public class PhysicalPage extends RectangleCollection {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

private final int pageNumber;

/* average font sizes for the page */
private final float avgFontSizeX, avgFontSizeY;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<PhysicalText> words, final float h, final float w, int pageNumber) {
    super(new Rectangle(0.0F, 0.0F, w, h), words);
    this.pageNumber = pageNumber;

    /* find minimum font sizes, and set word indices */
    float xSizeSum = 0.0f, ySizeSum = 0.0f;
    for (int i = 0, wordsSize = words.size(); i < wordsSize; i++) {
        final PhysicalText word = words.get(i);
        xSizeSum += word.getStyle().xSize;
        ySizeSum += word.getStyle().ySize;
    }
    avgFontSizeX = xSizeSum / (float) words.size();
    avgFontSizeY = ySizeSum / (float) words.size();
}

// -------------------------- STATIC METHODS --------------------------

private static void sortListByXCoordinate(final List<PhysicalContent> result) {
    final Comparator<PhysicalContent> sortByXIndex = new Comparator<PhysicalContent>() {
        @Override
        public int compare(final PhysicalContent o1, final PhysicalContent o2) {
            return Float.compare(o1.getPosition().getX(), o2.getPosition().getX());
        }
    };
    Collections.sort(result, sortByXIndex);
}

// --------------------- GETTER / SETTER METHODS ---------------------

public float getAvgFontSizeX() {
    return avgFontSizeX;
}

public float getAvgFontSizeY() {
    return avgFontSizeY;
}

public int getPageNumber() {
    return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

public void addWhitespace(final Collection<WhitespaceRectangle> whitespace) {
    contents.addAll(whitespace);
}

public PageNode compileLogicalPage() {
    long t0 = System.currentTimeMillis();
    final LayoutRecognizer layoutRecognizer = new LayoutRecognizer();

    final List<WhitespaceRectangle> whitespace = layoutRecognizer.findWhitespace(this);
    addWhitespace(whitespace);


    int num = 0;
    for (int y = 0; y < (int) getPosition().getHeight(); y++) {
        final List<PhysicalContent> line = selectIntersectingWithYIndex(y);
        boolean started = false;
        for (PhysicalContent content : line) {
            if (content.isText()) {
                PhysicalText text = content.getText();
            } else {
            }
        }
    }

    PageNode ret = new PageNode(pageNumber);
    //    ret.addColumns(layoutRecognizer.findColumnsForPage(this, ret));
    ret.addWhitespace(whitespace);
    return ret;
}

public List<PhysicalContent> selectIntersectingWithYIndex(int y) {
    final Rectangle searchRectangle = new Rectangle(0.0F, (float) y, getWidth(), 1.0F);
    final List<PhysicalContent> result = new ArrayList<PhysicalContent>(50);

    for (PhysicalContent content : getContent()) {
        if (searchRectangle.intersectsWith(content.getPosition())) {
            result.add(content);
        }
    }
    sortListByXCoordinate(result);

    return result;
}
}
