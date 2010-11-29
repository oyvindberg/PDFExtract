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

package org.elacin.pdfextract.physical.content;

import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 3, 2010 Time: 4:37:52 AM To change this template
 * use File | Settings | File Templates.
 */
public abstract class PhysicalContent implements HasPosition {
// ------------------------------ FIELDS ------------------------------

protected Rectangle pos;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalContent(final Rectangle pos) {
	this.pos = pos;
}

public PhysicalContent(@NotNull final Collection<? extends PhysicalContent> contents) {
	setPositionFromContentList(contents);
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

public Rectangle getPos() {
	return pos;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public boolean equals(@Nullable final Object o) {
	if (this == o) { return true; }
	if (o == null || getClass() != o.getClass()) { return false; }

	final PhysicalContent content = (PhysicalContent) o;

	if (!pos.equals(content.pos)) { return false; }

	return true;
}

@Override
public int hashCode() {
	return pos.hashCode();
}

@Override
public String toString() {
	final StringBuilder sb = new StringBuilder();
	sb.append(getClass().getSimpleName());
	sb.append("{position=").append(pos);
	sb.append('}');
	return sb.toString();
}

// --------------------- GETTER / SETTER METHODS ---------------------

protected void setPos(final Rectangle pos) {
	this.pos = pos;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public AssignablePhysicalContent getAssignablePhysicalContent() {
	throw new RuntimeException("not an AssignablePhysicalContent");
}

@NotNull
public GraphicContent getGraphicContent() {
	throw new RuntimeException("not a graphic");
}

@NotNull
public PhysicalText getPhysicalText() {
	throw new RuntimeException("not a text");
}

public boolean isAssignablePhysicalContent() {
	return false;
}

public boolean isFigure() {
	return false;
}

public boolean isGraphic() {
	return false;
}

public boolean isPicture() {
	return false;
}

public boolean isText() {
	return false;
}

public boolean isWhitespace() {
	return false;
}

// -------------------------- OTHER METHODS --------------------------

protected final void setPositionFromContentList(@NotNull final Collection<? extends
		PhysicalContent> contents)
{
	if (contents.isEmpty()) {
		//TODO: handle empty regions in a better way
		pos = new Rectangle(0.1f, 0.1f, 0.1f, 0.1f);
		return;
	}

	/* calculate bounds for this region */
	float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
	float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

	for (PhysicalContent content : contents) {
		minX = Math.min(minX, content.pos.getX());
		minY = Math.min(minY, content.pos.getY());
		maxX = Math.max(maxX, content.pos.getEndX());
		maxY = Math.max(maxY, content.pos.getEndY());
	}
	pos = new Rectangle(minX, minY, maxX - minX, maxY - minY);
}
}
