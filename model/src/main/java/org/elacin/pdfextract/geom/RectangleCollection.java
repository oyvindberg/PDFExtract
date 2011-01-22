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

package org.elacin.pdfextract.geom;

import org.elacin.pdfextract.content.PhysicalContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.elacin.pdfextract.Constants.RECTANGLE_COLLECTION_CACHE_ENABLED;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Nov 2, 2010 Time: 1:20:36 AM To change this template
 * use File | Settings | File Templates.
 */
public class RectangleCollection extends PhysicalContent {
// ------------------------------ FIELDS ------------------------------

@NotNull
private final List<PhysicalContent> contents;

/* calculating all the intersections while searching is expensive, so keep this cached.
will be pruned on update */
@NotNull
private final Map<Integer, List<PhysicalContent>> yCache
                                                         = new HashMap<Integer, List<PhysicalContent>>();
@NotNull
private final Map<Integer, List<PhysicalContent>> xCache
                                                         = new HashMap<Integer, List<PhysicalContent>>();

@Nullable
private final PhysicalContent parent;


private transient boolean _posSet;

// --------------------------- CONSTRUCTORS ---------------------------

public RectangleCollection(@NotNull final Collection<? extends PhysicalContent> newContents,
                           @Nullable final PhysicalContent parent)
{
    super(newContents);
    this.parent = parent;

    contents = new ArrayList<PhysicalContent>(newContents.size());
    contents.addAll(newContents);
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface HasPosition ---------------------

@Override
public Rectangle getPos() {
    if (!_posSet) {
        setPositionFromContentList(getContents());
        _posSet = true;
    }
    return pos;

}

// --------------------- GETTER / SETTER METHODS ---------------------

@NotNull
public List<PhysicalContent> getContents() {
    return contents;
}

public PhysicalContent getParent() {
    return parent;
}

// -------------------------- PUBLIC METHODS --------------------------

public void addContent(final PhysicalContent content) {
    contents.add(content);
    clearCache();
}

public void addContents(Collection<? extends PhysicalContent> newContents) {
    contents.addAll(newContents);
    clearCache();
}

@SuppressWarnings({"NumericCastThatLosesPrecision"})
public List<PhysicalContent> findContentAtXIndex(float x) {
    return findContentAtXIndex((int) x);
}

public List<PhysicalContent> findContentAtXIndex(int x) {
    if (!RECTANGLE_COLLECTION_CACHE_ENABLED || !xCache.containsKey(x)) {
        final Rectangle searchRectangle = new Rectangle((float) x, getPos().getY(), 1.0f,
                                                        getPos().getHeight());
        final List<PhysicalContent> result = findContentsIntersectingWith(searchRectangle);
        Collections.sort(result, Sorting.sortByLowerY);

        xCache.put(x, result);
    }
    return xCache.get(x);
}

@SuppressWarnings({"NumericCastThatLosesPrecision"})
public List<PhysicalContent> findContentAtYIndex(float y) {
    return findContentAtYIndex((int) y);
}

public List<PhysicalContent> findContentAtYIndex(int y) {
    if (!RECTANGLE_COLLECTION_CACHE_ENABLED || !yCache.containsKey(y)) {
        final Rectangle searchRectangle = new Rectangle(getPos().getX(), (float) y,
                                                        getPos().getWidth(), 1.0F);
        final List<PhysicalContent> result = findContentsIntersectingWith(searchRectangle);
        Collections.sort(result, Sorting.sortByLowerX);
        yCache.put(y, result);
    }
    return yCache.get(y);
}

@NotNull
public List<PhysicalContent> findContentsIntersectingWith(@NotNull final HasPosition search) {
    final List<PhysicalContent> ret = new ArrayList<PhysicalContent>(50);
    for (PhysicalContent r : contents) {
        if (search.getPos().intersectsWith(r.getPos())) {
            ret.add(r);
        }
    }
    return ret;
}

@NotNull
public List<PhysicalContent> findSurrounding(@NotNull final HasPosition content,
                                             final int distance)
{
    final Rectangle bound = content.getPos();

    Rectangle searchRectangle = new Rectangle(bound.getX() - (float) distance,
                                              bound.getY() - (float) distance,
                                              bound.getWidth() + (float) distance,
                                              bound.getHeight() + (float) distance);

    final List<PhysicalContent> ret = findContentsIntersectingWith(searchRectangle);

    if (ret.contains(content)) {
        ret.remove(content);
    }

    return ret;
}

public float getHeight() {
    return getPos().getHeight();
}

public float getWidth() {
    return getPos().getWidth();
}

public void removeContent(PhysicalContent toRemove) {
    if (!contents.remove(toRemove)) {
        throw new RuntimeException("Region " + this + ": Could not remove " + toRemove);
    }
    clearCache();
    setPositionFromContentList(contents);
}

public void removeContents(@NotNull Collection<PhysicalContent> listToRemove) {
    contents.removeAll(listToRemove);
    clearCache();
    setPositionFromContentList(contents);
}

@NotNull
public List<PhysicalContent> searchInDirectionFromOrigin(@NotNull Direction dir,
                                                         @NotNull HasPosition origin,
                                                         float distance)
{
    final Rectangle pos = origin.getPos();
    final float x = pos.getX() + dir.xDiff * distance;
    final float y = pos.getY() + dir.yDiff * distance;

    final Rectangle search = new Rectangle(x, y, pos.getWidth(), pos.getHeight());

    final List<PhysicalContent> ret = findContentsIntersectingWith(search);

    if (ret.contains(origin)) {
        ret.remove(origin);
    }

    return ret;
}

// -------------------------- OTHER METHODS --------------------------

protected void clearCache() {
    yCache.clear();
    xCache.clear();
    _posSet = false;
}

// -------------------------- ENUMERATIONS --------------------------

public enum Direction {
    N(0, 1),
    NE(1, 1),
    E(1, 0),
    SE(1, -1),
    S(0, -1),
    SW(-1, -1),
    W(-1, 0),
    NW(-1, 1);
    float xDiff;
    float yDiff;

    Direction(final float xDiff, final float yDiff) {
        this.xDiff = xDiff;
        this.yDiff = yDiff;
    }
}
}
