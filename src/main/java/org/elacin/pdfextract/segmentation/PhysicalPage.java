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
import org.elacin.pdfextract.HasPosition;
import org.elacin.pdfextract.segmentation.column.LayoutRecognizer;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains physicaltexts and whitespaces
 */
public class PhysicalPage implements HasPosition {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

private final int pageNumber;

/* average font sizes for the page */
private final float avgFontSizeX, avgFontSizeY;

private final List<Figure> figures = new ArrayList<Figure>();
private final List<Picture> pictures = new ArrayList<Picture>();
private final List<PhysicalText> words = new ArrayList<PhysicalText>();

private final RectangleCollection wholePage;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(List<PhysicalText> words,
                    final List<Figure> figures,
                    final List<Picture> pictures,
                    int pageNumber)
{

    this.words.addAll(words);
    this.figures.addAll(figures);
    this.pictures.addAll(pictures);
    this.pageNumber = pageNumber;


    List<PhysicalContent> contentList = new ArrayList<PhysicalContent>();
    contentList.addAll(words);
    contentList.addAll(figures);
    contentList.addAll(pictures);

    /* find bounds and average font sizes for the page */
    float xFontSizeSum = 0.0f, yFontSizeSum = 0.0f;
    for (PhysicalText word : words) {
        xFontSizeSum += word.getStyle().xSize;
        yFontSizeSum += word.getStyle().ySize;
    }
    avgFontSizeX = xFontSizeSum / (float) words.size();
    avgFontSizeY = yFontSizeSum / (float) words.size();

    wholePage = new RectangleCollection(contentList);
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

public Rectangle getPosition() {
    return wholePage.getPosition();
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

public List<PhysicalText> getWords() {
    return words;
}

// -------------------------- PUBLIC METHODS --------------------------

public void addWhitespace(final Collection<WhitespaceRectangle> whitespace) {
    wholePage.addContent(whitespace);
}

public PageNode compileLogicalPage() {
    final LayoutRecognizer layoutRecognizer = new LayoutRecognizer();

    ImageFiltering.filterImages(wholePage, pictures, figures);
    wholePage.addContent(pictures);
    wholePage.addContent(figures);


    /* start off by finding whitespace */
    final List<WhitespaceRectangle> whitespace = layoutRecognizer.findWhitespace(this);
    long t0 = System.currentTimeMillis();
    addWhitespace(whitespace);

    /* then follow the trails left between the whitespace and construct blocks of text from that */
    int blockNum = 0;
    final Rectangle bound = wholePage.getPosition();

    for (int y = (int) bound.getY(); y < (int) bound.getEndY(); y++) {
        final List<PhysicalContent> line = wholePage.findContentAtYIndex(y);

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
        for (PhysicalText word : words) {
            if (word.getBlockNum() == i) {
                paragraphNode.addWord(createWordNode(word.getText()));
            }
        }
        if (!paragraphNode.getChildren().isEmpty()) {
            ret.addChild(paragraphNode);
        }
    }

    //        ret.addColumns(layoutRecognizer.findColumnsForPage(wholePage, ret));
    ret.addWhitespace(whitespace);
    ret.addFigures(figures);
    ret.addPictures(pictures);

    if (log.isInfoEnabled()) {
        log.info("LOG00230:compileLogicalPage took " + (System.currentTimeMillis() - t0) + " ms");
    }
    return ret;
}

public RectangleCollection getContents() {
    return wholePage;
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
        markBothWaysFromCurrent(current, blockNum, wholePage.findContentAtYIndex(y), rotation,
                                fontName);
    }

    for (int x = (int) pos.getX(); x < (int) pos.getEndX(); x++) {
        markBothWaysFromCurrent(current, blockNum, wholePage.findContentAtXIndex(x), rotation,
                                fontName);
    }
    return true;
}
}
