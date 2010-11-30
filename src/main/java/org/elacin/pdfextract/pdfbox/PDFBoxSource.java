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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentatorImpl;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

//
///**
// * Created by IntelliJ IDEA. User: elacin Date: 14.11.10 Time: 22.45 To change this template use
// * File | Settings | File Templates.
// */
public class PDFBoxSource extends PageDrawer {
// ------------------------------ FIELDS ------------------------------

private static final Logger log         = Logger.getLogger(PDFBoxSource.class);
@NotNull
private static final byte[] SPACE_BYTES = {(byte) 32};

protected GraphicSegmentatorImpl graphicSegmentator;

protected int textPositionSequenceNumber = 0;

protected Color currentColor;

private BasicStroke basicStroke;

// --------------------------- CONSTRUCTORS ---------------------------

//--------------------------- CONSTRUCTORS ---------------------------
public PDFBoxSource() throws IOException {
	super();
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
public void drawImage(Image awtImage, AffineTransform at) {
	//		currentClippingPath = getGraphicsState().getCurrentClippingPath();
	final Rectangle2D bounds2D = getGraphicsState().getCurrentClippingPath().getBounds2D();
	graphicSegmentator.drawImage(awtImage, at, bounds2D);
}

@Override
public void drawPage(final Graphics g,
                     final PDPage p,
                     final Dimension pageDimension) throws IOException
{
	super.drawPage(g, p, pageDimension);
}

@Override
public void fillPath(int windingRule) throws IOException {
	currentColor = getGraphicsState().getNonStrokingColor().getJavaColor();
	getLinePath().setWindingRule(windingRule);
	//	currentClippingPath = getGraphicsState().getCurrentClippingPath();
	graphicSegmentator.fill(getLinePath(), currentColor);
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


/** Old version */
public void processEncodedText(@NotNull byte[] string) throws IOException {
	/* Note on variable names.  There are three different units being used
		* in this code.  Character sizes are given in glyph units, text locations
		* are initially given in text units, and we want to save the data in
		* display units. The variable names should end with Text or Disp to
		* represent if the values are in text or disp units (no glyph units are saved).
		*/

	final float fontSizeText = getGraphicsState().getTextState().getFontSize();
	final float horizontalScalingText =
			getGraphicsState().getTextState().getHorizontalScalingPercent() / 100f;
	//float verticalScalingText = horizontalScaling;//not sure if this is right but what else to do???
	final float riseText = getGraphicsState().getTextState().getRise();
	final float wordSpacingText = getGraphicsState().getTextState().getWordSpacing();
	final float characterSpacingText = getGraphicsState().getTextState().getCharacterSpacing();

	/* We won't know the actual number of characters until
	we process the byte data(could be two bytes each) but
	it won't ever be more than string.length*2(there are some cases
	were a single byte will result in two output characters "fi" */

	final PDFont font = getGraphicsState().getTextState().getFont();

	/* This will typically be 1000 but in the case of a type3 font
	this might be a different number
	*/
	final float glyphSpaceToTextSpaceFactor = 1f / font.getFontMatrix().getValue(0, 0);
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

	//	float[] individualWidthsBuffer = new float[string.length];
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

//		if (font.getSubType().contains("Type3") && c.equals("!")) {
//			System.out.println("font = " + font);
//		}
//

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

		if (maxVerticalDisplacementText <= 0.0f){
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
		float tx = ((characterHorizontalDisplacementText - adjustment / glyphSpaceToTextSpaceFactor)
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


		boolean add = true;
		if (characterBuffer.toString().trim().isEmpty()) {
			if (log.isTraceEnabled()) { log.trace("Dropping empty string"); }
			add = false;
		}
		if (fontSizeText == 0) {
			log.trace("ignoring text " + c + " because fontSize is 0");
			add = false;
		}

		if (add) {
			try {
				final ETextPosition text = new ETextPosition(page, textMatrixStDisp,
				                                             textMatrixEndDisp,
				                                             totalVerticalDisplacementDisp,
				                                             new float[]{widthText}, spaceWidthDisp,
				                                             characterBuffer.toString(), font,
				                                             fontSizeText,
				                                             (int) (fontSizeText * getTextMatrix()
						                                             .getXScale()), wordSpacingDisp,
				                                             textPositionSequenceNumber);
				correctPosition(string, fontSizeText, font, glyphSpaceToTextSpaceFactor, codeLength,
				                i, c, text);

				processTextPosition(text);
				textPositionSequenceNumber++;
			} catch (Exception e) {
				log.warn("LOG00570:Error adding '" + characterBuffer + "': ", e);
			}
		}

		textMatrixStDisp = getTextMatrix().multiply(dispMatrix);
		characterBuffer.setLength(0);
	}
}

///**
// * Process encoded text from the PDF Stream. You should override this method if you want to perform
// * an action when encoded text is being processed.
// *
// * @param string The encoded text
// * @throws IOException If there is an error processing the string
// */
//public void processEncodedText(@NotNull byte[] string) throws IOException {
//	/* Note on variable names.  There are three different units being used
//			 * in this code.  Character sizes are given in glyph units, text locations
//			 * are initially given in text units, and we want to save the data in
//			 * display units. The variable names should end with Text or Disp to
//			 * represent if the values are in text or disp units (no glyph units are saved).
//			 */
//	final float fontSizeText = getGraphicsState().getTextState().getFontSize();
//	final float horizontalScalingText =
//			getGraphicsState().getTextState().getHorizontalScalingPercent() / 100f;
//	//float verticalScalingText = horizontalScaling;//not sure if this is right but what else to do???
//	final float riseText = getGraphicsState().getTextState().getRise();
//	final float wordSpacingText = getGraphicsState().getTextState().getWordSpacing();
//	final float characterSpacingText = getGraphicsState().getTextState().getCharacterSpacing();
//
//	//We won't know the actual number of characters until
//	//we process the byte data(could be two bytes each) but
//	//it won't ever be more than string.length*2(there are some cases
//	//were a single byte will result in two output characters "fi"
//
//	final PDFont font = getGraphicsState().getTextState().getFont();
//	// all fonts are providing the width/height of a character in thousandths of a unit of text space
//	float fontMatrixXScaling = 1 / 1000f;
//	float fontMatrixYScaling = 1 / 1000f;
//	float glyphSpaceToTextSpaceFactor = 1 / 1000f;
//	// expect Type3 fonts, those are providing the width of a character in glyph space units
//	if (font instanceof PDType3Font) {
//		PDMatrix fontMatrix = font.getFontMatrix();
//		fontMatrixXScaling = fontMatrix.getValue(0, 0);
//		fontMatrixYScaling = fontMatrix.getValue(1, 1);
//		//This will typically be 1000 but in the case of a type3 font
//		//this might be a different number
//		glyphSpaceToTextSpaceFactor = 1f / fontMatrix.getValue(0, 0);
//	}
//	float spaceWidthText = 0;
//	try {
//		// to avoid crash as described in PDFBOX-614
//		// lets see what the space displacement should be
//		spaceWidthText = (font.getFontWidth(SPACE_BYTES, 0, 1) / glyphSpaceToTextSpaceFactor);
//	} catch (Throwable exception) {
//		log.warn(exception, exception);
//	}
//
//	if (spaceWidthText == 0) {
//		spaceWidthText = (font.getAverageFontWidth() / glyphSpaceToTextSpaceFactor);
//		//The average space width appears to be higher than necessary
//		//so lets make it a little bit smaller.
//		spaceWidthText *= .80f;
//	}
//
//	float maxVerticalDisplacementText = 0;
//
//	Matrix textStateParameters = new Matrix();
//	textStateParameters.setValue(0, 0, fontSizeText * horizontalScalingText);
//	textStateParameters.setValue(1, 1, fontSizeText);
//	textStateParameters.setValue(2, 1, riseText);
//
//	int pageRotation = page.findRotation();
//	float pageHeight = page.findMediaBox().getHeight();
//	float pageWidth = page.findMediaBox().getWidth();
//
//	Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
//	int codeLength = 1;
//	for (int i = 0; i < string.length; i += codeLength) {
//		// Decode the value to a Unicode character
//		codeLength = 1;
//		String c = font.encode(string, i, codeLength);
//		if (c == null && i + 1 < string.length) {
//			//maybe a multibyte encoding
//			codeLength++;
//			c = font.encode(string, i, codeLength);
//		}
//
//		// the space width has to be transformed into display units
//		float spaceWidthDisp = spaceWidthText * fontSizeText * horizontalScalingText
//				* getTextMatrix().getValue(0, 0) * ctm.getValue(0, 0);
//
//		//todo, handle horizontal displacement
//		// get the width and height of this character in text units
//		float characterHorizontalDisplacementText = font.getFontWidth(string, i, codeLength);
//		float characterVerticalDisplacementText = font.getFontHeight(string, i, codeLength);
//
//		// multiply the width/height with the scaling factor
//		characterHorizontalDisplacementText *= fontMatrixXScaling;
//		characterVerticalDisplacementText *= fontMatrixYScaling;
//
//		maxVerticalDisplacementText = Math
//				.max(maxVerticalDisplacementText, characterVerticalDisplacementText);
//
//
//		// PDF Spec - 5.5.2 Word Spacing
//		//
//		// Word spacing works the same was as character spacing, but applies
//		// only to the space character, code 32.
//		//
//		// Note: Word spacing is applied to every occurrence of the single-byte
//		// character code 32 in a string.  This can occur when using a simple
//		// font or a composite font that defines code 32 as a single-byte code.
//		// It does not apply to occurrences of the byte value 32 in multiple-byte
//		// codes.
//		//
//		// RDD - My interpretation of this is that only character code 32's that
//		// encode to spaces should have word spacing applied.  Cases have been
//		// observed where a font has a space character with a character code
//		// other than 32, and where word spacing (Tw) was used.  In these cases,
//		// applying word spacing to either the non-32 space or to the character
//		// code 32 non-space resulted in errors consistent with this interpretation.
//		//
//		float spacingText = 0;
//		if ((string[i] == 0x20) && codeLength == 1) {
//			spacingText += wordSpacingText;
//		}
//
//
//		final Matrix adjustedTextMatrix = getAdjustedTextMatrix(string, font, codeLength, i);
//
//		// Convert textMatrix to display units
//		Matrix textMatrixStart = textStateParameters.multiply(adjustedTextMatrix).multiply(ctm);
//		Matrix textMatrixStart_ = textStateParameters.multiply(getTextMatrix()).multiply(ctm);
//
//		// TODO : tx should be set for horizontal text and ty for vertical text
//		// which seems to be specified in the font (not the direction in the matrix).
//		float tx = characterHorizontalDisplacementText * fontSizeText * horizontalScalingText;
//		float ty = 0;
//		Matrix td = new Matrix();
//		td.setValue(2, 0, tx);
//		td.setValue(2, 1, ty);
//
//		// The text matrix gets updated after each glyph is placed.  The updated
//		// version will have the X and Y coordinates for the next glyph.
//		// textMatrixEnd contains the coordinates of the end of the last glyph without
//		// taking characterSpacingText and spacintText into account, otherwise it'll be
//		// impossible to detect new words within text extraction
//		Matrix textMatrixEnd = textStateParameters.multiply(td).multiply(adjustedTextMatrix)
//		                                          .multiply(ctm);
//
//		Matrix textMatrixEnd_ = textStateParameters.multiply(td).multiply(getTextMatrix())
//		                                          .multiply(ctm);
//
//		// add some spacing to the text matrix (see comment above)
//		tx = ((characterHorizontalDisplacementText) * fontSizeText + characterSpacingText
//				+ spacingText) * horizontalScalingText;
//		td.setValue(2, 0, tx);
//		setTextMatrix(td.multiply(getTextMatrix()));
//
//		// determine the width of this character
//		// XXX: Note that if we handled vertical text, we should be using Y here
//		float widthText = textMatrixEnd.getXPosition() - textMatrixStart.getXPosition();
//
//		//there are several cases where one character code will
//		//output multiple characters.  For example "fi" or a
//		//glyphname that has no mapping like "visiblespace"
//
//		if (c == null) {
//			c = "?";
//		}
//
//		float totalVerticalDisplacementDisp = maxVerticalDisplacementText * fontSizeText
//				* getTextMatrix().getYScale();
//
//
//		if (c.trim().isEmpty()) {
//			if (log.isDebugEnabled()) { log.debug("Dropping empty string"); }
//			continue;
//		}
//
//		try {
//			final ETextPosition text = new ETextPosition(pageRotation, pageWidth, pageHeight,
//			                                             textMatrixStart, textMatrixEnd,
//			                                             totalVerticalDisplacementDisp, widthText,
//			                                             spaceWidthDisp, c, font, fontSizeText,
//			                                             (int) (fontSizeText * getTextMatrix()
//					                                             .getXScale()),
//			                                             textPositionSequenceNumber);
//
////			correctPosition(string, fontSizeText, font, glyphSpaceToTextSpaceFactor, codeLength, i,
////			                c, text);
//
//
//			processTextPosition(text);
//
//			textPositionSequenceNumber++;
//		} catch (Exception e) {
//			log.warn("LOG00780:Error while creating textposition " + c, e);
//		}
//
//	}
//}

//private Matrix getAdjustedTextMatrix(final byte[] string,
//                                     final PDFont font,
//                                     final int codeLength,
//                                     final int i) throws IOException
//{
//	final BoundingBox cBB = font.getCharacterBoundingBox(string, i, codeLength);
//	PDRectangle fBB = null;
//	try {
//		fBB = font.getFontBoundingBox();
//	} catch (RuntimeException e) {
//		//ignore, this is frequently not implemented
//	}
//
//	/* this matrix moves the text back down when the current position has been moved too
//		*   high in order to make room for the largest glyph in the font*/
//	Matrix adjustedMatrix = (Matrix) getTextMatrix().clone();
//
//	float offsetY = getTextMatrix().getValue(2, 0);
//
//	if (cBB != null && fBB != null && cBB.getHeight() > 0.0f && fBB.getHeight() > 0.0f) {
//		final float fontHeight = fBB.getUpperRightY() - fBB.getLowerLeftY();
//
//		/* remove the upper and lower bounds filtered away by cBB */
//		final float spaceOverChar = (cBB.getLowerLeftY() - fBB.getLowerLeftY());
//		final float spaceUnderChar = fBB.getUpperRightY() - cBB.getUpperRightY();
//
//		offsetY = offsetY - fontHeight + spaceOverChar + spaceUnderChar;
//
//		if (font.getBaseFont().contains("CMEX")) {
//			offsetY += (cBB.getHeight());
//		}
//
//	}
//	adjustedMatrix.setValue(2, 0, offsetY);
//	return adjustedMatrix;
//}

private void correctPosition(final byte[] string,
                             final float fontSizeText,
                             final PDFont font,
                             final float glyphSpaceToTextSpaceFactor,
                             final int codeLength,
                             final int i,
                             final String c,
                             final ETextPosition text) throws IOException
{ /**
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
 * 3. Some fonts, CMEX identified so far, either exposes some wrong information or
 *      exposes a bug somewhere. I needed to adjust it by one characterHeight to
 *      properly position those glyphs
 */
	final BoundingBox cBB = font.getCharacterBoundingBox(string, i, codeLength);
	PDRectangle fBB = null;
	try {
		fBB = font.getFontBoundingBox();
	} catch (RuntimeException e) {
		//ignore, this is frequently not implemented
	}

	final Rectangle pos = text.getPos();
	float adjust = fontSizeText / glyphSpaceToTextSpaceFactor;
	if (cBB != null && fBB != null && cBB.getHeight() > 0.0f && fBB.getHeight() > 0.0f) {
		final float fontHeight = fBB.getUpperRightY() - fBB.getLowerLeftY();

		/* remove the upper and lower bounds filtered away by cBB */
		final float spaceOverChar = (cBB.getLowerLeftY() - fBB.getLowerLeftY());
		final float spaceUnderChar = fBB.getUpperRightY() - cBB.getUpperRightY();

		final float characterHeight = cBB.getUpperRightY() - cBB.getLowerLeftY();

		final float beforeRoomForGlyph = pos.getY() - (adjust * fontHeight);

		float startY = beforeRoomForGlyph + (adjust * spaceOverChar) + (adjust * spaceUnderChar);

		if (font.getBaseFont().contains("CMEX")) {
			startY += (adjust * characterHeight);
		}

		float x = text.getX() + (adjust * Math.max(cBB.getLowerLeftX(), fBB.getLowerLeftX()));
		//		float w = adjust * (cBB.getUpperRightX() - cBB.getLowerLeftX());
		//		if (w == 0.0f) {
		float w = text.getWidth();
		//		}

		float h = adjust * characterHeight;
		if (h == 0.0f){
			h = adjust * fBB.getHeight();
		}
		if (h < text.getHeight() / 4) {
			h = text.getHeight();
		}

		final Rectangle newPos = new Rectangle(x, startY, w, h);
		log.debug("LOG00730:Text " + c + ", " + "pos from " + pos + " to " + newPos);
		text.setPos(newPos);

		text.setBaseLine(beforeRoomForGlyph);
	} else {
		/* if we do not have the bounding box information available for this font,
			approximate the position by offsetting the y coordinate with the characters
			height
		 */
		text.setBaseLine(pos.getY());
		text.setPos(new Rectangle(pos.getX(), pos.getY() - pos.getHeight(), pos.getWidth(),
		                          pos.getHeight()));
	}
}


@Override
protected void processTextPosition(@NotNull final TextPosition text) {
	//ignore
}

@Override
public void setStroke(final BasicStroke newStroke) {
	basicStroke = newStroke;
}

@Override
public void strokePath() throws IOException {
	currentColor = getGraphicsState().getStrokingColor().getJavaColor();
	//	currentClippingPath = getGraphicsState().getCurrentClippingPath();
	graphicSegmentator.strokePath(getLinePath(), currentColor);
	getLinePath().reset();
}
}
