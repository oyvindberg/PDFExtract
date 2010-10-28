/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.pdfbox;

import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.util.ResourceLoader;
import org.apache.pdfbox.util.TextNormalize;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.operation.RecognizeRoles;
import org.elacin.pdfextract.segmentation.PhysicalPage;
import org.elacin.pdfextract.segmentation.word.PhysicalText;
import org.elacin.pdfextract.segmentation.word.PhysicalWordSegmentator;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PDFTextStripper extends PDFStreamEngine {
// ------------------------------ FIELDS ------------------------------

protected final List<ETextPosition> charactersForPage = new ArrayList<ETextPosition>();

private final DocumentNode root;

private int currentPageNo = 0;
private int startPage = 1;
private int endPage = Integer.MAX_VALUE;

/* used to filter out text which is written several times to create a bold effect */
private final
Map<String, List<TextPosition>>
        characterListMapping
        = new HashMap<String, List<TextPosition>>();

/* The normalizer is used to remove text ligatures/presentation forms and to correct
the direction of right to left text, such as Arabic and Hebrew. */
private final TextNormalize normalize = new TextNormalize("UTF-8");
private PDDocument doc;

// --------------------------- CONSTRUCTORS ---------------------------

public PDFTextStripper(final PDDocument doc,
                       final int startPage,
                       final int endPage) throws IOException
{

    super(ResourceLoader.loadProperties("org.elacin.PdfTextStripper.properties", true));

    root = new DocumentNode();

    this.doc = doc;
    this.startPage = startPage;
    this.endPage = endPage;
}

// ------------------------ OVERRIDING METHODS ------------------------

/**
 * This will process a TextPosition object and add the text to the list of characters on a page.  It
 * takes care of overlapping text.
 *
 * @param text The text to process.
 */
protected void processTextPosition(ETextPosition text) {

    final boolean showCharacter = suppressDuplicateOverlappingText(text);

    if (showCharacter) {
        /* In the wild, some PDF encoded documents put diacritics (accents on
        * top of characters) into a separate Tj element.  When displaying them
        * graphically, the two chunks get overlayed.  With text output though,
        * we need to do the overlay. This code recombines the diacritic with
        * its associated character if the two are consecutive.
        */
        if (charactersForPage.isEmpty()) {
            charactersForPage.add(text);
        } else {
            /* test if we overlap the previous entry. Note that we are making an
                assumption that we need to only look back one TextPosition to
                find what we are overlapping.
                This may not always be true. */

            TextPosition previousTextPosition = charactersForPage.get(charactersForPage.size() - 1);

            if (text.isDiacritic() && previousTextPosition.contains(text)) {
                previousTextPosition.mergeDiacritic(text, normalize);
            }

            /* If the previous TextPosition was the diacritic, merge it into
                this one and remove it from the list. */
            else if (previousTextPosition.isDiacritic() && text.contains(previousTextPosition)) {
                text.mergeDiacritic(previousTextPosition, normalize);
                charactersForPage.remove(charactersForPage.size() - 1);
                charactersForPage.add(text);
            } else {
                charactersForPage.add(text);
            }
        }
    }
}

// -------------------------- PUBLIC METHODS --------------------------

public DocumentNode getDocumentNode() {
    return root;
}

public void readText() throws IOException {
    resetEngine();
    try {
        if (doc.isEncrypted()) {
            doc.decrypt("");
        }
    } catch (Exception e) {
        throw new RuntimeException("Could not decrypt document", e);
    }
    currentPageNo = 0;

    for (final PDPage nextPage : (List<PDPage>) doc.getDocumentCatalog().getAllPages()) {
        PDStream contentStream = nextPage.getContents();
        currentPageNo++;
        if (contentStream != null) {
            COSStream contents = contentStream.getStream();
            processPage(nextPage, contents);
        }
    }

    /* postprocessing */
    root.combineChildren();
    //    root.combineChildren();
    new RecognizeRoles().doOperation(root);
}

// -------------------------- OTHER METHODS --------------------------

private boolean suppressDuplicateOverlappingText(final ETextPosition text) {
    String textCharacter = text.getCharacter();
    if (" ".equals(text.getCharacter())) {
        return false;
    }

    List<TextPosition> sameTextCharacters = characterListMapping.get(textCharacter);
    if (sameTextCharacters == null) {
        sameTextCharacters = new ArrayList<TextPosition>();
        characterListMapping.put(textCharacter, sameTextCharacters);
        return true;
    }

    /** RDD - Here we compute the value that represents the end of the rendered
     text.  This value is used to determine whether subsequent text rendered
     on the same line overwrites the current text.

     We subtract any positive padding to handle cases where extreme amounts
     of padding are applied, then backed off (not sure why this is done, but there
     are cases where the padding is on the order of 10x the character width, and
     the TJ just backs up to compensate after each character).  Also, we subtract
     an amount to allow for kerning (a percentage of the width of the last
     character). */

    boolean suppressCharacter = false;

    float tolerance = (text.getWidth() / (float) textCharacter.length()) / 3.0f;
    for (TextPosition character : sameTextCharacters) {
        String charCharacter = character.getCharacter();
        float charX = character.getX();
        float charY = character.getY();

        if (charCharacter != null && MathUtils.isWithinVariance(charX, text.getX(), tolerance)
                && MathUtils.isWithinVariance(charY, text.getY(), tolerance)) {
            suppressCharacter = true;
        }
    }
    boolean showCharacter = false;

    if (!suppressCharacter) {
        sameTextCharacters.add(text);
        showCharacter = true;
    }
    //    if (!showCharacter) {
    //        System.out.println("showCharacter = " + StringUtils.getTextPositionString(text));
    //    }
    return showCharacter;
}

/**
 * This will process the contents of a page.
 *
 * @param page    The page to process.
 * @param content The contents of the page.
 * @throws IOException If there is an error processing the page.
 */
protected void processPage(PDPage page, COSStream content) throws IOException {
    if (currentPageNo >= startPage && currentPageNo <= endPage) {
        charactersForPage.clear();
        characterListMapping.clear();

        processStream(page, page.findResources(), content);

        if (!charactersForPage.isEmpty()) {
            final float width = page.getArtBox().getWidth();
            final float height = page.getArtBox().getHeight();

            final List<PhysicalText> texts = PhysicalWordSegmentator.createWords(root.getStyles(),
                                                                                 charactersForPage);

            PhysicalPage physicalPage = new PhysicalPage(texts, height, width, currentPageNo);
            final PageNode notUsed = physicalPage.compileLogicalPage();

            if (texts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ETextPosition character : charactersForPage) {
                    sb.append(StringUtils.getTextPositionString(character)).append("\n");
                }
                throw new RuntimeException("words should not have been empty here. "
                        + "created nothing from these characters:" + sb);
            }


            for (PhysicalText text : texts) {
                root.addWord(new WordNode(text.getPosition(), currentPageNo, text.getStyle(),
                                          text.getContent(), text.getCharSpacing()));
            }

            //            final List<Rectangle> whitespaces = ColumnFinder.findColumnsFromWordNodes(texts, width,
            //                                                                                      height);
            root.getPageNumber(currentPageNo).addWhitespaces(notUsed.getWhitespaces());
            root.getPageNumber(currentPageNo).addColumns(notUsed.getColumns());

        }
    }
}
}
