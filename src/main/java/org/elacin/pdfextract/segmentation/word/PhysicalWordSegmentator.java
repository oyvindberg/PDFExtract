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
import org.apache.pdfbox.util.TextPosition;
import org.apache.pdfbox.util.TextPositionComparator;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.tree.DocumentStyles;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.FloatPoint;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 12, 2010
 * Time: 3:34:09 AM
 * <p/>
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * WordNodes, as used by this application. The difference between the two classes are
 * technical, but also semantic, in that they are defined to be at most a whole word (the
 * normal case: Word fragments will only occur when a word is split over two lines, or
 * if the word is formatted with two different styles) instead of arbitrary length.
 * <p/>
 * This makes it easier to reason about the information we have, and also reconstructs
 * some notion of word and character spacing, which will be an important property
 * for feature recognition.
 * <p/>
 * By using createWords() all this will be done, and the words will be added to the
 * the provided document tree.
 */
public class PhysicalWordSegmentator {
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = Loggers.getWordBuilderLog();

// -------------------------- STATIC METHODS --------------------------

    /**
     * This creates
     *
     * @param sf
     * @param textPositions
     * @return
     */
    static List<Text> getTextsFromTextPositions(final DocumentStyles sf, final List<TextPosition> textPositions) {
        List<Text> ret = new ArrayList<Text>(textPositions.size() * 2);

        FloatPoint lastWordBoundary = new FloatPoint(0.0F, 0.0F);
        StringBuilder contents = new StringBuilder();

        Collections.sort(textPositions, new TextPositionComparator());

        float width = 0.0f;
        boolean firstInLine = true;
        for (TextPosition textPosition : textPositions) {
            float x = textPosition.getXDirAdj();
            float y = textPosition.getYDirAdj();
            final Style style = sf.getStyleForTextPosition(textPosition);

            for (int j = 0; j < textPosition.getCharacter().length(); j++) {
                /* if we found a space */
                if (Character.isSpaceChar(textPosition.getCharacter().charAt(j)) || isTextPositionTooHigh(textPosition)) {
                    if (contents.length() != 0) {
                        /* else just output a new text */

                        final float distance;
                        if (firstInLine) {
                            distance = Float.MIN_VALUE;
                            firstInLine = false;
                        } else {
                            distance = x - lastWordBoundary.getX();
                        }

                        ret.add(new Text(contents.toString(), style, x, y, width, textPosition.getHeightDir(), distance));
                        contents.setLength(0);
                        x += width;
                        width = 0.0F;
                        lastWordBoundary.setPosition(x, y);
                    }

                    x += textPosition.getIndividualWidths()[j];
                } else {
                    /* include this character */
                    width += textPosition.getIndividualWidths()[j];
                    contents.append(textPosition.getCharacter().charAt(j));
                }


                /* if this is the last char */
                if (j == textPosition.getCharacter().length() - 1 && contents.length() != 0) {
                    final float distance;
                    if (firstInLine) {
                        distance = Float.MIN_VALUE;
                        firstInLine = false;
                    } else {
                        distance = x - lastWordBoundary.getX();
                    }

                    /* Some times textPosition.getIndividualWidths will contain zero, so work around that here */
                    if (width == 0.0F) {
                        width = textPosition.getWidthDirAdj();
                    }

                    ret.add(new Text(contents.toString(), style, x, y, width, textPosition.getHeightDir(), distance));
                    contents.setLength(0);
                    x += width;
                    width = 0.0F;
                    lastWordBoundary.setPosition(x, y);
                }
            }
        }

        return ret;
    }

    /**
     * Some times TextPositions will be far, far higher than the font size would allow. this is normally a faulty PDF or a
     * bug in PDFBox. since they destroy my algorithms ill just drop them
     *
     * @param textPosition
     * @return
     */
    private static boolean isTextPositionTooHigh(final TextPosition textPosition) {
        return textPosition.getHeightDir() > (float) (textPosition.getFontSize() * textPosition.getYScale() * 1.2);
    }

    /**
     * This method will process one line worth of TextPositions , and split and/or
     * combine them as to output words. The words are added directly to the tree
     *
     * @param root
     * @param wordNodes
     * @param pageNum
     * @param line
     */
    static void processLine(final DocumentNode root, final List<WordNode> wordNodes, final int pageNum, final List<TextPosition> line) {
        /* first convert into text elements */
        final List<Text> lineTexts = getTextsFromTextPositions(root.getStyles(), line);

        /* then calculate spacing */
        CharSpacingFinder.setCharSpacingForTexts(lineTexts);

        /* this will be used to keep all the state while combining text fragments into words */
        WordState currentState = new WordState();

        /* create WordNodes and add them to the tree */
        for (Text newText : lineTexts) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("in : " + newText);
            }

            /* if this is the first text element going into a word */
            if (currentState.len == 0) {
                currentState.x = newText.x;
                currentState.y = newText.y;
                currentState.charSpacing = newText.charSpacing;
            } else {
                /* if not, check if this new text means we should finish the current word */
                if (currentState.isTooFarAway(newText) || currentState.isDifferentStyle(newText.style)) {
                    wordNodes.add(currentState.createWord(pageNum));
                    currentState.x = newText.x;
                }
            }

            currentState.currentStyle = newText.style;
            currentState.maxHeight = Math.max(currentState.maxHeight, newText.height);

            /* copy text from the Text object, and adjust width */
            for (int textPositionIdx = 0; textPositionIdx < newText.content.length(); textPositionIdx++) {
                currentState.chars[currentState.len] = newText.content.charAt(textPositionIdx);
                currentState.len++;
            }
            currentState.width = newText.x + newText.width - currentState.x;
        }

        /* no words can span lines, so yield whatever is read */
        if (currentState.len != 0) {
            wordNodes.add(currentState.createWord(pageNum));
        }
    }

// -------------------------- PUBLIC METHODS --------------------------

    /**
     * This method will convert the text into WordNodes, which will be added to the
     * provided DocumentNode under the correct page
     * <p/>
     * To do this, the text is split on whitespaces, character and word distances are
     * approximated, and words are created based on those
     *
     * @param root          Document to which add words
     * @param pageNum       Page number
     * @param textToBeAdded text which is to be added
     */
    public List<WordNode> createWords(final DocumentNode root, final int pageNum, final List<ETextPosition> textToBeAdded) {
        long t0 = System.currentTimeMillis();

        List<WordNode> ret = new ArrayList<WordNode>(textToBeAdded.size());

        /* iterate through all incoming TextPositions, and process them
           in a line by line fashion. We do this to be able to calculate
           char and word distances for each line
        */

        List<TextPosition> line = new ArrayList<TextPosition>();

        Collections.sort(textToBeAdded, new TextPositionComparator());

        float lastY = Float.MAX_VALUE;
        float lastEndY = Float.MIN_VALUE;
        for (ETextPosition textPosition : textToBeAdded) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(StringUtils.getTextPositionString(textPosition));
            }

            /* If this not the first text on a line and also not on the same Y coordinate
                as the existing, complete this line */

            /**
             * Decide whether or not this textPosition is part of the current
             *  line we are building or not
             */
            boolean endLine = false;

            if (lastY == Float.MAX_VALUE) {
                /* if this is the first text in a line */
                lastY = textPosition.getY();
                lastEndY = lastY + textPosition.getHeightDir();
            } else if (lastY != textPosition.getYDirAdj()) {
                //            } else if (!isOnSameLine(lastY, lastEndY, textPosition.getYDirAdj(), textPosition.getYDirAdj() + textPosition.getHeightDir())) {
                /* end the current line if this element is not considered to be part of it */
                endLine = true;
            }

            if (endLine) {
                processLine(root, ret, pageNum, line);
                line.clear();
                lastY = Float.MAX_VALUE;
            }

            line.add(textPosition);
            lastY = Math.min(textPosition.getYDirAdj(), lastY);
            lastEndY = Math.max(lastY + textPosition.getHeightDir(), lastEndY);
        }

        if (!line.isEmpty()) {
            processLine(root, ret, pageNum, line);
            line.clear();
        }

        /* if the page contained no text, create an empty page node and return */
        try {
            root.getPageNumber(pageNum);
        } catch (RuntimeException e) {
            /* this means the page does not exist - create it here */
            root.addChild(new PageNode(pageNum));
        }

        LOG.debug("PhysicalWordSegmentator.createWords took " + (System.currentTimeMillis() - t0) + " ms");
        return ret;
    }

// -------------------------- INNER CLASSES --------------------------

    /**
     * This class is used while combining Text objects into Words, as a simple way of
     * grouping all state together.
     */
    private static class WordState {
        private final char[] chars = new char[512];
        private int len;
        private int maxHeight;
        private int width;
        private int x;
        private int y;
        private Style currentStyle;
        public int charSpacing;

        public WordNode createWord(final int pageNum) {
            String wordText = new String(chars, 0, len);
            //TODO
            final WordNode word = new WordNode(new Rectangle(x, y - maxHeight, width, maxHeight), pageNum, currentStyle, wordText, charSpacing);

            if (LOG.isDebugEnabled()) {
                LOG.debug("out: " + word);
            }

            /* then reset state for next word */
            len = 0;
            x += width;
            maxHeight = 0;
            width = 0;

            return word;
        }

        public boolean isTooFarAway(final Text text) {
            if (text.distanceToPreceeding < 0) return false;

            if (text.distanceToPreceeding > text.charSpacing) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": " + text + " is too far away");
                }
                return true;
            }

            return false;
        }

        public boolean isDifferentStyle(final Style newStyle) {
            return !currentStyle.equals(newStyle);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("WordState");
            sb.append("{len=").append(len);
            sb.append(", maxHeight=").append(maxHeight);
            sb.append(", width=").append(width);
            sb.append(", x=").append(x);
            sb.append(", y=").append(y);
            sb.append(", currentStyle=").append(currentStyle);
            sb.append('}');
            return sb.toString();
        }
    }
}
