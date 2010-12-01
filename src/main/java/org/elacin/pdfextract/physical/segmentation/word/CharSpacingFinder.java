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
import org.elacin.pdfextract.logical.Formulas;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Sep 23, 2010 Time: 2:22:28 PM To change this
 * template use File | Settings | File Templates.
 */
class CharSpacingFinder {
// ------------------------------ FIELDS ------------------------------

private static final Logger  log                    = Logger.getLogger(CharSpacingFinder.class);
private static final boolean WRITE_SPACINGS_TO_FILE = true;

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
		printDebugPost(texts, distances, fontSizeAverage, charSpacing);
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
	float smallest = Float.MAX_VALUE;
	float largest = Float.MIN_VALUE;
	for (final float distance : distances) {
		smallest = Math.min(smallest, distance);
		largest = Math.max(largest, distance);
	}
	final float diff = largest - Math.max(0.0f, smallest);

	final float median = findMedianOfDistances(distances, -10.0f);

	if (log.isInfoEnabled()) {
		log.info("LOG00790:median:" + median + ", diff:" + diff + ", smallest:" + smallest + ", "
				         + "" + ", " + "" + "largest: " + largest);
	}


	if (diff < smallest * 1.10f || Math.abs(diff - smallest) < 0.20f) {
		if (log.isDebugEnabled()) {
			log.debug("if (diff < smallest*1.10f || Math.abs(diff - smallest) < 0.20f) {");
		}
		return largest;
	}


	if (MathUtils.isWithinPercent(smallest, largest, 10.0f)) {
		if (log.isDebugEnabled()) {
			log.debug("MathUtils.isWithinPercent(smallest, largest, 10.0f)");
		}
		return largest;
	}


	if (!MathUtils.isWithinPercent(diff, largest, 15) && MathUtils
			.isWithinPercent(median, largest, 15)) {
		if (log.isDebugEnabled()) { log.debug("MathUtils.isWithinPercent(median, largest, 15)"); }
		return largest;
	}

	final float NUM_STEPS = 20.0f;
	float step = diff / NUM_STEPS;
	final Float[] dists = distances.toArray(new Float[distances.size()]);
	Arrays.sort(dists);


	final float lowerLimit = getLowerCharspaceLimitForFontSize(averageFontSize);

	int lastNumStepts = -1;

	final float[] ret = new float[3];

	for (float dist : dists) {
		if (dist < smallest) {
			continue;
		}

		int currentNumSteps = (int) (dist / step);

		if (lastNumStepts == -1) {
			lastNumStepts = currentNumSteps;
			continue;
		}

		if (currentNumSteps - lastNumStepts > 3 && dist > lowerLimit && dist > smallest * 1.10f) {
			if (dist > median * 1.2f && dist <= diff) {
				ret[0] = dist;
				break;
			} else if (dist > median) {
				ret[1] = dist;
				break;
			}
			// else {
			//				ret[2] = dist;
			//			}
		}
		lastNumStepts = currentNumSteps;
	}

	final float charSpacing;
	if (ret[0] > 0.0f) {
		charSpacing = ret[0];
	} else if (ret[1] > 0.0f) {
		charSpacing = ret[1];
	} else if (ret[2] > 0.0f) {
		charSpacing = ret[2];
	} else {
		return largest;
		//		charSpacing = largest;
	}
	if (log.isInfoEnabled()) {
		log.info("lowerLimit:" + lowerLimit + ", ret: " + Arrays.toString(ret));
	}


	return charSpacing * 0.95f;
}

private static float findDistanceClosestTo(final List<Float> distances, final float limit) {
	float closestToLimit = Float.MAX_VALUE;
	float ret = 0.0f;

	for (Float distance : distances) {
		final float absDiff = Math.abs(limit - distance);
		if (absDiff < closestToLimit) {
			closestToLimit = absDiff;
			ret = distance;
		}
	}
	return ret * 0.95f;
}

private static float findFirstDistanceSmallerThan(final List<Float> distances, final float value) {
	float closestToLimit = Float.MAX_VALUE;
	float ret = 0.0f;

	for (float distance : distances) {
		final float diff = value - distance;
		if (diff > 0 && diff < closestToLimit) {
			closestToLimit = diff;
			ret = distance;
		}
	}
	return ret;
}

@SuppressWarnings({"NumericCastThatLosesPrecision"})
private static float findMedianOfDistances(final List<Float> distances, final float lowerLimit) {
	final Float[] floats = distances.toArray(new Float[distances.size()]);
	Arrays.sort(floats);

	final float RESOLUTION = 20.0f;

	final int lowerLimitI = (int) (lowerLimit * RESOLUTION);
	int currentDistance = Integer.MIN_VALUE;
	int currentOccurences = 0;

	int maxOccurrences = 0;
	int ret = Integer.MIN_VALUE;
	for (float d : floats) {
		int dInt = (int) (d * RESOLUTION);

		if (dInt < lowerLimitI) {
			continue;
		}
		if (currentDistance == Integer.MIN_VALUE) {
			currentDistance = dInt;
		}

		if (dInt != currentDistance) {
			if (currentOccurences >= maxOccurrences) {
				ret = currentDistance;
				maxOccurrences = currentOccurences;
			}
			currentDistance = dInt;
			currentOccurences = 1;
		} else {
			currentOccurences++;
		}
	}

	return (float) ret / RESOLUTION;
}

private static float getAverageFontSize(@NotNull final Collection<PhysicalText> texts) {
	int fontSizeSum = 0;
	for (PhysicalText text : texts) {
		fontSizeSum += text.getStyle().xSize;
	}
	return (float) fontSizeSum / (float) texts.size();
}

@NotNull
private static List<Float> getDistancesBetweenTextObjects(@NotNull final Collection<PhysicalText> texts) {
	List<Float> distances = new ArrayList<Float>(texts.size());
	boolean first = true;
	for (PhysicalText text : texts) {
		/* skip the first word fragment, and only include this distance if it is not too big */
		if (!first ){//&& text.distanceToPreceeding < (Math.max(text.getStyle().xSize,
		                                           //          5.0f) * 6.0f)) {
			distances.add(text.distanceToPreceeding);
		}
		first = false;
	}
	return distances;
}

private static float getLowerCharspaceLimitForFontSize(final float averageFontSize) {
	/* lower than 0.35 does not realisticly happen with 7pt, scale that with bigger font sizes */
	float lowerLimit = (0.35f / 7.0f) * (averageFontSize * (1.3f));
	/* the above doesnt seem to scale well with really big text, so limit it at 15 */
	if (lowerLimit > 15.0f) {
		lowerLimit = 15.0f;
	}
	return lowerLimit;
}

private static void printDebugPost(@NotNull final Collection<PhysicalText> texts,
                                   final List<Float> distances,
                                   final float fontSizeAverage,
                                   final float charSpacing)
{
	log.debug("spacing: charSpacing=" + charSpacing);

	StringBuilder out = new StringBuilder();

	boolean first = true;
	for (PhysicalText text : texts) {
		if (!first && text.distanceToPreceeding > charSpacing) {
			out.append(" ");
		}
		out.append(text.text);
		first = false;
	}

	if (WRITE_SPACINGS_TO_FILE) {
		if (out.length() > 5 && !Formulas.textSeemsToBeFormula(texts) && !TextUtils
				.stringContainsAnyCharacterOf(out.toString(), "()[]")) {
			final Logger logger = Logger.getLogger("spacingdata");
			//	final String sep = "#&#";
			logger.info("\n" + out);
			logger.info("\t" + fontSizeAverage);
			logger.info("\t" + distances);
		}
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
		textWithDistances.append(text.text);
		textOnly.append(text.text);
		first = false;
	}

	log.debug("spacing: -----------------");
	log.debug("spacing: fontSizeAverage: " + fontSizeAverage);
	log.debug("spacing: content: " + textWithDistances);
	log.debug("spacing: content: " + textOnly);
	log.debug("spacing: distances: " + distances);
}
}
