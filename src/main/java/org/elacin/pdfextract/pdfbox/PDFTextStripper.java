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

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.util.TextNormalize;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.logical.operation.RecognizeRoles;
import org.elacin.pdfextract.physical.PhysicalPage;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.physical.segmentation.WordSegmentator;
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentatorImpl;
import org.elacin.pdfextract.physical.segmentation.word.WordSegmentatorImpl;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PDFTextStripper extends PDFBoxSource {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PDFTextStripper.class);

@NotNull
protected final List<ETextPosition> charactersForPage = new ArrayList<ETextPosition>();

@NotNull
private final DocumentNode root;

private int currentPageNo;
private int startPage = 1;
private int endPage   = Integer.MAX_VALUE;

/* used to filter out text which is written several times to create a bold effect */
@NotNull
private final Map<String, List<TextPosition>> characterListMapping
        = new HashMap<String, List<TextPosition>>();

/* The normalizer is used to remove text ligatures/presentation forms and to correct
the direction of right to left text, such as Arabic and Hebrew. */
@NotNull
private final TextNormalize normalize = new TextNormalize("UTF-8");
private final PDDocument doc;

// --------------------------- CONSTRUCTORS ---------------------------

public PDFTextStripper(final PDDocument doc,
                       final int startPage,
                       final int endPage) throws IOException {
    super();

    root = new DocumentNode();
    this.doc = doc;
    this.startPage = startPage;
    this.endPage = endPage;
}

// ------------------------ OVERRIDING METHODS ------------------------

/**
 * This will process a TextPosition object and add the text to the list of characters on a page.
 * It
 * takes care of overlapping text.
 *
 * @param text The text to process.
 */
protected void processTextPosition(@NotNull TextPosition text) {
    super.processTextPosition(text);

    if (!includeText(text)) {
        if (log.isDebugEnabled()) {
            log.debug("LOG00770: ignoring textposition " + TextUtils.getTextPositionString(text)
                    + " because it seems to be rendered two times");
        }
        return;
    }

    if (!MathUtils.isWithinPercent(text.getDir(), (float) page.findRotation(), 1)) {
        if (log.isDebugEnabled()) {
            log.debug("LOG00560: ignoring textposition " + TextUtils.getTextPositionString(text)
                    + "because it has " + "wrong rotation. TODO :)");
        }
        return;
    }

    /* In the wild, some PDF encoded documents put diacritics (accents on
              * top of characters) into a separate Tj element.  When displaying them
              * graphically, the two chunks get overlayed.  With text output though,
              * we need to do the overlay. This code recombines the diacritic with
              * its associated character if the two are consecutive.
              */
    if (charactersForPage.isEmpty()) {
        charactersForPage.add((ETextPosition) text);
    } else {
        /* test if we overlap the previous entry. Note that we are making an
                                      assumption that we need to only look back one TextPosition
                                      to
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
            charactersForPage.add((ETextPosition) text);
        } else {
            charactersForPage.add((ETextPosition) text);
        }
    }
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public DocumentNode getDocumentNode() {
    return root;
}

public void processDocument() throws IOException {
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
    new RecognizeRoles().doOperation(root);
}

// -------------------------- OTHER METHODS --------------------------

private boolean includeText(@NotNull final TextPosition text) {
    String c = text.getCharacter();

    List<TextPosition> sameTextCharacters = characterListMapping.get(c);
    if (sameTextCharacters == null) {
        sameTextCharacters = new ArrayList<TextPosition>();
        characterListMapping.put(c, sameTextCharacters);
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

    final float tolerance = (text.getWidth() / (float) c.length()) / 3.0f;

    for (TextPosition other : sameTextCharacters) {
        String otherChar = other.getCharacter();
        float charX = other.getX();
        float charY = other.getY();

        if (otherChar != null && MathUtils.isWithinVariance(charX, text.getX(), tolerance)
                && MathUtils.isWithinVariance(charY, text.getY(), tolerance)) {
            suppressCharacter = true;
        }
    }
    boolean showCharacter = false;

    if (!suppressCharacter) {
        sameTextCharacters.add(text);
        showCharacter = true;
    }
    return showCharacter;
}

/**
 * This will process the contents of a page.
 *
 * @param page    The page to process.
 * @param content The contents of the page.
 * @throws IOException If there is an error processing the page.
 */
protected void processPage(@NotNull PDPage page, COSStream content) throws IOException {
    if (currentPageNo >= startPage && currentPageNo <= endPage) {
        charactersForPage.clear();
        characterListMapping.clear();

        /* show which page we are working on in the log */
        MDC.put("page", currentPageNo);

        pageSize = page.findCropBox().createDimension();

        /* this is used to 'draw' images on during pdf parsing */
        graphicSegmentator = new GraphicSegmentatorImpl((float) pageSize.getWidth(),
                (float) pageSize.getHeight());

        processStream(page, page.findResources(), content);

        /* getRotation might return null, something which doesnt play well with javas unboxing,
          *   so take some care*/
        final int rot;
        if (page.getRotation() == null) {
            rot = 0;
        } else {
            rot = page.getRotation();
        }

        WordSegmentator segmentator = new WordSegmentatorImpl(root.getStyles());

        if (!charactersForPage.isEmpty()) {

            try {
                /* segment words */
                final List<PhysicalText> texts = segmentator.segmentWords(charactersForPage);
                PhysicalPage physicalPage = new PhysicalPage(texts,
                        graphicSegmentator,
                        currentPageNo);


                final PageNode pageNode = physicalPage.compileLogicalPage();

                root.addChild(pageNode);

            } catch (Exception e) {
                log.error("LOG00350:Error while creating physical page", e);
            }
        }

        MDC.remove("page");
    }
}
}
