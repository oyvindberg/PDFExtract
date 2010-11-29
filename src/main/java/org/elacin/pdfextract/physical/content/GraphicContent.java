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

import org.apache.log4j.Logger;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 3, 2010 Time: 4:43:12 PM To change this template
 * use File | Settings | File Templates.
 */
public class GraphicContent extends AssignablePhysicalContent {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(GraphicContent.class);
private final boolean filled;
private final boolean picture;
private       boolean canBeAssigned;
private       boolean backgroundColor;

// --------------------------- CONSTRUCTORS ---------------------------

public GraphicContent(final Rectangle position,
                      boolean picture,
                      boolean filled,
                      boolean backgroundColor)
{
	super(position, null);
	this.filled = filled;
	this.picture = picture;
	this.backgroundColor = backgroundColor;

	if (log.isDebugEnabled()) {
		log.debug("LOG00280:GraphicContent at " + position + ", filled: " + filled + ", picture = "
				          + picture);
	}
}

public GraphicContent(final Rectangle position,
                      boolean picture,
                      boolean filled,
                      @NotNull Color color)
{
	this(position, picture, filled, color.equals(Color.white));
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public String toString() {
	final StringBuilder sb = new StringBuilder();
	sb.append("GraphicContent");
	sb.append("{canBeAssigned=").append(canBeAssigned);
	sb.append(", filled=").append(filled);
	sb.append(", picture=").append(picture);
	sb.append(", pos=").append(getPos());
	sb.append(", backgroundColor=").append(backgroundColor);
	sb.append('}');
	return sb.toString();
}

// ------------------------ OVERRIDING METHODS ------------------------

@NotNull
@Override
public GraphicContent getGraphicContent() {
	return this;
}

@Override
public boolean isAssignablePhysicalContent() {
	return canBeAssigned;
}

@Override
public boolean isFigure() {
	return !picture;
}

@Override
public boolean isGraphic() {
	return true;
}

@Override
public boolean isPicture() {
	return picture;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isBackgroundColor() {
	return backgroundColor;
}

public boolean isFilled() {
	return filled;
}

public void setCanBeAssigned(final boolean canBeAssigned) {
	this.canBeAssigned = canBeAssigned;
}

// -------------------------- PUBLIC METHODS --------------------------

public boolean canBeCombinedWith(@NotNull final GraphicContent other) {
	if (this == other) {
		return false;
	}

	if (isPicture() && !other.isPicture()) {
		return false;
	}

	return getPos().distance(other.getPos()) < 2.0f;
}

@NotNull
public GraphicContent combineWith(@NotNull final GraphicContent other) {
	final boolean isBackground = other.backgroundColor && backgroundColor;
	final boolean isFilled = other.filled || filled;
	return new GraphicContent(getPos().union(other.getPos()), picture, isFilled, isBackground);
}

public boolean isCharacter(@NotNull final PhysicalPageRegion region) {
	return getStyle() != null && getStyle().equals(Style.GRAPHIC_CHARACTER);
}

/** consider the graphic a separator if the aspect ratio is high */
public boolean isHorizontalSeparator() {
	return getStyle() != null && getStyle().equals(Style.GRAPHIC_HSEP);
}

public boolean isMathBar(final PhysicalPageRegion region) {
	return getStyle() != null && getStyle().equals(Style.GRAPHIC_MATH_BAR);
}

public boolean isSeparator() {
	return isVerticalSeparator() || isHorizontalSeparator();
}

/** consider the graphic a separator if the aspect ratio is high */
public boolean isVerticalSeparator() {
	return getStyle() != null && getStyle().equals(Style.GRAPHIC_VSEP);
}
}
