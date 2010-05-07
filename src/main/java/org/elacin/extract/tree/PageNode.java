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

import java.awt.print.PageFormat;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 23, 2010
 * Time: 9:33:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageNode extends AbstractParentNode<ParagraphNode, DocumentNode> {
    // ------------------------------ FIELDS ------------------------------

    private final PageFormat pageFormat;
    private int pageNumber;

    // --------------------------- CONSTRUCTORS ---------------------------

    public PageNode(final PageFormat pageFormat, int pageNumber) {
        this.pageNumber = pageNumber;
        this.pageFormat = pageFormat;
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public PageFormat getPageFormat() {
        return pageFormat;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    // -------------------------- PUBLIC METHODS --------------------------

    @Override
    public boolean addTextNode(final TextNode node) {
        Loggers.getCreateTreeLog().debug("Adding new TextNode: " + node);
        getParent().textNodes.add(node);

        for (ParagraphNode paragraph : getChildren()) {
            if (paragraph.addTextNode(node)) return true;
        }

        /* since we couldnt add this node to an existing paragraph, go create a new one :) */
        final ParagraphNode paragraph = new ParagraphNode();
        paragraph.addTextNode(node);
        addChild(paragraph);

        return true;
    }

    /**
     * Returns a Comparator which compares coordinates within a page
     *
     * @return
     */
    @Override
    public Comparator<ParagraphNode> getChildComparator() {
        return new StandardNodeComparator();
    }

    @Override
    public void combineChildren() {
        final ParagraphNode[] paragraphNodes = getChildren().toArray(new ParagraphNode[getChildren().size()]);
        for (int i = 0; i < paragraphNodes.length; i++) {
            if (paragraphNodes[i] == null) {
                continue;
            }
            ParagraphNode firstParagraph = paragraphNodes[i];
            for (int j = 0; j < paragraphNodes.length; j++) {
                if (i == j) continue;

                if (paragraphNodes[j] == null) {
                    continue;
                }

                ParagraphNode secondParagraph = paragraphNodes[j];

                if (firstParagraph.overlapsWith(secondParagraph)) {
                    Loggers.getCreateTreeLog().info("combining paragraphs " + firstParagraph + " and " + secondParagraph);
                    firstParagraph.combineWith(secondParagraph);

                    /* remove secondParagraph */
                    paragraphNodes[j] = null;
                    getChildren().remove(secondParagraph);
                }
            }
        }

        /* do further combining lower in the tree */
        for (ParagraphNode paragraphNode : getChildren()) {
            paragraphNode.combineChildren();
        }
    }
}
