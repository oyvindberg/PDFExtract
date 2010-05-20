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
 * To change this template use File | Settings | File Templates.
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
    public void fillPage(DocumentNode root, final int pageNum, List<TextPosition> textToBeAdded) {
        long t0 = System.currentTimeMillis();

        /* iterate through all incoming TextPositions, and process them
            in a line by line fashion. We do this to be able to calculate
            char and word distances for each line
         */
        float lastY = Float.MIN_VALUE;
        List<TextPosition> line = new ArrayList<TextPosition>();

        for (TextPosition textPosition : textToBeAdded) {
            /* If this not the first text on a line and also not on the same Y coordinate
                as the existing, complete this line */
            if (lastY != Float.MIN_VALUE && (lastY != textPosition.getYDirAdj())) {
                processLine(root, pageNum, line);
                line.clear();
            }

            line.add(textPosition);
            lastY = textPosition.getYDirAdj();
        }

        if (!line.isEmpty()) {
            processLine(root, pageNum, line);
            line.clear();
        }

        System.out.println("WordBuilder.fillPage took " + (System.currentTimeMillis() - t0) + " ms");
    }

    // -------------------------- OTHER METHODS --------------------------

    List<Text> createTextObjects(StyleFactory sf, List<TextPosition> textPositions) {
        List<Text> ret = new ArrayList<Text>(textPositions.size() * 2);
        Collections.sort(textPositions, new TextPositionComparator());

        Point boundary = new Point(0, 0);
        float distance;

        StringBuilder contents = new StringBuilder();
        for (TextPosition textPosition : textPositions) {
            float x = textPosition.getXDirAdj(), textWidth = 0, spaceWidth = 0;
            boolean wasWhitespace = false;
            final Style style = sf.getStyleForTextPosition(textPosition);


            for (int j = 0; j < textPosition.getCharacter().length(); j++) {
                final char currentChar = textPosition.getCharacter().charAt(j);

                if (Character.isWhitespace(currentChar)) {
                    if (!wasWhitespace) {
                        /* here stops current word */
                        if (contents.length() != 0) {
                            distance = x - boundary.getX();
                            //                            distance = boundary == null ? Float.MIN_VALUE : x - boundary.getX();
                            ret.add(new Text(distance, textPosition.getHeightDir(), style, contents.toString(), textWidth, x + spaceWidth,
                                    textPosition.getY()));
                            contents.setLength(0);
                            boundary.setPosition(x + textWidth, textPosition.getY());
                            x += textWidth;
                            textWidth = 0f;
                        }
                        wasWhitespace = true;
                        spaceWidth += textPosition.getIndividualWidths()[j];
                    }
                } else { /* if we now found text */
                    if (wasWhitespace) {
                        x += spaceWidth;
                        spaceWidth = 0;
                        wasWhitespace = false;
                    }

                    contents.append(currentChar);
                    textWidth += textPosition.getIndividualWidths()[j];
                }
            }

            /* finally, make a text of what remains */
            if (contents.length() != 0) {
                distance = x - boundary.getX();
                //                distance = boundary == null ? Float.MIN_VALUE : x - boundary.getX();
                ret.add(new Text(distance, textPosition.getHeightDir(), style, contents.toString(), textWidth, x, textPosition.getY()));
                contents.setLength(0);
            }
            boundary.setPosition(x + textWidth, textPosition.getY());
        }
        return ret;
    }

    /**
     * This method will process one line worth of TextPositions , and split and/or
     * combine them as to output words. The words are added directly to the tree
     *
     * @param root
     * @param pageNum
     * @param textPositions
     */
    void processLine(final DocumentNode root, final int pageNum, final List<TextPosition> textPositions) {
        /* first convert into text elements */
        final List<Text> texts = createTextObjects(root.getStyles(), textPositions);

        /* then calculate spacing */
        setSpacingForTexts(texts);

        /* this will be used to keep all the state while parsing the text for words */
        WordState currentState = new WordState();

        /* Then iterate through all the Text objects, and create words */
        for (Text newText : texts) {
            if (Loggers.getWordBuilderLog().isDebugEnabled()) {
                Loggers.getWordBuilderLog().debug("in : " + newText);
            }

            /* if this is the first text element going into a word */
            if (currentState.len == 0) {
                currentState.x = newText.x;
                currentState.y = newText.y;
                currentState.wordSpacing = newText.wordSpacing;
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

            /* then copy text from the Text object */
            for (int textPositionIdx = 0; textPositionIdx < newText.content.length(); textPositionIdx++) {
                currentState.chars[currentState.len] = newText.content.charAt(textPositionIdx);
                currentState.len++;
            }
            currentState.width += newText.width;
        }

        /* no words can span lines, so yield whatever is read */
        if (currentState.len != 0) {
            root.addWord(currentState.createWord(pageNum));
        }
    }

    void setSpacingForTexts(List<Text> texts) {
        if (texts.isEmpty()) return;

        List<Integer> distances = new ArrayList<Integer>(texts.size() - 1);

        /* skip the first, as its set to negative infinity :) */
        float sum = 0f;
        for (int i = 1; i < texts.size(); i++) {
            Text text = texts.get(i);
            if (text.distanceToPreceeding <= 2 * text.style.xSize) {
                sum += text.distanceToPreceeding;
                distances.add(text.distanceToPreceeding);
            }
        }
        float average = sum / distances.size();

        int wordSpacing;
        int charSpacing = 0;

        /* this algorithm wont work with very few elements. if that is the case
                  try what PDFBox guessed
        */

        if (distances.isEmpty()) {
            wordSpacing = round(texts.get(0).style.wordSpacing);
            charSpacing = (int) (wordSpacing * 0.6);
            return;
        } else if (distances.size() < 4) {
            wordSpacing = distances.get(distances.size() - 1);
            charSpacing = (int) (wordSpacing * 0.6);
            return;
        } else {

            Collections.sort(distances);

            /* iterate backwards - do this because char spacing seems to vary a lot more than
                does word spacing
            */
            wordSpacing = distances.get(distances.size() - 1);

            boolean foundCharSpacing = false;

            for (int i = distances.size() - 2; i >= 0; i--) {
                int distance = distances.get(i);
                if (distance < 0.1 * wordSpacing) {
                    charSpacing = distance;
                    foundCharSpacing = true;
                    break;
                }
            }

            int former;
            if (!foundCharSpacing) {
                former = distances.get(distances.size() - 1);
                for (int i = distances.size() - 2; i >= 0; i--) {
                    int distance = distances.get(i);

                    if (distance < former * 0.90 && distance < average) {
                        charSpacing = distance;
                        break;
                    }
                    former = distance;
                }
            }
        }

        for (Text text : texts) {
            text.wordSpacing = wordSpacing;
            text.charSpacing = charSpacing;
        }

        if (log.isTraceEnabled()) {
            log.trace("average=" + average + ", charSpacing=" + charSpacing + ", wordSpacing=" + wordSpacing);
            log.trace("sorted: " + distances);
        }
    }

    // -------------------------- INNER CLASSES --------------------------

    private static class Text {
        int x, y, width, height, distanceToPreceeding, wordSpacing, charSpacing;
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
            sb.append(", wordSpacing=").append(wordSpacing);
            sb.append('}');
            return sb.toString();
        }
    }

    private class WordState {
        private final char[] chars = new char[512];
        private int len = 0;
        private int maxHeight = 0;
        private int width = 0;
        private int x = 0;
        private int y = 0;
        private Style currentStyle = null;
        public int wordSpacing;
        public int charSpacing;

        public WordNode createWord(final int pageNum) {
            String wordText = new String(chars, 0, len);
            final WordNode word = new WordNode(new Rectangle(x, y, width, maxHeight), pageNum, currentStyle, wordText, wordSpacing, charSpacing);

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

            //            if (x + width + text.distanceToPreceeding < text.x + width)
            //                return false;

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
