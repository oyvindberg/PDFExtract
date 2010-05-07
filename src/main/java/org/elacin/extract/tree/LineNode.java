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

package org.elacin.extract.tree;

import org.elacin.extract.Loggers;
import org.elacin.extract.text.Style;

import java.util.Comparator;

import static org.elacin.extract.builder.TextNodeBuilder.withinNum;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 8, 2010
 * Time: 8:29:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class LineNode extends AbstractParentNode<TextNode, ParagraphNode> {
    // --------------------------- CONSTRUCTORS ---------------------------

    public LineNode(final TextNode child) {
        super(child);
    }

    // ------------------------ OVERRIDING METHODS ------------------------

    @Override
    protected void appendLocalInfo(final StringBuilder sb, final int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append(getClass().getSimpleName()).append(": ").append(getText()).append("\n");
    }

    // -------------------------- PUBLIC METHODS --------------------------

    /**
     * Decides whether or not node node belongs to this line
     *
     * @param node
     * @return
     */
    public boolean accepts(final AbstractNode node) {

        /* if this line is empty so far, accept anything*/
        if (getChildren().isEmpty()) {
            if (Loggers.getCreateTreeLog().isTraceEnabled()) {
                Loggers.getCreateTreeLog().trace(toString() + ": accepts() " + node + "true: line had noe children");
            }
            return true;
        }

        /* assure the node is located vertically on the current line */
        if (!isOnSameLine(node)) {
            if (Loggers.getCreateTreeLog().isTraceEnabled()) {
                Loggers.getCreateTreeLog().trace(toString() + ": accepts() " + node + "false: was not considered to be on same line");
            }
            return false;
        }


        /* find the last used style, we want its widthOfSpace in order to decide */
        final Style currentStyle = getCurrentStyle();

        final boolean ret = withinNum(currentStyle.widthOfSpace * 2, getPosition().getX() + getPosition().getWidth() + currentStyle.widthOfSpace,
                node.getPosition().getX());

        if (Loggers.getCreateTreeLog().isTraceEnabled()) {
            Loggers.getCreateTreeLog().trace(toString() + ": accepts() " + node + ret + ": after withinNum");
        }
        return ret;
    }

    @Override
    public boolean addTextNode(final TextNode node) {
        /* check that node belongs to this line, return if it doesn't */
        if (!accepts(node)) {
            return false;
        }
        addChild(node);
        return true;
    }

    @Override
    public void combineChildren() {
        //do nothing for now:
    }

    /**
     * adds all the text from otherLine to this line. no checking!
     *
     * @param otherLine
     */
    public void combineWith(final LineNode otherLine) {
        for (TextNode textNode : otherLine.getChildren()) {
            addChild(textNode);
        }
    }

    /**
     * Returns a Comparator which compares only X coordinates
     *
     * @return
     */
    @Override
    public Comparator<TextNode> getChildComparator() {
        return new Comparator<TextNode>() {
            public int compare(final TextNode o1, final TextNode o2) {
                if (o1.getPosition().getX() < o2.getPosition().getX()) return -1;
                else if (o1.getPosition().getX() > o2.getPosition().getX()) return 1;

                return 0;
            }
        };
    }

    public Style getCurrentStyle() {
        return getChildren().get(getChildren().size() - 1).getStyle();
    }

    /**
     * checks if the the average height of node falls within current line
     *
     * @param node
     * @return
     */
    public boolean isOnSameLine(final AbstractNode node) {
        double otherMiddleY = node.getPosition().getY() + (node.getPosition().getHeight() / 2);
        return getPosition().getY() <= otherMiddleY && (getPosition().getY() + getPosition().getHeight()) >= otherMiddleY;
    }

    @Override
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getChildren().size(); i++) {
            final TextNode textNode = getChildren().get(i);
            sb.append(textNode.getText().trim());

            /* if this node is not the last on this line, add
                space /if there were a space in the original document/ */
            if (i != getChildren().size() - 1) {
                if (textNode.isSpaceBetweenThisAnd(getChildren().get(i + 1))) {
                    sb.append(" ");
                }
            }
        }

        return sb.toString();
    }
}
