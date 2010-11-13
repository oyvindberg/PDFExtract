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

package org.elacin.pdfextract.style;

import org.apache.log4j.Logger;
import org.apache.pdfbox.util.TextPosition;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:39 PM <p/> <p/> This class
 * is meant to belong to a document, so that all the styles used in a document will be available
 * here. There is some optimization to avoid excessive object creation.
 */
public class DocumentStyles implements Serializable {
// ------------------------------ FIELDS ------------------------------

private static final Logger            log    = Logger.getLogger(DocumentStyles.class);
@NotNull
final                Map<Float, Style> styles = new HashMap<Float, Style>();

final Collection<Style> stylesCollection = styles.values();

// -------------------------- STATIC METHODS --------------------------

@NotNull
private static Style getStyle(float xSize, float ySize, String font) {
	return new Style(font, xSize, ySize);
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public Map<Float, Style> getStyles() {
	return styles;
}

// -------------------------- PUBLIC METHODS --------------------------

public Style getStyleForTextPosition(@NotNull TextPosition position) {
	float result = position.getFontSize();
	result = 31.0F * result + position.getXScale();
	result = 31.0F * result + position.getYScale();
	result = 31.0F * result + (float) position.getFont().hashCode();
	result = 31.0F * result + position.getWidthOfSpace();

	Style existing = styles.get(result);
	if (existing == null) {
		float realFontSizeX = position.getFontSize() * position.getXScale();
		float realFontSizeY = position.getFontSize() * position.getYScale();

		/* build a string with fontname / type */
		final String baseFontName = position.getFont().getBaseFont()
				== null ? "null" : position.getFont().getBaseFont();

		final String fontname = baseFontName + " (" + position.getFont().getSubType() + ")";

		existing = getStyle(realFontSizeX, realFontSizeY, fontname);
		styles.put(result, existing);
	}


	return existing;
}
}
