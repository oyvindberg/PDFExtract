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

import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:36:44 PM To change this
 * template use File | Settings | File Templates.
 */
public class PhysicalText extends AssignablePhysicalContent {
// ------------------------------ FIELDS ------------------------------

public final float  distanceToPreceeding;
public       float  charSpacing;
public final String content;
public final Style  style;
public final int    rotation;
@NotNull
private final TreeSet<Integer> seqNums = new TreeSet<Integer>();

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalText(final String content,
                    final Style style,
                    final float x,
                    final float y,
                    final float width,
                    final float height,
                    final float distanceToPreceeding,
                    final int rotation,
                    final int seqNo)
{
	this(content, style, new Rectangle(x,
	                                   y * 1.01f, width,
	                                   height * 0.98f), distanceToPreceeding, rotation);
	seqNums.add(seqNo);
}

PhysicalText(final String content,
             final Style style,
             final Rectangle position,
             final float distanceToPreceeding,
             final int rotation)
{
	super(position);
	this.distanceToPreceeding = distanceToPreceeding;
	this.style = style;
	this.content = content;
	this.rotation = rotation;
}

// ------------------------ CANONICAL METHODS ------------------------

@Override
public String toString() {
	final StringBuilder sb = new StringBuilder();
	sb.append("Text");
	sb.append("{d=").append(distanceToPreceeding);
	sb.append(", pos=").append(position);
	sb.append(", text='").append(content).append('\'');
	sb.append(", style=").append(style);
	sb.append(", charSpacing=").append(charSpacing);
	sb.append(", seqNums=").append(seqNums);
	sb.append('}');
	return sb.toString();
}

// ------------------------ OVERRIDING METHODS ------------------------

@NotNull
@Override
public PhysicalText getText() {
	return this;
}

@Override
public boolean isText() {
	return true;
}

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static boolean isCloseEnoughToBelongToSameWord(@NotNull final PhysicalText otherText) {
	return otherText.distanceToPreceeding <= otherText.charSpacing;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public float getCharSpacing() {
	return charSpacing;
}

public String getContent() {
	return content;
}

public float getDistanceToPreceeding() {
	return distanceToPreceeding;
}

public int getRotation() {
	return rotation;
}

@NotNull
public TreeSet<Integer> getSeqNums() {
	return seqNums;
}

public Style getStyle() {
	return style;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public PhysicalText combineWith(@NotNull final PhysicalText next) {
	final PhysicalText newText = new PhysicalText(content
			                                              + next.content, style, position.union(next.position), distanceToPreceeding, rotation);
	newText.seqNums.addAll(seqNums);
	newText.seqNums.addAll(next.seqNums);
	return newText;
}

public float getAverageCharacterWidth() {
	return getPosition().getWidth() / (float) getContent().length();
}

public boolean isSameStyleAs(@NotNull final PhysicalText next) {
	return style.equals(next.style);
}
}
