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

package org.elacin.pdfextract.builder;

import org.apache.log4j.Logger;
import org.apache.pdfbox.util.TextPosition;
import org.apache.pdfbox.util.TextPositionComparator;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Point;
import org.elacin.pdfextract.util.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elacin.pdfextract.util.MathUtils.round;

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
 * By using fillPage() all this will be done, and the words will be added to the
 * the provided document tree.
 */
public class WordBuilder {
    // ------------------------------ FIELDS ------------------------------

    private static final Logger log = Loggers.getWordBuilderLog();

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
    public void fillPage(DocumentNode root, final int pageNum, List<ETextPosition> textToBeAdded) {
        long t0 = System.currentTimeMillis();

        /* iterate through all incoming TextPositions, and process them
            in a line by line fashion. We do this to be able to calculate
            char and word distances for each line
         */
        float lastY = Float.MAX_VALUE;
        float lastEndY = Float.MIN_VALUE;

        List<TextPosition> line = new ArrayList<TextPosition>();

        Collections.sort(textToBeAdded, new TextPositionComparator());

        for (ETextPosition textPosition : textToBeAdded) {
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
                processLine(root, pageNum, line);
                line.clear();
                lastY = Float.MAX_VALUE;
            }

            line.add(textPosition);
            lastY = Math.min(textPosition.getYDirAdj(), lastY);
            lastEndY = Math.max(lastY + textPosition.getHeightDir(), lastEndY);
        }

        if (!line.isEmpty()) {
            processLine(root, pageNum, line);
            line.clear();
        }

        log.debug("WordBuilder.fillPage took " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * This method will process one line worth of TextPositions , and split and/or
     * combine them as to output words. The words are added directly to the tree
     *
     * @param root
     * @param pageNum
     * @param line
     */
    void processLine(final DocumentNode root, final int pageNum, final List<TextPosition> line) {
        /* first convert into text elements */
        final List<Text> lineTexts = getTextsFromTextPositions(root.getStyles(), line);

        /* then calculate spacing */
        setCharSpacingForTexts(lineTexts);

        /* this will be used to keep all the state while combining text fragments into words */
        WordState currentState = new WordState();

        /* create WordNodes and add them to the tree */
        for (Text newText : lineTexts) {
            if (Loggers.getWordBuilderLog().isDebugEnabled()) {
                Loggers.getWordBuilderLog().debug("in : " + newText);
            }

            /* if this is the first text element going into a word */
            if (currentState.len == 0) {
                currentState.x = newText.x;
                currentState.y = newText.y;
                currentState.charSpacing = newText.charSpacing;
            } else {
                /* if not, check if this new text means we should finish the current word */
                if (currentState.isTooFarAway(newText) || currentState.isDifferentStyle(newText.style)) {
                    root.addWord(currentState.createWord(pageNum));
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
            root.addWord(currentState.createWord(pageNum));
        }
    }

    /**
     * This creates
     *
     * @param sf
     * @param textPositions
     * @return
     */
    List<Text> getTextsFromTextPositions(StyleFactory sf, List<TextPosition> textPositions) {
        List<Text> ret = new ArrayList<Text>(textPositions.size() * 2);

        Point lastWordBoundary = new Point(0, 0);
        StringBuilder contents = new StringBuilder();

        float x, y, width = 0;
        boolean firstInLine = true;

        Collections.sort(textPositions, new TextPositionComparator());

        for (TextPosition textPosition : textPositions) {
            x = textPosition.getXDirAdj();
            y = textPosition.getYDirAdj();
            final Style style = sf.getStyleForTextPosition(textPosition);

            for (int j = 0; j < textPosition.getCharacter().length(); j++) {
                /* if we found a space */
                if (Character.isWhitespace(textPosition.getCharacter().charAt(j))) {
                    if (contents.length() != 0) {
                        /* else just output a new text */

                        final float distance;
                        if (firstInLine) {
                            distance = Float.MIN_VALUE;
                            firstInLine = false;
                        } else {
                            distance = x - lastWordBoundary.getX();
                        }

                        ret.add(new Text(distance, textPosition.getHeightDir(), style, contents.toString(), width, x, y));
                        contents.setLength(0);
                        x += width;
                        width = 0;
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
                    if (width == 0) {
                        width = textPosition.getWidthDirAdj();
                    }

                    ret.add(new Text(distance, textPosition.getHeightDir(), style, contents.toString(), width, x, y));
                    contents.setLength(0);
                    x += width;
                    width = 0;
                    lastWordBoundary.setPosition(x, y);
                }
            }
        }

        return ret;
    }

    /**
     * @param texts
     */
    void setCharSpacingForTexts(List<Text> texts) {
        if (texts.isEmpty()) return;

        /* Start by making a list of all distances, and an average*/
        float sum = 0f;
        List<Integer> distances = new ArrayList<Integer>(texts.size() - 1);

        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            /* skip the first word fragment, and only include this distance if it is not too big */
            if (i != 0 && text.distanceToPreceeding < text.style.xSize * 6) {
                sum += text.distanceToPreceeding;
                distances.add(text.distanceToPreceeding);
            }
        }
        float average = sum / distances.size();

        /* spit out some debug information */
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < texts.size(); i++) {
                Text text = texts.get(i);
                if (i != 0) {
                    sb.append(">").append(text.distanceToPreceeding).append(">");
                }
                sb.append(text.content);
            }
            log.debug("spacing: -----------------");
            log.debug("spacing: content: " + sb);
            log.debug("spacing: unsorted: " + distances);
        }

        /**
         * Then, start to figure out what the most probable char space is.
         *
         * There are two special cases:
         *  - only one text fragment, in which case we set a spacing of 0
         *  - all distances are the same, in which case we assume it is one continuous word, and
         *      not a list of single text fragments
         *
         *
         * //TODO: describe algorithm better here :)
         *
         */
        int charSpacing = 0;
        Collections.sort(distances);

        if (distances.isEmpty()) {
            charSpacing = 0;
        } else if (distances.get(0) == (int) average) {
            /* if all distances are the same, set that as character width */
            charSpacing = (int) average;
            log.debug("spacing: all distances equal, setting as character space");
        } else {
            int minDistance = distances.get(0);

            /* iterate backwards - do this because char spacing seems to vary a lot more than does word spacing */
            int biggestScore = Integer.MIN_VALUE;
            int lastDistance = distances.get(distances.size() - 1);

            for (int i = distances.size() - 1; i >= 0; i--) {
                int distance = distances.get(i);

                if (distance < average) {
                    int score = (int) ((lastDistance - distance + minDistance) / (Math.max(1, distance + minDistance)) + distance * 0.5);

                    if (score > biggestScore) {
                        biggestScore = score;
                        charSpacing = distance;
                        log.debug("spacing: " + charSpacing + " is now the most probable. score: " + score);
                    }
                }
                lastDistance = distance;
            }
        }

        if (charSpacing < 0) {
            log.debug("spacing: got suspiciously low charSpacing " + charSpacing + " setting to 3");
            charSpacing = 3;
        }

        /* correct for rounding */
        charSpacing += 1;

        /* and set the values in all texts */
        for (Text text : texts) {
            text.charSpacing = charSpacing;
        }

        if (log.isDebugEnabled()) {
            log.debug("spacing: sorted: " + distances);
            log.debug("spacing: average=" + average + ", charSpacing=" + charSpacing);
        }
    }

    // -------------------------- INNER CLASSES --------------------------

    private static class Text {
        final int x, y, width, height, distanceToPreceeding;
        int charSpacing;
        String content;
        Style style;

        Text(final float distanceToPreceeding, final float height, final Style style, final String content, final float width, final float x, final float y) {
            this.distanceToPreceeding = round(distanceToPreceeding);
            this.height = round(height);
            this.width = round(width);
            this.x = round(x);
            this.y = round(y);
            this.style = style;
            this.content = content;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Text");
            sb.append("{d=").append(distanceToPreceeding);
            sb.append(", x=").append(x);
            sb.append(", endX=").append(x + width);
            sb.append(", y=").append(y);
            sb.append(", width=").append(width);
            sb.append(", height=").append(height);
            sb.append(", text='").append(content).append('\'');
            sb.append(", style=").append(style);
            sb.append(", charSpacing=").append(charSpacing);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * This class is used while combining Text objects into Words, as a simple way of
     * grouping all state together.
     */
    private class WordState {
        private final char[] chars = new char[512];
        private int len = 0;
        private int maxHeight = 0;
        private int width = 0;
        private int x = 0;
        private int y = 0;
        private Style currentStyle = null;
        public int charSpacing;

        public WordNode createWord(final int pageNum) {
            String wordText = new String(chars, 0, len);
            final WordNode word = new WordNode(new Rectangle(x, y, width, maxHeight), pageNum, currentStyle, wordText, charSpacing);

            if (log.isDebugEnabled()) {
                log.debug("out: " + word);
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
                if (log.isDebugEnabled()) {
                    log.debug(this + ": " + text + " is too far away");
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
