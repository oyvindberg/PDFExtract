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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.builder.WordBuilder;
import org.elacin.pdfextract.operation.RecognizeRoles;
import org.elacin.pdfextract.tree.DocumentNode;

import java.io.IOException;
import java.util.List;

public class Pdf2Xml extends PDFTextStripper {
    // ------------------------------ FIELDS ------------------------------

    private DocumentNode root;
    private WordBuilder newBuilder;


    // --------------------------- CONSTRUCTORS ---------------------------

    public Pdf2Xml() throws IOException {
        //        super(ResourceLoader.loadProperties( "Resources/PageDrawer.properties", true ) );
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public DocumentNode getRoot() {
        return root;
    }

    // ------------------------ OVERRIDING METHODS ------------------------

    @Override
    protected void endDocument(final PDDocument pdf) throws IOException {

        root.combineChildren();
        new RecognizeRoles().doOperation(root);
        getOutput().write(root.printTree());
    }

    @Override
    protected void startDocument(final PDDocument pdf) throws IOException {
        root = new DocumentNode();
        newBuilder = new WordBuilder();

    }

    @Override
    protected void writePage() throws IOException {

        for (final List<TextPosition> textPositions : (List<List<TextPosition>>) charactersByArticle) {
            if (!textPositions.isEmpty()) {
                newBuilder.fillPage(root, getCurrentPageNo(), textPositions);
            }
        }

    }
}
