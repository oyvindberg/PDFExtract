/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.physical.segmentation.graphics;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.logical.Formulas;
import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 13.11.10 Time: 03.29 To change this template use
 * File | Settings | File Templates.
 */
public class GraphicSegmentatorImpl implements GraphicsDrawer, GraphicSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(GraphicSegmentatorImpl.class);

private static final int ARBITRARY_NUMBER_OF_CHARS_REQUIRED_PER_LINE = 14;

/**
 * These three lists will hold the contents while we are drawing it. This is grouped based on
 * physical properties only
 */
@NotNull
final List<GraphicContent> figures  = new ArrayList<GraphicContent>();
@NotNull
final List<GraphicContent> pictures = new ArrayList<GraphicContent>();


/** Will these three will hold the contents after segmentation */

/* these graphics are considered content */
@NotNull
private final List<GraphicContent> contentGraphics = new ArrayList<GraphicContent>();

/* These will be used to split a page into page regions */
@NotNull
private final List<GraphicContent> graphicalRegions = new ArrayList<GraphicContent>();

/* This contains all the segmented pictures (except those which has been dropped for being too
 * big), and is only here for rendering purposes. */
@NotNull
private final List<GraphicContent> graphicsToRender = new ArrayList<GraphicContent>();

private boolean didSegment = false;

/* we need the pages dimensions here, because the size of regions is calculated based on content.
*   it should be possible for graphic to cover all the contents if it doesnt cover all the page*/
private final float w;
private final float h;

// --------------------------- CONSTRUCTORS ---------------------------

public GraphicSegmentatorImpl(final float w, final float h) {
	this.w = w;
	this.h = h;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface GraphicSegmentator ---------------------

@NotNull
public List<GraphicContent> getContentGraphics() {
	assert didSegment;
	return contentGraphics;
}

@NotNull
public List<GraphicContent> getGraphicalRegions() {
	assert didSegment;
	return graphicalRegions;
}

@NotNull
public List<GraphicContent> getGraphicsToRender() {
	assert didSegment;
	return graphicsToRender;
}

public void segmentGraphicsUsingContentInRegion(@NotNull PhysicalPageRegion region) {
	assert !didSegment;

	if (figures.isEmpty()) {
		if (log.isInfoEnabled()) { log.info("no figures to combine"); }
	} else {
		combineGraphicsUsingRegion(region, figures);
	}
	if (pictures.isEmpty()) {
		if (log.isInfoEnabled()) { log.info("no pictures to combine"); }
	} else {
		combineGraphicsUsingRegion(region, pictures);
	}

	for (GraphicContent graphic : getGraphicContents()) {
		if (isTooBigGraphic(graphic)) {
			if (log.isInfoEnabled()) { log.info("LOG00501:considered too big " + graphic); }
			continue;
		}

		if (graphicContainsTextFromRegion(region, graphic)) {
			if (log.isInfoEnabled()) { log.info("LOG00502:considered container " + graphic); }
			graphic.setCanBeAssigned(false);
			graphic.setStyle(Style.GRAPHIC_CONTAINER);
			graphicalRegions.add(graphic);
		} else if (canBeConsideredMathBarInRegion(graphic, region)) {
			if (log.isInfoEnabled()) { log.info("LOG00504:considered math bar " + graphic); }
			graphic.setCanBeAssigned(true);
			graphic.setStyle(Style.GRAPHIC_MATH_BAR);
			contentGraphics.add(graphic);
		} else if (canBeConsideredHorizontalSeparator(graphic)) {
			if (log.isInfoEnabled()) { log.info("LOG00505:considered hsep " + graphic); }
			graphic.setCanBeAssigned(true);
			graphic.setStyle(Style.GRAPHIC_HSEP);
			contentGraphics.add(graphic);
		} else if (canBeConsideredVerticalSeparator(graphic)) {
			if (log.isInfoEnabled()) { log.info("LOG00506:considered vsep " + graphic); }
			graphic.setCanBeAssigned(true);
			graphic.setStyle(Style.GRAPHIC_VSEP);
			contentGraphics.add(graphic);
		} else if (canBeConsideredCharacterInRegion(graphic, region)) {
			if (log.isInfoEnabled()) { log.info("LOG00503:considered character " + graphic); }
			graphic.setStyle(Style.GRAPHIC_CHARACTER);
			graphic.setCanBeAssigned(true);
			contentGraphics.add(graphic);
		} else {
			if (log.isInfoEnabled()) { log.info("LOG00507:considered image " + graphic); }
			graphic.setCanBeAssigned(true);
			graphic.setStyle(Style.GRAPHIC_IMAGE);
			contentGraphics.add(graphic);
		}

		graphicsToRender.add(graphic);
	}

	clearTempLists();
	didSegment = true;
}

// --------------------- Interface GraphicsDrawer ---------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
public void drawImage(@NotNull final Image image,
                      @NotNull final AffineTransform at,
                      @NotNull final Rectangle2D bounds)
{
	assert !didSegment;

	/* transform the coordinates by using the affinetransform. */
	Point2D upperLeft = at.transform(new Point2D.Float(0.0F, 0.0F), null);

	Point2D dim = new Point2D.Float((float) image.getWidth(null), (float) image.getHeight(null));
	Point2D lowerRight = at.transform(dim, null);

	/* this is necessary because the image might be rotated */
	float x = (float) Math.min(upperLeft.getX(), lowerRight.getX());
	float endX = (float) Math.max(upperLeft.getX(), lowerRight.getX());
	float y = (float) Math.min(upperLeft.getY(), lowerRight.getY());
	float endY = (float) Math.max(upperLeft.getY(), lowerRight.getY());

	/* respect the bound if set */
	x = (float) Math.max(bounds.getMinX(), x);
	y = (float) Math.max(bounds.getMinY(), y);
	if (bounds.getMaxX() > 0.0) {
		endX = (float) Math.min(bounds.getMaxX(), endX);
	}
	if (bounds.getMaxY() > 0.0) {
		endY = (float) Math.min(bounds.getMaxY(), endY);
	}

	/* build the finished position - this will also do some sanity checking */
	Rectangle pos;
	try {
		pos = new Rectangle(x, y, endX - x, endY - y);
	} catch (Exception e) {
		log.warn("LOG00590:Error while adding graphics: " + e.getMessage());
		return;
	}


	pictures.add(new GraphicContent(pos, true, true, Color.BLACK));
}

public void fill(@NotNull final GeneralPath originalPath, @NotNull final Color color) {
	assert !didSegment;
	List<GeneralPath> paths = PathSplitter.splitPath(originalPath);

	for (GeneralPath path : paths) {
		try {
			final Rectangle pos = convertRectangle(path.getBounds());
			addFigure(new GraphicContent(pos, false, true, color));
		} catch (Exception e) {
			log.warn("LOG00580:Error while filling path " + path + ": ", e);
		}
	}
}

public void strokePath(@NotNull final GeneralPath originalPath, @NotNull final Color color) {
	assert !didSegment;

	List<GeneralPath> paths = PathSplitter.splitPath(originalPath);

	for (GeneralPath path : paths) {
		try {
			addFigure(new GraphicContent(convertRectangle(path.getBounds()), false, false, color));
		} catch (Exception e) {
			log.warn("LOG00600:Error while drawing " + path + ": " + e.getMessage());
		}
	}
}

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static boolean canBeConsideredCharacterInRegion(GraphicContent g,
                                                       final PhysicalPageRegion region)
{
	float doubleCharArea = region.getAvgFontSizeY() * region.getAvgFontSizeX() * 2.0f;
	return g.getPos().area() < doubleCharArea;
}

/** consider the graphic a separator if the aspect ratio is high */
public static boolean canBeConsideredHorizontalSeparator(GraphicContent g) {
	if (g.getPos().getHeight() > 15.0f) {
		return false;
	}

	return g.getPos().getWidth() / g.getPos().getHeight() > 15.0f;
}

public static boolean canBeConsideredMathBarInRegion(GraphicContent g,
                                                     final PhysicalPageRegion region)
{
	if (!canBeConsideredHorizontalSeparator(g)) {
		return false;
	}

	final List<PhysicalContent> surrounding = region.findSurrounding(g, 6);
	boolean foundOver = false, foundUnder = false, foundMath = false;

	for (PhysicalContent content : surrounding) {
		if (content.getPos().getY() < g.getPos().getEndY()) {
			foundUnder = true;
		}
		if (content.getPos().getEndY() > g.getPos().getY()) {
			foundOver = true;
		}
		if (content.isText()) {
			if (Formulas.textContainsMath(content.getPhysicalText())) {
				foundMath = true;
			}
		}
		if (foundOver && foundUnder && foundMath) {
			return true;
		}
	}
	return false;
}

/** consider the graphic a separator if the aspect ratio is high */
public static boolean canBeConsideredVerticalSeparator(GraphicContent g) {
	if (g.getPos().getWidth() > 15.0f) {
		return false;
	}

	return g.getPos().getHeight() / g.getPos().getWidth() > 15.0f;
}

// -------------------------- STATIC METHODS --------------------------

private static void combineGraphicsUsingRegion(@NotNull final PhysicalPageRegion region,
                                               @NotNull final List<GraphicContent> list)
{
	/**
	 * Segment images
	 *
	 * We segment figures and pictures separately.
	 *
	 * The segmentation is done by first finding a list of graphical content which contains
	 *  a certain amount of text which is then excluded from segmentation (because we later on
	 *  use these graphics to separate text, so that information is most probably useful).
	 *
	 * Then we try to identify clusters of graphics, and combine them
	 *
	 *  */

	if (log.isInfoEnabled()) { log.info("size() before = " + list.size()); }

	for (Iterator<GraphicContent> iterator = list.iterator(); iterator.hasNext();) {
		final GraphicContent content = iterator.next();
		if (content.isFigure() && content.isBackgroundColor()) {
			iterator.remove();
		}
	}

//	List<GraphicContent> saved = new ArrayList<GraphicContent>();
//
//	final Style mostCommonStyle = region.getMostCommonStyle();
//
//	for (Iterator<GraphicContent> iterator = list.iterator(); iterator.hasNext();) {
//		final GraphicContent graphic = iterator.next();
//		if (!graphic.isBackgroundColor()){
//			continue;
//		}
//
//		final List<PhysicalContent> contents = region.findContentsIntersectingWith(graphic);
//
//		/** find character count for the contents.
//		 * Also find average font Y size. some of the figures can be vast but contain a certain
//		 * amount of text, so look for a certain number of chars per 'line' */
//		int numChars = 0;
//		float averageYSize = 0.0f;
//		for (PhysicalContent content : contents) {
//			final PhysicalText text = content.getPhysicalText();
//			if (content.isText() && text.getStyle().equals(mostCommonStyle)) {
//				numChars += text.getText().length();
//				averageYSize += (float) (text.getStyle().ySize * text.getText().length());
//			}
//		}
//		averageYSize /= (float) numChars;
//
//		float appxNumLines = Math.max(1.0f, graphic.getPos().getHeight() / (averageYSize * 4));
//
//		if (enoughCharsToBeSaved(numChars, appxNumLines)) {
//			if (graphic.isBackgroundColor()) {
//				log.warn("removing graphic " + graphic);
//				iterator.remove();
//			}
////			else {
////				if (log.isDebugEnabled()) {
////					log.debug("saving = " + graphic + ", numChars = " + numChars);
////				}
////				iterator.remove();
////				saved.add(graphic);
////			}
//		}
//	}

	for (int i = 0; i < list.size(); i++) {
		final GraphicContent current = list.get(i);
		for (int j = i + 1; j < list.size(); j++) {
			final GraphicContent combineWith = list.get(j);
			if (current.canBeCombinedWith(combineWith)) {
				if (log.isTraceEnabled()) {
					log.info("combining graphics " + current + " and " + combineWith);
				}
				list.remove(j);
				list.remove(i);
				list.add(current.combineWith(combineWith));
				i = 0; //start over
				break;
			}
		}
	}

//	list.addAll(saved);

	if (log.isInfoEnabled()) { log.info("size() after = " + list.size()); }
}

@NotNull
private static Rectangle convertRectangle(@NotNull final java.awt.Rectangle bounds) {
	return new Rectangle((float) bounds.x, (float) bounds.y, (float) bounds.width,
	                     (float) bounds.height);
}

private static boolean enoughCharsToBeSaved(final int numChars, final float numLines) {
	return numChars > (int) Math.min(ARBITRARY_NUMBER_OF_CHARS_REQUIRED_PER_LINE * numLines, 70);
}

private static boolean graphicContainsTextFromRegion(@NotNull final PhysicalPageRegion region,
                                                     @NotNull final GraphicContent graphic)
{
	for (PhysicalContent content : region.getContents()) {
		if (graphic.getPos().contains(content.getPos())) {
			return true;
		}
	}
	return false;
}

// -------------------------- OTHER METHODS --------------------------

@NotNull
private List<GraphicContent> getGraphicContents() {
	List<GraphicContent> ret = new ArrayList<GraphicContent>();
	ret.addAll(figures);
	ret.addAll(pictures);
	return ret;
}

private void addFigure(@NotNull final GraphicContent newFigure) {
	/* some times bounding boxes around text might be drawn twice, in white and in another colour.
		take advantage of the fact that figures with equal positions are deemed equal for the set,
		find an existing one with same position, and combine them. Prefer to keep that which stands
		out from the background, as that is more useful :)
	 */
	final int index = figures.indexOf(newFigure);
	if (index != -1){
		final GraphicContent existing = figures.get(index);
		figures.remove(index);
		figures.add(existing.combineWith(newFigure));
		return;
	} else {
		figures.add(newFigure);
	}
}

private void clearTempLists() {
	figures.clear();
	pictures.clear();
}

private boolean isTooBigGraphic(@NotNull final PhysicalContent graphic) {
	return graphic.getPos().area() >= (w * h);
}
}
