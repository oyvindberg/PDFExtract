/*
 * Copyright 2010-2011 Øyvind Berg (elacin@gmail.com)
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



package org.elacin.pdfextract.logical.operation;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.logical.Operation;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 31.01.11 Time: 10.46 To change this template use
 * File | Settings | File Templates.
 */
public class TagText implements Operation {

// ------------------------------ FIELDS ------------------------------
    private static final Logger log = Logger.getLogger(TagText.class);
    public Style                bodyText;
    public Map<Style, Integer>  styleCounts;

// ------------------------ INTERFACE METHODS ------------------------
// --------------------- Interface Operation ---------------------
    public void doOperation(final DocumentNode root) {

        if (root.getWords().isEmpty() || root.getChildren().isEmpty()) {
            throw new RuntimeException("tried to analyze empty document");
        }

        setStyleCountsFrom(root);
        setBodyText(styleCounts);

        /* create a list of possible styles for headings */
        List<Style> headerCandidates = new ArrayList<Style>(root.getStyles().size());

        for (PageNode page : root.getChildren()) {
            for (ParagraphNode paragraph : page.getChildren()) {
                for (LineNode line : paragraph.getChildren()) {
                    Style lineStyle = line.getStyle();

                    if (headerCandidates.contains(lineStyle) || bodyText.equals(lineStyle)) {
                        continue;
                    }

                    if (canBeHeaderStyle(bodyText, line) || (canBeLineId(line))) {
                        headerCandidates.add(lineStyle);
                    }
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info("LOG01480:headerCandidates = " + headerCandidates);
        }

        /* extract title */
        PageNode            firstPage           = root.getChildren().get(0);
        List<ParagraphNode> firstPageParagraphs = firstPage.getChildren();

        for (int i = 0; i < firstPageParagraphs.size(); i++) {
            final ParagraphNode titleParagraph = firstPageParagraphs.get(i);

            if (headerCandidates.contains(titleParagraph.getStyle())) {

                /* check if the next text logically belongs with this */
                if (i + 1 != firstPageParagraphs.size() - 1) {
                    ParagraphNode peekNext = firstPageParagraphs.get(i + 1);

                    if (peekNext.getStyle().equals(titleParagraph.getStyle())) {
                        firstPage.removeChild(peekNext);
                        titleParagraph.addChildren(peekNext.getChildren());
                    }
                }

                root.setTitle(titleParagraph);
                firstPage.removeChild(titleParagraph);
                headerCandidates.remove(titleParagraph.getStyle());
                log.warn("LOG01430:Title is " + root.getTitle());

                break;
            }
        }

        Style div1     = null,
              div2     = null,
              div3     = null;
        int   divFound = 0;

        for (PageNode p : root.getChildren()) {
            for (ParagraphNode prf : p.getChildren()) {
                Style currentStyle = prf.getStyle();

                if (div3 != null) {
                    continue;
                }

                if (!Character.isDigit(prf.getText().charAt(0))) {
                    continue;
                }

                if (headerCandidates.contains(currentStyle)) {
                    switch (divFound) {
                    case 0 :
                        div1 = currentStyle;

                        break;
                    case 1 :
                        div2 = currentStyle;

                        break;
                    case 2 :
                        div3 = currentStyle;

                        break;
                    default :
                        assert false;
                    }

                    headerCandidates.remove(currentStyle);
                    divFound++;
                }
            }
        }

        for (PageNode pageNode : root.getChildren()) {
            for (ParagraphNode prf : pageNode.getChildren()) {
                if (prf.getStyle().xSize < bodyText.xSize) {
                    WordNode firstWord = prf.getChildren().get(0).getChildren().get(0);
                    char     ch        = firstWord.getText().charAt(0);

                    if (Character.isDigit(ch) || (ch == '*')) {
                        prf.addRole(Role.FOOTNOTE);
                    }
                }
            }
        }

        for (PageNode p : root.getChildren()) {
            for (ParagraphNode prf : p.getChildren()) {
                Style currentStyle = prf.getStyle();
                Role  r            = null;

                if (!Character.isDigit(prf.getText().charAt(0))) {
                    continue;
                }


                if (currentStyle.equals(div1)) {
                    r = Role.DIV1;
                } else if (currentStyle.equals(div2)) {
                    r = Role.DIV2;
                } else if (currentStyle.equals(div3)) {
                    r = Role.DIV3;
                }

                if (r != null) {
                    prf.addRole(r);
                }
            }
        }
    }

// -------------------------- OTHER METHODS --------------------------
    private boolean canBeHeaderStyle(final Style bodyText, final LineNode line) {

        boolean b = line.getStyle().xSize >= bodyText.xSize;

        if (b) {
            log.warn("LOG01450:Line " + line + " can be header style");
        }

        return b;
    }

    private boolean canBeLineId(final LineNode line) {

        boolean fontSame            = bodyText.fontName.equals(line.getStyle().fontName);
        boolean smallerThanBodyText = bodyText.xSize >= line.getStyle().xSize;

        if (fontSame || smallerThanBodyText) {
            return false;
        }

        final String firstWord = line.getText().trim().split("\\s")[0];

        if (firstWord.length() > 3) {
            return false;
        }

        if (Character.isDigit(firstWord.charAt(0)) || firstWord.contains(".")
                || ("abcdABCI".indexOf(firstWord.charAt(0)) != -1)) {
            log.warn("LOG01440:Line " + line + " can be line id");

            return true;
        }

        return false;
    }

    private void setBodyText(final Map<Style, Integer> styleCounts) {

        int maxCount = Integer.MIN_VALUE;

        for (Map.Entry<Style, Integer> entry : styleCounts.entrySet()) {
            if (maxCount < entry.getValue()) {
                maxCount = entry.getValue();
                bodyText = entry.getKey();
            }
        }

        log.warn("LOG01410:Bodytext is " + bodyText);
    }

    private void setStyleCountsFrom(final DocumentNode root) {

        styleCounts = new HashMap<Style, Integer>(root.getStyles().size());

        for (int i = 0; i < root.getStyles().size(); i++) {
            styleCounts.put(root.getStyles().get(i), 0);
        }

        for (WordNode word : root.getWords()) {
            if (!styleCounts.containsKey(word.getStyle())) {
                continue;
            }

            int old = styleCounts.get(word.getStyle());

            styleCounts.put(word.getStyle(), old + word.getText().length());
        }
    }
}
