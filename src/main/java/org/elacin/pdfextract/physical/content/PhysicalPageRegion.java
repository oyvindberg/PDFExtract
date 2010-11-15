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
import org.elacin.pdfextract.physical.segmentation.line.LineSegmentator;
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

/* average font sizes for the page region */
private final float avgFontSizeX;
private final float avgFontSizeY;

/* the physical page containing this region */
private final int pageNumber;

@NotNull
private List<WhitespaceRectangle> whitespace = new ArrayList<WhitespaceRectangle>();

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPageRegion(@NotNull final Collection<? extends PhysicalContent> contents,
                          final @Nullable PhysicalContent containedIn,
                          final int pageNumber)
{
	super(contents, containedIn);
	this.pageNumber = pageNumber;

	/* find bounds and average font sizes for the page */
	float xFontSizeSum = 0.0f, yFontSizeSum = 0.0f;
	for (PhysicalContent content : contents) {
		if (content.isText()) {
			final PhysicalText text = content.getText();
			xFontSizeSum += text.getStyle().xSize;
			yFontSizeSum += text.getStyle().ySize;
		}
	}
	avgFontSizeX = xFontSizeSum / (float) contents.size();
	avgFontSizeY = yFontSizeSum / (float) contents.size();
}

public PhysicalPageRegion(@NotNull final List<? extends PhysicalContent> contents,
                          final int pageNumber)
{
	this(contents, null, pageNumber);
}

// -------------------------- STATIC METHODS --------------------------

private static boolean tooMuchContentCrossesBoundary(@NotNull RectangleCollection contents,
                                                     @NotNull final HasPosition boundary)
{
	List<PhysicalContent> list = contents.findContentAtXIndex(boundary.getPosition().getX());
	list.addAll(contents.findContentAtXIndex(boundary.getPosition().getEndX()));

	int intersecting = 0;
	//    int intersectingLimit = Math.max((int) (picture.getPosition().getHeight() / HIT_PER_PIXELS), 2);
	int intersectingLimit = 2;

	for (PhysicalContent content : list) {
		if (content.isText()) {
			boolean makesFiltered = false;
			/* starts left of picture, and ends within it */
			if (content.getPosition().getX() < boundary.getPosition().getX() - 1.0f
					&& content.getPosition().getEndX() > boundary.getPosition().getX() + 1.0f) {
				makesFiltered = true;
				if (log.isInfoEnabled()) {
					log.info("LOG00300: image = " + boundary + ", content = " + content);
				}
			}
			/* starts inside picture, and ends right of it */
			if ((content.getPosition().getEndX() > boundary.getPosition().getEndX() + 1.0f
					&& content.getPosition().getX() < boundary.getPosition().getEndX())) {
				makesFiltered = true;
				if (log.isInfoEnabled()) {
					log.info("LOG00310:image = " + boundary + ", content = " + content);
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

public float getAvgFontSizeX() {
	return avgFontSizeX;
}

public float getAvgFontSizeY() {
	return avgFontSizeY;
}

public int getPageNumber() {
	return pageNumber;
}

@NotNull
public List<WhitespaceRectangle> getWhitespace() {
	return whitespace;
}

// -------------------------- PUBLIC METHODS --------------------------

public void addWhitespace(final Collection<WhitespaceRectangle> whitespace) {
	this.whitespace.addAll(whitespace);
	addContent(whitespace);
}

@NotNull
public List<ParagraphNode> createParagraphNodes() {
	//	/* start off by finding whitespace */
	//	final List<WhitespaceRectangle> whitespace = new LayoutRecognizer().findWhitespace(this);
	//	addWhitespace(whitespace);
	//
	//	/* then follow the trails left between the whitespace and construct blocks of text from that */
	//	int blockNum = 0;
	//	for (float y = getPosition().getY(); y < getPosition().getEndY(); y++) {
	//		final List<PhysicalContent> row = findContentAtYIndex(y);
	//
	//		/* iterate through the line to find possible start of blocks */
	//		for (PhysicalContent contentInRow : row) {
	//			if (contentInRow.isText() && !contentInRow.getText().isAssignedBlock()) {
	//				/* find all connected texts and mark with this blockNum*/
	//				final PhysicalText text = contentInRow.getText();
	//				markEverythingConnectedFrom(contentInRow, blockNum, text.getRotation(),
	//				                            text.style.font);
	//
	//				blockNum++;
	//			}
	//		}
	//	}
	//
	//	/* compile paragraphs of text based on the assigned block numbers */
	//	List<ParagraphNode> ret = new ArrayList<ParagraphNode>();
	//	final LineSegmentator segmentator = new LineSegmentator();
	//
	//	for (int i = 0; i < blockNum; i++) {
	//		//		ParagraphNode paragraphNode = new ParagraphNode();
	//		/* collect all the words logically belonging to this paragraph */
	//		List<PhysicalContent> paragraphContent = new ArrayList<PhysicalContent>();
	//		for (PhysicalContent word : getContents()) {
	//			if (word.isAssignablePhysicalContent()) {
	//				if (word.getAssignablePhysicalContent().getBlockNum() == i) {
	//					if (word.isText()) {
	//						paragraphContent.add(word);
	//						//						paragraphNode.addWord(createWordNode(word.getText()));
	//					}
	//				}
	//			}
	//		}
	//		/* then group words in lines */
	//
	//		final List<LineNode> lines = segmentator.segmentLines(paragraphContent, pageNumber);
	//		final ParagraphNode paragraph = new ParagraphNode();
	//		for (LineNode line : lines) {
	//			paragraph.addChild(line);
	//		}
	//		ret.add(paragraph);
	//		//		if (!paragraphNode.getChildren().isEmpty()) {
	//		//			ret.add(paragraphNode);
	//		//		}
	//	}

	List<ParagraphNode> ret = new ArrayList<ParagraphNode>();
	final LineSegmentator segmentator = new LineSegmentator();

	final List<LineNode> lines = segmentator.segmentLines(this, 2.0f);
	if (!lines.isEmpty()) {
		final ParagraphNode paragraph = new ParagraphNode();
		for (LineNode line : lines) {
			paragraph.addChild(line);
		}
		ret.add(paragraph);
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
	final List<PhysicalContent> subContents = findRectanglesIntersectingWith(makeSmaller(bound));
	if (subContents.isEmpty()) {
		log.warn("LOG00370:got empty subregion for bound " + bound);
		return null;
	}

	final PhysicalPageRegion newRegion = new PhysicalPageRegion(subContents, containedIn,
	                                                            pageNumber);

	if (tooMuchContentCrossesBoundary(newRegion, bound)) {
		throw new RuntimeException("Considering bound " + bound
				                           + " as wallpaper. too much contents crossed the boundary");
	}

	removeContent(subContents);

	return newRegion;
}

private static HasPosition makeSmaller(final HasPosition bound) {
	final Rectangle p = bound.getPosition();
	return new Rectangle(p.getX() + 1.0f, p.getY() + 1.0f, p.getWidth() - 1.0f,
	                     p.getHeight() - 1.0f);
}

public boolean isContainedInFigure() {
	return getContainedIn() != null && (getContainedIn().isFigure()
			|| getContainedIn().isPicture());
}

public List<PhysicalPageRegion> splitInHorizontalColumns() {
	List<PhysicalPageRegion> subregions = new ArrayList<PhysicalPageRegion>();


	if (getPosition().getHeight() < avgFontSizeY * 2) {
		return subregions;
	}
	if (position.getWidth() < avgFontSizeX * 2) {
		return subregions;
	}


	/* start of by finding the median of a sample of vertical distances */
	final int LIMIT = (int) avgFontSizeY * 3;

	int[] distanceCount = new int[LIMIT];
	final Rectangle pos = getPosition();
	for (float x = pos.getX(); x <= pos.getEndX(); x += pos.getWidth() / 3) {

		final List<PhysicalContent> column = findContentAtXIndex(x);
		for (int i = 1; i < column.size(); i++) {
			final PhysicalContent current = column.get(i - 1);
			final PhysicalContent below = column.get(i);
			/* increase count for this distance (rounded to int) */
			final int d = (int) (below.getPosition().getY() - current.getPosition().getEndY());
			if (d > 0 && d < LIMIT) {
				distanceCount[d]++;
			}
		}
	}

	int highestFrequency = -1;
	int index = -1;
	//((int) (avgFontSizeY * 0.5f))
	for (int i = 2; i < distanceCount.length; i++) {
		if (distanceCount[i] >= highestFrequency) {
			index = i;
			highestFrequency = distanceCount[i];
		}
	}
	if (index == -1) {
		return subregions;
	}

	int boundaryLimit = index;
	if (log.isInfoEnabled()) { log.info("LOG00550:boundarylimit = " + boundaryLimit); }


	Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();

	float lastBoundary = -1000.0f;
	for (float y = pos.getY(); y <= pos.getEndY(); y++) {
		final List<PhysicalContent> row = findContentAtYIndex(y);

		workingSet.addAll(row);
		if (listContainsNoText(row)) {
			if (!workingSet.isEmpty() && workingSet.size() != getContents().size() && (
					y - lastBoundary > boundaryLimit)) {
				PhysicalPageRegion subRegion = new PhysicalPageRegion(workingSet, this, pageNumber);

				if (log.isInfoEnabled()) {
					log.info(String.format("LOG00530:horizontal split at y =%s for %s ", y, this));
					log.info("LOG00510: created subregion " + subRegion);
				}

				subregions.add(subRegion);
				removeContent(workingSet);
				workingSet.clear();
				lastBoundary = y;
			}
		} else {
			lastBoundary = y;
		}
	}
	return subregions;
}

public List<PhysicalPageRegion> splitInVerticalColumns() {
	List<PhysicalPageRegion> subregions = new ArrayList<PhysicalPageRegion>();

	if (getPosition().getWidth() < avgFontSizeX * 3) {
		return subregions;
	}

	Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();

	float lastBoundary = -1000.0f;
	for (float x = getPosition().getX(); x <= getPosition().getEndX(); x++) {
		final List<PhysicalContent> column = findContentAtXIndex(x);
		workingSet.addAll(column);
		if (listContainsNoText(column)) {
			if (!workingSet.isEmpty() && workingSet.size() != getContents().size()
					&& x - lastBoundary >= avgFontSizeX * 0.5f) {

				PhysicalPageRegion subRegion = new PhysicalPageRegion(workingSet, this, pageNumber);

				if (log.isInfoEnabled()) {
					log.info(this + " vertical split at x " + "=" + x);
					log.info("LOG00510: got subregion " + subRegion);
				}

				subregions.add(subRegion);
				removeContent(workingSet);
				workingSet.clear();
				lastBoundary = x;
			}
		} else {
			lastBoundary = x;
		}
	}
	return subregions;
}

// -------------------------- OTHER METHODS --------------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private boolean markEverythingConnectedFrom(@NotNull final PhysicalContent current,
                                            final int blockNum,
                                            final int rotation,
                                            final String fontName)
{
	if (!current.isAssignablePhysicalContent()) {
		return false;
	}
	if (current.getAssignablePhysicalContent().isAssignedBlock()) {
		return false;
	}
	if (current.isText() && current.getText().getRotation() != rotation) {
		return false;
	}

	current.getAssignablePhysicalContent().setBlockNum(blockNum);

	final Rectangle pos = current.getPosition();

	/* try searching for texts in all directions */
	for (int y = (int) pos.getY(); y < (int) pos.getEndY(); y++) {
		markBothWaysFromCurrent(current, blockNum, findContentAtYIndex(y), rotation, fontName);
	}

	for (int x = (int) pos.getX(); x < (int) pos.getEndX(); x++) {
		markBothWaysFromCurrent(current, blockNum, findContentAtXIndex(x), rotation, fontName);
	}
	return true;
}

private boolean listContainsNoText(final List<PhysicalContent> workingSet) {
	for (PhysicalContent content : workingSet) {
		if (content.isText()) {
			return false;
		}
	}
	return true;
}

private void markBothWaysFromCurrent(final PhysicalContent current,
                                     final int blockNum,
                                     @NotNull final List<PhysicalContent> line,
                                     final int rotation,
                                     final String fontName)
{
	final int currentIndex = line.indexOf(current);

	/* left/up*/
	boolean continue_ = true;
	for (int index = currentIndex - 1; index >= 0 && continue_; index--) {
		continue_ &= markEverythingConnectedFrom(line.get(index), blockNum, rotation, fontName);
	}
	/* right / down */
	continue_ = true;
	for (int index = currentIndex + 1; index < line.size() && continue_; index++) {
		continue_ &= markEverythingConnectedFrom(line.get(index), blockNum, rotation, fontName);
	}
}
}
