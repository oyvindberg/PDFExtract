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

package org.elacin.extract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.extract.builder.StyleFactory;
import org.elacin.extract.builder.TextNodeBuilder;
import org.elacin.extract.operation.RecognizeRoles;
import org.elacin.extract.text.Style;
import org.elacin.extract.tree.DocumentNode;
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
        //        super(ResourceLoader.loadProperties( "Resources/PageDrawer.properties", true ) );
    }

    public Pdf2Xml(String encoding) throws IOException {
        this();
        outputEncoding = encoding;
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
            if (!textPositions.isEmpty()) {
                TextNodeBuilder.fillPage(root, currentPage, textPositions, styleFactory);
            }
        }
        currentPage++;
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
            Loggers.getTextExtractorLog().info("style = " + style + " was used in " + style.numCharsWithThisStyle + " chars");
        }

        Loggers.getTextExtractorLog().debug("most used style is " + breadtextStyle + " with " + highestNumberOfCharsInStyle + " chars");
    }
}
