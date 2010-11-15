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

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.TextPosition;
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

private final Rectangle pos;
// --------------------------- CONSTRUCTORS ---------------------------

public ETextPosition(final PDPage page,
                     final Matrix textPositionSt,
                     final Matrix textPositionEnd,
                     final float maxFontH,
                     final float[] individualWidths,
                     final float spaceWidth,
                     final String string,
                     final PDFont currentFont,
                     final float fontSizeValue,
                     final int fontSizeInPt,
                     final float ws)
{
	super(page, textPositionSt, textPositionEnd, maxFontH, individualWidths, spaceWidth, string,
	      currentFont, fontSizeValue, fontSizeInPt, ws);
	pos = new Rectangle(getX(), getY(), getWidth(), getHeight());
	//	pos = new Rectangle(getXDirAdj(), getYDirAdj(), getWidthDirAdj(), getHeightDir());
}


// --------------------- GETTER / SETTER METHODS ---------------------


@NotNull
public Rectangle getPos() {
	return pos;
}

}