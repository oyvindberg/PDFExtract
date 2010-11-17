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
import org.elacin.pdfextract.tree.XmlPrinter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:39 PM <p/> <p/> This class
 * is meant to belong to a document, so that all the styles used in a document will be available
 * here. There is some optimization to avoid excessive object creation.
 */
public class DocumentStyles implements Serializable, XmlPrinter {
// ------------------------------ FIELDS ------------------------------

private static final Logger              log    = Logger.getLogger(DocumentStyles.class);
@NotNull
final                Map<Integer, Style> styles = new HashMap<Integer, Style>();

final Collection<Style> stylesCollection = styles.values();

// -------------------------- STATIC METHODS --------------------------

@NotNull
private static Style getStyle(int xSize, int ySize, String font, int id) {
	return new Style(font, xSize, ySize, id);
}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public Map<Integer, Style> getStyles() {
	return styles;
}

// -------------------------- PUBLIC METHODS --------------------------

public Style getStyleForTextPosition(@NotNull TextPosition position) {
	int result = (int) position.getFontSize();
	result = 31 * result + (int) position.getXScale();
	result = 31 * result + (int) position.getYScale();
	final String baseFont = position.getFont().getBaseFont();
	result = 31 * result + (baseFont == null ? 0 : baseFont.hashCode());
	result = 31 * result + position.getFont().getSubType().hashCode();

	Style existing = styles.get(result);
	if (existing == null) {
		int realFontSizeX = (int) (position.getFontSize() * position.getXScale());
		int realFontSizeY = (int) (position.getFontSize() * position.getYScale());

		/* build a string with fontname / type */
		String baseFontName = position.getFont().getBaseFont()
				== null ? "null" : position.getFont().getBaseFont();

		final int plusIndex = baseFontName.indexOf("+");
		if (plusIndex != -1) {
			baseFontName = baseFont.substring(plusIndex + 1);
		}

		final String fontname = baseFontName + " (" + position.getFont().getSubType() + ")";

		existing = getStyle(realFontSizeX, realFontSizeY, fontname, styles.size());
		styles.put(result, existing);
	}


	return existing;
}

@Override
public void writeXmlRepresentation(final Appendable out,
                                   final int indent,
                                   final boolean verbose) throws IOException
{

	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}
	out.append("<styles>\n");
	for (Style style : stylesCollection) {
		for (int i = 0; i < indent + 4; i++) {
			out.append(" ");
		}
		out.append("<style");
		out.append(" id=\"").append(String.valueOf(style.id)).append("\"");
		out.append(" font=\"").append(style.font).append("\"");
		out.append(" size=\"").append(String.valueOf(style.xSize)).append("\"");
		out.append("/>\n");
	}

	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}

	out.append("</styles>\n");

}
}
