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

package org.elacin.pdfextract.physical.segmentation.word;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:22:28 PM To change this
 * template use File | Settings | File Templates.
 */
class CharSpacingFinder {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(CharSpacingFinder.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

/**
 * @param texts
 */
public static void setCharSpacingForTexts(@NotNull final Collection<PhysicalText> texts) {
	if (texts.isEmpty()) {
		return;
	}

	/* start by finding a list of distances, and the average font size
		   for the text */
	final List<Float> distances = getDistancesBetweenTextObjects(texts);
	final float fontSizeAverage = getAverageFontSize(texts);

	/* spit out some debug information */
	if (log.isDebugEnabled()) {
		printDebugPre(texts, distances, fontSizeAverage);
	}
	float charSpacing = calculateCharspacingForDistances(distances, fontSizeAverage);

	/* and set the values in all texts */
	for (PhysicalText text : texts) {
		text.charSpacing = charSpacing;
	}

	if (log.isDebugEnabled()) {
		printDebugPost(texts, charSpacing);
	}
}

// -------------------------- STATIC METHODS --------------------------

static float calculateCharspacingForDistances(@NotNull final List<Float> distances,
                                              final float averageFontSize)
{
	/* consider two simple cases */
	if (distances.size() == 1) {
		return distances.get(0);
	} else if (distances.isEmpty()) {
		return 0.0f;
	}

	/* find smallest and largest distances, and calculate the average distance - it will
			be used below */
	float sum = 0.0f;
	float smallest = Float.MAX_VALUE;
	float largest = Float.MIN_VALUE;
	for (final float distance : distances) {
		sum += distance;
		if (distance < smallest) {
			smallest = distance;
		}
		if (distance > largest) {
			largest = distance;
		}
	}
	final float averageDistance = sum / (float) distances.size();
	final float diff = largest - smallest;


	if (MathUtils.isWithinPercent(smallest, largest, 10.0f)) {
		return largest;
	}

	//    float minSize = 0.0f;
	//    for (int i = 0, distancesSize = distances.size() - 1; i < distancesSize; i++) {
	//        final Float d1 = distances.get(i);
	//        final Float d2 = distances.get(i + 1);
	//
	//        final float v = Math.min(d1, d2);
	//        if (v > minSize) {
	//            minSize = v;
	//        }
	//    }

	//    log.warn("minSize = " + minSize);

	//    float charSpacing = ((float) (StrictMath.log(averageFontSize) * 0.60f) * (averageDistance
	//            * 0.3f) + smallest * 0.8f);
	//
	//    if (charSpacing > minSize) {
	//        charSpacing = minSize;
	//    }

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

	float charSpacing = (averageFontSize / 10.0F) * (largest * 0.2f) + smallest * 0.6f;


	/* lower than 0.35 does not realisticly happen with 7pt, scale that with bigger font sizes */
	float lowerLimit = (0.35f / 7.0f) * (averageFontSize * (1.3f));
	/* the above doesnt seem to scale well with really big text, so bound it at 15 */
	if (lowerLimit > 15.0f) {
		lowerLimit = 15.0f;
	}
	charSpacing = Math.max(lowerLimit, charSpacing);

	if (charSpacing > largest || charSpacing < smallest) {
		float largestOfSmallest = 0.0f;
		for (int i = 0, size = distances.size(); i < size - 1; i++) {
			float smallerDistance = Math.min(distances.get(i), distances.get(i + 1));
			largestOfSmallest = Math.max(largestOfSmallest, smallerDistance);

		}
		charSpacing = largestOfSmallest;
	}


	return charSpacing;
}

private static float getAverageFontSize(@NotNull final Collection<PhysicalText> texts) {
	float fontSizeSum = 0.0f;
	for (PhysicalText text : texts) {
		fontSizeSum += text.style.xSize;
	}
	return fontSizeSum / (float) texts.size();
}

@NotNull
private static List<Float> getDistancesBetweenTextObjects(@NotNull final Collection<PhysicalText> texts) {
	List<Float> distances = new ArrayList<Float>(texts.size());
	boolean first = true;
	for (PhysicalText text : texts) {
		/* skip the first word fragment, and only include this distance if it is not too big */
		if (!first && text.distanceToPreceeding < text.style.xSize * 6.0f) {
			//			distances.add(Math.max(0.0f, text.distanceToPreceeding));
			distances.add(text.distanceToPreceeding);
		}
		first = false;
	}
	return distances;
}

private static void printDebugPost(@NotNull final Collection<PhysicalText> texts,
                                   final float charSpacing)
{
	log.debug("spacing: charSpacing=" + charSpacing);

	StringBuilder out = new StringBuilder();

	boolean first = true;
	for (PhysicalText text : texts) {
		if (!first && text.distanceToPreceeding > charSpacing) {
			out.append(" ");
		}
		out.append(text.content);
		first = false;
	}
	log.debug("spacing: output: " + out);
}

private static void printDebugPre(@NotNull final Collection<PhysicalText> texts,
                                  final List<Float> distances,
                                  final float fontSizeAverage)
{
	StringBuilder textWithDistances = new StringBuilder();
	StringBuilder textOnly = new StringBuilder();

	boolean first = true;
	for (PhysicalText text : texts) {
		if (!first) {
			textWithDistances.append("> ").append(text.distanceToPreceeding).append(">");
		}
		textWithDistances.append(text.content);
		textOnly.append(text.content);
		first = false;
	}

	log.debug("spacing: -----------------");
	log.debug("spacing: fontSizeAverage: " + fontSizeAverage);
	log.debug("spacing: content: " + textWithDistances);
	log.debug("spacing: content: " + textOnly);
	log.debug("spacing: distances: " + distances);
}
}
