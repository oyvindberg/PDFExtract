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
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.TextPositionComparator;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.physical.segmentation.WordSegmentator;
import org.elacin.pdfextract.style.DocumentStyles;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.elacin.pdfextract.util.MathUtils.isWithinVariance;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 12, 2010 Time: 3:34:09 AM <p/>
 *
 *
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * PhyscalTexts, as used by this application. The difference between the two classes is technical,
 * but also semantic, in that they are defined to be at most a whole word (the normal case: Word
 * fragments will only occur when a word is split over two lines, or if the word is formatted with
 * two different styles) instead of arbitrary length.
 *
 * This makes it easier to reason about the information we have, and also reconstructs some notion
 * of word and character spacing, which will be an important property for feature recognition.
 */
public class WordSegmentatorImpl implements WordSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(WordSegmentatorImpl.class);

/**
 * Some math fonts has apparently wrong height alignment. list those here so we can do manual
 * correction
 */
@NotNull
private static List<String> strangeMathFonts = new ArrayList<String>() {
	{
		add("CMEX");
		//		add("CMMI");
		//        add("CMSY10");
	}
};

private final DocumentStyles styles;
private final float          pageRotation;

// --------------------------- CONSTRUCTORS ---------------------------

public WordSegmentatorImpl(final DocumentStyles styles, final float rotation) {

	this.styles = styles;
	pageRotation = rotation;
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface WordSegmentator ---------------------

/**
 * This method will convert the text into WordNodes. <p/> To do this, the text is split on
 * whitespaces, character and word distances are approximated, and words are created based on those
 */
@NotNull
@Override
public List<PhysicalText> segmentWords(@NotNull final List<ETextPosition> text) {

	long t0 = System.currentTimeMillis();


	List<PhysicalText> ret = new ArrayList<PhysicalText>(text.size());

	/** iterate through all incoming TextPositions, and process them
	 in a line by line fashion. We do this to be able to calculate
	 char and word distances for each line
	 */
	List<ETextPosition> line = new ArrayList<ETextPosition>();

	Collections.sort(text, new TextPositionComparator());

	float minY = 0.0f;
	float maxY = 0.0f;
	float maxX = 0.0f;

	for (ETextPosition tp : text) {
		if (!MathUtils.isWithinPercent(tp.getDir(), pageRotation, 1)) {
			log.warn("LOG00560: ignoring textposition " + StringUtils.getTextPositionString(tp)
					         + "because it has " + "wrong rotation. TODO :)");
			continue;
		}
		/* if this is the first text in a line */
		if (line.isEmpty()) {
			minY = tp.getPos().getY();
			maxY = minY + tp.getPos().getHeight();
			maxX = tp.getPos().getEndX();
		}

		if (isOnAnotherLine(minY, tp) || isTooFarAwayHorizontally(maxX, tp)) {
			if (!line.isEmpty()) {
				ret.addAll(createWordsInLine(line));
				line.clear();
			}
			minY = Float.MAX_VALUE;
		}

		/* then add the current text to start next line */
		line.add(tp);
		minY = Math.min(tp.getPos().getY(), minY);
		maxY = Math.max(minY + tp.getPos().getHeight(), maxY);
		maxX = tp.getPos().getEndX();
	}

	if (!line.isEmpty()) {
		ret.addAll(createWordsInLine(line));
		line.clear();
	}

	if (log.isDebugEnabled()) {
		log.debug("LOG00010:word segmentation took " + (System.currentTimeMillis() - t0) + " ms");
	}
	;
	return ret;
}

// -------------------------- STATIC METHODS --------------------------

//private static List<String> mathGlyphsWithProblems = new ArrayList<String>(){
//	{
////		add("|");
//		add("∫");
//		add("∑");
//		add("√");
//	}
//};

private static boolean fontSeemsToNeedVerticalAdjustment(@NotNull final ETextPosition tp) {
	final PDFont font = tp.getFont();
	boolean badFont = false;
	for (String strangeMathFont : strangeMathFonts) {
		if (font.getBaseFont() != null && font.getBaseFont().contains(strangeMathFont)) {
			badFont = true;
			break;
		}
	}
	return badFont;
	//	if (!badFont){
	//		return false;
	//	}
	//	return mathGlyphsWithProblems.contains(tp.getCharacter());
	//	return false;
}

private static boolean isOnAnotherLine(final float y, @NotNull final ETextPosition tp) {

	return !isWithinVariance(y, tp.getPos().getY(), 3.0f);
}

private static boolean isTooFarAwayHorizontally(final float endX, @NotNull final ETextPosition tp) {

	return !isWithinVariance(endX, tp.getPos().getX(), tp.getFontSizeInPt());
}

// -------------------------- OTHER METHODS --------------------------

/** This creates */
@NotNull
@SuppressWarnings({"ObjectAllocationInLoop"})
List<PhysicalText> splitTextPositionsOnSpace(@NotNull final List<ETextPosition> texts) {

	final List<PhysicalText> ret = new ArrayList<PhysicalText>(texts.size() * 2);

	final FloatPoint lastWordBoundary = new FloatPoint(0.0F, 0.0F);
	final StringBuilder contents = new StringBuilder();

	Collections.sort(texts, new TextPositionComparator());

	float width = 0.0f;
	boolean firstInLine = true;
	for (ETextPosition tp : texts) {
		if (log.isDebugEnabled()) {
			log.debug("LOG00000:in:" + StringUtils.getTextPositionString(tp));
		}
		float x = tp.getPos().getX();
		float y = tp.getPos().getY();
		final String s = tp.getCharacter();
		final Style style = styles.getStyleForTextPosition(tp);

		/** iterate through all the characters in textposition. Notice that the loop is a bit evil
		 * in that it iterates one extra time as apposed to what you would expect, in order make sure
		 * that the last piece of the textposition is created into a text.
		 */
		for (int i = 0; i <= s.length(); i++) {
			/* if this is either the last character or a space then just output a new text */
			if (i == s.length() || Character.isSpaceChar(s.charAt(i))) {
				if (contents.length() > 0) {
					/* calculate distance to the former word in the line if there was one */
					final float distance;
					if (firstInLine) {
						distance = Float.MIN_VALUE;
						firstInLine = false;
					} else {
						distance = x - lastWordBoundary.getX();
					}

					/** this is a workaround for when pdfbox sometimes miscalculates the
					 * coordinates of certain mathematical symbols */

					float adjustedHeight = tp.getPos().getHeight();

					float adjustedY = y - adjustedHeight;
					if (fontSeemsToNeedVerticalAdjustment(tp)) {
						adjustedY = y;
					}

					if (tp.isSmallOperator()) {
						adjustedHeight *= 0.4f;
						//						adjustedY += adjustedHeight;
					}

					/** output this word */
					ret.add(new PhysicalText(contents.toString(), style, x, adjustedY, width, adjustedHeight, distance, (int) tp.getDir(), tp.getSequenceNum()));

					/** change X coordinate to include this text */
					x += width;

					/** mark where this word ended so we can calculate distance between it and
					 the subsequent one */
					lastWordBoundary.setPosition(x, y);

					/** rest the rest of the state for the next word */
					width = 0.0F;
					contents.setLength(0);
				}

				if (i != s.length()) {
					x += tp.getIndividualWidths()[i];
				}
			} else {
				/** include this character in the word we are building */
				width += tp.getIndividualWidths()[i];
				contents.append(s.charAt(i));
			}
		}
	}

	return ret;
}

/**
 * This method will process one line worth of TextPositions , and split and/or combine them as to
 * output words.
 */
@NotNull
private List<PhysicalText> createWordsInLine(@NotNull final List<ETextPosition> line) {

	final Comparator<PhysicalText> sortByLowerX = new Comparator<PhysicalText>() {
		public int compare(@NotNull final PhysicalText o1, @NotNull final PhysicalText o2) {

			return Float.compare(o1.getPosition().getX(), o2.getPosition().getX());
		}
	};
	PriorityQueue<PhysicalText> queue = new PriorityQueue<PhysicalText>(line.size(), sortByLowerX);

	/* first convert into text elements */
	queue.addAll(splitTextPositionsOnSpace(line));

	/* then calculate spacing */
	CharSpacingFinder.setCharSpacingForTexts(queue);

	/* finally iterate through all texts from left to right, and combine into words as we go */
	List<PhysicalText> ret = new ArrayList<PhysicalText>();
	while (!queue.isEmpty()) {
		final PhysicalText current = queue.remove();
		final PhysicalText next = queue.peek();

		if (next != null && PhysicalText.isCloseEnoughToBelongToSameWord(next)
				&& current.isSameStyleAs(next)) {
			PhysicalText combinedWord = current.combineWith(next);
			queue.remove(next);
			queue.add(combinedWord);
			continue;
		}
		ret.add(current);
	}
	for (PhysicalText text : ret) {
		if (log.isInfoEnabled()) { log.info("LOG00540: created PhysicalText" + text); }
	}

	return ret;
}
}
