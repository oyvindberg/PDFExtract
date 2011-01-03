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

package org.elacin.pdfextract.util;

import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.physical.content.HasPosition;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 08.12.10
 * Time: 03.40
 * To change this template use File | Settings | File Templates.
 */
public class Sorting {
// ------------------------------ FIELDS ------------------------------

public static final Comparator<HasPosition> sortByLowerY = new Comparator<HasPosition>() {
    public int compare(@NotNull final HasPosition o1, @NotNull final HasPosition o2) {
        return Float.compare(o1.getPos().getY(), o2.getPos().getY());
    }
};

public static final Comparator<HasPosition> sortByLowerYThenLowerX = new Comparator<HasPosition>
() {
    public int compare(@NotNull final HasPosition o1, @NotNull final HasPosition o2) {
        final int compare = Float.compare(o1.getPos().getY(), o2.getPos().getY());
        if (compare != 0) {
            return compare;
        }
        return Float.compare(o1.getPos().getX(), o2.getPos().getX());
    }
};

public static final Comparator<HasPosition> sortByLowerX = new Comparator<HasPosition>() {
    public int compare(@NotNull final HasPosition o1, @NotNull final HasPosition o2) {
        return Float.compare(o1.getPos().getX(), o2.getPos().getX());
    }
};

public static final Comparator<HasPosition> sortBySmallestArea = new Comparator<HasPosition>() {
    public int compare(@NotNull final HasPosition o1, @NotNull final HasPosition o2) {
        return Float.compare(o1.getPos().area(), o2.getPos().area());
    }
};

public static final Comparator<Style> sortStylesById = new Comparator<Style>() {
    public int compare(final Style o1, final Style o2) {
        return o1.id.compareTo(o2.id);
    }
};

public static final Comparator<ETextPosition> sortTextByBaseLine = new Comparator<ETextPosition>
() {
    public int compare(final ETextPosition o1, final ETextPosition o2) {
        return Float.compare(o1.getBaseLine(), o2.getBaseLine());
    }
};
public static final Comparator<HasPosition> regionComparator = new Comparator<HasPosition>() {
    public int compare(final HasPosition o1, final HasPosition o2) {
        if (o1.getPos().getEndX() < o2.getPos().getX()) {
            return -1;
        }
        if (o1.getPos().getX() > o2.getPos().getEndX()) {
            return 1;
        }
        if (o1.getPos().getEndY() < o2.getPos().getY()) {
            return -1;
        }
        if (o1.getPos().getY() > o2.getPos().getEndY()) {
            return 1;
        }
        if (!MathUtils.isWithinPercent(o1.getPos().getY(), o2.getPos().getY(), 4)) {
            return Float.compare(o1.getPos().getY(), o2.getPos().getY());
        }
        return Float.compare(o1.getPos().getX(), o2.getPos().getX());

    }
};

// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
public static <T extends PhysicalContent> PriorityQueue<T> createSmallestFirstQueue(@NotNull
final List<T> graphicalRegions) {
    final int capacity = Math.max(1, graphicalRegions.size());
    PriorityQueue<T> queue = new PriorityQueue<T>(capacity, sortBySmallestArea);
    queue.addAll(graphicalRegions);
    return queue;
}
}
