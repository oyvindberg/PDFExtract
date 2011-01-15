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
import org.apache.pdfbox.pdmodel.font.PDFont;
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
import java.util.*;


public class PDFTextFilter extends PDFBoxSource {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PDFTextFilter.class);

@NotNull
private final DocumentNode root;
private       int          currentPageNo;
private int startPage = 1;
private int endPage   = Integer.MAX_VALUE;

/* used to filter out text which is written several times to create a bold effect */
@NotNull
private final Map<String, List<TextPosition>> characterListMapping = new HashMap<String, List<TextPosition>>();

/* The normalizer is used to remove text ligatures/presentation forms and to correct
the direction of right to left text, such as Arabic and Hebrew. */
@NotNull
private final TextNormalize normalize = new TextNormalize("UTF-8");
private final PDDocument doc;

// --------------------------- CONSTRUCTORS ---------------------------

public PDFTextFilter(final PDDocument doc,
                     final int startPage,
                     final int endPage) throws IOException {

    root = new DocumentNode();
    this.doc = doc;
    this.startPage = startPage;
    this.endPage = endPage;
}

// ------------------------ OVERRIDING METHODS ------------------------

/**
 * This will process a TextPosition object and add the text to the list of characters on a page.
 * <p/>
 * This method also filter out unwanted textpositions
 * .
 *
 * @param text The text to process.
 */
protected void processTextPosition(@NotNull TextPosition text_) {
    ETextPosition text = (ETextPosition) text_;

    super.processTextPosition(text);

    if (text.getFontSize() == 0.0f) {
        if (log.isDebugEnabled()) {
            log.debug("LOG01100:ignoring text " + text.getCharacter() + " because fontSize is 0");
        }
        return;
    }

    if (!WordSegmentatorImpl.USE_EXISTING_WHITESPACE && "".equals(text.getCharacter().trim())) {
        return;
    }

    if (text.getCharacter().length() == 0) {
        if (log.isDebugEnabled()) {
            log.debug("LOG01110:Tried to render no text. wtf?");
        }
        return;
    }

    java.awt.Rectangle javapos = new java.awt.Rectangle((int) text.getPos().getX(),
            (int) text.getPos().getY(), (int) text.getPos().getWidth(), (int) text.getPos().getHeight());
    if (!getGraphicsState().getCurrentClippingPath().intersects(javapos)) {
        if (log.isDebugEnabled()) {
            log.debug("LOG01090:Dropping text \"" + text.getCharacter() + "\" because it "
                    + "was outside clipping path");
        }
        return;
    }


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


    /** In the wild, some PDF encoded documents put diacritics (accents on
     * top of characters) into a separate Tj element.  When displaying them
     * graphically, the two chunks get overlayed.  With text output though,
     * we need to do the overlay. This code recombines the diacritic with
     * its associated character if the two are consecutive.
     */
    if (charactersForPage.isEmpty()) {
        charactersForPage.add(text);
    } else {
        /** test if we overlap the previous entry. Note that we are making an assumption that we
         * need to only look back one TextPosition to find what we are overlapping.
         * This may not always be true. */
        TextPosition previousTextPosition = charactersForPage.get(charactersForPage.size() - 1);

        if (text.isDiacritic() && previousTextPosition.contains(text)) {
            previousTextPosition.mergeDiacritic(text, normalize);
        }

        /** If the previous TextPosition was the diacritic, merge it into this one and remove it
         * from the list. */
        else if (previousTextPosition.isDiacritic() && text.contains(previousTextPosition)) {
            text.mergeDiacritic(previousTextPosition, normalize);
            charactersForPage.remove(charactersForPage.size() - 1);
            charactersForPage.add(text);
        } else {
            charactersForPage.add(text);
        }
    }
}

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
        graphicSegmentator = new GraphicSegmentatorImpl((float) pageSize.getWidth(), (float) pageSize.getHeight());

        processStream(page, page.findResources(), content);

        filterOutBadFonts(charactersForPage);

        /* filter out remaining definite bad characters */
        filterOutControlCodes(charactersForPage);

        WordSegmentator segmentator = new WordSegmentatorImpl(root.getStyles());

        if (!charactersForPage.isEmpty()) {
            try {
                /* segment words */
                final List<PhysicalText> texts = segmentator.segmentWords(charactersForPage);

                PhysicalPage physicalPage = new PhysicalPage(texts, graphicSegmentator, currentPageNo);

                final PageNode pageNode = physicalPage.compileLogicalPage();

                root.addChild(pageNode);
            } catch (Exception e) {
                log.error("LOG00350:Error while creating physical page", e);
            }
        }

        MDC.remove("page");
    }
}

private void filterOutControlCodes(List<ETextPosition> text) {
    for (Iterator<ETextPosition> iterator = text.iterator(); iterator.hasNext();) {
        TextPosition tp = iterator.next();
        if (Character.isISOControl(tp.getCharacter().charAt(0))) {
            if (log.isDebugEnabled()) {
                log.debug("Removing character \"" + tp.getCharacter() + "\"");
            }
            ;
            iterator.remove();
        }
    }
}

// -------------------------- OTHER METHODS --------------------------

private void filterOutBadFonts(List<ETextPosition> text) {
    final Map<PDFont, Integer> badCharsForStyle = new HashMap<PDFont, Integer>();
    final Map<PDFont, Integer> numCharsForStyle = new HashMap<PDFont, Integer>();

    for (TextPosition tp : text) {
        if (!badCharsForStyle.containsKey(tp.getFont())) {
            badCharsForStyle.put(tp.getFont(), 0);
            numCharsForStyle.put(tp.getFont(), 0);
        }

        char c = tp.getCharacter().charAt(0);
        if (Character.isISOControl(c)) {
            badCharsForStyle.put(tp.getFont(), badCharsForStyle.get(tp.getFont()) + 1);
        }
        numCharsForStyle.put(tp.getFont(), numCharsForStyle.get(tp.getFont()) + 1);
    }

    final List<PDFont> ignoredFonts = new ArrayList<PDFont>();
    for (PDFont font : numCharsForStyle.keySet()) {
        int badChars = badCharsForStyle.get(font);
        int totalChars = numCharsForStyle.get(font);
        if (badChars > totalChars * 0.10f) {
            ignoredFonts.add(font);
            log.warn("LOG01060:Ignoring all content using font " + font.getBaseFont() + " as it "
                    + "seems to be missing UTF-8 conversion information");
        }
    }

    for (Iterator<ETextPosition> iterator = text.iterator(); iterator.hasNext();) {
        TextPosition tp = iterator.next();
        if (ignoredFonts.contains(tp.getFont())) {
            iterator.remove();
        }
    }
}
}
