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
import org.elacin.pdfextract.tree.TextNode;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 12, 2010
 * Time: 3:34:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class NewTextNodeBuilder {
    // ------------------------------ FIELDS ------------------------------

    private static final Logger log = Loggers.getTextNodeBuilderLog();
    private final DocumentNode root;
    private int pageNum;

    // --------------------------- CONSTRUCTORS ---------------------------

    public NewTextNodeBuilder(final DocumentNode newRoot) {
        root = newRoot;
    }

    // -------------------------- PUBLIC STATIC METHODS --------------------------

    public static String getTextPositionString(final TextPosition position) {
        StringBuilder sb = new StringBuilder("pos{");
        sb.append("c=\"").append(position.getCharacter()).append("\"");
        //        sb.append(", X=").append(position.getX());
        //        sb.append(", XScale=").append(position.getXScale());
        //        sb.append(", Y=").append(position.getY());
        //        sb.append(", Dir=").append(position.getDir());
        sb.append(", XDirAdj=").append(position.getXDirAdj());
        sb.append(", YDirAdj=").append(position.getYDirAdj());
        //        sb.append(", XScale=").append(position.getXScale());
        //        sb.append(", YScale=").append(position.getYScale());
        //        sb.append(", Height=").append(position.getHeight());
        //        sb.append(", Width=").append(position.getWidth());
        sb.append(", endY=").append(position.getYDirAdj() + position.getHeightDir());
        sb.append(", endX=").append(position.getXDirAdj() + position.getWidthDirAdj());

        sb.append(", HeightDir=").append(position.getHeightDir());
        sb.append(", WidthDirAdj=").append(position.getWidthDirAdj());

        sb.append(", WidthOfSpace=").append(position.getWidthOfSpace());
        sb.append(", WordSpacing()=").append(position.getWordSpacing());
        sb.append(", FontSize=").append(position.getFontSize());
        //        sb.append(", FontSizeInPt=").append(position.getFontSizeInPt());
        sb.append(", getIndividualWidths=").append(Arrays.toString(position.getIndividualWidths()));
        sb.append(", font=").append(position.getFont().getBaseFont());

        sb.append("}");
        return sb.toString();
    }

    // -------------------------- PUBLIC METHODS --------------------------

    public void fillPage(final int pageNum, List<TextPosition> textPositions) {
        final List<Line> lines = divideInLines(textPositions);
        processLines(lines);
        this.pageNum = pageNum;
    }

    // -------------------------- OTHER METHODS --------------------------

    private List<Line> divideInLines(final List<TextPosition> textPositions) {
        Map<Float, Line> linesMap = new HashMap<Float, Line>();

        for (TextPosition textPosition : textPositions) {
            if (linesMap.get(textPosition.getYDirAdj()) == null) {
                linesMap.put(textPosition.getYDirAdj(), new Line());
            }

            linesMap.get(textPosition.getYDirAdj()).textPositions.add(textPosition);
            Collections.sort(linesMap.get(textPosition.getYDirAdj()).textPositions, new TextPositionComparator());
        }
        List<Line> lines = new ArrayList<Line>();
        lines.addAll(linesMap.values());
        Collections.sort(lines);

        for (Line line : lines) {
            line.findWordSpacing();
        }


        return lines;
    }

    /**
     * This method will process all the TextPositions contained within each Line provided , and split and/or
     * combine them as to output words. The words are added directly to the tree
     *
     * @param lines
     */
    void processLines(List<Line> lines) {
        /* this will be used to keep all the state while parsing the text for words */
        WordState state = new WordState();

        for (Line line : lines) {
            state.y = line.getY();

            for (Text text : line.texts) {
                if (Loggers.getTextNodeBuilderLog().isDebugEnabled()) {
                    Loggers.getTextNodeBuilderLog().debug("in : " + text);
                }

                /* if this is the first text element going into a word */
                if (state.len == 0) {
                    state.x = text.x;
                } else {
                    /* if not, check if this new text means we should finish of the current word */
                    if (state.isTooFarAway(line, text) || state.isDifferentStyle(text.style)) {
                        state.createWord(line.wordSpacing, line.charSpacing);
                        state.x = text.x;
                    }
                }

                state.currentStyle = text.style;
                state.maxHeight = Math.max(state.maxHeight, text.height);

                /* then copy text from the Text object */
                for (int textPositionIdx = 0; textPositionIdx < text.content.length(); textPositionIdx++) {
                    state.chars[state.len] = text.content.charAt(textPositionIdx);
                    state.len++;
                }
                state.width += text.width;
            }
            /* no words can span lines, so yield whatever is read */
            if (state.len != 0) {
                state.createWord(line.wordSpacing, line.charSpacing);
            }
        }
    }

    // -------------------------- INNER CLASSES --------------------------

    private static class Text {
        float x, y, width, height, distanceToPreceeding;
        String content;
        Style style;
        public float wordSpacing;
        public float charSpacing;

        Text(final float distanceToPreceeding, final float height, final Style style, final String content, final float width, final float x, final float y) {
            this.distanceToPreceeding = distanceToPreceeding;
            this.height = height;
            this.style = style;
            this.content = content;
            this.width = width;
            this.x = x;
            this.y = y;
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

    class Line implements Comparable<Line> {
        final List<Text> texts = new ArrayList<Text>();
        final List<TextPosition> textPositions = new ArrayList<TextPosition>();
        float wordSpacing, charSpacing;

        Line() {
        }

        public int compareTo(final Line o) {
            return Float.compare(getY(), o.getY());
        }

        public float getY() {
            return textPositions.get(0).getYDirAdj();
        }

        public void createTextsFromTextPositions() {
            Collections.sort(textPositions, new TextPositionComparator());

            Point2D boundary = null;
            float distance;

            StringBuilder contents = new StringBuilder();
            for (TextPosition textPosition : textPositions) {
                float x = textPosition.getXDirAdj(), textWidth = 0, spaceWidth = 0;
                boolean wasWhitespace = false;
                final Style style = root.getStyles().getStyleForTextPosition(textPosition);


                for (int j = 0; j < textPosition.getCharacter().length(); j++) {
                    final char currentChar = textPosition.getCharacter().charAt(j);

                    if (Character.isWhitespace(currentChar)) {
                        if (!wasWhitespace) {
                            /* here stops current word */
                            if (contents.length() != 0) {
                                distance = boundary == null ? Float.MIN_VALUE : (float) (x - boundary.getX());
                                texts.add(new Text(distance, textPosition.getHeightDir(), style, contents.toString(), textWidth, x + spaceWidth,
                                        textPosition.getY()));
                                contents.setLength(0);
                                boundary = new Point2D.Float(x + textWidth, textPosition.getY());
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
                    distance = boundary == null ? Float.MIN_VALUE : (float) (x - boundary.getX());
                    texts.add(new Text(distance, textPosition.getHeightDir(), style, contents.toString(), textWidth, x, textPosition.getY()));
                    contents.setLength(0);
                }
                boundary = new Point2D.Float(x + textWidth, textPosition.getY());
            }
        }

        public void findWordSpacing() {
            createTextsFromTextPositions();
            List<Float> distances = new ArrayList<Float>();

            if (texts.isEmpty()) return;


            /* skip the first, as its set to negative infinity :) */
            float sum = 0f;
            for (int i = 1; i < texts.size(); i++) {
                Text text = texts.get(i);
                if (text.distanceToPreceeding <= text.style.xSize) {
                    sum += text.distanceToPreceeding;
                    distances.add(text.distanceToPreceeding);
                }
            }

            /* this algorithm wont work with very few elements. if that is the case
                try what PDFBox guessed
             */
            if (distances.isEmpty()) {
                wordSpacing = texts.get(0).style.wordSpacing;
                charSpacing = (wordSpacing * 0.6f);
                return;
            } else if (distances.size() < 4) {
                wordSpacing = distances.get(distances.size() - 1);
                charSpacing = (wordSpacing * 0.6f);
                return;
            }

            float average = sum / distances.size();
            Collections.sort(distances);

            /* iterate backwards - do this because char spacing seems to vary a lot more than
                does word spacing
            */
            wordSpacing = distances.get(distances.size() - 1);

            boolean foundCharSpacing = false;

            for (int i = distances.size() - 2; i >= 0; i--) {
                float distance = distances.get(i);
                if (distance < 0.1 * wordSpacing) {
                    charSpacing = distance;
                    foundCharSpacing = true;
                    break;
                }
            }

            float former;
            if (!foundCharSpacing) {
                former = distances.get(distances.size() - 1);
                for (int i = distances.size() - 2; i >= 0; i--) {
                    float distance = distances.get(i);

                    if (distance < former * 0.90 && distance < average) {
                        charSpacing = distance;
                        break;
                    }
                    former = distance;
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
    }

    private class WordState {
        private final char[] chars = new char[512];
        private int len = 0;
        private float maxHeight = 0;
        private float width = 0;
        private float x = 0;
        private float y = 0;
        private Style currentStyle = null;

        public void createWord(final float wordSpacing, final float charSpacing) {
            String wordText = new String(chars, 0, len);
            final TextNode node = new TextNode(new Rectangle2D.Float(x, y, width, maxHeight), pageNum, currentStyle, wordText, wordSpacing, charSpacing);

            if (log.isDebugEnabled()) {
                log.debug("out: " + node);
            }

            root.addTextNode(node);

            /* then reset state for next word */
            len = 0;
            x += width;
            maxHeight = 0.0f;
            width = 0.0f;
        }

        public boolean isTooFarAway(final Line line, final Text text) {
            if (text.distanceToPreceeding < 0) return false;

            //            if (x + width + text.distanceToPreceeding < text.x + width)
            //                return false;

            if (text.distanceToPreceeding > line.charSpacing) {
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
