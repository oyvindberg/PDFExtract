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

import static org.testng.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 11, 2010
 * Time: 7:23:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSpacing {
    private DocumentNode doc;
    private ArrayList<LineNode> lines;

    @BeforeClass(groups = "TestSpacing")
    public void setUp() throws IOException {
        doc = PDFDocumentLoader.readPDF("renderX/spacing.pdf", "spacing_out.xml");
        lines = DocumentNavigator.getLineNodes(doc);
    }

    @Test()
    public void testMinusTwoWordInterval() {
        check(lines.get(56), "In this text, spaces between words are reduced by -2pt In this text, spaces between words are reduced by");
        check(lines.get(57), "-2pt In this text, spaces between words are reduced by -2pt In this text, spaces between words are reduced");
        check(lines.get(58), "by -2pt In this text, spaces between words are reduced by -2pt");
    }

    private void check(final LineNode lineNode, final String s) {
        assertEquals(lineNode.getText(), s, lineNode.toString());
        assertEquals(lineNode.getChildren().size(), s.split(" ").length, lineNode + " has wrong number of TextNodes." + lineNode.toString());
    }

    @Test()
    public void testNormalWordInterval() {
        check(lines.get(51), "In this text, spaces between words are increased by 0pt (i.e. normally spaced). In this text, spaces");
        check(lines.get(52), "between words are increased by 0pt (i.e. normally spaced). In this text, spaces between words are");
        check(lines.get(53), "increased by 0pt (i.e. normally spaced). In this text, spaces between words are increased by 0pt");
        check(lines.get(54), "(i.e. normally spaced). In this text, spaces between words are increased by 0pt (i.e. normally");
        check(lines.get(55), "spaced).");
    }

    @Test()
    public void testPlusSixWordInterval() {
        check(lines.get(43), "In this text, spaces between words are increased by 6pt In this text, spaces between");
        check(lines.get(44), "words are increased by 6pt In this text, spaces between words are increased by");
        check(lines.get(49), "6pt In this text, spaces between words are increased by 6pt In this text, spaces");
        check(lines.get(50), "between words are increased by 6pt");
    }

    @Test()
    public void testPlusTwoWordInterval() {
        check(lines.get(40), "are increased by 2pt In this text, spaces between words are increased by 2pt In this text,");
        check(lines.get(41), "spaces between words are increased by 2pt In this text, spaces between words are increased");
        check(lines.get(42), "by 2pt");
    }

    @Test()
    public void testNormalWordSpace() {
        check(lines.get(34), "In this text, spaces between words are normal. In this text, spaces between words are normal. In");
        check(lines.get(35), "this text, spaces between words are normal. In this text, spaces between words are normal. In this");
        check(lines.get(36), "text, spaces between words are normal. In this text, spaces between words are normal. In this text,");
        check(lines.get(37), "spaces between words are normal. In this text, spaces between words are normal. In this text,");
        check(lines.get(38), "spaces between words are normal. In this text, spaces between words are normal.");
    }

    @Test()
    public void TestMinuxOneCharInterval() {
        check(lines.get(30), "This text has inter-character intervals reduced by -1pt This text has inter-character intervals reduced by -1pt This text has");
        check(lines.get(31), "inter-character intervals reduced by -1pt This text has inter-character intervals reduced by -1pt This text has inter-character");
        check(lines.get(32), "intervals reduced by -1pt This text has inter-character intervals reduced by -1pt");
    }

    @Test()
    public void testNormalCharInterval() {
        check(lines.get(25), "This text has inter-character intervals increased by 0pt (i.e. normally spaced). This text has");
        check(lines.get(26), "inter-character intervals increased by 0pt (i.e. normally spaced). This text has inter-character");
        check(lines.get(27), "intervals increased by 0pt (i.e. normally spaced). This text has inter-character intervals increased");
        check(lines.get(28), "by 0pt (i.e. normally spaced). This text has inter-character intervals increased by 0pt (i.e. normally");
        check(lines.get(29), "spaced). This text has inter-character intervals increased by 0pt (i.e. normally spaced).");
    }

    @Test()
    public void testPlusFourCharInterval() {
        check(lines.get(18), "This text has inter-character intervals increased by");
        check(lines.get(19), "4pt This text has inter-character intervals increased");
        check(lines.get(20), "by 4pt This text has inter-character intervals increased");
        check(lines.get(21), "by 4pt This text has inter-character intervals increased");
        check(lines.get(22), "by 4pt This text has inter-character intervals increased");
        check(lines.get(23), "by 4pt This text has inter-character intervals increased");
        check(lines.get(24), "by 4pt");
    }

    @Test()
    public void testPlusTwoCharInterval() {
        check(lines.get(13), "This text has inter-character intervals increased by 2pt This text has");
        check(lines.get(14), "inter-character intervals increased by 2pt This text has inter-character");
        check(lines.get(15), "intervals increased by 2pt This text has inter-character intervals");
        check(lines.get(16), "increased by 2pt This text has inter-character intervals increased by");
        check(lines.get(17), "2pt This text has inter-character intervals increased by 2pt");
    }

    @Test()
    public void testMinusOneCharInterval() {
        check(lines.get(9), "This text has inter-character intervals increased by 1pt This text has inter-character");
        check(lines.get(10), "intervals increased by 1pt This text has inter-character intervals increased by 1pt");
        check(lines.get(11), "This text has inter-character intervals increased by 1pt This text has inter-character");
        check(lines.get(12), "intervals increased by 1pt This text has inter-character intervals increased by 1pt");
    }

    @Test()
    public void testNormallySpaced() {
        check(lines.get(5), "This text is normally spaced. This text is normally spaced. This text is normally spaced. This text");
        check(lines.get(6), "is normally spaced. This text is normally spaced. This text is normally spaced. This text is normally");
        check(lines.get(7), "spaced. This text is normally spaced. This text is normally spaced. This text is normally spaced.");
        check(lines.get(8), "This text is normally spaced. This text is normally spaced.");
    }

    @Test()
    public void testHeader() {
        check(lines.get(0), "Text Attributes - Character and Word Spacing");
        check(lines.get(1), "Page 1");
        check(lines.get(2), "Text Attributes - Character and Word Spacing");
        check(lines.get(3), "This test contains examples of character and word spacing.");
        check(lines.get(4), "Character spacing:");

        check(lines.get(33), "Word spacing:");

        check(lines.get(45), "© RenderX 2000");
        check(lines.get(46), "XSL Formatting Objects Test Suite");
        check(lines.get(47), "Text Attributes - Character and Word Spacing");
        check(lines.get(48), "Page 2");

    }
}
