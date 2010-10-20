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

package org.elacin.pdfextract.segmentation.word;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.Loggers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:22:28 PM To change this template use File |
 * Settings | File Templates.
 */
class CharSpacingFinder {
    // ------------------------------ FIELDS ------------------------------

    static final Logger LOG = Loggers.getWordBuilderLog();

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    public static float calculateCharspacingForDistances(final List<Float> distances, final float averageFontSize) {
        if (distances.size() == 1) {
            return distances.get(0);
        } else if (distances.isEmpty()) {
            return 0.0f;
        }

        /* calculate the average distance - it will be used below */
        float sum = 0.0f;
        float smallest = Float.MAX_VALUE;

        for (final float distance : distances) {
            sum += distance;
            if (distance < smallest) {
                smallest = distance;
            }
        }
        final float averageDistance = sum / (float) distances.size();

        /**
         * The reasoning behind this algorithm is the following:
         *
         *  - The 'size' of a font is normally way bigger than spacing between
         *      words, so it is useful to consider a fraction of it. 10% is
         *      easy to reason about and thus use as a unit.
         *
         *  - It is necessary to use the font size, because obviously the 'normal'
         *      word spacing will vary with it (bigger fonts have bigger spacing)
         *
         *  - To consider how 'many' ten percent font size units we want,
         *      we consider the average distance between characters
         *
         *  - Finally, we offset the calculates charspacing with the minimum 
         *      distance found. This is to facilitate correct spacing of lines
         *      with abnormally long intra-word spacing. 
         *
         */
        final float charSpacing = (averageFontSize / 10.0F) * (averageDistance * 0.4f) + smallest * 0.6f;

        return charSpacing;
    }

    /**
     * @param texts
     */
    public static void setCharSpacingForTexts(final List<Text> texts) {
        if (texts.isEmpty()) {
            return;
        }

        /* start by finding a list of distances, and the average font size
           for the text
        */
        List<Float> distances = getDistancesBetweenTextObjects(texts);
        final float fontSizeAverage = getAverageFontSize(texts);

        /* spit out some debug information */
        if (LOG.isDebugEnabled()) {
            printDebugPre(texts, distances);
        }
        float charSpacing = calculateCharspacingForDistances(distances, fontSizeAverage);

        /* and set the values in all texts */
        for (Text text : texts) {
            text.charSpacing = charSpacing;
        }

        if (LOG.isDebugEnabled()) {
            printDebugPost(texts, distances, charSpacing);
        }
    }

    // -------------------------- STATIC METHODS --------------------------

    private static float getAverageFontSize(final Collection<Text> texts) {
        float fontSizeSum = 0.0f;
        for (Text text : texts) {
            fontSizeSum += text.style.xSize;
        }
        return fontSizeSum / (float) texts.size();
    }

    private static List<Float> getDistancesBetweenTextObjects(final List<Text> texts) {
        List<Float> distances = new ArrayList<Float>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            /* skip the first word fragment, and only include this distance if it is not too big */
            if (i != 0 && text.distanceToPreceeding < text.style.xSize * 6.0f) {
                distances.add(Math.max(0.0f, text.distanceToPreceeding));
            }
        }

        return distances;
    }

    private static void printDebugPost(final List<Text> texts, final List<Float> distances, final float charSpacing) {
        LOG.debug("spacing: sorted: " + distances);
        LOG.debug("spacing: charSpacing=" + charSpacing);

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            if (i != 0 && text.distanceToPreceeding > charSpacing) {
                out.append(" ");
            }
            out.append(text.content);
        }
        LOG.debug("spacing: output: " + out);
    }

    private static void printDebugPre(final List<Text> texts, final List<Float> distances) {
        StringBuilder textWithDistances = new StringBuilder();
        StringBuilder textOnly = new StringBuilder();

        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            if (i != 0) {
                textWithDistances.append("> ").append(text.distanceToPreceeding).append(">");
            }
            textWithDistances.append(text.content);
            textOnly.append(text.content);
        }
        LOG.debug("spacing: -----------------");
        LOG.debug("spacing: content: " + textWithDistances);
        LOG.debug("spacing: content: " + textOnly);
        LOG.debug("spacing: unsorted: " + distances);
    }
}
