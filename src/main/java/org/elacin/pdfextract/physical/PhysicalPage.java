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
import org.elacin.pdfextract.physical.segmentation.graphics.GraphicSegmentator;
import org.elacin.pdfextract.tree.LayoutRegionNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;
import org.elacin.pdfextract.util.RectangleCollection;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


public class PhysicalPage {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPage.class);

/** The physical page number (ie the sequence encountered in the document) */
private final int pageNumber;


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

/** Contains all the graphics on the page */
@NotNull
private final GraphicSegmentator graphics;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPage(@NotNull List<? extends PhysicalContent> contents,
                    @NotNull final GraphicSegmentator graphics,
                    int pageNumber) throws Exception
{
	this.pageNumber = pageNumber;
	originalWholePage = new PhysicalPageRegion(contents, pageNumber);
	regions.add(originalWholePage);

	this.graphics = graphics;
}

// -------------------------- STATIC METHODS --------------------------

@NotNull
private static PriorityQueue<GraphicContent> createSmallestFirstQueue(@NotNull final List<GraphicContent> graphicalRegions) {
	final Comparator<GraphicContent> smallestComparator = new Comparator<GraphicContent>() {
		public int compare(@NotNull final GraphicContent o1, @NotNull final GraphicContent o2) {
			return Float.compare(o1.getPos().area(), o2.getPos().area());
		}
	};

	final int capacity = Math.max(1, graphicalRegions.size());
	PriorityQueue<GraphicContent> queue = new PriorityQueue<GraphicContent>(capacity,
	                                                                        smallestComparator);
	queue.addAll(graphicalRegions);
	return queue;
}

// --------------------- GETTER / SETTER METHODS ---------------------

public int getPageNumber() {
	return pageNumber;
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
public PageNode compileLogicalPage() {
	long t0 = System.currentTimeMillis();

	graphics.segmentGraphicsUsingContentInRegion(originalWholePage);
	originalWholePage.addContent(graphics.getContentGraphics());

	/** separate out the content which is contained within a graphic.
	 * sort the graphics by smallest, because they frequently do overlap.
	 * */
	PriorityQueue<GraphicContent> queue = createSmallestFirstQueue(graphics.getGraphicalRegions());

	while (!queue.isEmpty()) {
		final GraphicContent graphic = queue.remove();

		try {
			/* if we extract by a white graphic, dont set it as belonging to that. these are
			 *  oftenly used just to separate text in the pdf but are not visible */
			final PhysicalPageRegion region = originalWholePage.extractSubRegion(graphic, graphic);
			if (null != region) {
				regions.add(region);
				if (log.isInfoEnabled()) {
					log.info("LOG00340:Added subregion " + region);
				}
			}
			//			} else {
			//				if (!graphic.isBackgroundColor()) {
			//			if (!graphic.getStyle().equals(Style.GRAPHIC_CONTAINER)){
			//				throw new RuntimeException("expected " + Style.GRAPHIC_CONTAINER + " got " +
			//						                           graphic.getStyle());
			//			}
			//			graphic.setCanBeAssigned(true);
			//			originalWholePage.addContent(graphic);
			//				}
			//			}
		} catch (Exception e) {
			log.info("LOG00320:Could not divide page::" + e.getMessage());
			if (graphic.getPos().area() < getContents().getPos().area() * 0.4f) {
				if (log.isInfoEnabled()) { log.info("LOG00690:Adding " + graphic + " as content");}
				graphic.setCanBeAssigned(true);
				originalWholePage.addContent(graphic);
			} else {
				graphics.getGraphicsToRender().remove(graphic);
			}
		}
	}


	List<PhysicalPageRegion> newRegions = new ArrayList<PhysicalPageRegion>();
	for (PhysicalPageRegion region : regions) {
		newRegions.addAll(region.splitInVerticalColumns());
	}
	regions.addAll(newRegions);


	PageNode page = new PageNode(pageNumber);

	for (PhysicalPageRegion region : regions) {
		LayoutRegionNode regionNode = new LayoutRegionNode(region.isContainedInGraphic());

		List<ParagraphNode> paragraphs = region.createParagraphNodes();

		for (ParagraphNode paragraph : paragraphs) {
			regionNode.addChild(paragraph);
		}
		page.addChild(regionNode);
	}

	verifyThatAllContentHasLine();

	/* for rendering only */
	List<GraphicContent> filled = new ArrayList<GraphicContent>();
	List<GraphicContent> unFilled = new ArrayList<GraphicContent>();
	List<GraphicContent> pictures = new ArrayList<GraphicContent>();

	for (GraphicContent content : graphics.getGraphicsToRender()) {
		if (content.isPicture()) {
			pictures.add(content);
		} else if (content.isFilled()) {
			filled.add(content);
		} else {
			unFilled.add(content);
		}
	}
	page.addDebugFeatures(Color.CYAN, pictures);
	page.addDebugFeatures(Color.CYAN, filled);
	page.addDebugFeatures(Color.CYAN, unFilled);

	if (log.isInfoEnabled()) {
		log.info("LOG00230:compileLogicalPage took " + (System.currentTimeMillis() - t0) + " ms");
	}
	return page;
}

@NotNull
public RectangleCollection getContents() {
	return originalWholePage;
}

// -------------------------- OTHER METHODS --------------------------

private void verifyThatAllContentHasLine() {
	for (PhysicalPageRegion region : regions) {
		for (PhysicalPageRegion subRegion : region.getSubregions()) {
			for (PhysicalContent content : subRegion.getContents()) {
				if (content.isAssignablePhysicalContent() && !content.getAssignablePhysicalContent()
				                                                     .isAssignedBlock()) {
					log.error("LOG00711:content " + content + "not assigned line");
				}
			}
		}
		for (PhysicalContent content : region.getContents()) {
			if (content.isAssignablePhysicalContent() && !content.getAssignablePhysicalContent()
			                                                     .isAssignedBlock()) {
				log.error("LOG00710:content " + content + "not assigned line");
			}
		}
	}
}
}
