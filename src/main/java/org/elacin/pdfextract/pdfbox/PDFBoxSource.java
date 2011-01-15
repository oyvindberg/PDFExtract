///*
// * Copyright 2010 Øyvind Berg (elacin@gmail.com)
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
package org.elacin.pdfextract.pdfbox;

import org.apache.fontbox.util.BoundingBox;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMatrix;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentatorImpl;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by IntelliJ IDEA. User: elacin Date: 14.11.10 Time: 22.45 To change this template use
 * File | Settings | File Templates.
 */
public class PDFBoxSource extends PageDrawer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log         = Logger.getLogger(PDFBoxSource.class);
@NotNull
private static final byte[] SPACE_BYTES = {(byte) 32};

private static final boolean NO_DESCENDERS = true;

protected GraphicSegmentatorImpl graphicSegmentator;

protected Color currentColor;

@NotNull
protected final List<ETextPosition> charactersForPage = new ArrayList<ETextPosition>();

/* i couldnt find this information anywhere, so calculate it and cache it here */
protected Map<PDFont, Boolean> areFontsMonospaced = new HashMap<PDFont, Boolean>();

private BasicStroke basicStroke;

// --------------------------- CONSTRUCTORS ---------------------------

//--------------------------- CONSTRUCTORS ---------------------------
public PDFBoxSource() throws IOException {
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
public void drawImage(Image awtImage, AffineTransform at) {
    final Shape currentClippingPath = getGraphicsState().getCurrentClippingPath();
    graphicSegmentator.drawImage(awtImage, at, currentClippingPath);
}

@Override
public void drawPage(final Graphics g,
                     final PDPage p,
                     final Dimension pageDimension) throws IOException {
    super.drawPage(g, p, pageDimension);
}

@Override
public void fillPath(int windingRule) throws IOException {
    currentColor = getGraphicsState().getNonStrokingColor().getJavaColor();
    getLinePath().setWindingRule(windingRule);

    final Shape currentClippingPath = getGraphicsState().getCurrentClippingPath();
    graphicSegmentator.fill(getLinePath(), currentColor, currentClippingPath);
    getLinePath().reset();
}

@Override
public Graphics2D getGraphics() {
    throw new RuntimeException("PDFBoxSource does not have Graphics2D");
}

@Override
public BasicStroke getStroke() {
    return basicStroke;
}

/**
 * Old version
 */
public void processEncodedText(@NotNull byte[] string) throws IOException {
    /* Note on variable names.  There are three different units being used
         * in this code.  Character sizes are given in glyph units, text locations
         * are initially given in text units, and we want to save the data in
         * display units. The variable names should end with Text or Disp to
         * represent if the values are in text or disp units (no glyph units are saved).
         */

    final float fontSizeText = getGraphicsState().getTextState().getFontSize();
    final float horizontalScalingText = getGraphicsState().getTextState()
            .getHorizontalScalingPercent() / 100f;
    //float verticalScalingText = horizontalScaling;//not sure if this is right but what else to
    // do???
    final float riseText = getGraphicsState().getTextState().getRise();
    final float wordSpacingText = getGraphicsState().getTextState().getWordSpacing();
    final float characterSpacingText = getGraphicsState().getTextState().getCharacterSpacing();

    /* We won't know the actual number of characters until
     we process the byte data(could be two bytes each) but
     it won't ever be more than string.length*2(there are some cases
     were a single byte will result in two output characters "fi" */

    final PDFont font = getGraphicsState().getTextState().getFont();

    /* This will typically be 1000 but in the case of a type3 font this might be a different
    number */
    final float glyphSpaceToTextSpaceFactor;

    if (font instanceof PDType3Font) {
        PDMatrix fontMatrix = font.getFontMatrix();
        float fontMatrixXScaling = fontMatrix.getValue(0, 0);
        glyphSpaceToTextSpaceFactor = 1.0f / fontMatrixXScaling;
    } else {
        glyphSpaceToTextSpaceFactor = /*1.0f / */1000f;
    }

    float spaceWidthText = 0.0F;

    try {
        spaceWidthText = (font.getFontWidth(SPACE_BYTES, 0, 1) / glyphSpaceToTextSpaceFactor);
    } catch (Throwable exception) {
        log.warn(exception, exception);
    }

    if (spaceWidthText == 0.0F) {
        spaceWidthText = (font.getAverageFontWidth() / glyphSpaceToTextSpaceFactor);
        spaceWidthText *= .80f;
    }


    /* Convert textMatrix to display units */
    final Matrix initialMatrix = new Matrix();
    initialMatrix.setValue(0, 0, 1.0F);
    initialMatrix.setValue(0, 1, 0.0F);
    initialMatrix.setValue(0, 2, 0.0F);
    initialMatrix.setValue(1, 0, 0.0F);
    initialMatrix.setValue(1, 1, 1.0F);
    initialMatrix.setValue(1, 2, 0.0F);
    initialMatrix.setValue(2, 0, 0.0F);
    initialMatrix.setValue(2, 1, riseText);
    initialMatrix.setValue(2, 2, 1.0F);

    final Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
    final Matrix dispMatrix = initialMatrix.multiply(ctm);

    Matrix textMatrixStDisp = getTextMatrix().multiply(dispMatrix);
    Matrix textMatrixEndDisp;

    final float xScaleDisp = textMatrixStDisp.getXScale();
    final float yScaleDisp = textMatrixStDisp.getYScale();

    final float spaceWidthDisp = spaceWidthText * xScaleDisp * fontSizeText;
    final float wordSpacingDisp = wordSpacingText * xScaleDisp * fontSizeText;

    float maxVerticalDisplacementText = 0.0F;

    StringBuilder characterBuffer = new StringBuilder(string.length);

    int codeLength = 1;
    for (int i = 0; i < string.length; i += codeLength) {
        // Decode the value to a Unicode character
        codeLength = 1;
        String c = font.encode(string, i, codeLength);
        if (c == null && i + 1 < string.length) {
            //maybe a multibyte encoding
            codeLength++;
            c = font.encode(string, i, codeLength);
        }
        c = inspectFontEncoding(c);

        //todo, handle horizontal displacement
        // get the width and height of this character in text units
        float fontWidth = font.getFontWidth(string, i, codeLength);
        if (fontWidth == 0.0f) {
            fontWidth = spaceWidthDisp;
        }
        float characterHorizontalDisplacementText = (fontWidth / glyphSpaceToTextSpaceFactor);

        maxVerticalDisplacementText = Math.max(maxVerticalDisplacementText,
                font.getFontHeight(string, i, codeLength)
                        / glyphSpaceToTextSpaceFactor);

        if (maxVerticalDisplacementText <= 0.0f) {
            maxVerticalDisplacementText = font.getFontBoundingBox().getHeight() /
                    glyphSpaceToTextSpaceFactor;
        }
        /**
         PDF Spec - 5.5.2 Word Spacing

         Word spacing works the same was as character spacing, but applies
         only to the space character, code 32.

         Note: Word spacing is applied to every occurrence of the single-byte
         character code 32 in a string.  This can occur when using a simple
         font or a composite font that defines code 32 as a single-byte code.
         It does not apply to occurrences of the byte value 32 in multiple-byte
         codes.

         RDD - My interpretation of this is that only character code 32's that
         encode to spaces should have word spacing applied.  Cases have been
         observed where a font has a space character with a character code
         other than 32, and where word spacing (Tw) was used.  In these cases,
         applying word spacing to either the non-32 space or to the character
         code 32 non-space resulted in errors consistent with this interpretation. */
        float spacingText = characterSpacingText;
        if ((string[i] == (byte) 0x20) && codeLength == 1) {
            spacingText += wordSpacingText;
        }

        /* The text matrix gets updated after each glyph is placed.  The updated
                  * version will have the X and Y coordinates for the next glyph.
                  */
        Matrix glyphMatrixStDisp = getTextMatrix().multiply(dispMatrix);


        //The adjustment will always be zero.  The adjustment as shown in the
        //TJ operator will be handled separately.
        float adjustment = 0.0F;
        // TODO : tx should be set for horizontal text and ty for vertical text
        // which seems to be specified in the font (not the direction in the matrix).
        float tx = ((characterHorizontalDisplacementText - adjustment /
                glyphSpaceToTextSpaceFactor)
                * fontSizeText) * horizontalScalingText;
        float ty = 0.0F;

        Matrix td = new Matrix();
        td.setValue(2, 0, tx);
        td.setValue(2, 1, ty);

        setTextMatrix(td.multiply(getTextMatrix()));

        Matrix glyphMatrixEndDisp = getTextMatrix().multiply(dispMatrix);

        float sx = spacingText * horizontalScalingText;
        float sy = 0.0F;

        Matrix sd = new Matrix();
        sd.setValue(2, 0, sx);
        sd.setValue(2, 1, sy);

        setTextMatrix(sd.multiply(getTextMatrix()));

        float widthText = glyphMatrixEndDisp.getXPosition() - glyphMatrixStDisp.getXPosition();

        characterBuffer.append(c);

        textMatrixEndDisp = glyphMatrixEndDisp;

        float totalVerticalDisplacementDisp = maxVerticalDisplacementText * fontSizeText
                * yScaleDisp;


        try {
            final ETextPosition text = new ETextPosition(page, textMatrixStDisp,
                    textMatrixEndDisp,
                    totalVerticalDisplacementDisp,
                    new float[]{widthText}, spaceWidthDisp,
                    characterBuffer.toString(), font,
                    fontSizeText,
                    (int) (fontSizeText * getTextMatrix()
                            .getXScale()), wordSpacingDisp);

            correctPosition(font, string, i, c, fontSizeText, glyphSpaceToTextSpaceFactor,
                    horizontalScalingText, codeLength,
                    text);

            processTextPosition(text);

        } catch (Exception e) {
            log.warn("LOG00570:Error adding '" + characterBuffer + "': " + e.getMessage());
        }

        textMatrixStDisp = getTextMatrix().multiply(dispMatrix);
        characterBuffer.setLength(0);
    }
}

private void correctPosition(final PDFont fontObj, final byte[] string,
                             final int i, final String c, final float fontSizeText,
                             final float glyphSpaceToTextSpaceFactor,
                             float horizontalScalingText, final int codeLength,
                             final ETextPosition text) throws IOException { /**
 * Provide precise positioning of glyphs.
 *
 * There are several problems right which needs to be worked around:
 *
 * 1. Sometimes the PDF will make room for a glyph which belongs to a font with
 *      one or more very tall glyphs by jumping up on the page before drawing.
 *   Since most glyphs are (much) shorter than the tallest one, we need to make
 *      up for that by adjusting the Y coordinate back down. The distance which
 *      is jumped up is embedded in the PDF files, so there is no other way to go
 *      about this.
 *
 *  'beforeRoomForGlyph' is the position we were at before the jump back.
 *   Then we need to add spaceOverChar which is my estimate of where the glyph
 *      should begin. the result is kept in 'startY'
 *
 * 2. The default height we get might also be too big, so recalculate that based
 *      on character bounding
 *
 */
    final BoundingBox character = fontObj.getCharacterBoundingBox(string, i, codeLength);
    PDRectangle fontBB = null;
    try {
        fontBB = fontObj.getFontBoundingBox();
    } catch (RuntimeException e) {
        //ignore, this is frequently not implemented
    }

    final Rectangle pos = text.getPos();
    float adjust = (fontSizeText * horizontalScalingText) / glyphSpaceToTextSpaceFactor;
    adjust *= getTextMatrix().getXScale();

    final Rectangle newPos;
    if (character != null && fontBB != null && character.getHeight() > 0.0f
            && fontBB.getHeight() > 0.0f) {
        /* remove the upper and lower bounds filtered away by character */
        final float spaceUnderChar = Math.min(fontBB.getLowerLeftY(), character.getLowerLeftY());
        final float spaceOverChar = fontBB.getUpperRightY() - character.getUpperRightY();

        final float fontHeight = fontBB.getHeight();

        final float beforeRoomForGlyph = pos.getEndY() - adjust * Math.max(fontHeight,
                pos.getHeight());

        /* calculate the upper left corner of the rendered glyph */
        float yStart = beforeRoomForGlyph;
        yStart += adjust * spaceOverChar;
        yStart -= adjust * spaceUnderChar;
        yStart -= pos.getHeight();

        /* determine start X coordinate. */
        final float x;
        if (isMonoSpacedFont(fontObj)) {
            x = pos.getX();
        } else {
            float leftOfText = text.getX() - (adjust * fontBB.getLowerLeftX());
            x = leftOfText + adjust * character.getLowerLeftX();
        }

        /* It was much easier to write the word segmentation code with full font width,
        *   so lets keep that. I havent seen this causing any problems */
        float w = pos.getWidth();

        /* Line segmentation code was obviously much easier by not having any descenders which
        *   can even overlap into the following line. Math symbols need to*/
        final float characterHeight;
        if (NO_DESCENDERS && Character.getType(c.charAt(0)) != (int) Character.MATH_SYMBOL) {
            characterHeight = character.getUpperRightY();
        } else {
            characterHeight = character.getHeight();
        }
        final float h = adjust * characterHeight;

        newPos = new Rectangle(x, yStart, w, h);
    } else {
        /* here we have a lot less information, so keep most of what was calculated. Just offset
        *   the Y coordinate*/
        float h = pos.getHeight();
        float w = pos.getWidth();
        float startY = pos.getY() - h * 0.8f;

        if (fontObj instanceof PDType3Font) {
            /* type 3 fonts typically have almost no information
            * try to mitigate the damage by keeping them small.*/
            h *= 0.5f;
            startY += h; /* this is a _very_ quick and dirty hack */
        }

        newPos = new Rectangle(pos.getX(), startY, w, h);
    }
    if (log.isTraceEnabled()) {
        log.trace("LOG00730:Text " + c + ", " + "pos from " + pos + " to " + newPos);
    }
    text.setBaseLine(pos.getY());
    text.setPos(newPos);
}

private boolean isMonoSpacedFont(PDFont fontObj) {
    if (areFontsMonospaced.containsKey(fontObj)) {
        return areFontsMonospaced.get(fontObj);
    }

    List<Float> widths = (List<Float>) fontObj.getWidths();

    boolean monospaced = true;
    if (widths == null) {
        monospaced = false;
    } else {
        final float firstWidth = widths.get(0);
        for (int i = 1; i < widths.size(); i++) {
            final float width = widths.get(i);
            if (!MathUtils.isWithinPercent(width, firstWidth, 1.0f)) {
                monospaced = false;
                break;
            }
        }
    }

    if (monospaced) {
        log.debug("LOG01080:Font " + fontObj.getBaseFont() + " is monospaced");
    }

    areFontsMonospaced.put(fontObj, monospaced);

    return monospaced;
}

@Override
protected void processTextPosition(@NotNull final TextPosition text) {
}

@Override
public void setStroke(final BasicStroke newStroke) {
    basicStroke = newStroke;
}

@Override
public void strokePath() throws IOException {
    currentColor = getGraphicsState().getStrokingColor().getJavaColor();
    final Shape currentClippingPath = getGraphicsState().getCurrentClippingPath();

    graphicSegmentator.strokePath(getLinePath(), currentColor, currentClippingPath);
    getLinePath().reset();
}
}
