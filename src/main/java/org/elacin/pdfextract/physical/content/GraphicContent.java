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
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 3, 2010 Time: 4:43:12 PM To change this template
 * use File | Settings | File Templates.
 */
public class GraphicContent extends AssignablePhysicalContent {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(GraphicContent.class);
private final boolean filled;
private final boolean picture;

private boolean canBeAssigned;

// --------------------------- CONSTRUCTORS ---------------------------

public GraphicContent(final Rectangle position, boolean picture, boolean filled) {
	super(position);
	this.filled = filled;
	this.picture = picture;

	if (log.isDebugEnabled()) {
		log.debug("LOG00280:GraphicContent at " + position + ", filled: " + filled + ", picture = "
				          + picture);
	}
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public String toString() {
	final StringBuilder sb = new StringBuilder();
	sb.append("GraphicContent");
	sb.append("{canBeAssigned=").append(canBeAssigned);
	sb.append(", filled=").append(filled);
	sb.append(", picture=").append(picture);
	sb.append(", pos=").append(getPosition());
	sb.append('}');
	return sb.toString();
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
public boolean isAssignablePhysicalContent() {
	return canBeAssigned;
}

@Override
public boolean isFigure() {
	return true;
}

@Override
public boolean isPicture() {
	return picture;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isFilled() {
	return filled;
}

public void setCanBeAssigned(final boolean canBeAssigned) {
	this.canBeAssigned = canBeAssigned;
}

// -------------------------- PUBLIC METHODS --------------------------

public boolean canBeConsideredContentInRegion(@NotNull final PhysicalPageRegion region) {
	return getPosition().getHeight() < region.getAvgFontSizeY() * 2.0f
			&& getPosition().getWidth() < region.getAvgFontSizeX() * 2.0f;
}

/** consider the graphic a separator if the aspect ratio is really high */
public boolean canBeConsideredSeparatorInRegion(final PhysicalPageRegion page) {
	final Rectangle pos = getPosition();
	if (pos.getWidth() > 10.0f && pos.getHeight() > 10.0f) {
		return false;
	}

	return Math.max(pos.getWidth() / pos.getHeight(), pos.getHeight() / pos.getWidth()) > 15.0f;
}
}
