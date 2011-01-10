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
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.Sorting;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 13.11.10 Time: 03.29 To change this template use
 * File | Settings | File Templates.
 */
public class GraphicSegmentatorImpl implements DrawingSurface, GraphicSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(GraphicSegmentatorImpl.class);

/**
 * These three lists will hold the contents while we are drawing it. This is grouped based on
 * physical properties only
 */
@NotNull
final List<GraphicContent> figures     = new ArrayList<GraphicContent>();
final List<GeneralPath>    figurePaths = new ArrayList<GeneralPath>();
@NotNull
final List<GraphicContent> pictures    = new ArrayList<GraphicContent>();


/**
 * Will these three will hold the contents after segmentation
 */

/* these graphics are considered content */
@NotNull
private final List<GraphicContent> contents = new ArrayList<GraphicContent>();

/* These will be used to split a page into page regions */
@NotNull
private final List<GraphicContent> containers = new ArrayList<GraphicContent>();

/* these are separators which might divide regions of a page */
private final List<GraphicContent> verticalSeparators   = new ArrayList<GraphicContent>();
private final List<GraphicContent> horizontalSeparators = new ArrayList<GraphicContent>();

/* This contains all the segmented pictures (except those which has been dropped for being too
big), and is only
here for rendering purposes. */
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
public List<GraphicContent> getContents() {
    assert didSegment;
    return contents;
}

@NotNull
public List<GraphicContent> getContainers() {
    assert didSegment;
    return containers;
}

@NotNull
public List<GraphicContent> getGraphicsToRender() {
    assert didSegment;
    return graphicsToRender;
}

public void segmentGraphicsUsingContentInRegion(@NotNull PhysicalPageRegion region) {
    assert !didSegment;

    for (GeneralPath figurePath : figurePaths) {
        try {
            final Rectangle pos = convertRectangle(figurePath.getBounds());
            addFigure(new GraphicContent(pos, false, Color.BLACK));
        } catch (Exception e) {
            log.warn("LOG00580:Error while filling path " + figurePath + ": ", e);
        }
    }


    if (pictures.isEmpty()) {
        if (log.isInfoEnabled()) {
            log.info("no pictures to combine");
        }
    } else {
        combineGraphicsUsingRegion(region, pictures);
    }

    categorizeGraphics(region, getGraphicContents());

    /* this is a hack to deal with situations where one creates a table or similar with
        horizontal lines only. These would not be separators */
    List<GraphicContent> combinedGraphics = combineHorizontalSeparators();
    categorizeGraphics(region, combinedGraphics);


    Collections.sort(horizontalSeparators, Sorting.sortByLowerY);
    Collections.sort(verticalSeparators, Sorting.sortByLowerX);

    clearTempLists();
    didSegment = true;

    if (log.isInfoEnabled()) {
        logGraphics();
    }
}

public List<GraphicContent> getVerticalSeparators() {
    return verticalSeparators;
}

public List<GraphicContent> getHorizontalSeparators() {
    return horizontalSeparators;
}

// --------------------- Interface DrawingSurface ---------------------

@SuppressWarnings({"NumericCastThatLosesPrecision"})
public void drawImage(@NotNull final Image image,
                      @NotNull final AffineTransform at,
                      @NotNull final Rectangle2D bounds) {
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


    pictures.add(new GraphicContent(pos, true, Color.BLACK));
}

public void fill(@NotNull final GeneralPath originalPath, @NotNull final Color color) {
    addVectorPath(originalPath, color);
}

private boolean addVectorPath(GeneralPath originalPath, Color color) {
    assert !didSegment;

    if (color.equals(Color.WHITE)) {
        return true;
    }

    List<GeneralPath> paths = PathSplitter.splitPath(originalPath);

    for (GeneralPath path : paths) {
        boolean addedPath = false;
        for (GeneralPath figurePath : figurePaths) {
            if (figurePath.intersects(path.getBounds())) {
                figurePath.append(path, true);
                addedPath = true;
                break;
            }
        }

        if (!addedPath) {
            GeneralPath newPath = new GeneralPath(path);
            figurePaths.add(newPath);
        }
    }
    return false;
}

public void strokePath(@NotNull final GeneralPath originalPath, @NotNull final Color color) {
    addVectorPath(originalPath, color);
}

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static boolean canBeConsideredCharacterInRegion(GraphicContent g,
                                                       final PhysicalPageRegion region) {
    float doubleCharArea = region.getAvgFontSizeY() * region.getAvgFontSizeX() * 2.0f;
    return g.getPos().area() < doubleCharArea;
}

/**
 * consider the graphic a separator if the aspect ratio is high
 */
public static boolean canBeConsideredHorizontalSeparator(GraphicContent g) {
    if (g.getPos().getHeight() > 15.0f) {
        return false;
    }

    return g.getPos().getWidth() / g.getPos().getHeight() > 15.0f;
}

public static boolean canBeConsideredMathBarInRegion(GraphicContent g,
                                                     final PhysicalPageRegion region) {
    if (!canBeConsideredHorizontalSeparator(g)) {
        return false;
    }

    final List<PhysicalContent> surrounding = region.findSurrounding(g, 15);
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

/**
 * consider the graphic a separator if the aspect ratio is high
 */
public static boolean canBeConsideredVerticalSeparator(GraphicContent g) {
    if (g.getPos().getWidth() > 15.0f) {
        return false;
    }

    return g.getPos().getHeight() / g.getPos().getWidth() > 15.0f;
}

// -------------------------- STATIC METHODS --------------------------

private static void combineGraphicsUsingRegion(@NotNull final PhysicalPageRegion region,
                                               @NotNull final List<GraphicContent> list) {
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
    final long t0 = System.currentTimeMillis();
    final int originalSize = list.size();

    for (Iterator<GraphicContent> iterator = list.iterator(); iterator.hasNext();) {
        final GraphicContent content = iterator.next();
        if (content.isFigure() && content.isBackgroundColor()) {
            iterator.remove();
        }
    }
    Collections.sort(list, Sorting.sortByLowerYThenLowerX);

    for (int i = 0; i < list.size(); i++) {
        final GraphicContent current = list.get(i);

        /* for every current - check the rest of the graphics in the list
        *   to see if its possible to combine */

        for (int j = i + 1; j < list.size(); j++) {
            float minX = current.getPos().getX();
            float minY = current.getPos().getY();
            float maxX = current.getPos().getEndX();
            float maxY = current.getPos().getEndY();

            Color c = current.getColor();

            final int firstCombinable = j;
            /* since we sorted the elements there might be several in a row - combine them all*/
            while (j < list.size() && current.canBeCombinedWith(list.get(j))) {
                minX = Math.min(minX, list.get(j).getPos().getX());
                minY = Math.min(minY, list.get(j).getPos().getY());
                maxX = Math.max(maxX, list.get(j).getPos().getEndX());
                maxY = Math.max(maxY, list.get(j).getPos().getEndY());
                if (!Color.WHITE.equals(c)) {
                    c = list.get(j).getColor();
                }
                j++;
            }

            /**
             * combine if  we found some
             * */

            /*
             *  i = 0
             *  firstCombinable = 2
             *  j = 3
             * --
             *  combine 0 and 2 only, j is one too high.
              *  */
            if (firstCombinable != j) {

                /* first remove */

                final int numToCombine = j - firstCombinable;
                for (int u = 0; u < numToCombine; u++) {
                    list.remove(firstCombinable); // removing elements from the first one
                }

                list.remove(i);

                /* then add the new graphic */
                list.add(new GraphicContent(new Rectangle(minX, minY, maxX - minX, maxY - minY),
                        current.isPicture(), c));
                i = -1; // start over
                break;
            }
        }
    }

    System.err.println(System.currentTimeMillis());
    if (log.isInfoEnabled()) {
        log.info("Combined " + originalSize + " graphical elements into " + list.size() + " in "
                + (System.currentTimeMillis() - t0) + "ms");
    }
}

@NotNull
private static Rectangle convertRectangle(@NotNull final java.awt.Rectangle bounds) {
    return new Rectangle((float) bounds.x, (float) bounds.y, (float) bounds.width,
            (float) bounds.height);
}

private static boolean graphicContainsTextFromRegion(@NotNull final PhysicalPageRegion region,
                                                     @NotNull final GraphicContent graphic) {
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
    /* some times bounding boxes around text might be drawn twice, in white and in another colour
    .
       take advantage of the fact that figures with equal positions are deemed equal for the set,
       find an existing one with same position, and combine them. Prefer to keep that which
       stands
       out from the background, as that is more useful :)
    */
    if (newFigure.getColor().equals(Color.WHITE)) {
        return;
    }
    figures.add(newFigure);
}

private void categorizeGraphics(PhysicalPageRegion region, List<GraphicContent> list) {
    for (GraphicContent graphic : list) {
        if (isTooBigGraphic(graphic)) {
            if (log.isInfoEnabled()) {
                log.info("LOG00501:considered too big " + graphic);
            }
            continue;
        }

        if (graphicContainsTextFromRegion(region, graphic)) {
            graphic.setCanBeAssigned(false);
            graphic.setStyle(Style.GRAPHIC_CONTAINER);
            containers.add(graphic);
        } else if (canBeConsideredMathBarInRegion(graphic, region)) {
            graphic.setCanBeAssigned(true);
            graphic.setStyle(Style.GRAPHIC_MATH_BAR);
            contents.add(graphic);
        } else if (canBeConsideredHorizontalSeparator(graphic)) {
            graphic.setCanBeAssigned(true);
            graphic.setStyle(Style.GRAPHIC_HSEP);
            horizontalSeparators.add(graphic);
        } else if (canBeConsideredVerticalSeparator(graphic)) {
            graphic.setCanBeAssigned(true);
            graphic.setStyle(Style.GRAPHIC_VSEP);
            verticalSeparators.add(graphic);
        } else if (canBeConsideredCharacterInRegion(graphic, region)) {
            graphic.setStyle(Style.GRAPHIC_CHARACTER);
            graphic.setCanBeAssigned(true);
            contents.add(graphic);
        } else {
            graphic.setCanBeAssigned(true);
            graphic.setStyle(Style.GRAPHIC_IMAGE);
            contents.add(graphic);
        }

        graphicsToRender.add(graphic);
    }
}

private void clearTempLists() {
    figures.clear();
    pictures.clear();
}

private List<GraphicContent> combineHorizontalSeparators() {
    Map<String, List<GraphicContent>> hsepsForXCoordinate = new HashMap<String,
            List<GraphicContent>>();

    for (int i = 0; i < horizontalSeparators.size(); i++) {
        GraphicContent hsep = horizontalSeparators.get(i);

        int x = (int) hsep.getPos().getX();
        int w = (int) hsep.getPos().getWidth();
        String combineString = String.valueOf(x) + hsep.getColor() + w;

        if (!hsepsForXCoordinate.containsKey(combineString)) {
            hsepsForXCoordinate.put(combineString, new ArrayList<GraphicContent>());
        }
        hsepsForXCoordinate.get(combineString).add(hsep);
    }

    List<GraphicContent> combinedGraphics = new ArrayList<GraphicContent>();
    for (List<GraphicContent> sepList : hsepsForXCoordinate.values()) {
        if (sepList.size() < 2) {
            continue;
        }

        Collections.sort(sepList, Sorting.sortByLowerY);

        if (log.isInfoEnabled()) {
            log.info("LOG00970:Combining " + sepList);
        }
        horizontalSeparators.removeAll(sepList);

        GraphicContent newlyCombined = sepList.get(0);
        for (int i = 1; i < sepList.size(); i++) {
            GraphicContent graphicPart = sepList.get(i);

            if (newlyCombined.getPos().distance(graphicPart.getPos()) > 50.0f) {
                combinedGraphics.add(newlyCombined);
                newlyCombined = graphicPart;
            } else {
                newlyCombined = newlyCombined.combineWith(graphicPart);
            }
        }

        combinedGraphics.add(newlyCombined);
    }
    return combinedGraphics;
}

private boolean isTooBigGraphic(@NotNull final PhysicalContent graphic) {
    return graphic.getPos().area() >= (w * h);
}

private void logGraphics() {
    for (GraphicContent g : containers) {
        log.info("LOG00502:considered container: " + g);
    }

    for (GraphicContent g : horizontalSeparators) {
        log.info("LOG00505:considered hsep: " + g);
    }

    for (GraphicContent g : verticalSeparators) {
        log.info("LOG00506:considered vsep: " + g);
    }

    for (GraphicContent g : contents) {
        log.info("LOG00980:considered content: " + g);
    }
}
}

