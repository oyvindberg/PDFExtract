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
import org.elacin.pdfextract.builder.NewTextNodeBuilder;
import org.elacin.pdfextract.operation.RecognizeRoles;
import org.elacin.pdfextract.tree.DocumentNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Pdf2Xml extends PDFTextStripper {
    // ------------------------------ FIELDS ------------------------------

    private DocumentNode root;

    private Map<Integer, Vector<List<TextPosition>>> textForPages = new HashMap<Integer, Vector<List<TextPosition>>>();

    // --------------------------- CONSTRUCTORS ---------------------------

    public Pdf2Xml() throws IOException {
        setSortByPosition(true);

        //        super(ResourceLoader.loadProperties( "Resources/PageDrawer.properties", true ) );
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public DocumentNode getRoot() {
        return root;
    }

    // ------------------------ OVERRIDING METHODS ------------------------

    @Override
    protected void endDocument(final PDDocument pdf) throws IOException {
        //        new Analyze(root).analyze(textForPages);

        NewTextNodeBuilder newBuilder = new NewTextNodeBuilder(root);
        /* iterate through all pages of textpositions and add the information to the tree */
        for (final Map.Entry<Integer, Vector<List<TextPosition>>> entry : textForPages.entrySet()) {
            for (final List<TextPosition> textPositions : entry.getValue()) {
                if (!textPositions.isEmpty()) {
                    newBuilder.fillPage(entry.getKey(), textPositions);
                    //                    TextNodeBuilder.fillPage(root, entry.getKey(), textPositions);
                }
            }
        }


        root.combineChildren();
        new RecognizeRoles().doOperation(root);

        textForPages.clear();

        getOutput().write(root.printTree());
    }

    @Override
    protected void startDocument(final PDDocument pdf) throws IOException {
        root = new DocumentNode();
    }

    @Override
    protected void writePage() throws IOException {
        textForPages.put(getCurrentPageNo(), (Vector<List<TextPosition>>) charactersByArticle.clone());
    }
}
