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


import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Mar 18, 2010 Time: 2:32:20 PM <p/> A style is a
 * collection of a font (described by a string), its X and Y sizes, and an estimate of the widt of a
 * space in this font. All these properties are immutable.
 */
public class Style implements Serializable {
// ------------------------------ FIELDS ------------------------------

public final float xSize, ySize;
public final String font;

private transient boolean toStringCreated;
private transient String  toStringCache;

// --------------------------- CONSTRUCTORS ---------------------------

Style(final String font, final float xSize, final float ySize) {

	this.font = font;
	this.xSize = xSize;
	this.ySize = ySize;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public boolean equals(@Nullable final Object o) {

	if (this == o) {
		return true;
	}
	if (o == null || getClass() != o.getClass()) {
		return false;
	}

	final Style style = (Style) o;

	if (Float.compare(style.xSize, xSize) != 0) {
		return false;
	}
	if (Float.compare(style.ySize, ySize) != 0) {
		return false;
	}
	if (!font.equals(style.font)) {
		return false;
	}

	return true;
}

@Override
public int hashCode() {

	int result = (xSize != +0.0f ? Float.floatToIntBits(xSize) : 0);
	result = 31 * result + (ySize != +0.0f ? Float.floatToIntBits(ySize) : 0);
	result = 31 * result + font.hashCode();
	return result;
}

@Override
public String toString() {

	if (!toStringCreated) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(font);
		sb.append(", x=").append(xSize);
		sb.append(", y=").append(ySize);
		sb.append('}');
		toStringCache = sb.toString();
		toStringCreated = true;
	}

	return toStringCache;
}
}
