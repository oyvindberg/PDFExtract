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

import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.LineNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.testng.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 26, 2010
 * Time: 5:47:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestArticle1 {
    // ------------------------------ FIELDS ------------------------------

    private DocumentNode doc;
    private ArrayList<LineNode> lines;

    // -------------------------- PUBLIC METHODS --------------------------

    @BeforeClass(groups = "TestArticle1")
    public void setUp() throws IOException {
        doc = PDFDocumentLoader.readPDF("article1.pdf", "article1_out.xml", 6);
        lines = DocumentNavigator.getLineNodes(doc);
    }

    @Test()
    public void test1() {
        /* not sure if this is possible vart-øydelagt */

        /**
         *
         * content: [|D|P|*|(|D|e|t|)|g|a|m|l|-|e|R|o|m|a|]|v|a|r|t|ø|y|d|e|l|a|g|t|a|v|b|a|r|b|a|r|-|a|-|n|e|.
         *               ^           ^
         * unsorted: [0, -5, 339, -5, -5, -5, -5, -5, 1295, -37, -37, -37, -37, -37, 2000, 1, 1, 1, 1, 700, 1, 1, 1, 98, -3, -3, -3, -3, -3, -3, -3, 800, -21, 999, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18]
         *
         */


        assertExistsLineWithText("[DP *(Det) gaml-e Roma] vart øydelagt av barbar-a-ne.");
    }

    @Test()
    public void test2() {
        assertExistsLineWithText("[DP *(Gli) elefanti di colorebianco] sono estinti.");
    }

    @Test()
    public void test3() {
        assertExistsLineWithText("projections αP. Above the αP projections is the projection that hosts");
    }

    @Test()
    public void test4() {
        assertExistsLineWithText("de-n ny-e forstå-ing-a hennar av seg sjølv");
    }

    // -------------------------- OTHER METHODS --------------------------

    private void assertExistsLineWithText(final String text) {
        for (LineNode line : lines) {
            if (line.getText().equals(text)) {
                return;
            }
        }
        fail("Line \"" + text + "\" was not found");
    }
}
