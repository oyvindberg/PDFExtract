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
import org.elacin.pdfextract.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.lang.Character.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:39 PM <p/> <p/> This class
 * is meant to belong to a document, so that all the styles used in a document will be available
 * here. There is some optimization to avoid excessive object creation.
 */
public class DocumentStyles implements Serializable, XmlPrinter {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(DocumentStyles.class);

private static List<String> mathFonts = new ArrayList<String>() {{
	add("CMSY");
	add("CMEX");
	add("CMMI");
}};


final Map<String, Style> styles = new HashMap<String, Style>();

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface XmlPrinter ---------------------

@Override
public void writeXmlRepresentation(final Appendable out,
                                   final int indent,
                                   final boolean verbose) throws IOException
{
	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}

	out.append("<styles>\n");

	/* output the styles sorted by id */
	final Comparator<Style> sortById = new Comparator<Style>() {
		@Override
		public int compare(final Style o1, final Style o2) {
			return o1.id.compareTo(o2.id);
		}
	};

	List<Style> sortedStyles = new ArrayList<Style>(styles.values());
	Collections.sort(sortedStyles, sortById);
	for (Style style : sortedStyles) {
		for (int i = 0; i < indent + 4; i++) {
			out.append(" ");
		}
		out.append("<style");
		out.append(" id=\"").append(String.valueOf(style.id)).append("\"");
		out.append(" font=\"").append(style.fontName).append("\"");
		out.append(" size=\"").append(String.valueOf(style.xSize)).append("\"");
		if (style.isItalic()) {
			out.append(" italic=\"true\"");
		}
		if (style.isMathFont()) {
			out.append(" math=\"true\"");
		}
		if (style.isBold()) {
			out.append(" bold=\"true\"");
		}
		out.append("/>\n");
	}

	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}

	out.append("</styles>\n");
}

// -------------------------- PUBLIC METHODS --------------------------

public Style getStyleForTextPosition(@NotNull TextPosition position) {


	int xSize = (int) (position.getFontSize() * position.getXScale());
	int ySize = (int) (position.getFontSize() * position.getYScale());


	/* find the appropriate font name to use  - baseFont might sometimes be null*/
	String font;
	if (position.getFont().getBaseFont() == null) {
		font = position.getFont().getSubType();
	} else {
		font = position.getFont().getBaseFont();
	}

	/* a lot of embedded fonts have names like XCSFS+Times, so remove everything before and
	including '+' */
	final int plusIndex = font.indexOf('+');
	if (plusIndex != -1) {
		font = font.substring(plusIndex + 1, font.length());
	}

	boolean mathFont = font.length() > 4 && mathFonts.contains(font.substring(0, 4));
	boolean bold = font.toLowerCase().contains("bold");
	boolean italic = font.toLowerCase().contains("italic");


	/**
	 * Find the plain font name, without any other information
	 * Three typical font names can be:
	 * - LPPMinionUnicode-Italic
	 * - LPPMyriadCondLightUnicode (as apposed to for example LPPMyriadCondUnicode and
	 * LPPMyriadLightUnicode-Bold)
	 * - Times-Bold (Type1)
	 *
	 * I want to separate the LPPMinion part for example from the first, so i look for the
	 *  index of the first capital letter after a small one.
	 *  Also stop if we reach an '-' or whitespace, as that is a normal separators
	 * */

	boolean foundLowercase = false;
	int index = -1;
	for (int i = 0; i < font.length(); i++) {
		final char ch = font.charAt(i);

		if (!foundLowercase && isLowerCase(ch)) {
			foundLowercase = true;
		} else if (foundLowercase && isUpperCase(ch)) {
			index = i;
			break;
		} else if ('-' == ch || isWhitespace(ch)) {
			index = i;
			break;
		}
	}

	if (index != -1) {
		font = font.substring(0, index);
	}


	/* this is latex specific */
	if (font.contains("CMBX")) {
		font = font.replace("CMBX", "CMR");
		bold = true;
		italic = false;
	} else if (font.contains("CMTI")) {
		font = font.replace("CMTI", "CMR");
		bold = false;
		italic = true;
	}

	String id = String.format("%s-%d%s%s%s", font, xSize, (italic ? "I" : ""), (bold ? "B" : ""),
	                          (mathFont ? "M" : ""));

	if (!styles.containsKey(id)) {
		final Style style = new Style(font, xSize, ySize, id, italic, bold, mathFont);
		styles.put(id, style);
		if (log.isInfoEnabled()) {
			log.info("LOG00800:New style:" + style + " from " + StringUtils
					.getTextPositionString(position));
		}
	}

	return styles.get(id);
}
}