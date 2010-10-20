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
import org.elacin.pdfextract.util.MathUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:22:28 PM To change this template use File |
 * Settings | File Templates.
 */
class CharSpacingFinder {
    // ------------------------------ FIELDS ------------------------------

    static final Logger LOG = Loggers.getWordBuilderLog();

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    /**
     * This is the algorithm to find out the most probable charSpacing given a list of integer distances
     * <p/>
     * <p/>
     * There are two special cases: - only one text fragment, in which case we set a spacing of 0 - all distances are
     * almost the same, in which case we assume it is one continuous word, and not a list of single text fragments
     * <p/>
     * The general algorithm consists of these steps:
     *
     * @param distances
     * @param distanceCount
     * @param averageFontSize
     * @return
     */
    public static int calculateCharspacingForDistances(final int[] distances, final int distanceCount, final float averageFontSize) {
        /* calculate the average distance - it will be used below */
        double sum = 0.0;
        for (int i = 0; i < distanceCount; i++) {
            sum += distances[i];
        }
        double averageDistance = sum / (double) distanceCount;
        LOG.debug("averageFontSize = " + averageFontSize + ", averageDistance = " + averageDistance);

        /* this algorithm depends on that the distances be sorted */
        Arrays.sort(distances);


        double charSpacing = Double.MIN_VALUE;
        if (distanceCount == 0) {
            charSpacing = 0.0;
        } else if (MathUtils.isWithinPercent(distances[0], distances[distanceCount - 1], 10)) {
            charSpacing = distances[distanceCount - 1];
            LOG.debug("spacing: all distances equal, setting as character space");
        } else {
            /* iterate backwards - do this because char spacing seems to vary a lot more than does word spacing */
            double biggestScore = Double.MIN_VALUE;
            double lastDistance = (double) (distances[distanceCount - 1] + 1);

            for (int i = distanceCount - 1; i >= 0; i--) {
                double distance = (double) distances[i];

                /* this is just an optimization - dont consider a certain distance if we already did */
                //noinspection FloatingPointEquality
                if (distance == charSpacing || distance == lastDistance) {
                    continue;
                }


                if (distance < Math.max(averageFontSize * 3.0, averageDistance)) {
                    /* the essential scoring here is based on */
                    double score = ((lastDistance - distance) / (Math.max(1, distance)) + distance * 0.5);

                    if (distance > 0.0) {
                        /* weight the score to give some priority to spacings around the same size as the given average font size.

                        * This works by measuring the distance from distance to the given font size as the logarithm of distance with
                        *   font size as the base.
                        * One is subtracted from this logarithm to re-base the number around 0, and the absolute value is calculated.
                        *
                        * For say distances of 32 or 8 with a font size 16, this number will in both cases be 0.25.
                        *   The penalty given to the score for that would then be score*(1 + (0.25 * 0.4))
                        * */

                        //score *= (Math.abs(StrictMath.log(distance) / Math.max(1.0, StrictMath.log(averageFontSize)) - 1.0) * 0.4 + 1.0);

                        final double distanceLog = Math
                                .abs(StrictMath.log(distance) / Math.max(1.0, StrictMath.log(averageDistance))) - 1.0;
                        score *= (distanceLog * 0.1) + 1.0;

                        /* and give some priority to bigger distances */
                        score = score * (double) i / (double) distanceCount;
                    } else {
                        /* for zero or negative distances, divide the score by two */
                        score *= 0.5;
                    }

                    if (score > biggestScore) {
                        biggestScore = score;
                        charSpacing = distance;
                        LOG.debug("spacing: " + charSpacing + " is now the most probable. score: " + score);
                    } else {
                        LOG.debug("spacing: " + distance + " got score " + score);
                    }
                }
                lastDistance = distance;
            }
        }

        final double expectedMinimum = Math.max(3, averageFontSize / 2);
        if (charSpacing < expectedMinimum) {
            LOG.debug("spacing: got suspiciously low charSpacing " + charSpacing + " setting to " + expectedMinimum);
            charSpacing = expectedMinimum;
        }

        /* return after converting to int and correcting for rounding */
        //noinspection NumericCastThatLosesPrecision
        return 1 + (int) charSpacing;
    }

    /**
     * @param texts
     */
    public static void setCharSpacingForTexts(final List<Text> texts) {
        if (texts.isEmpty()) {
            return;
        }

        /* Start by making a list of all distances, and an average*/
        int[] distances = new int[texts.size() - 1];

        int distanceCount = 0;
        int fontSizeSum = 0;
        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            /* skip the first word fragment, and only include this distance if it is not too big */
            if (i != 0 && text.distanceToPreceeding < text.style.xSize * 6) {
                distances[distanceCount] = Math.max(0, text.distanceToPreceeding);
                distanceCount++;
            }
            fontSizeSum += text.style.xSize;
        }

        /* spit out some debug information */
        if (LOG.isDebugEnabled()) {
            printDebugPre(texts, distances);
        }

        final float fontSizeAverage = MathUtils.deround(fontSizeSum / (distanceCount + 1));

        int charSpacing = calculateCharspacingForDistances(distances, distanceCount, fontSizeAverage);

        /* and set the values in all texts */
        for (Text text : texts) {
            text.charSpacing = charSpacing;
        }

        if (LOG.isDebugEnabled()) {
            printDebugPost(texts, distances, charSpacing);
        }
    }

    // -------------------------- STATIC METHODS --------------------------

    private static void printDebugPost(final List<Text> texts, final int[] distances, final int charSpacing) {
        LOG.debug("spacing: sorted: " + Arrays.toString(distances));
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

    private static void printDebugPre(final List<Text> texts, final int[] distances) {
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
        LOG.debug("spacing: unsorted: " + Arrays.toString(distances));
    }
}
