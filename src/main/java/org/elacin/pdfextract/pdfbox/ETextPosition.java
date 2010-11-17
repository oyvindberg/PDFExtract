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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

/**
 * This represents a string and a position on the screen of those characters.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.12 $
 */
public class ETextPosition extends TextPosition {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(ETextPosition.class);
@NotNull
private final Rectangle pos;
private final int       sequenceNum;
private       boolean   smallOperator;

// --------------------------- CONSTRUCTORS ---------------------------

public ETextPosition(final PDPage page,
                     final Matrix textPositionSt,
                     final Matrix textPositionEnd,
                     final float maxFontH,
                     @NotNull final float[] individualWidths,
                     final float spaceWidth,
                     @NotNull final String string,
                     final PDFont currentFont,
                     final float fontSizeValue,
                     final int fontSizeInPt,
                     final float ws,
                     final int num)
{
	super(page, textPositionSt, textPositionEnd, maxFontH, individualWidths, spaceWidth, string, currentFont, fontSizeValue, fontSizeInPt, ws);

	sequenceNum = num;

	if ("∫".equals(string)) {
		System.out.println("this = " + this);
	}

	float x = getX();
	float y = getY();
	float w = getWidth();
	float h = getHeight();

	//	float x = getXDirAdj();
	//	float y = getYDirAdj();
	//	float w = getWidthDirAdj();
	//	float h = getHeightDir();

	if (h <= 0.0f && w < 0.0f) {
		throw new IllegalArgumentException("Passed text '" + string + "' with no size.");
	}

	if (h <= 0.0f) {
		float sumwidths = 0.0f;
		int widthsCounted = 0;
		for (int i = 0; i < individualWidths.length; i++) {
			float width = individualWidths[i];
			if (width > 0.0f) {
				widthsCounted++;
				sumwidths += width;
			}
		}
		if (widthsCounted == 0) {
			h = getWidth() / (float) string.length();
		} else {
			h = sumwidths / (float) widthsCounted;
		}
		/* maintaining that characters tend to be higher than they are wide, increase the height
			somewhat (but don't double it ) */
		h *= 1.5;

		if (log.isDebugEnabled()) {
			log.debug(String.format("LOG00630:Guessing height of text %s at (%s,"
					                        + "%s). height = %f", string, x, y, h));
		}
	}

	if (w <= 0.0f) {
		/* it so happens some times that some fonts apparently does not have proper tempW data
					*   for all glyphs. if we come accross a textposition where some of the individual widths
					*   are 0, take a guess at how wide it might be and use that value instead. update total
					*   tempW accordingly */
		float tempW = 0.0f;
		boolean adjustedWidth = false;
		final float guessedCharWidth = h * 0.5f;

		for (int i = 0; i < individualWidths.length; i++) {
			if (individualWidths[i] <= 0.0f) {
				individualWidths[i] = guessedCharWidth;
				adjustedWidth = true;
			}
			tempW += individualWidths[i];
		}

		if (adjustedWidth) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("LOG00630:Guessing width of text %s at (%s,"
						                        + "%s). adjusted from %f to %f", string, getX(), getY(), getWidth(), tempW));
			}
			;
		} else {
			assert MathUtils.isWithinVariance(w, tempW, 0.01f);
		}
		w = tempW;
	}

	pos = new Rectangle(x, y, w, h);
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public Rectangle getPos() {
	return pos;
}

public int getSequenceNum() {
	return sequenceNum;
}

public boolean isSmallOperator() {
	return smallOperator;
}

// -------------------------- PUBLIC METHODS --------------------------

public void setIsSmallOperator(final boolean smallOperator) {
	if (log.isInfoEnabled()) {
		log.info("LOG00680:" + this + " is small operator");
	}
	this.smallOperator = smallOperator;
}
}