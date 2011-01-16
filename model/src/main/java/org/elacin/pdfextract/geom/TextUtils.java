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


import org.elacin.pdfextract.content.StyledText;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 19, 2010 Time: 3:46:09 AM To change this
 * template use File | Settings | File Templates.
 */
public class TextUtils {
// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
public static Rectangle findBounds(@NotNull final Collection<? extends HasPosition> contents) {
    final Rectangle newPos;

    if (contents.isEmpty()) {
        //TODO: handle empty regions in a better way
        newPos = new Rectangle(0.1f, 0.1f, 0.1f, 0.1f);
//        throw new RuntimeException("no content!");
    } else {
        /* calculate bounds for this region */
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        for (HasPosition content : contents) {
            minX = Math.min(minX, content.getPos().getX());
            minY = Math.min(minY, content.getPos().getY());
            maxX = Math.max(maxX, content.getPos().getEndX());
            maxY = Math.max(maxY, content.getPos().getEndY());
        }
        newPos = new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
    return newPos;
}

@NotNull
public static Style findDominatingStyle(@NotNull final Collection<? extends HasPosition>
                                                contents) {
    if (!listContainsStyledText(contents)) {
        return Style.NO_STYLE;
    }

    boolean textFound = false;
    Map<Style, Integer> letterCountPerStyle = new HashMap<Style, Integer>(10);
    for (HasPosition content : contents) {
        if (!(content instanceof StyledText)) {
            continue;
        }
        StyledText text = (StyledText) content;

        final Style style = text.getStyle();
        if (!letterCountPerStyle.containsKey(style)) {
            letterCountPerStyle.put(style, 0);
        }
        final int numChars = text.getText().length();
        letterCountPerStyle.put(style, letterCountPerStyle.get(style) + numChars);
        textFound = true;
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

public static boolean listContainsStyledText(@NotNull final Collection<? extends HasPosition>
                                                     list) {
    for (HasPosition content : list) {
        if (content instanceof StyledText) {
            return true;
        }
    }
    return false;
}

public static boolean stringContainsAnyCharacterOf(String string, String chars) {
    for (int i = 0; i < string.length(); i++) {
        if (chars.indexOf(string.charAt(i)) != -1) {
            return true;
        }
    }
    return false;
}
}
