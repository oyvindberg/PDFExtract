/*
 * Copyright 2010 √òyvind Berg (elacin@gmail.com)
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
import org.elacin.pdfextract.physical.segmentation.line.LineSegmentator;
import org.elacin.pdfextract.physical.segmentation.paragraph.ParagraphSegmentator;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.RectangleCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 8, 2010 Time: 7:44:41 PM To change this template
 * use File | Settings | File Templates.
 */
public class PhysicalPageRegion extends RectangleCollection {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPageRegion.class);

protected final LineSegmentator lineSegmentator = new LineSegmentator(5.0f);

protected final ParagraphSegmentator paragraphSegmentator = new ParagraphSegmentator();

/* average font sizes for this page region */
private transient boolean fontInfoFound;
private transient float   _avgFontSizeX;
private transient float   _avgFontSizeY;
private transient Style   _mostCommonStyle;

/* the physical page containing this region */
private final int pageNumber;

@NotNull
private final List<WhitespaceRectangle> whitespace = new ArrayList<WhitespaceRectangle>();
@NotNull
private final List<PhysicalPageRegion>  subregions = new ArrayList<PhysicalPageRegion>();

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPageRegion(@NotNull final Collection<? extends PhysicalContent> contents,
                          final @Nullable PhysicalContent containedIn,
                          final int pageNumber)
{
	super(contents, containedIn);
	this.pageNumber = pageNumber;
}

public PhysicalPageRegion(@NotNull final List<? extends PhysicalContent> contents,
                          final int pageNumber)
{
	this(contents, null, pageNumber);
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
protected void clearCache() {
	super.clearCache();
	fontInfoFound = false;
}

// -------------------------- STATIC METHODS --------------------------

protected static boolean columnContainsBlockingGraphics(@NotNull final List<PhysicalContent> column,
                                                        final float x,
                                                        final float minY,
                                                        final float maxY)
{
	HasPosition search = new Rectangle(x, minY, 1.0f, maxY - minY);
	for (PhysicalContent obstacle : column) {
		if (obstacle.isGraphic()) {
			if (obstacle.getPos().intersectsWith(search)) {
				return true;
			}
		}
	}
	return false;
}

private static boolean listContainsText(@NotNull final Collection<PhysicalContent> workingSet) {
	for (PhysicalContent content : workingSet) {
		if (content.isText()) {
			return true;
		}
	}
	return false;
}

private static boolean listContainsTextOrSeparator(@NotNull final Collection<PhysicalContent> workingSet) {
	for (PhysicalContent content : workingSet) {
		if (content.isText()) {
			return true;
		}
		if (content.isGraphic() && content.getGraphicContent().isSeparator()) {
			return true;
		}
	}
	return false;
}

@NotNull
protected static Rectangle returnABitSmallerPosition(@NotNull final HasPosition bound) {
	final Rectangle p = bound.getPos();
	return new Rectangle(p.getX() + 1.0f, p.getY() + 1.0f, p.getWidth() - 1.0f,
	                     p.getHeight() - 1.0f);
}

protected static boolean tooMuchContentCrossesBoundary(@NotNull RectangleCollection contents,
                                                       @NotNull final HasPosition boundary)
{
	List<PhysicalContent> list = contents.findContentAtXIndex(boundary.getPos().getX());
	list.addAll(contents.findContentAtXIndex(boundary.getPos().getEndX()));

	int intersecting = 0;
	int intersectingLimit = 2;

	for (PhysicalContent content : list) {
		if (content.isText()) {
			boolean makesFiltered = false;
			/* starts left of picture, and ends within it */
			if (content.getPos().getX() < boundary.getPos().getX() - 1.0f
					&& content.getPos().getEndX() > boundary.getPos().getX() + 1.0f) {
				makesFiltered = true;
				if (log.isInfoEnabled()) {
					log.info("LOG00300: graphics = " + boundary + ", content = " + content);
				}
			}
			/* starts inside picture, and ends right of it */
			if ((content.getPos().getEndX() > boundary.getPos().getEndX() + 1.0f
					&& content.getPos().getX() < boundary.getPos().getEndX())) {
				makesFiltered = true;
				if (log.isInfoEnabled()) {
					log.info("LOG00310:graphics = " + boundary + ", content = " + content);
				}
			}

			if (makesFiltered) {
				intersecting++;
				if (intersecting >= intersectingLimit) {
					return true;
				}
			}
		}
	}
	return false;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public int getPageNumber() {
	return pageNumber;
}

@NotNull
public List<PhysicalPageRegion> getSubregions() {
	return subregions;
}

@NotNull
public List<WhitespaceRectangle> getWhitespace() {
	return whitespace;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public List<ParagraphNode> createParagraphNodes() {
	paragraphSegmentator.setContainedInGraphic(isContainedInGraphic());
	paragraphSegmentator.setMedianVerticalSpacing(findMedianOfVerticalDistancesForRegion());

	final List<LineNode> lines = lineSegmentator.segmentLines(this);

	final List<ParagraphNode> ret = paragraphSegmentator.segmentParagraphs(lines);

	for (PhysicalPageRegion subregion : subregions) {
		ret.addAll(subregion.createParagraphNodes());
	}

	return ret;
}

/**
 * Returns a subregion with all the contents which is contained by bound. If more than two pieces of
 * content crosses the boundary of bound, it is deemed inappropriate for dividing the page, and an
 * exception is thrown
 *
 * @return the new region
 */
@Nullable
public PhysicalPageRegion extractSubRegion(@NotNull final HasPosition bound,
                                           @Nullable final PhysicalContent containedIn)
{
	/* decrease the area within which we are looking for content a big, this is related to
	*   the neverending problems of iffy character positioning for some fonts*/
	final Rectangle smallerBound = returnABitSmallerPosition(bound);
	final List<PhysicalContent> subContents = findContentsIntersectingWith(smallerBound);

	return extractSubRegionFromContentList(bound, containedIn, subContents);
}

public float getAvgFontSizeX() {
	if (!fontInfoFound) {
		findAndSetFontInformation();
	}
	return _avgFontSizeX;
}

public float getAvgFontSizeY() {
	if (!fontInfoFound) {
		findAndSetFontInformation();
	}
	return _avgFontSizeY;
}

public Style getMostCommonStyle() {
	if (!fontInfoFound) {
		findAndSetFontInformation();
	}
	return _mostCommonStyle;
}


@NotNull
public List<PhysicalPageRegion> splitInVerticalColumns() {
	List<PhysicalPageRegion> ret = new ArrayList<PhysicalPageRegion>();

	/**
	 * Dont bother splitting columns narrower than this
	 */
	if (getPos().getWidth() < getAvgFontSizeX() * 3) {
		return ret;
	}

	/* state for the algorithm */
	Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();
	PhysicalContent currentUpperRightAddedToWorkingSet = null;
	float lastBoundary = -1000.0f;
	float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;


	/**
	 * check for every x index if is a column boundary
	 */
	for (float x = getPos().getX(); x <= getPos().getEndX(); x++) {
		/* start by finding the content of this column, that is everything which
			intersects, and make a decision based on that
		 */
		final List<PhysicalContent> column = findContentAtXIndex(x);
		workingSet.addAll(column);

		/* keep track of current vertical bounds */
		for (PhysicalContent newContent : column) {
			minY = Math.min(newContent.getPos().getY(), minY);
			maxY = Math.max(newContent.getPos().getEndY(), maxY);
		}

		/**
		 * Check if this column could be a boundary... there are a whole group of checks here :)
		 */
		if (!listContainsText(column)) {
			if (!listContainsText(workingSet)) {
				continue;
			}

			if (columnContainsBlockingGraphics(column, x, minY, maxY)) {
				continue;
			}

			if (containsAllRemainingContent(workingSet)) {
				continue;
			}

			if (columnBoundaryWouldBeTooNarrow(lastBoundary, x)) {
				/* accept it anyway if it divides a lot of text*/
				if (workingSet.size() < 20) {
					continue;
				}

				/* check also how much text exists to the right of this boundary */
				HasPosition search = new Rectangle(x, minY, getPos().getEndX(), maxY);
				final List<PhysicalContent> contentRight = findContentsIntersectingWith(search);
				if (contentRight.size() < 20) {
					continue;
				}
			}

			if (checkIfBelongsWithContentOnRight(currentUpperRightAddedToWorkingSet)) {
				continue;
			}


			if (log.isInfoEnabled()) {
				log.info(String.format("LOG00510:vertical split at x =%s for %s ", x, this));
			}
			PhysicalPageRegion sub = extractSubRegionFromContentList(null, getContainedIn(),
			                                                         workingSet);

			/* reset vertical values */
			minY = Float.MAX_VALUE;
			maxY = Float.MIN_VALUE;

			if (sub != null) {
				ret.add(sub);
			}

			workingSet.clear();
			lastBoundary = x;
		} else {
			lastBoundary = x;
			currentUpperRightAddedToWorkingSet = column.get(0);
		}
	}

	return ret;
}

// -------------------------- OTHER METHODS --------------------------

@Nullable
protected PhysicalPageRegion extractSubRegionFromContentList(@Nullable final HasPosition bound,
                                                             @Nullable final PhysicalContent containedIn,
                                                             @NotNull final Collection<PhysicalContent> subContents)
{
	if (subContents.isEmpty()) {
		log.warn("LOG00370:Tried to extract empty region " + bound);
		return null;
	}

	PhysicalContent contained = containedIn != null ? containedIn : this;


	final PhysicalPageRegion newRegion = new PhysicalPageRegion(subContents, contained, pageNumber);
	if (bound != null) {
		if (tooMuchContentCrossesBoundary(newRegion, bound)) {
			throw new RuntimeException("Too much content crossed the bounds we tried to divide by."
					                           + "This happens often with background pictures. "
					                           + bound);
		}
	}

	removeContent(subContents);

	if (isContainedInGraphic()) {
		subregions.add(newRegion);
		return null;
	}

	return newRegion;
}

private Style findDominatingStyleFor(final List<PhysicalContent> contents) {
	Map<Style, Integer> letterCountPerStyle = new HashMap<Style, Integer>(10);
	boolean textFound = false;

	for (PhysicalContent content : contents) {
		if (content.isText()) {
			final Style style = content.getPhysicalText().getStyle();
			if (!letterCountPerStyle.containsKey(style)) {
				letterCountPerStyle.put(style, 0);
			}
			final int numChars = content.getPhysicalText().getText().length();
			letterCountPerStyle.put(style, letterCountPerStyle.get(style) + numChars);
			textFound = true;
		}
	}

	assert textFound;

	int highestNumChars = -1;
	Style style = null;
	for (Map.Entry<Style, Integer> entry : letterCountPerStyle.entrySet()) {
		if (entry.getValue() > highestNumChars) {
			style = entry.getKey();
			highestNumChars = entry.getValue();
		}
	}
	return style;
}

protected boolean checkIfBelongsWithContentOnRight(@Nullable final PhysicalContent content) {
	if (content == null || !content.isText()) {
		return false;
	}

	final float y = content.getPos().getY() + content.getPos().getHeight() * 0.5f;
	final List<PhysicalContent> row = findContentAtYIndex(y);
	final int index = row.indexOf(content);
	if (index == -1) {
		throw new RuntimeException("Region " + this + " Could not find " + content + " at y=" + y);
	}

	if (index == row.size() - 1) {
		return false;
	}

	final PhysicalContent next = row.get(index + 1);
	if (!next.isText()) {
		return false;
	}

	/** i'll consider this too far away for now - splitting this distance should be fine*/
	if (next.getPos().distance(getPos()) > (float) (content.getPhysicalText().style.xSize * 3)) {
		return false;
	}

	return next.getPhysicalText().getSeqNums().first()
			== content.getPhysicalText().getSeqNums().last() + 1;
}

protected boolean columnBoundaryWouldBeTooNarrow(final float lastBoundary, final float x) {
	return x - lastBoundary < getAvgFontSizeX() * 0.5f;
}

protected boolean containsAllRemainingContent(@NotNull final Collection<PhysicalContent> list) {
	return list.size() == getContents().size();
}

/* find average font sizes, and most used style for the region */

protected void findAndSetFontInformation() {
	float xFontSizeSum = 0.0f, yFontSizeSum = 0.0f;
	int numCharsFound = 0;
	for (PhysicalContent content : getContents()) {
		if (content.isText()) {
			final Style style = content.getPhysicalText().getStyle();

			final int length = content.getPhysicalText().getText().length();
			xFontSizeSum += (float) (style.xSize * length);
			yFontSizeSum += (float) (style.ySize * length);
			numCharsFound += length;
		}
	}
	if (numCharsFound == 0) {
		_avgFontSizeX = Float.MIN_VALUE;
		_avgFontSizeY = Float.MIN_VALUE;
		_mostCommonStyle = null;
	} else {
		_avgFontSizeX = xFontSizeSum / (float) numCharsFound;
		_avgFontSizeY = yFontSizeSum / (float) numCharsFound;
		_mostCommonStyle = findDominatingStyleFor(getContents());
	}
	fontInfoFound = true;
}

/**
 * Finds an approximation of the normal vertical line spacing for the region.
 *
 * This is done by looking at three vertical rays, calculating distances between all the lines
 * intersecting those lines, and then returning the median of all those distances.
 *
 * minimum value is 2, max is (avgFontSize * 3) -1. -1 if nothing is found;
 */
protected int findMedianOfVerticalDistancesForRegion() {
	final int LIMIT = (int) getAvgFontSizeY() * 3;

	int[] distanceCount = new int[LIMIT];
	final Rectangle pos = getPos();
	for (float x = pos.getX(); x <= pos.getEndX(); x += pos.getWidth() / 3) {
		final List<PhysicalContent> column = findContentAtXIndex(x);
		for (int i = 1; i < column.size(); i++) {
			final PhysicalContent current = column.get(i - 1);
			final PhysicalContent below = column.get(i);

			/* increase count for this distance (rounded down to an int) */
			final int d = (int) (below.getPos().getY() - current.getPos().getEndY());
			if (d > 0 && d < LIMIT) {
				distanceCount[d]++;
			}
		}
	}

	int highestFrequency = -1;
	int index = -1;
	for (int i = 2; i < distanceCount.length; i++) {
		if (distanceCount[i] >= highestFrequency) {
			index = i;
			highestFrequency = distanceCount[i];
		}
	}

	return index + 1;
}

protected boolean isContainedInGraphic() {
	return getContainedIn() != null && getContainedIn().isGraphic() && !getContainedIn()
			.getGraphicContent().isBackgroundColor();
}
}

