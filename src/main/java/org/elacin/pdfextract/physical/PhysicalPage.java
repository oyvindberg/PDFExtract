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

package org.elacin.pdfextract.physical;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.content.GraphicContent;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.WhitespaceRectangle;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.util.RectangleCollection;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;


public class PhysicalPage {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

/** The physical page number (ie the sequence encountered in the document) */
private final int pageNumber;

/**
 * During analysis this will contain a list of figures which has been confirmed to contain text,
 * before they are separated into PhysicalPageRegions a bit later in the process.
 */
@NotNull
private final List<GraphicContent> graphicalRegions = new ArrayList<GraphicContent>();

/**
 * This contains all images except those which has been dropped (because they are too big), and is
 * only here for rendering purposes.
 */
@NotNull
private final List<GraphicContent> graphicsToRender = new ArrayList<GraphicContent>();

/**
 * The analysis will split the contents into regions based on how things are contained within images
 * or within detected columns.
 */
@NotNull
private final List<PhysicalPageRegion> regions = new ArrayList<PhysicalPageRegion>();

/**
 * This initially contains everything on the page. after creating the regions, content will be moved
 * from here. ideally this should be quite empty after the analysis.
 */
@NotNull
private final PhysicalPageRegion originalWholePage;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(@NotNull List<? extends PhysicalContent> contents,
                    @NotNull final List<GraphicContent> graphicContents,
                    int pageNumber) throws Exception
{
	this.pageNumber = pageNumber;
	originalWholePage = new PhysicalPageRegion(contents, pageNumber);

	List<GraphicContent> addToContents = new ArrayList<GraphicContent>();
	List<GraphicContent> separators = new ArrayList<GraphicContent>();

	for (GraphicContent graphic : graphicContents) {

		if (isTooBigPicture(originalWholePage, graphic)) {
			if (log.isInfoEnabled()) { log.info("LOG00510::considered too big " + graphic); }
			//ta vekk
			graphicsToRender.add(graphic);
			continue;
		}

		if (doesContainSomething(originalWholePage, graphic)) {
			if (log.isInfoEnabled()) { log.info("LOG00500:contains content " + graphic); }
			graphic.setCanBeAssigned(false);
			graphicalRegions.add(graphic);

		} else if (graphic.canBeConsideredContentInRegion(originalWholePage)) {
			if (log.isInfoEnabled()) { log.info("LOG00520:considered content " + graphic); }
			graphic.setCanBeAssigned(true);
			addToContents.add(graphic);

		} else if (graphic.canBeConsideredSeparatorInRegion(originalWholePage)) {
			if (log.isInfoEnabled()) { log.info("LOG00490:considered separator " + graphic); }
			graphic.setCanBeAssigned(true);
			addToContents.add(graphic);
			separators.add(graphic);

		} else {
			log.warn("LOG00510: unknown function " + graphic);
			graphic.setCanBeAssigned(false);
		}

		graphicsToRender.add(graphic);
	}

	int num = 0;
	for (int i = 0, size = separators.size(); i < size; i++) {
		final GraphicContent separator = separators.get(i);
		if (separator.isAssignedBlock()) {
			continue;
		}


	}


	originalWholePage.addContent(addToContents);
	regions.add(originalWholePage);
}

// -------------------------- STATIC METHODS --------------------------

@NotNull
private static PriorityQueue<GraphicContent> createSmallestFirstQueue(@NotNull final List<GraphicContent> graphicalRegions) {
	final Comparator<GraphicContent> smallestComparator = new Comparator<GraphicContent>() {
		@Override
		public int compare(@NotNull final GraphicContent o1, @NotNull final GraphicContent o2) {
			return Float.compare(o1.getPosition().getHeight(), o2.getPosition().getHeight());
		}
	};

	final int capacity = Math.max(1, graphicalRegions.size());
	PriorityQueue<GraphicContent> queue = new PriorityQueue<GraphicContent>(capacity,
	                                                                        smallestComparator);
	queue.addAll(graphicalRegions);
	return queue;
}

private static boolean doesContainSomething(@NotNull final PhysicalPageRegion region,
                                            @NotNull final GraphicContent graphic)
{
	for (PhysicalContent content : region.getContents()) {
		if (graphic.getPosition().contains(content.getPosition())) {
			return true;
		}
	}
	return false;
}


private static boolean isTooBigPicture(@NotNull final RectangleCollection region,
                                       @NotNull final PhysicalContent graphic)
{
	return graphic.getPosition().area() >= region.getPosition().area();
}


// --------------------- GETTER / SETTER METHODS ---------------------

public int getPageNumber() {
	return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public PageNode compileLogicalPage() {
	long t0 = System.currentTimeMillis();

	/** separate out the content which is contained within a graphic.
	 * sort the graphics by smallest, because they frequently do overlap.
	 * */
	PriorityQueue<GraphicContent> queue = createSmallestFirstQueue(graphicalRegions);

	while (!queue.isEmpty()) {
		final GraphicContent graphic = queue.remove();

		try {

			if (isContentContainedWithinAnyContentInList(queue, graphic)) {
				continue;
			}

			final PhysicalPageRegion region = originalWholePage.extractSubRegion(graphic, graphic);
			if (null != region) {
				regions.add(region);
				if (log.isInfoEnabled()) {
					log.info("LOG00340:Added subregion " + region);
				}
			} else {
				graphic.setCanBeAssigned(true);
				originalWholePage.addContent(graphic);
			}
		} catch (Exception e) {
			log.warn("LOG00320:Error while dividing page by " + graphic + ":" + e.getMessage());
			//legg til
			//			graphicsToRender.remove(graphic);
		}
	}

	for (PhysicalPageRegion region : regions) {
		region.findVerticalBoundaries();
		region.findSubRegions();
	}

	//	originalWholePage.addContent(graphicalRegions);

	PageNode ret = new PageNode(pageNumber);

	final List<WhitespaceRectangle> allWhitespace = new ArrayList<WhitespaceRectangle>();
	for (PhysicalPageRegion region : regions) {
		List<ParagraphNode> paragraphs = region.createParagraphNodes();

		allWhitespace.addAll(region.getWhitespace());
		for (ParagraphNode paragraph : paragraphs) {
			ret.addChild(paragraph);
		}
	}


	ret.addDebugFeatures(Color.RED, regions);
	//		ret.addDebugFeatures(Color.BLACK, allWhitespace);
	ret.addDebugFeatures(Color.GREEN, graphicsToRender);
	//        ret.addColumns(layoutRecognizer.findColumnsForPage(wholePage, ret));

	if (log.isInfoEnabled()) {
		log.info("LOG00230:compileLogicalPage took " + (System.currentTimeMillis() - t0) + " ms");
	}
	return ret;
}

private boolean isContentContainedWithinAnyContentInList(final Collection<GraphicContent> queue,
                                                         final PhysicalContent graphic)
{
	boolean found = false;
	for (GraphicContent content : queue) {
		if (content.getPosition().contains(graphic.getPosition())) {
			if (log.isInfoEnabled()) {
				log.info("LOG00520:" + graphic + "contained in " + content + ", dropping");
				found = true;
			}
		}
	}
	return found;
}

@NotNull
public RectangleCollection getContents() {
	return originalWholePage;
}
}
