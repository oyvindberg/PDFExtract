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
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.Collection;
import java.util.List;

/**
 * Contains physicaltexts and whitespaces
 */
public class PhysicalPage extends RectangleCollection {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

private final int pageNumber;

/* average font sizes for the page */
private final float avgFontSizeX, avgFontSizeY;

private final List<Figure> figures;
private final List<Picture> pictures;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<PhysicalText> words,
                    final List<Figure> figures,
                    final List<Picture> pictures,
                    final float h,
                    final float w,
                    int pageNumber)
{
    super(new Rectangle(0.0F, 0.0F, w, h), words);

    this.figures = figures;
    this.pictures = pictures;

    contents.addAll(figures);
    contents.addAll(pictures);

    this.pageNumber = pageNumber;

    /* find minimum font sizes, and set word indices */
    float xSizeSum = 0.0f, ySizeSum = 0.0f;
    for (final PhysicalText word : words) {
        xSizeSum += word.getStyle().xSize;
        ySizeSum += word.getStyle().ySize;
    }
    avgFontSizeX = xSizeSum / (float) words.size();
    avgFontSizeY = ySizeSum / (float) words.size();
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
    clearCache();
}

public PageNode compileLogicalPage() {
    final LayoutRecognizer layoutRecognizer = new LayoutRecognizer();

    /* start off by finding whitespace */
    final List<WhitespaceRectangle> whitespace = layoutRecognizer.findWhitespace(this);
    long t0 = System.currentTimeMillis();
    addWhitespace(whitespace);

    /* then follow the trails left between the whitespace and construct blocks of text from that */
    int blockNum = 0;
    for (int y = 0; y < (int) getPosition().getHeight(); y++) {
        final List<PhysicalContent> line = findContentAtYIndex(y);

        /* iterate through the line to find possible start of blocks */
        for (PhysicalContent content : line) {
            if (content.isText() && !content.getText().isAssignedBlock()) {
                /* find all connected texts and mark with this blockNum*/
                final PhysicalText text = content.getText();
                markEverythingConnectedFrom(content, blockNum, text.getRotation(), text.style.font);

                blockNum++;
            }
        }
    }

    PageNode ret = new PageNode(pageNumber);
    for (int i = 0; i < blockNum; i++) {
        ParagraphNode paragraphNode = new ParagraphNode();
        for (PhysicalContent content : contents) {
            if (content.isText() && content.getText().getBlockNum() == i) {
                paragraphNode.addWord(createWordNode(content.getText()));
            }
        }
        if (!paragraphNode.getChildren().isEmpty()) {
            ret.addChild(paragraphNode);
        }
    }

    //    ret.addColumns(layoutRecognizer.findColumnsForPage(this, ret));
    ret.addWhitespace(whitespace);
    if (log.isInfoEnabled()) {
        log.info("LOG00230:compileLogicalPage took " + (System.currentTimeMillis() - t0) + " ms");
    }
    ret.addFigures(figures);
    ret.addPictures(pictures);
    return ret;
}

// -------------------------- OTHER METHODS --------------------------

private WordNode createWordNode(final PhysicalText text) {
    return new WordNode(text.getPosition(), pageNumber, text.style, text.content, text.charSpacing);
}

private void markBothWaysFromCurrent(final PhysicalContent current,
                                     final int blockNum,
                                     final List<PhysicalContent> line,
                                     final int rotation,
                                     final String fontName)
{
    final int currentIndex = line.indexOf(current);

    /* left/up*/
    boolean continue_ = true;
    for (int index = currentIndex - 1; index >= 0 && continue_; index--) {
        continue_ &= markEverythingConnectedFrom(line.get(index), blockNum, rotation, fontName);
    }
    /* right / down */
    continue_ = true;
    for (int index = currentIndex + 1; index < line.size() && continue_; index++) {
        continue_ &= markEverythingConnectedFrom(line.get(index), blockNum, rotation, fontName);
    }
}

private boolean markEverythingConnectedFrom(final PhysicalContent current,
                                            final int blockNum,
                                            final int rotation,
                                            final String fontName)
{
    if (!current.isAssignablePhysicalContent()) {
        return false;
    }
    if (current.getAssignablePhysicalContent().isAssignedBlock()) {
        return false;
    }
    if (current.isText() && current.getText().getRotation() != rotation) {
        return false;
    }

    current.getAssignablePhysicalContent().setBlockNum(blockNum);

    final Rectangle pos = current.getPosition();

    /* try searching for texts in all directions */
    for (int y = (int) pos.getY(); y < (int) pos.getEndY(); y++) {
        markBothWaysFromCurrent(current, blockNum, findContentAtYIndex(y), rotation, fontName);
    }

    for (int x = (int) pos.getX(); x < (int) pos.getEndX(); x++) {
        markBothWaysFromCurrent(current, blockNum, findContentAtXIndex(x), rotation, fontName);
    }
    return true;
}
}
