package org.elacin.extract.tree;

import org.elacin.extract.Loggers;

import java.util.Comparator;

import static org.elacin.extract.builder.TextNodeBuilder.withinNum;


/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 8, 2010
 * Time: 8:56:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParagraphNode extends AbstractParentNode<LineNode, PageNode> {
    // --------------------------- CONSTRUCTORS ---------------------------

    public ParagraphNode() {
    }

    // -------------------------- PUBLIC METHODS --------------------------

    @Override
    public boolean addTextNode(final TextNode node) {
        if (getChildren().isEmpty()) {
            if (Loggers.getCreateTreeLog().isDebugEnabled()) {
                Loggers.getCreateTreeLog().debug(toString() + ": Adding a new LineNode for node " + node);
            }
            addChild(new LineNode(node));
            return true;
        }

        /* try the lines already added and see if this fits at the end of one of them */
        for (LineNode line : getChildren()) {
            if (line.addTextNode(node)) {
                return true;
            }
        }

        if (isNewLineInThisParagraph(node)) {
            if (Loggers.getCreateTreeLog().isDebugEnabled()) {
                Loggers.getCreateTreeLog().debug(toString() + ": Node " + node + " is considered to be a new line in this paragraph");
            }
            addChild(new LineNode(node));
            return true;
        }


        return false;
    }

    @Override
    public void combineChildren() {

        /* combine all contained line nodes which are clearly on the same line.
         *  using array to avoid concurrent modification of iterator */
        LineNode[] lineNodes = getChildren().toArray(new LineNode[getChildren().size()]);

        for (int i = 0; i < lineNodes.length; i++) {
            if (lineNodes[i] == null) {
                continue;
            }

            LineNode firstLine = lineNodes[i];

            for (int j = 0; j < lineNodes.length; j++) {
                if (i == j) continue;

                if (lineNodes[j] == null) {
                    continue;
                }
                LineNode secondLine = lineNodes[j];

                if (firstLine.isOnSameLine(secondLine)) {
                    if (Loggers.getCreateTreeLog().isDebugEnabled()) {
                        Loggers.getCreateTreeLog().debug("combining lines " + firstLine + " and " + secondLine);
                    }
                    firstLine.combineWith(secondLine);

                    /* remove secondLine */
                    lineNodes[j] = null;
                    getChildren().remove(secondLine);
                }
            }
        }

        /* call one level further down the tree */
        for (LineNode lineNode : getChildren()) {
            lineNode.combineChildren();
        }
    }

    /**
     * Adds all lines from otherParagraph to this paragraph. No checks here!
     *
     * @param otherParagraph
     */
    public void combineWith(final ParagraphNode otherParagraph) {
        /* add the lines which we could not combine with the existing lines in the paragraph */
        for (LineNode lineNode : otherParagraph.getChildren()) {
            addChild(lineNode);
        }
    }

    /**
     * Returns a Comparator which compares coordinates within a page
     *
     * @return
     */
    @Override
    public Comparator<LineNode> getChildComparator() {
        return new StandardNodeComparator();
    }

    public boolean isNewLineInThisParagraph(final AbstractNode node) {

        LineNode lastLineNode = getChildren().get(getChildren().size() - 1);

        /* if the last line still has no text, accept this */
        if (lastLineNode.getChildren().isEmpty()) {
            return true;
        }

        /* Use the size of the preceding and new text to determine if the two lines are
             logically part of the same paragraph. Use the smaller of the two values.
         */
        double ySize = Math.min(lastLineNode.getCurrentStyle().ySize, node.getStyle().ySize);

        if (withinNum(ySize, getPosition().getY() + getPosition().getHeight(), node.getPosition().getY())) {

            double widthOfSpace = lastLineNode.getCurrentStyle().widthOfSpace;

            /* if the new node starts at more or less the same X position, accept it */
            if (withinNum(widthOfSpace, getPosition().getX(), node.getPosition().getX())) {
                return true;
            }

            /* also accept it if its X value is contained between current position's lower X and (higher X + widthOfSpace)*/
            if (getPosition().getX() < node.getPosition().getX() &&
                    getPosition().getX() + getPosition().getWidth() + widthOfSpace > node.getPosition().getX()) {
                return true;
            }
        }
        return false;
    }

}
