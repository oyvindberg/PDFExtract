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

package org.elacin.pdfextract;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.segmentation.word.PhysicalWordSegmentator;
import org.elacin.pdfextract.operation.RecognizeRoles;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.pdfbox.PDFTextStripper;
import org.elacin.pdfextract.segmentation.column.ColumnFinder;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Rectangle;

import java.io.IOException;
import java.util.List;

public class Pdf2Xml extends PDFTextStripper {
// ------------------------------ FIELDS ------------------------------

    private DocumentNode root;
    private PhysicalWordSegmentator wordSegment;
    private final Logger log = Loggers.getPdfExtractorLog();

// --------------------------- CONSTRUCTORS ---------------------------

    public Pdf2Xml() throws IOException {
        //        super(ResourceLoader.loadProperties( "Resources/PageDrawer.properties", true ) );
    }

// ------------------------ OVERRIDING METHODS ------------------------

    @Override
    protected void endDocument(final PDDocument pdf) throws IOException {
        root.combineChildren();
        root.combineChildren();
        //        root.combineChildren();
        new RecognizeRoles().doOperation(root);
    }

    @Override
    protected void startDocument(final PDDocument pdf) throws IOException {
        root = new DocumentNode();
        wordSegment = new PhysicalWordSegmentator();
    }

    @Override
    protected void writePage() throws IOException {
        final int width = (int) getCurrentPage().getArtBox().getWidth();
        final int height = (int) getCurrentPage().getArtBox().getHeight();

        for (final List<ETextPosition> textPositions : charactersByArticle) {
            final List<WordNode> words = wordSegment.createWords(root, getCurrentPageNo(), textPositions);
            final List<Rectangle> whitespaces = ColumnFinder.findColumnsFromWordNodes(words, width, height);

            for (WordNode word : words) {
                root.addWord(word);
            }
            words.get(0).getPage().setWhitespaces(whitespaces);

            if (!words.isEmpty()) {
                System.out.println("page = " + words.get(0).getPageNum());
            }
        }
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public DocumentNode getRoot() {
        return root;
    }
}
