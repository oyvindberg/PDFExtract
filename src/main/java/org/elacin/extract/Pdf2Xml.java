/**
 * Copyright (c) 2003-2005, www.pdfbox.org
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of pdfbox; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://www.pdfbox.org
 *
 * Slightly modified 2008 by DFKI Language Technology Lab http://www.dfki.de/lt
 *
 */

package org.elacin.extract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.extract.builder.StyleFactory;
import org.elacin.extract.builder.TextNodeBuilder;
import org.elacin.extract.operation.RecognizeRoles;
import org.elacin.extract.text.Style;
import org.elacin.extract.tree.DocumentNode;
import org.elacin.extract.tree.PageNode;
import org.elacin.extract.tree.TextNode;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class Pdf2Xml extends PDFTextStripper {
    // ------------------------------ FIELDS ------------------------------

    private StyleFactory styleFactory = new StyleFactory();
    private int currentPage = 0;
    private Style breadtextStyle = null;
    private DocumentNode root;

    // --------------------------- CONSTRUCTORS ---------------------------

    public Pdf2Xml() throws IOException {
    }

    // ------------------------ OVERRIDING METHODS ------------------------

    @Override
    protected void endDocument(final PDDocument pdf) throws IOException {
        combineTreeNodes();

        findBreadtextStyle();
        new RecognizeRoles(breadtextStyle).doOperation(root);
        getOutput().write(root.printTree());
    }

    @Override
    protected void startDocument(final PDDocument pdf) throws IOException {
        root = new DocumentNode(pdf);
    }

    @Override
    protected void writePage() throws IOException {
        for (final List<TextPosition> textPositions : (Vector<List<TextPosition>>) charactersByArticle) {
            parsePage(textPositions);
        }
    }

    // -------------------------- PUBLIC METHODS --------------------------

    //    private void setMargins() {
    //        for (TextNode leafText : concreteTexts) {
    //            int fragmentsRight = (int) (leafText.getPosition().getX() + leafText.getPosition().getWidth());
    //            if (fragmentsRight > rightMargin) {
    //                this.rightMargin = fragmentsRight;
    //            }
    //
    //            int fragmentsLeft = (int) leafText.getPosition().getX();
    //            if (fragmentsLeft < leftMargin) {
    //                leftMargin = fragmentsLeft;
    //            }
    //        }
    //        log.warn("Found documents margins: left: " + leftMargin + ", right: " + rightMargin);
    //    }


    public void combineTreeNodes() {
        root.combineChildren();
    }

    // -------------------------- OTHER METHODS --------------------------

    void parsePage(final List<TextPosition> textPositions) throws IOException {
        final PageNode pageNode = new PageNode(root.getPdf().getPageFormat(currentPage), currentPage);
        root.addChild(pageNode);
        TextNodeBuilder.fillPage(pageNode, textPositions, styleFactory);
        currentPage++;
    }

    /* for all styles, sum up the number of words which uses it */
    private void findBreadtextStyle() {
        int highestNumberOfCharsInStyle = 0;

        for (TextNode node : root.textNodes) {
            node.getStyle().numCharsWithThisStyle += node.text.length();
        }

        for (Style style : styleFactory.getStyles().keySet()) {
            if (style.numCharsWithThisStyle > highestNumberOfCharsInStyle) {
                highestNumberOfCharsInStyle = style.numCharsWithThisStyle;
                breadtextStyle = style;
            }
            Loggers.getTextExtractorLog().debug("style = " + style + " was used in " + style.numCharsWithThisStyle + " chars");
        }

        Loggers.getTextExtractorLog().debug("most used style is " + breadtextStyle + " with " + highestNumberOfCharsInStyle + " chars");
    }
}