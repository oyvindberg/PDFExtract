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

package org.elacin.pdfextract.content;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.Constants;
import org.elacin.pdfextract.geom.HasPosition;
import org.elacin.pdfextract.geom.Rectangle;
import org.elacin.pdfextract.geom.RectangleCollection;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.style.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 8, 2010 Time: 7:44:41 PM To change this template
 * use File | Settings | File Templates.
 */
public class PhysicalPageRegion extends RectangleCollection {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PhysicalPageRegion.class);


/* average font sizes for this page region */
private transient boolean fontInfoFound;
private transient float   _avgFontSizeX;
private transient float   _avgFontSizeY;
@Nullable
private transient Style   _mostCommonStyle;
private transient float   _shortestText;
private transient boolean _posSet;


private transient boolean medianFound;
private transient int     _medianOfVerticalDistances;

/* the physical page containing this region */
@NotNull
private final PhysicalPage page;

@NotNull
private final List<PhysicalPageRegion> subregions = new ArrayList<PhysicalPageRegion>();

@NotNull
private final List<WhitespaceRectangle> whitespace = new ArrayList<WhitespaceRectangle>();

@Nullable
private GraphicContent containingGraphic;

// --------------------------- CONSTRUCTORS ---------------------------

public PhysicalPageRegion(@NotNull final Collection<? extends PhysicalContent> contents,
                          @Nullable final PhysicalContent parent,
                          @NotNull PhysicalPage page)
{
    super(contents, parent);
    this.page = page;
}

public PhysicalPageRegion(@NotNull final List<? extends PhysicalContent> contents,
                          @NotNull PhysicalPage page)
{
    this(contents, null, page);
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

@Override
public Rectangle getPos() {
    if (Constants.WHITESPACE_USE_WHOLE_PAGE && page.getMainRegion() == this) {
        return page.getPageDimensions();
    }

    if (!_posSet) {
        pos = super.getPos();
        if (containingGraphic != null) {
            pos = super.getPos().union(containingGraphic);
        }
        _posSet = true;
    }
    return pos;
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
public void clearCache() {
    super.clearCache();
    fontInfoFound = false;
    medianFound = false;
    _posSet = false;
}

// --------------------- GETTER / SETTER METHODS ---------------------

@Nullable
public GraphicContent getContainingGraphic() {
    return containingGraphic;
}

@NotNull
public PhysicalPage getPage() {
    return page;
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

public void addWhitespace(final Collection<WhitespaceRectangle> whitespace) {
    this.whitespace.addAll(whitespace);
    addContents(whitespace);
}

public void ensureAllContentInLeafNodes() {
    if (!subregions.isEmpty()) {
        Collection<PhysicalContent> contents = new ArrayList<PhysicalContent>();
        for (PhysicalContent content : getContents()) {
            if (!(content instanceof PhysicalPageRegion)) {
                contents.add(content);
            }
        }
        doExtractSubRegion(contents, null, null);
    }

    for (PhysicalPageRegion subregion : subregions) {
        subregion.ensureAllContentInLeafNodes();
    }
}

public void extractSubRegionFromBound(@NotNull Rectangle bound) {
    final List<PhysicalContent> subContents = findContentsIntersectingWith(bound.getPos());
    doExtractSubRegion(subContents, bound, null);
}

public void extractSubRegionFromContent(final Set<PhysicalContent> set) {
    doExtractSubRegion(set, null, null);
}

/**
 * Returns a subregion with all the contents which is contained by bound. If more than two pieces of
 * content crosses the boundary of bound, it is deemed inappropriate for dividing the page, and an
 * exception is thrown
 *
 * @return the new region
 */
public void extractSubRegionFromGraphic(@NotNull final GraphicContent graphic) {
    /* we can allow us to search a bit outside the graphic */
    final Rectangle pos = graphic.getPos();
    final float extra = 2.0f;
    final Rectangle searchPos = new Rectangle(pos.getX() - extra, pos.getY() - extra,
                                              pos.getWidth() + 2 * extra,
                                              pos.getHeight() + 2 * extra);

    final List<PhysicalContent> subContents = findContentsIntersectingWith(searchPos);
    doExtractSubRegion(subContents, graphic, graphic);
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

public int getMedianOfVerticalDistances() {
    if (!medianFound) {
        findAndSetMedianOfVerticalDistancesForRegion();
    }
    return _medianOfVerticalDistances;
}

public float getMinimumColumnSpacing() {
    return getAvgFontSizeX() * 0.8f;
}

public float getMinimumRowSpacing() {
    if (!fontInfoFound) {
        findAndSetFontInformation();
    }

    return getMedianOfVerticalDistances();//* 0.8f;
    //    return _shortestText * 1.2f;
    //
}

@Nullable
public Style getMostCommonStyle() {
    if (!fontInfoFound) {
        findAndSetFontInformation();
    }
    return _mostCommonStyle;
}

public boolean isGraphicalRegion() {
    return containingGraphic != null;
}

public void setContainingGraphic(@Nullable GraphicContent containingGraphic) {
    if (this.containingGraphic != null) {
        removeContent(this.containingGraphic);
    }

    if (containingGraphic != null) {
        this.containingGraphic = containingGraphic;
        //        addContent(containingGraphic);
    }
}

// -------------------------- OTHER METHODS --------------------------

private void doExtractSubRegion(@NotNull final Collection<PhysicalContent> subContents,
                                @Nullable final HasPosition bound,
                                @Nullable final GraphicContent graphic)
{
    if (subContents.isEmpty()) {
        if (log.isInfoEnabled()) {
            log.info("LOG00960:bound " + bound + " contains no content in " + this + ". wont "
                             + "extract");
        }
        return;
    }

    if (subContents.size() == getContents().size()) {
        if (log.isInfoEnabled()) {
            log.info("LOG00950:bound " + bound + " contains all content in " + this + ". wont "
                             + "extract");
        }
        return;
    }


    boolean onlyWhitespace = true;
    for (PhysicalContent subContent : subContents) {
        if (!(subContent instanceof WhitespaceRectangle)) {
            onlyWhitespace = false;
            break;
        }
    }
    if (onlyWhitespace) {
        if (log.isInfoEnabled()) {
            log.info("LOG01330:Tried to extract only whitespace. removing them");
        }
        removeContents(subContents);
        return;
    }


    final PhysicalPageRegion newRegion = new PhysicalPageRegion(subContents, this, page);

    if (graphic == null) {
        newRegion.setContainingGraphic(containingGraphic);
    } else {
        newRegion.setContainingGraphic(graphic);
    }

    /* move this regions subregions if they are contained by the new region */
    List<PhysicalPageRegion> toMove = new ArrayList<PhysicalPageRegion>();
    for (PhysicalContent subContent : subContents) {
        if (subContent instanceof PhysicalPageRegion) {
            toMove.add((PhysicalPageRegion) subContent);
        }
    }

    for (PhysicalPageRegion regionToMove : toMove) {
        assert subregions.remove(regionToMove);
        assert newRegion.subregions.add(regionToMove);
    }


    log.warn("LOG00890:Extracted PPR:" + newRegion + " from " + this);

    removeContents(subContents);

    addContent(newRegion);
    subregions.add(newRegion);
}

/* find average font sizes, and most used style for the region */
protected void findAndSetFontInformation() {
    float xFontSizeSum = 0.0f, yFontSizeSum = 0.0f;
    int numCharsFound = 0;
    float shortestText = Float.MAX_VALUE;
    for (PhysicalContent content : getContents()) {
        if (content.isText()) {
            final Style style = content.getPhysicalText().getStyle();

            final int length = content.getPhysicalText().getText().length();
            xFontSizeSum += (float) (style.xSize * length);
            yFontSizeSum += (float) (style.ySize * length);
            numCharsFound += length;
            shortestText = Math.min(shortestText, content.getPos().getHeight());
        }
    }
    if (numCharsFound == 0) {
        _avgFontSizeX = Float.MIN_VALUE;
        _avgFontSizeY = Float.MIN_VALUE;
        _mostCommonStyle = null;
        _shortestText = 0.0f;
    } else {
        _avgFontSizeX = xFontSizeSum / (float) numCharsFound;
        _avgFontSizeY = yFontSizeSum / (float) numCharsFound;
        _mostCommonStyle = TextUtils.findDominatingStyle(getContents());
        _shortestText = shortestText;
    }
    fontInfoFound = true;
}

/**
 * Finds an approximation of the normal vertical line spacing for the region. <p/> This is done by
 * looking at three vertical rays, calculating distances between all the lines intersecting those
 * lines, and then returning the median of all those distances. <p/> minimum value is 2, max is
 * (avgFontSize * 3) -1. -1 if nothing is found;
 */
protected int findAndSetMedianOfVerticalDistancesForRegion() {
    final int LIMIT = (int) getAvgFontSizeY() * 3;

    int[] distanceCount = new int[LIMIT];
    for (float x = getPos().getX(); x <= getPos().getEndX(); x += getPos().getWidth() / 3) {
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

    float temp = Math.max(index, (int) (getAvgFontSizeY() * 0.5f));
    _medianOfVerticalDistances = (int) (temp + Math.max(1.0f, temp * 0.1f));
    medianFound = true;

    return _medianOfVerticalDistances;
}
}

