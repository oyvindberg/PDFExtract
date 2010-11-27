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


import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:20 PM <p/> A style is a
 * collection of a font (described by a string), its X and Y sizes, and an estimate of the widt of a
 * space in this font. All these properties are immutable.
 */
public class Style implements Serializable {
// ------------------------------ FIELDS ------------------------------

public static final Style GRAPHIC = new Style("Image", -1, -1, -1, false, false, false);
public static final Style FORMULA = new Style("Formula", -2, -2, -2, false, false, true);


public final int xSize, ySize;
public final  String  fontName;
public final  int     id;
private final boolean italic;
private final boolean bold;
private final boolean mathFont;

private transient boolean toStringCreated;
private transient String  toStringCache;

// --------------------------- CONSTRUCTORS ---------------------------

Style(final String fontName,
      final int xSize,
      final int ySize,
      final int id,
      final boolean italic,
      final boolean bold,
      final boolean font)
{
	this.fontName = fontName;
	this.xSize = xSize;
	this.ySize = ySize;
	this.id = id;
	this.italic = italic;
	this.bold = bold;
	mathFont = font;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public boolean equals(final Object o) {
	if (this == o) { return true; }
	if (o == null || getClass() != o.getClass()) { return false; }

	final Style style = (Style) o;

	if (id != style.id) { return false; }

	return true;
}

@Override
public int hashCode() {
	return id;
}

@Override
public String toString() {
	if (!toStringCreated) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(fontName);
		sb.append(", size=").append(xSize);
		if (italic) {
			sb.append(" (italic)");
		}
		if (bold) {
			sb.append(" (bold)");
		}
		if (mathFont) {
			sb.append(" (math)");
		}
		sb.append('}');
		toStringCache = sb.toString();
		toStringCreated = true;
	}

	return toStringCache;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isBold() {
	return bold;
}

public boolean isItalic() {
	return italic;
}

public boolean isMathFont() {
	return mathFont;
}

// -------------------------- PUBLIC METHODS --------------------------

public boolean isCompatibleWith(final Style other) {

	if (mathFont != other.mathFont){
		return false;
	}

	if (xSize != other.xSize) {
		return false;
	}

	if (ySize != other.ySize) {
		return false;
	}

	if (bold != other.bold) {
		return false;
	}

	if (italic != other.italic) {
		return false;
	}

	return fontName.equals(other.fontName);
}
}
